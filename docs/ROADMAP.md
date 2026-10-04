# HappyPies — Roadmapa

**Legenda:** ✅ zrobione · 🔄 w trakcie · ⏳ do zrobienia · 💤 odłożone

---

## 📍 Teraz

**Faza 0 — fundament** · branch `feature/client-list`

Najbliższe kroki:
1. Zacommitować `ROADMAP.md` i `.gitignore` (`CLAUDE.md` i `plan.md` zostają lokalnie)
2. Wypchnąć `feature/client-list` (branch jest tylko lokalnie!) i zmergować do `main`
3. Decyzja: migracja na Koin multiplatform — robimy czy nie?

---

## Faza 0 — fundament 🔄

- ✅ Inicjalizacja projektu KMP
- ✅ Firebase SDK: Android natywnie, iOS przez SPM (Protocol Injection Bridge)
- ✅ Logowanie z rolami (trener / klient), wspólny UI Compose na obu platformach
- 🔄 Lista klientów i szczegóły klienta (trener) — przetestowane (także przelogowanie między kontami), czeka na merge
- ⏳ Zaproszenie klienta mailem: deep link → rejestracja/logowanie
- ⏳ Security Rules z podziałem na role (teraz: każdy zalogowany czyta `clients`)
- 💤 Koin multiplatform zamiast ręcznego przekazywania zależności (`koinViewModel()`)
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

## Historia

| Data | Co się wydarzyło |
|---|---|
| 2026-07-08 | Inicjalizacja projektu KMP |
| 2026-07-11 | Firebase SDK; decyzje: natywne SDK zamiast GitLive, wspólny UI w Compose Multiplatform |
| 2026-08-23 | Logowanie z rolami zmergowane do `main` |
| 2026-08-24 | Lista i szczegóły klientów (`75ceb94`, niezmergowane) |
| 2026-10-04 | Powstała ta roadmapa; testy przelogowania (trener/klient w każdej kombinacji) OK |
