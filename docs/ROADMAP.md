# HappyPies — Roadmapa

**Legenda:** ✅ zrobione · 🔄 w trakcie · ⏳ do zrobienia · 💤 odłożone

---

## 📍 Teraz

**Faza 1 — MVP** · branch: `feature/reports`

Najbliższe kroki:
1. ✅ Koin multiplatform wdrożony, przetestowany i zmergowany do `main`
2. ✅ Warstwa danych raportów: model i repozytorium Android / iOS (krok 1 brancha `feature/reports`)
3. 🔄 Lista raportów w szczegółach klienta i formularz „Nowy raport” (kroki 2–3): przetestowane ręcznie na Androidzie 2026-10-11; **kod niescommitowany na `feature/reports`, czeka na review kodu**, potem commity
4. ⏳ Szczegóły raportu z komentarzem, potem widok klienta (kroki 4–5)

Zakres MVP ustalony 2026-10-11: klient i trener dodają raporty (tytuł, opis, zdjęcia, film), druga strona zostawia jeden komentarz. Bez kalendarza, powiadomień push i encji psa. Szczegóły: Faza 1 poniżej.

Kolejność ustalona 2026-10-11: najpierw raporty. Na koniec MVP zostają zaproszenia, usuwanie klienta, testy i kompresja wideo. Plan awaryjny: jeśli nie wystarczy czasu, pokazujemy trenerowi dodawanie raportów na użytkowniku założonym ręcznie w konsoli Firebase.

---

## Faza 0 — fundament ✅

Pozostałe pozycje fundamentu (zaproszenia, testy, Security Rules) przeniesione na koniec MVP — patrz „Na koniec MVP” w Fazie 1.


- ✅ Inicjalizacja projektu KMP
- ✅ Firebase SDK: Android natywnie, iOS przez SPM (Protocol Injection Bridge)
- ✅ Logowanie z rolami (trener / klient), wspólny UI Compose na obu platformach
- ✅ Lista klientów i szczegóły klienta (trener)
- ✅ Wymagania funkcjonalne i niefunkcjonalne spisane (FR/NFR), decyzje w ADR, mockupy MVP w stylu A (2026-10-11)
- ✅ Koin multiplatform zamiast ręcznego przekazywania zależności (`koinViewModel()`) — wdrożone 2026-10-04, czeka na commit
- ✅ Test grafu Koina w `commonTest` (`ViewModelModuleTest`, działa na JVM i iOS; `verify()` odpada, bo jest tylko dla JVM)
- 💤 CI (opcjonalnie)

## Faza 1 — działające MVP ⏳

Jeden typ raportu, bez podziału na raporty trenera i klienta (ADR 0004). Po MVP trener doprecyzuje oczekiwania. Wymagania z ID (lokalnie): `docs/requirements/functional.md`.

**Raporty** (branch `feature/reports`, merge po kroku 5, gdy raporty tekstowe działają u trenera i klienta)
- ✅ Krok 1. Warstwa danych: model `reports` i repozytorium; jeden most Firestore na platformę, repozytoria `User` / `Client` / `Report` i mapowanie w `commonMain` z testami, błędy domenowe `DataException` (ADR 0006), bez UI (FR-07, FR-29)
- 🔄 Krok 2. Lista raportów w szczegółach klienta, widok trenera, wszystkie raporty klienta od najnowszego (stronicowanie usunięte 2026-10-12, wraca w kroku 7), odświeżanie, komunikaty błędów wg typu; przy okazji profil osoby w jednej kolekcji `users` i raporty z samymi ID (ADR 0007) oraz sesja w jednym miejscu z automatycznym logowaniem po ponownym otwarciu aplikacji (ADR 0008). Przetestowane ręcznie na Androidzie, czeka na review kodu i commit (FR-06, FR-10, NFR-07)
- 🔄 Krok 3. Formularz „Nowy raport”: tytuł (wymagany), opis (opcjonalny), autor z sesji. Przetestowane ręcznie na Androidzie, czeka na review kodu i commit (FR-07, FR-29)
- ⏳ Sesja reaktywna (decyzja 2026-10-12, zaraz po commicie kroków 2–3). `SessionManager` wyprowadza stan ze zmian w Firebase Auth zamiast ustawiać go ręcznie w `restore`/`signIn`/`signOut`. Most wystawia listener-callback (np. `addUserListener`), nie `Flow`; `callbackFlow` + `mapLatest` + `stateIn` w `commonMain`; `distinctUntilChanged()` na całym `AppUser`; `signIn()` czeka na rozstrzygnięcie profilu; nowy stan „profil niedostępny” (brak sieci) z ponowieniem; znika `restore()` i `currentUser`. Źródło: AuthStateListener; IdTokenListener dopiero, jeśli role przejdą do custom claims. Nowy ADR częściowo zastępujący ADR 0008
- ⏳ Krok 4. Szczegóły raportu: edycja, usuwanie, „Dodany przez” i `lastEditedBy`, jeden komentarz drugiej strony (FR-13, FR-31, FR-32)
- ⏳ Krok 5. Widok klienta: ekran startowy z listą jego raportów i dodawaniem własnych (FR-10, FR-29)
- ⏳ Krok 6. Lista klientów: sortowanie wg ostatniego raportu i znaczniki „nowy raport” / „bez komentarza” (FR-33)
- ⏳ Krok 7 (decyzja 2026-10-12: na koniec brancha). Stronicowanie listy raportów od nowa: pierwsza wersja (kursor po 20) usunięta przed commitem. Wymagania z review: (1) bezstratny timestamp, bo `TimestampValue` w milisekundach przycina mikrosekundy Firestore i kursor pomijałby raporty z tej samej milisekundy, a tie-breaker po ID nic by nie dawał; kursor ma przechowywać dokładne wartości; (2) kursor jako mapa pole → wartość zamiast listy sparowanej pozycyjnie z `orderBy`, walidowany w `CollectionQuery`; (3) tie-breaker po ID dokumentu jako ostatni klucz sortowania; (4) uwaga na kursor z szacowanego `createdAt` świeżo dodanego raportu (`ESTIMATE`)

**Zdjęcia i film** (osobny branch po zmergowaniu raportów, najbardziej ryzykowna część: upload przez bridge na iOS)
- ⏳ Zdjęcia i film w raporcie, upload do Firebase Storage (nowy interfejs bridge'a) (FR-15)

**Konta i profil**
- ⏳ Reset hasła i wylogowanie (FR-23, FR-24)
- ⏳ Komunikaty błędów logowania wg rodzaju (złe hasło, nieznane konto, brak sieci, zbyt wiele prób). Dziś jeden ogólny komunikat, bo błędy Firebase Auth są różne na Androidzie i iOS; wymaga klasyfikacji w moście, jak w `FirestoreGateway`
- ⏳ Profil klienta: lista psów (imię, rasa) edytowana przez klienta; trener widzi ją w szczegółach klienta; bez wpływu na raporty (FR-26)

**UI**
- ⏳ Motyw stylu A (paleta, Nunito, tryb jasny i ciemny wg ustawień systemu); logo bez zmian, w trybie ciemnym na jasnym panelu (NFR-19)

**Na koniec MVP** (decyzja 2026-10-11; do tego czasu klient w demo zakładany ręcznie w konsoli Firebase)
- ⏳ Zaproszenie klienta mailem: deep link → rejestracja (tylko po zaproszeniu), ważność 7 dni, ponowne wysłanie (FR-02, FR-25)
- ⏳ Usunięcie klienta wraz z danymi, w tym kaskadowe kasowanie mediów (Cloud Function) (FR-04)
- ⏳ Security Rules z podziałem na role dla `users` i `reports` (teraz tymczasowo „każdy zalogowany”; klient ma zmieniać tylko swoje `dogs` i czytać dokument swojego trenera). Reguły i indeksy wdrażane z plików w repo (`firestore.rules`, `firestore.indexes.json`, `firebase deploy`), a nie klikane w konsoli (FR-03)
- ⏳ Testy logiki ViewModeli (stany Loading/Success/Error) z fałszywymi repozytoriami
- ⏳ Luka: brak testu, że `appModule` (Android) dostarcza wszystkie interfejsy
- ⏳ Luka: brak testów kontraktu `FirestoreGateway`, które sprawdzałyby, że Android i iOS zachowują się tak samo (błąd → rodzaj, wartość → typ). Minimum: test instrumentowany Androida na emulatorze Firestore i spisana tabela zachowań w ADR 0006
- ⏳ Kompresja wideo na urządzeniu i limity rozmiaru: ustalamy na realnych filmach (kompromis jakość / koszt) (FR-16, NFR-05)

## Faza 2 — po MVP ⏳

Do uzgodnienia z trenerem po MVP.

- ⏳ Kalendarz raportów klienta i zbiorczy kalendarz trenera (FR-11)
- ⏳ Powiadomienia push (FCM) i lista powiadomień w aplikacji (FR-21, FR-22)
- ⏳ Miniatury zdjęć i wideo (FR-17)
- ⏳ Ekran „porządki” + kwartalne zbiorcze przypomnienie o starych filmach: usuń / zachowaj / nie przypominaj (FR-18–FR-20)
- ⏳ Encja Pies (klient ma 1..n psów, raport wskazuje psa) (FR-27, FR-28)
- ⏳ Wątek komentarzy zamiast jednego komentarza
- ⏳ Dopracowanie UI/UX na obu platformach

## Faza 3 — dodatki (jeśli budżet pozwoli) ⏳

- ⏳ Wielu trenerów (multi-tenant) — dane już są filtrowane po `trainerId`
- ⏳ Statystyki i postępy klienta w czasie
- ⏳ Eksport raportów do PDF

## Publikacja 🚀 ⏳

- ⏳ Nowy projekt Firebase na koncie trenera (RODO) przed prawdziwymi danymi
- ⏳ Konta deweloperskie Google Play / App Store (zakłada i opłaca trener)
- ⏳ Pierwsza publikacja w sklepach (szczegóły: `plan.md` §7)

---

## ⚠️ Świadome ograniczenia

- **Zapisy czekają na potwierdzenie serwera.** Repozytoria czekają (`await`) na zapis w Firestore, więc offline zapis nie kończy się, dopóki nie wróci sieć, a UI pokaże wskaźnik ładowania. Firestore zapisuje lokalnie od razu, ale z tego trybu offline w MVP świadomie nie korzystamy.
- **Odczyty offline nie mówią, że dane pochodzą z cache.** Domyślne `get()` bez sieci po cichu zwraca dane z cache, dla zapytań potencjalnie niepełne. UI nie pokazuje, że lista może być nieaktualna (przy stronicowaniu z kroku 7 dodatkowo ryzyko fałszywego „nie ma kolejnej strony”). Rozwiązanie na później: `fromCache` w wyniku gatewaya albo jawne źródło w zapytaniu.
- **Usunięcie raportu nie usuwa plików z Storage.** `deleteReport` kasuje tylko dokument. Do rozwiązania razem ze zdjęciami i filmem (F1-04), inaczej zostaną osierocone pliki.

## 🐞 Znane problemy

- [x] **Po śmierci procesu sesja była pusta.** Naprawione 2026-10-11 na `feature/reports`: `SessionManager` odtwarza sesję z Firebase Auth przy starcie (ADR 0008).

- [ ] **iOS: wolne animacje przy przejściach między ekranami.** Zgłoszone 2026-10-04, nie wiadomo, czy to regresja po Koinie. Do sprawdzenia: (1) czy to build Debug (Kotlin/Native + Compose w Debug bywa dużo wolniejszy) — uruchom schemat w konfiguracji Release na urządzeniu; (2) porównanie z commitem `75ceb94` sprzed Koina; (3) dopiero potem szukanie przyczyny w kodzie nawigacji. Na razie założenie, a nie diagnoza.

---

## 📚 Do poczytania (w wolnym czasie)

- [ ] **Argumenty ViewModelu w Navigation 3.** Wątpliwość: `ClientDetailsViewModel` dostaje `clientId` w konstruktorze (przez Koina `parametersOf`), a nie przez `SavedStateHandle`. Ustalenie z sesji 2026-10-04: w Nav3 trasa to zwykły obiekt w back stacku, a nie wpis z argumentami, więc `SavedStateHandle` nie jest automatycznie wypełniany; ID przetrwa śmierć procesu, bo siedzi w trasie zapisywanej przez `rememberNavBackStack`. **Do zweryfikowania w dokumentacji** (to moje ustalenie z pamięci, nie sprawdzone w źródle): czy oficjalne receptury Nav3 faktycznie zalecają fabrykę/assisted injection dla kluczy tras, i czy `SavedStateHandle` ma w Nav3 jakąś rolę.
  Gdzie: [Navigation 3 — dokumentacja](https://developer.android.com/guide/navigation/navigation-3), receptury Nav3 (repo `android/nav3-recipes`), [Koin: parametry](https://insert-koin.io/docs/reference/koin-core/injection-parameters/).
- [ ] **Koin głębiej.** Service locator a DI generowane w czasie kompilacji (czemu błędy wychodzą dopiero w runtime), zakresy (`scope`), `verify()` / `checkModules`, `viewModelOf`, start Koina na iOS, ograniczenia. Dla porównania: `kotlin-inject` jako alternatywa z weryfikacją w czasie kompilacji.
  Gdzie: [Koin dla KMP](https://insert-koin.io/docs/reference/koin-mp/kmp/), [Koin Compose Multiplatform](https://insert-koin.io/docs/reference/koin-compose/multiplatform/).

---

## Historia

| Data | Co się wydarzyło |
|---|---|
| 2026-07-08 | Inicjalizacja projektu KMP |
| 2026-07-11 | Firebase SDK; decyzje: natywne SDK zamiast GitLive, wspólny UI w Compose Multiplatform |
| 2026-08-23 | Logowanie z rolami zmergowane do `main` |
| 2026-08-24 | Lista i szczegóły klientów (`75ceb94`) |
| 2026-10-04 | Koin multiplatform zmergowany do `main` (`f492034`), dodany test grafu Koina. Decyzja: migracja na Koin multiplatform + testy jako kolejny krok. Powstała ta roadmapa; testy przelogowania OK; `feature/client-list` zmergowane do `main` (`c68900e`) |
| 2026-10-11 | Przegląd wymagań i ustalenie zakresu MVP: jeden typ raportu z komentarzem drugiej strony, bez kalendarza, push i encji psa; tylko usunięcie klienta; wideo bez limitu długości (limity rozmiaru na koniec MVP). Powstały FR/NFR, ADR-y i mockupy w stylu A (ciepły i miękki, paleta z logo, tryb ciemny) |
| 2026-10-11 | Wspólna warstwa danych na `feature/reports` (ADR 0006): jeden generyczny most Firestore na platformę, repozytoria i mapowanie w `commonMain`, błędy domenowe `DataException`, testy mapowania na JVM i iOS |
| 2026-10-11 | Kroki 2–3 na `feature/reports` (do review): jedna kolekcja `users` z profilem, raporty z samymi ID (ADR 0007), lista raportów ze stronicowaniem kursorem i odświeżaniem, formularz „Nowy raport”, testy ViewModeli |
| 2026-10-11 | Sesja w jednym miejscu (ADR 0008): `SessionManager` z jawnym stanem, odtwarzanie z Firebase Auth przy starcie, nawigacja wynikająca ze stanu sesji; `ReportRepository.changes` jako `Flow` |
