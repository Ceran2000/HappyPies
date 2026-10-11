# HappyPies — Roadmapa

**Legenda:** ✅ zrobione · 🔄 w trakcie · ⏳ do zrobienia · 💤 odłożone

---

## 📍 Teraz

**Faza 1 — MVP** · branch: `feature/reports`

Najbliższe kroki:
1. ✅ Koin multiplatform wdrożony, przetestowany i zmergowany do `main`
2. ⏳ Model raportu i lista raportów w szczegółach klienta (start Fazy 1)
3. ⏳ Formularz raportu, edycja i usuwanie, komentarz drugiej strony
4. ⏳ Zdjęcia i film w raporcie

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

**Raporty**
- ⏳ Model `reports`, repozytorium (bridge Android / iOS) i Security Rules: klient widzi swoje, trener wszystkie (FR-03, FR-07, FR-29)
- ⏳ Szczegóły klienta: lista raportów chronologicznie (FR-10)
- ⏳ Formularz raportu: tytuł (wymagany), opis; edycja i usuwanie przez trenera i klienta, „Dodany przez” i `lastEditedBy` (FR-07, FR-31, FR-32)
- ⏳ Zdjęcia i film w raporcie, upload do Firebase Storage (nowy interfejs bridge'a) (FR-15)
- ⏳ Jeden komentarz pod raportem, zostawia go strona niebędąca autorem (FR-13)
- ⏳ Lista klientów: sortowanie wg ostatniego raportu i znaczniki „nowy raport” / „bez komentarza” (FR-33)

**Konta i profil**
- ⏳ Reset hasła i wylogowanie (FR-23, FR-24)
- ⏳ Profil klienta: lista psów (imię, rasa) edytowana przez klienta; trener widzi ją w szczegółach klienta; bez wpływu na raporty (FR-26)

**UI**
- ⏳ Motyw stylu A (paleta, Nunito, tryb jasny i ciemny wg ustawień systemu); logo bez zmian, w trybie ciemnym na jasnym panelu (NFR-19)

**Na koniec MVP** (decyzja 2026-10-11; do tego czasu klient w demo zakładany ręcznie w konsoli Firebase)
- ⏳ Zaproszenie klienta mailem: deep link → rejestracja (tylko po zaproszeniu), ważność 7 dni, ponowne wysłanie (FR-02, FR-25)
- ⏳ Usunięcie klienta wraz z danymi, w tym kaskadowe kasowanie mediów (Cloud Function) (FR-04)
- ⏳ Security Rules z podziałem na role dla `clients` i `users` (teraz: każdy zalogowany czyta `clients`) (FR-03)
- ⏳ Testy logiki ViewModeli (stany Loading/Success/Error) z fałszywymi repozytoriami
- ⏳ Luka: brak testu, że `appModule` (Android) dostarcza wszystkie interfejsy
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

## 🐞 Znane problemy

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
