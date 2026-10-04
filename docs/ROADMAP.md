# HappyPies — Roadmapa

**Legenda:** ✅ zrobione · 🔄 w trakcie · ⏳ do zrobienia · 💤 odłożone

---

## 📍 Teraz

**Faza 0 — fundament** · branch: `feature/koin-multiplatform`

Najbliższe kroki:
1. ✅ Koin multiplatform wdrożony, zacommitowany i przetestowany ręcznie na Androidzie i iOS
2. 🔄 Testy ViewModeli (`commonTest`, fałszywe repozytoria) — graf Koina już pokryty
3. ⏳ Zaproszenie klienta mailem + Security Rules z podziałem na role

---

## Faza 0 — fundament 🔄

- ✅ Inicjalizacja projektu KMP
- ✅ Firebase SDK: Android natywnie, iOS przez SPM (Protocol Injection Bridge)
- ✅ Logowanie z rolami (trener / klient), wspólny UI Compose na obu platformach
- ✅ Lista klientów i szczegóły klienta (trener)
- ⏳ Zaproszenie klienta mailem: deep link → rejestracja/logowanie
- ⏳ Security Rules z podziałem na role (teraz: każdy zalogowany czyta `clients`)
- ✅ Koin multiplatform zamiast ręcznego przekazywania zależności (`koinViewModel()`) — wdrożone 2026-10-04, czeka na commit
- ✅ Test grafu Koina w `commonTest` (`ViewModelModuleTest`, działa na JVM i iOS; `verify()` odpada, bo jest tylko dla JVM)
- ⏳ Testy logiki ViewModeli (stany Loading/Success/Error) z fałszywymi repozytoriami
- ⏳ Luka: brak testu, że `appModule` (Android) dostarcza wszystkie interfejsy
- 💤 CI (opcjonalnie)

## Faza 1 — działające MVP ⏳

- ⏳ CRUD raportów: `TrainingSession` (niebieski) i `ClientSelfReport` (pomarańczowy) jako osobne kolekcje
- ⏳ Upload 1 zdjęcia / wideo do raportu (nowy interfejs bridge'a dla Firebase Storage)
- ⏳ Kompresja wideo na urządzeniu (480–720p, limit 5 min)
- ⏳ Widok raportów: lista chronologiczna ↔ kalendarz (wspólny dla trenera i klienta)
- ⏳ Zbiorczy kalendarz trenera (wszyscy klienci)
- ⏳ Push przy nowym / edytowanym raporcie (FCM)
- ⏳ Lista powiadomień w aplikacji

## Faza 2 — dopracowanie ⏳

- ⏳ Miniatury zdjęć i wideo
- ⏳ Ekran „porządki” + kwartalne zbiorcze przypomnienie o starych filmach (usuń / zachowaj / nie przypominaj)
- ⏳ Zapis kto i kiedy edytował opis (`lastEditedBy`, `updatedAt`)
- ⏳ Zarządzanie klientami: archiwizacja / usuwanie kont
- ⏳ Edycję `TrainingSession` może robić tylko trener, a w `ClientSelfReport` trener może dodać komentarz
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
| 2026-10-04 | Koin multiplatform zmergowany na branchu (`a536c83`), dodany test grafu Koina. Decyzja: migracja na Koin multiplatform + testy jako kolejny krok. Powstała ta roadmapa; testy przelogowania OK; `feature/client-list` zmergowane do `main` (`c68900e`) |
