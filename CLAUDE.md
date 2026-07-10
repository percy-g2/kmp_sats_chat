# SatsChat — project rules

Kotlin / Compose Multiplatform app targeting **Android + iOS**.

- **Modules:** `androidApp` (Android application), `shared` (KMP library: `commonMain`,
  `androidMain`, `iosMain`, plus `commonTest` / `androidHostTest` / `iosTest`).
- **iOS entry point:** `iosApp/` (Xcode project; add SwiftUI here).
- **Package / applicationId:** `com.androdevlinux.satschat`.
- **Toolchain:** Kotlin 2.4.0, AGP 9.0.1, Compose Multiplatform 1.11.1, minSdk 24 / compileSdk 36.
  Dependencies are managed in the version catalog `gradle/libs.versions.toml`.

## Build & test

| Task | Command |
|------|---------|
| Android debug build | `./gradlew :androidApp:assembleDebug` |
| Shared unit tests (Android host) | `./gradlew :shared:testAndroidHostTest` |
| iOS tests | `./gradlew :shared:iosSimulatorArm64Test` |
| iOS app | open `iosApp/` in Xcode and run |

## Git workflow (enforced by the skills)

- **Never commit or push directly to a protected branch:** `main`, `master`, `dev`, `develop`,
  `release/*`. Work happens on `type/scope-slug` branches.
- **Use Conventional Commits** for commit titles and PR titles (`feat`, `fix`, `chore`, `docs`,
  `refactor`, `test`, `build`).
- **Never attribute commits or PRs to Claude** — no `Co-Authored-By: Claude …` trailer and no
  "🤖 Generated with Claude Code" footer, anywhere.
- **Use the skills** instead of raw git for these flows:
  - `/push-code` — safe commit + push (auto-branches off a protected branch).
  - `/create-pr` — open a PR to the correct base branch.
  - `/review-pr <n>` — review PR #n via `gh` and post the review.
  - `/pcr` — push-code → create-pr → review-pr, end to end.
- Preview + confirm before any push or PR creation. Never force-push or rewrite pushed history
  unless explicitly asked (then `--force-with-lease` only).
- **Never commit secrets:** `local.properties`, keystores (`*.jks`/`*.keystore`),
  `google-services.json`, `GoogleService-Info.plist`, `*.p8`/`*.p12`/`*.mobileprovision`,
  `.env*`, or anything under `**/build/`.

## Code conventions

- Put shared business/UI logic in `shared/src/commonMain`; drop to `androidMain` / `iosMain`
  only for platform APIs, wired via `expect` / `actual`. Keep every `expect` matched by an
  `actual` on **both** platforms.
- UI is Compose Multiplatform (`material3`). Reference assets/strings through the generated
  `composeResources` accessors (`Res.string`, `Res.drawable`) in common code — not Android `R.`.
- Don't leak Android framework types (`Context`, `Activity`) into `commonMain`.
- Handle coroutines with explicit `Dispatchers` and structured concurrency; respect iOS
  main-thread constraints for UI/native calls.
