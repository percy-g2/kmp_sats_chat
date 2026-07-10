# SatsChat — project rules

Kotlin / Compose Multiplatform app targeting **Android + iOS**.

- **Modules:** `androidApp` (Android application) + a KMP module tree — `:shared` is the umbrella
  (CMP shell + Nav3/Koin wiring; produces the iOS `Shared` framework), depending on library modules:
  `:core:{crypto,model,database,config}`, `:messaging:{transport,smp}`, `:lightning`,
  `:feature:{chat,wallet,payinchat}`. Every module uses the AGP KMP library plugin
  (`androidLibrary {}`) with `commonMain`/`androidMain`/`iosMain` + `commonTest`/`androidHostTest`/`iosTest`.
  (The plan calls the umbrella `:app`; it is currently `:shared` — an optional rename is a follow-up.)
- **Environments:** `-Penv=regtest|signet|mainnet` (default regtest) is the single source of truth.
  It selects the `:core:config` BuildKonfig values and locks `:androidApp` to the matching flavor —
  **exactly one env per Gradle invocation**. Network endpoints live ONLY in `:core:config`
  (`scripts/no-hardcoded-endpoints.sh` guards the rest). Never commit real mainnet endpoints.
- **iOS entry point:** `iosApp/` (Xcode project; add SwiftUI here).
- **Package / applicationId:** `com.androdevlinux.satschat`.
- **Toolchain:** Kotlin 2.4.0, AGP 9.0.1, Compose Multiplatform 1.11.1, minSdk 24 / compileSdk 36.
  Dependencies are managed in the version catalog `gradle/libs.versions.toml`.

## Build & test

| Task | Command |
|------|---------|
| Verify env matrix | `./gradlew verifyEnvMatrix -Penv=regtest` |
| Android debug build (regtest) | `./gradlew :androidApp:assembleRegtestDebug -Penv=regtest` |
| Unit tests (JVM host, all modules) | `./gradlew testAndroidHostTest -Penv=regtest` |
| iOS tests | `./gradlew iosSimulatorArm64Test -Penv=regtest` |
| iOS framework | `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -Penv=regtest` |
| Endpoint guard | `bash scripts/no-hardcoded-endpoints.sh` |
| iOS app | open `iosApp/` in Xcode and run |

There is no Desktop/JVM target, so JVM-host testing runs via `testAndroidHostTest` (not `jvmTest`).
`:androidApp` is locked to the `-Penv` flavor, so build the flavor whose env you pass (e.g.
`-Penv=signet :androidApp:assembleSignetDebug`). `mainnetDebug` only builds with `CI=true`.

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
