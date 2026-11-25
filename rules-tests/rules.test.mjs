import { test, before, beforeEach, after, describe } from "node:test";
import { readFileSync } from "node:fs";
import { initializeTestEnvironment, assertSucceeds, assertFails } from "@firebase/rules-unit-testing";
import {
  doc, getDoc, setDoc, updateDoc, deleteDoc, addDoc, collection, query, where, orderBy, limit, getDocs,
  serverTimestamp, deleteField, Timestamp
} from "firebase/firestore";
import { ref, uploadBytes, getMetadata, deleteObject } from "firebase/storage";

const T = "trainer", K = "client", T2 = "otherTrainer", K2 = "otherClient", X = "stranger";
const past = Timestamp.fromDate(new Date("2026-11-01T10:00:00Z"));
let env;

const db = (uid) => env.authenticatedContext(uid).firestore();
const storage = (uid) => env.authenticatedContext(uid).storage();
const report = (fields) => ({
  clientId: K, title: "Spacer", description: null, authorId: T, authorRole: "TRAINER", lastEditedBy: T,
  createdAt: past, updatedAt: past, media: [], ...fields
});
const newReport = (uid, role, fields = {}) => ({
  clientId: K, title: "Nowy", description: null, authorId: uid, authorRole: role, lastEditedBy: uid,
  createdAt: serverTimestamp(), updatedAt: serverTimestamp(), media: [], ...fields
});
const comment = (uid, role) => ({ authorId: uid, authorRole: role, text: "Super", updatedAt: serverTimestamp() });

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-rules",
    firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8") },
    storage: { rules: readFileSync(new URL("../storage.rules", import.meta.url), "utf8") }
  });
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const f = ctx.firestore();
    await setDoc(doc(f, "users", T), { role: "TRAINER", name: "Trener", email: "t@x.pl" });
    await setDoc(doc(f, "users", T2), { role: "TRAINER", name: "Inny trener", email: "t2@x.pl" });
    await setDoc(doc(f, "users", K), { role: "CLIENT", name: "Klient", email: "k@x.pl", trainerId: T, dogs: [] });
    await setDoc(doc(f, "users", K2), { role: "CLIENT", name: "Obcy klient", email: "k2@x.pl", trainerId: T2, dogs: [] });
    await setDoc(doc(f, "reports", "byTrainer"), report({}));
    await setDoc(doc(f, "reports", "byClientWithComment"), report({
      authorId: K, authorRole: "CLIENT", lastEditedBy: K,
      comment: { authorId: T, authorRole: "TRAINER", text: "Brawo", updatedAt: past }
    }));
    await setDoc(doc(f, "invitations", "inv"), { trainerId: T, name: "Ewa", email: "e@x.pl", code: "ABCD2345", expiresAt: past });
    await setDoc(doc(f, "mail", "m"), { to: "e@x.pl" });
    await uploadBytes(ref(ctx.storage(), `clients/${K}/media/f1`), new Uint8Array([1, 2, 3]));
  });
});

after(async () => { await env.cleanup(); });

describe("users", () => {
  test("czyta swój dokument", () => assertSucceeds(getDoc(doc(db(T), "users", T))));
  test("trener czyta swojego klienta", () => assertSucceeds(getDoc(doc(db(T), "users", K))));
  test("klient czyta swojego trenera", () => assertSucceeds(getDoc(doc(db(K), "users", T))));
  test("obcy nie czyta klienta", () => assertFails(getDoc(doc(db(X), "users", K))));
  test("inny trener nie czyta cudzego klienta", () => assertFails(getDoc(doc(db(T2), "users", K))));
  test("klient nie czyta innego klienta", () => assertFails(getDoc(doc(db(K), "users", K2))));
  test("klient nie czyta cudzego trenera", () => assertFails(getDoc(doc(db(K), "users", T2))));
  test("niezalogowany nie czyta nic", () => assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), "users", K))));
  test("nieistniejący dokument daje pusty wynik", () => assertSucceeds(getDoc(doc(db(K), "users", "nope"))));
  test("trener listuje swoich klientów", () =>
    assertSucceeds(getDocs(query(collection(db(T), "users"), where("trainerId", "==", T), where("role", "==", "CLIENT")))));
  test("trener nie listuje wszystkich klientów", () =>
    assertFails(getDocs(query(collection(db(T), "users"), where("role", "==", "CLIENT")))));
  test("klient zmienia swoje psy", () => assertSucceeds(updateDoc(doc(db(K), "users", K), { dogs: [{ name: "Burek", breed: "kundel" }] })));
  test("klient nie zmienia sobie roli", () => assertFails(updateDoc(doc(db(K), "users", K), { role: "TRAINER" })));
  test("klient nie zmienia sobie trenera", () => assertFails(updateDoc(doc(db(K), "users", K), { trainerId: T2 })));
  test("klient nie zmienia psów razem z rolą", () => assertFails(updateDoc(doc(db(K), "users", K), { dogs: [], role: "TRAINER" })));
  test("trener nie zmienia psów klienta", () => assertFails(updateDoc(doc(db(T), "users", K), { dogs: [] })));
  test("trener nie zmienia swojego dokumentu", () => assertFails(updateDoc(doc(db(T), "users", T), { name: "X" })));
  test("nikt nie tworzy użytkownika", () => assertFails(setDoc(doc(db(X), "users", X), { role: "TRAINER", name: "X", email: "x@x.pl" })));
  test("nikt nie usuwa użytkownika", () => assertFails(deleteDoc(doc(db(K), "users", K))));
});

describe("reports: odczyt", () => {
  const clientReports = (uid) => query(collection(db(uid), "reports"), where("clientId", "==", K), orderBy("createdAt", "desc"), limit(10));
  test("klient czyta raport", () => assertSucceeds(getDoc(doc(db(K), "reports", "byTrainer"))));
  test("trener czyta raport klienta", () => assertSucceeds(getDoc(doc(db(T), "reports", "byTrainer"))));
  test("obcy nie czyta raportu", () => assertFails(getDoc(doc(db(X), "reports", "byTrainer"))));
  test("inny trener nie czyta raportu", () => assertFails(getDoc(doc(db(T2), "reports", "byTrainer"))));
  test("inny klient nie czyta raportu", () => assertFails(getDoc(doc(db(K2), "reports", "byTrainer"))));
  test("usunięty raport daje pusty wynik", () => assertSucceeds(getDoc(doc(db(K), "reports", "deleted"))));
  test("klient listuje swoje raporty", () => assertSucceeds(getDocs(clientReports(K))));
  test("trener listuje raporty klienta", () => assertSucceeds(getDocs(clientReports(T))));
  test("inny trener nie listuje raportów", () => assertFails(getDocs(clientReports(T2))));
  test("nikt nie listuje wszystkich raportów", () => assertFails(getDocs(collection(db(T), "reports"))));
});

describe("reports: tworzenie", () => {
  test("klient dodaje raport", () => assertSucceeds(addDoc(collection(db(K), "reports"), newReport(K, "CLIENT"))));
  test("trener dodaje raport klientowi", () => assertSucceeds(addDoc(collection(db(T), "reports"), newReport(T, "TRAINER"))));
  test("nie można podszyć się pod autora", () => assertFails(addDoc(collection(db(K), "reports"), newReport(K, "CLIENT", { authorId: T }))));
  test("nie można podać cudzej roli", () => assertFails(addDoc(collection(db(K), "reports"), newReport(K, "TRAINER"))));
  test("lastEditedBy musi być autorem", () => assertFails(addDoc(collection(db(K), "reports"), newReport(K, "CLIENT", { lastEditedBy: T }))));
  test("inny trener nie dodaje raportu", () => assertFails(addDoc(collection(db(T2), "reports"), newReport(T2, "TRAINER"))));
  test("data utworzenia z serwera", () => assertFails(addDoc(collection(db(K), "reports"), newReport(K, "CLIENT", { createdAt: past }))));
  test("bez gotowego komentarza", () => assertFails(addDoc(collection(db(K), "reports"), newReport(K, "CLIENT", { comment: comment(T, "TRAINER") }))));
});

describe("reports: edycja i usuwanie", () => {
  const edit = (uid, fields = {}) => ({ title: "Zmieniony", description: "Opis", media: [], lastEditedBy: uid, updatedAt: serverTimestamp(), ...fields });
  test("klient edytuje raport trenera", () => assertSucceeds(updateDoc(doc(db(K), "reports", "byTrainer"), edit(K))));
  test("trener edytuje raport", () => assertSucceeds(updateDoc(doc(db(T), "reports", "byTrainer"), edit(T))));
  test("lastEditedBy musi być edytującym", () => assertFails(updateDoc(doc(db(K), "reports", "byTrainer"), edit(K, { lastEditedBy: T }))));
  test("data edycji z serwera", () => assertFails(updateDoc(doc(db(K), "reports", "byTrainer"), edit(K, { updatedAt: past }))));
  test("nie można przenieść raportu do innego klienta", () => assertFails(updateDoc(doc(db(T), "reports", "byTrainer"), { ...edit(T), clientId: K2 })));
  test("nie można zmienić autora", () => assertFails(updateDoc(doc(db(K), "reports", "byTrainer"), { ...edit(K), authorId: K })));
  test("inny trener nie edytuje", () => assertFails(updateDoc(doc(db(T2), "reports", "byTrainer"), edit(T2))));
  test("klient usuwa raport", () => assertSucceeds(deleteDoc(doc(db(K), "reports", "byTrainer"))));
  test("trener usuwa raport", () => assertSucceeds(deleteDoc(doc(db(T), "reports", "byTrainer"))));
  test("inny trener nie usuwa", () => assertFails(deleteDoc(doc(db(T2), "reports", "byTrainer"))));
});

describe("reports: komentarz", () => {
  test("druga strona dodaje komentarz", () => assertSucceeds(updateDoc(doc(db(K), "reports", "byTrainer"), { comment: comment(K, "CLIENT") })));
  test("autor raportu nie komentuje", () => assertFails(updateDoc(doc(db(T), "reports", "byTrainer"), { comment: comment(T, "TRAINER") })));
  test("komentarz tylko jako własny", () => assertFails(updateDoc(doc(db(K), "reports", "byTrainer"), { comment: comment(T, "TRAINER") })));
  test("komentarz z prawdziwą rolą", () => assertFails(updateDoc(doc(db(K), "reports", "byTrainer"), { comment: comment(K, "TRAINER") })));
  test("bez dodatkowych pól w komentarzu", () => assertFails(updateDoc(doc(db(K), "reports", "byTrainer"), { comment: { ...comment(K, "CLIENT"), extra: 1 } })));
  test("komentarz bez zmiany treści raportu", () => assertFails(updateDoc(doc(db(K), "reports", "byTrainer"), { comment: comment(K, "CLIENT"), title: "X" })));
  test("autor edytuje swój komentarz", () => assertSucceeds(updateDoc(doc(db(T), "reports", "byClientWithComment"), { comment: comment(T, "TRAINER") })));
  test("nie można nadpisać cudzego komentarza", () => assertFails(updateDoc(doc(db(K), "reports", "byClientWithComment"), { comment: comment(K, "CLIENT") })));
  test("autor usuwa swój komentarz", () => assertSucceeds(updateDoc(doc(db(T), "reports", "byClientWithComment"), { comment: deleteField() })));
  test("nie można usunąć cudzego komentarza", () => assertFails(updateDoc(doc(db(K), "reports", "byClientWithComment"), { comment: deleteField() })));
});

describe("invitations i mail", () => {
  test("trener czyta swoje zaproszenie", () => assertSucceeds(getDoc(doc(db(T), "invitations", "inv"))));
  test("trener listuje swoje zaproszenia", () => assertSucceeds(getDocs(query(collection(db(T), "invitations"), where("trainerId", "==", T)))));
  test("inny trener nie listuje wszystkich", () => assertFails(getDocs(collection(db(T2), "invitations"))));
  test("klient nie czyta zaproszenia", () => assertFails(getDoc(doc(db(K), "invitations", "inv"))));
  test("trener anuluje swoje zaproszenie", () => assertSucceeds(deleteDoc(doc(db(T), "invitations", "inv"))));
  test("inny trener nie anuluje", () => assertFails(deleteDoc(doc(db(T2), "invitations", "inv"))));
  test("nikt nie tworzy zaproszenia", () => assertFails(setDoc(doc(db(T), "invitations", "new"), { trainerId: T })));
  test("nikt nie zmienia zaproszenia", () => assertFails(updateDoc(doc(db(T), "invitations", "inv"), { expiresAt: serverTimestamp() })));
  test("nikt nie czyta maili", () => assertFails(getDoc(doc(db(T), "mail", "m"))));
  test("nikt nie pisze maili", () => assertFails(setDoc(doc(db(T), "mail", "x"), { to: "a@b.pl" })));
});

describe("storage", () => {
  const file = (uid, clientId, name = "f1") => ref(storage(uid), `clients/${clientId}/media/${name}`);
  const bytes = (n) => new Uint8Array(n);
  test("klient czyta swój plik", () => assertSucceeds(getMetadata(file(K, K))));
  test("trener czyta plik klienta", () => assertSucceeds(getMetadata(file(T, K))));
  test("inny trener nie czyta", () => assertFails(getMetadata(file(T2, K))));
  test("obcy nie czyta", () => assertFails(getMetadata(file(X, K))));
  test("klient wgrywa plik", () => assertSucceeds(uploadBytes(file(K, K, "new"), bytes(10))));
  test("trener wgrywa plik klientowi", () => assertSucceeds(uploadBytes(file(T, K, "byTrainer"), bytes(10))));
  test("inny trener nie wgrywa", () => assertFails(uploadBytes(file(T2, K, "byOtherTrainer"), bytes(10))));
  test("klient nie wgrywa do innego klienta", () => assertFails(uploadBytes(file(K, K2, "byOtherClient"), bytes(10))));
  test("plik do 35 MB przechodzi", () => assertSucceeds(uploadBytes(file(K, K, "big"), bytes(35 * 1024 * 1024))));
  test("plik ponad 35 MB odrzucony", () => assertFails(uploadBytes(file(K, K, "huge"), bytes(35 * 1024 * 1024 + 1))));
  test("nie można nadpisać pliku", () => assertFails(uploadBytes(file(K, K), bytes(10))));
  test("nie można usunąć pliku", () => assertFails(deleteObject(file(K, K))));
  test("poza folderem media zakaz", () => assertFails(uploadBytes(ref(storage(K), `clients/${K}/other/x`), bytes(10))));
});