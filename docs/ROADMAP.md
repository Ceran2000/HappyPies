# HappyPies — Roadmapa

**Legenda:** ✅ zrobione · 🔄 w trakcie · ⏳ do zrobienia · 💤 odłożone

---

## 📍 Teraz

**Faza 1 — MVP** · branch: `feature/reports`

Najbliższe kroki:
1. ✅ Koin multiplatform wdrożony, przetestowany i zmergowany do `main`
2. ✅ Warstwa danych raportów: model i repozytorium Android / iOS (krok 1 brancha `feature/reports`)
3. ✅ Lista raportów w szczegółach klienta i formularz „Nowy raport” (kroki 2–3): po review kodu scommitowane 2026-10-12 (`dbd7091`)
4. ✅ Konto wyprowadzane z Firebase Auth (`AccountRepository` i model `Account` zamiast `SessionManager`/`UserProfile`, ADR 0009): po review scommitowane 2026-10-12
5. ✅ Szczegóły raportu z komentarzem (krok 4: 4a–4d); test powrotu z tła na iOS przeniesiony na koniec kroku 5
6. ✅ Motyw stylu A (`1cf0319`); mockup ekranu startowego klienta zaakceptowany
7. ✅ Widok klienta (krok 5): 5a imię trenera u klienta, 5b wspólna karta raportu, 5c ekran startowy klienta wg mockupu; nowy wygląd szczegółów raportu. Został test powrotu z tła na iOS (z kroku 4)
8. ⏳ Krok 6: sortowanie listy klientów i znaczniki (FR-33)

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
- ✅ Krok 2. Lista raportów w szczegółach klienta, widok trenera, wszystkie raporty klienta od najnowszego (stronicowanie usunięte 2026-10-12, wraca w kroku 7), odświeżanie, komunikaty błędów wg typu; przy okazji profil osoby w jednej kolekcji `users` i raporty z samymi ID (ADR 0007) oraz sesja w jednym miejscu z automatycznym logowaniem po ponownym otwarciu aplikacji (ADR 0008). Przetestowane ręcznie na Androidzie, po review scommitowane (`dbd7091`) (FR-06, FR-10, NFR-07)
- ✅ Krok 3. Formularz „Nowy raport”: tytuł (wymagany), opis (opcjonalny), autor z sesji. Przetestowane ręcznie na Androidzie, po review scommitowane (`dbd7091`) (FR-07, FR-29)
- ✅ Konto wyprowadzane z Firebase Auth (dawniej „sesja reaktywna”, decyzja 2026-10-12, ADR 0009): `AccountRepository` bez własnego stanu sesji, jeden model `Account` (`uid`/`email` z Auth, `name`/`role` z `users/{uid}`); stan z listenera Firebase Auth (callback w moście `addUserListener`, `callbackFlow` w `commonMain`) i profilu `users/{uid}`. Trwały błąd profilu → wylogowanie i komunikat na logowaniu; chwilowy → ekran „Spróbuj ponownie / Wyloguj”. Custom claims odrzucone (plan Blaze). Po review scommitowane 2026-10-12
- ✅ Krok 4. Szczegóły raportu: edycja, usuwanie, „Dodany przez” i `lastEditedBy`, jeden komentarz drugiej strony (FR-13, FR-31, FR-32). Plan ustalony 2026-10-22, spec ekranu: `docs/design/report-details.md`; każdy etap osobnym commitem:
  - ✅ 4a. Raporty na żywo (`88e8842`, 2026-10-22): w moście listenery `addDocumentListener` / `addQueryListener` (callback + `ListenerHandle`, ten sam wzorzec co `addAuthStateListener`), w `commonMain` `callbackFlow`; repozytorium wystawia `observeReports` / `observeReport`, znika `ReportRepository.changes`; szczegóły klienta zbierają flow (bez pull-to-refresh, „Spróbuj ponownie” po błędzie)
  - ✅ 4b. Ekran szczegółów raportu (po review scommitowane 2026-10-22: `6614355`, `bf76b65`; usuwanie na razie przyciskiem „Usuń”, ikony razem z motywem A): trasa `ReportDetail` (dopisana w `RouteSerialization`), „Dodany przez” / „Edytowany przez”, usuwanie z potwierdzeniem, powrót gdy raport zniknie. Ekran od razu ze stanem przez `stateIn(WhileSubscribed(5_000))`, żeby listener stawał, gdy ekran nie jest widoczny (koszty); `ClientDetailsViewModel` przechodzi na ten sam wzorzec
  - ✅ 4c. Komentarz (`7fd7c16`, 2026-10-25): tylko druga strona, edycja w karcie pod ikoną ołówka, usuwanie przez autora; tekst w `TextFieldState` w ViewModelu, flagi UI w modelu stanu; ołówek i kosz na razie jako przyciski tekstowe. Test ręczny częściowy: bez panelu klienta sprawdzony tylko raport trenera (brak „Dodaj komentarz”); dodawanie, edycja i usuwanie pokryte testami ViewModelu, do sprawdzenia na urządzeniu w kroku 5
  - ✅ 4d. Edycja raportu (`34f6e2a`, 2026-10-25): trasy `AddReport` / `EditReport`, osobne `AddReportViewModel` / `EditReportViewModel` na wspólnej abstrakcyjnej bazie `ReportFormViewModel` (bez sprawdzania typów), wspólne `ReportFormLayout` / `ReportFields`; zapis bez zmian nic nie zapisuje; media dojdą do klasy bazowej, także dla istniejących raportów. Test ręczny OK. Stany brzegowe (2026-10-25): odczyt offline i obrót ekranu OK; zawieszony zapis offline to świadome ograniczenie (zapisy offline w Fazie 2); powrót z tła na iOS do sprawdzenia na koniec kroku 5; jednoczesna edycja z dwóch urządzeń pominięta jako mało realna (wygrywa ostatni zapis)
- ✅ Krok 5. Widok klienta: ekran startowy z listą jego raportów i dodawaniem własnych (FR-10, FR-29). Test ręczny OK (2026-10-25), w tym komentarz klienta pod raportem trenera (zaległość z 4c); do sprawdzenia jeszcze powrót z tła na iOS
  - ✅ 5a. Prawdziwe imię trenera w przestrzeni klienta (`4ced2df`): `TrainerRepository`, wymagane `Client.trainerId`
  - ✅ 5b. Wspólna karta raportu i jej mapowanie w `ui/common` (`67d9733`)
  - ✅ 5c. Ekran startowy klienta (`21b2a73`): powitanie, psy i trener, raporty, pusty stan, „Nowy raport”; inicjały wylogowują po potwierdzeniu. Tekst nagłówka liczony w modelu stanu z `client` i `trainer`
  - ✅ Nowy wygląd szczegółów raportu (`3064e74`): `Scaffold`, usuwanie jako ikona w pasku, tytuł nad opisem, etykieta „KOMENTARZ: {imię}”, przerywany przycisk „Dodaj komentarz”, stopka z „Dodany / Edytowany przez” nad „Edytuj raport”; wylogowanie tylko na ekranach startowych
- ⏳ Krok 6. Lista klientów: sortowanie wg ostatniego raportu i znaczniki „nowy raport” / „bez komentarza” (FR-33)
- ⏳ Krok 7 (decyzja 2026-10-12: na koniec brancha). Stronicowanie listy raportów od nowa: pierwsza wersja (kursor po 20) usunięta przed commitem. Wymagania z review: (1) bezstratny timestamp, bo `TimestampValue` w milisekundach przycina mikrosekundy Firestore i kursor pomijałby raporty z tej samej milisekundy, a tie-breaker po ID nic by nie dawał; kursor ma przechowywać dokładne wartości; (2) kursor jako mapa pole → wartość zamiast listy sparowanej pozycyjnie z `orderBy`, walidowany w `CollectionQuery`; (3) tie-breaker po ID dokumentu jako ostatni klucz sortowania; (4) uwaga na kursor z szacowanego `createdAt` świeżo dodanego raportu (`ESTIMATE`)

**Zdjęcia i film** (osobny branch po zmergowaniu raportów, najbardziej ryzykowna część: upload przez bridge na iOS)
- ⏳ Zdjęcia i film w raporcie, upload do Firebase Storage (nowy interfejs bridge'a) (FR-15)

**Konta i profil**
- ⏳ Reset hasła i wylogowanie (FR-23, FR-24)
- ⏳ Komunikaty błędów logowania wg rodzaju (złe hasło, nieznane konto, brak sieci, zbyt wiele prób). Dziś jeden ogólny komunikat, bo błędy Firebase Auth są różne na Androidzie i iOS; wymaga klasyfikacji w moście, jak w `FirestoreGateway`
- ⏳ Profil klienta: lista psów (imię, rasa) edytowana przez klienta; trener widzi ją w szczegółach klienta; bez wpływu na raporty (FR-26)

**UI**
- ✅ Motyw stylu A (`1cf0319`, 2026-10-25): `HappyPiesTheme` z paletą jasną i ciemną wg ustawień systemu (NFR-19), Nunito, zaokrąglenia, ikony z `material-icons-extended` zamiast przycisków tekstowych; kontrast wg WCAG 2.1 AA (NFR-17), obramowania pól ciemniejsze niż na mockupach
- ⏳ Układ ekranów wg mockupów (po merge raportów): nagłówki, karty z cieniem, logo bez zmian, w trybie ciemnym na jasnym panelu

**Na koniec MVP** (decyzja 2026-10-11; do tego czasu klient w demo zakładany ręcznie w konsoli Firebase)
- ⏳ Zaproszenie klienta mailem: deep link → rejestracja (tylko po zaproszeniu), ważność 7 dni, ponowne wysłanie (FR-02, FR-25)
- ⏳ Usunięcie klienta wraz z danymi, w tym kaskadowe kasowanie mediów (Cloud Function) (FR-04)
- ⏳ Security Rules z podziałem na role dla `users` i `reports` (teraz tymczasowo „każdy zalogowany”; klient ma zmieniać tylko swoje `dogs` i czytać dokument swojego trenera; nikt nie może sam zmienić sobie `role` ani `trainerId`, bo dziś klient mógłby nadać sobie rolę trenera). Reguły i indeksy wdrażane z plików w repo (`firestore.rules`, `firestore.indexes.json`, `firebase deploy`), a nie klikane w konsoli (FR-03)
- ⏳ Testy logiki ViewModeli (stany Loading/Success/Error) z fałszywymi repozytoriami
- ⏳ Luka: brak testu, że `appModule` (Android) dostarcza wszystkie interfejsy
- ⏳ Luka: brak testów kontraktu `FirestoreGateway`, które sprawdzałyby, że Android i iOS zachowują się tak samo (błąd → rodzaj, wartość → typ). Minimum: test instrumentowany Androida na emulatorze Firestore i spisana tabela zachowań w ADR 0006
- ⏳ Alert budżetu w Google Cloud od razu po przejściu na plan Blaze (najtańsze zabezpieczenie przed niespodzianką w kosztach)
- ⏳ Kompresja wideo na urządzeniu i limity rozmiaru: ustalamy na realnych filmach (kompromis jakość / koszt) (FR-16, NFR-05)

## Faza 2 — po MVP ⏳

Do uzgodnienia z trenerem po MVP.

- ⏳ Kalendarz raportów klienta i zbiorczy kalendarz trenera (FR-11)
- ⏳ Powiadomienia push (FCM) i lista powiadomień w aplikacji (FR-21, FR-22)
- ⏳ Miniatury zdjęć i wideo (FR-17)
- ⏳ Ekran „porządki” + kwartalne zbiorcze przypomnienie o starych filmach: usuń / zachowaj / nie przypominaj (FR-18–FR-20)
- ⏳ Encja Pies (klient ma 1..n psów, raport wskazuje psa) (FR-27, FR-28)
- ⏳ Wątek komentarzy zamiast jednego komentarza
- ⏳ Zapisy offline (decyzja 2026-10-25): zapis nie czeka na serwer, formularz zamyka się po zapisie lokalnym; snackbar „Jesteś offline” na poziomie aplikacji (monitor łączności w moście: `ConnectivityManager` / `NWPathMonitor`); dialog, gdy zapis zostanie odrzucony po powrocie sieci (callback błędu zadania zapisu, tylko w tej samej sesji aplikacji); nowy raport z ID nadanym po stronie klienta (`document()` + `set()`), żeby ponowienie nie robiło duplikatów
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

- **Zapisy czekają na potwierdzenie serwera.** Repozytoria czekają (`await`) na zapis w Firestore, więc offline zapis nie kończy się, dopóki nie wróci sieć, a UI pokaże wskaźnik ładowania. Firestore zapisuje lokalnie od razu, ale z tego trybu offline w MVP świadomie nie korzystamy. Rozwiązanie po MVP: „Zapisy offline” w Fazie 2.
- **Odczyty offline nie mówią, że dane pochodzą z cache.** Domyślne `get()` bez sieci po cichu zwraca dane z cache, dla zapytań potencjalnie niepełne. UI nie pokazuje, że lista może być nieaktualna (przy stronicowaniu z kroku 7 dodatkowo ryzyko fałszywego „nie ma kolejnej strony”). Rozwiązanie na później: `fromCache` w wyniku gatewaya albo jawne źródło w zapytaniu.
- **Listenery Firestore a koszty.** Start listenera kosztuje tyle co pełny odczyt wyników, kolejne zmiany tylko po 1 odczycie za zmieniony dokument. Po powrocie na ekran (listener zatrzymany przez `WhileSubscribed`) i przerwie dłuższej niż ok. 30 min płacimy znów za pełny odczyt. Do kroku 7 słuchamy całej historii raportów klienta, bez `limit`.
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
| 2026-10-12 | Review kodu kroków 2–3 i commit `dbd7091`: `DocumentResult` i jedna reguła błędów w moście Firestore, walidacja `CollectionQuery`, `currentUser` jako właściwość; stronicowanie usunięte (wraca w kroku 7). Decyzja: następna jest sesja reaktywna |
| 2026-10-12 | Konto wyprowadzane z Firebase Auth (ADR 0009): `SessionManager`, `UserProfile`, `UserRepository` i `AppUser` zastąpione jednym modelem `Account` i `AccountRepository`; listener Firebase Auth w moście (callback), ekran „konto niedostępne” zamiast fałszywego wylogowania przy braku sieci |
| 2026-10-22 | Review mostu Firestore: błędy jako `DataException` prosto z platform (`f1050d6`). Krok 4a: raporty na żywo przez listenery Firestore (`88e8842`). Krok 4b: ekran szczegółów raportu z usuwaniem, listenery zatrzymywane przez `WhileSubscribed` (decyzja kosztowa), nazwy osób przez `ClientSpacePeople` (`6614355`, `bf76b65`). Decyzja: zostajemy przy Firestore zamiast Realtime Database (przy naszej skali bez oszczędności, a migracja droga) |
| 2026-10-25 | Krok 4c: komentarz pod raportem (`7fd7c16`). Ustalenia stylu: flagi UI (`show…`, `…Enabled`) w modelu stanu, `update {}` zamiast `.value =`, prosty niepusty stan zamiast `null`, `TextFieldState` w ViewModelu, wyzwalacz ponowienia jako `MutableSharedFlow` z buforem 1. Doprecyzowane FR-09 i FR-13: autor raportu nie komentuje, bo w MVP jest jedno miejsce na komentarz; wątek komentarzy po MVP |
| 2026-10-25 | Krok 4d: edycja raportu (`34f6e2a`): osobne ekrany i ViewModele dodawania i edycji na wspólnej bazie formularza; `ErrorWithRetry` we wspólnym `ui/common`. Zasada: komponenty dostają tylko potrzebne dane, warianty jako osobne typy zamiast opcjonalnych parametrów |
| 2026-10-25 | Krok 4 zamknięty (stany brzegowe: odczyt offline i obrót OK, zapisy offline przeniesione do Fazy 2). Mockup ekranu startowego klienta. Motyw stylu A (`1cf0319`): paleta, Nunito, ikony; NFR-17 (WCAG 2.1 AA) wiążące. Security Rules mają zabraniać zmiany `role` i `trainerId` |
| 2026-10-25 | Krok 5: widok klienta (`4ced2df`, `67d9733`, `21b2a73`) i nowy wygląd szczegółów raportu (`3064e74`) wg mockupów; wylogowanie tylko na ekranach startowych. Ustalenie: tekst do wyświetlenia jako gettery w modelu stanu nad obiektami domenowymi, bez pośrednich klas |