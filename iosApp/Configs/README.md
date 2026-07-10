# iOS env configurations

These xcconfigs make the iOS app env-aware and keep the Kotlin framework's baked env in lockstep with
the selected Xcode scheme — the iOS counterpart of the Android flavor lock.

## What is wired (in git)

- **`Regtest.xcconfig` / `Signet.xcconfig` / `Mainnet.xcconfig`** — each sets `SATSCHAT_ENV`,
  `PRODUCT_NAME`, `PRODUCT_BUNDLE_IDENTIFIER` (`.rt` / `.signet` / none — the three installs coexist),
  and `SATSCHAT_DEEPLINK_SCHEME`.
- **The "Compile Kotlin Framework" build phase** already passes `-Penv=${SATSCHAT_ENV:-regtest}` to
  Gradle (see `project.pbxproj`), so the framework's BuildKonfig matches the scheme.
- **`AppConfig.assertEnvMatches(expected)`** (in `:core:config`) is the runtime guard.

## Remaining Xcode-side wiring (do in Xcode — pbxproj surgery by hand is error-prone)

1. **Build configurations (6).** Duplicate the `Debug`/`Release` configs into
   `Debug-Regtest`, `Release-Regtest`, `Debug-Signet`, `Release-Signet`, `Debug-Mainnet`,
   `Release-Mainnet`, and set each one's *Base Configuration* to the matching env xcconfig above
   (both Debug-Regtest and Release-Regtest → `Regtest.xcconfig`, etc.).
2. **Schemes (3, shared).** Create shared schemes `SatsChat-Regtest`, `SatsChat-Signet`, and
   `SatsChat` (Run/Test → the `*-Regtest`/`*-Signet` debug config; `SatsChat` uses `Release-Mainnet`
   for Archive). Mark them **Shared** so they're tracked in git (the current scheme lives in
   `xcuserdata`, which is gitignored).
3. **Deeplink + env key in `Info.plist`.** Add:
   - `CFBundleURLTypes` → one URL type with `CFBundleURLSchemes = [$(SATSCHAT_DEEPLINK_SCHEME)]`.
   - A custom key `SATSCHAT_ENV` = `$(SATSCHAT_ENV)` so the value is readable at runtime.
4. **Call the runtime guard** from `iOSApp.swift` on launch:
   ```swift
   let env = Bundle.main.object(forInfoDictionaryKey: "SATSCHAT_ENV") as? String ?? ""
   AppConfig().assertEnvMatches(expected: env)   // crashes on mismatch
   ```
5. **Per-env AppIcon** (optional polish) and separate APNs entitlements per bundle id (regtest may
   omit push).

Once step 2 lands, the `ios` job in `.github/workflows/ci.yml` auto-enables the `xcodebuild build`
scheme checks (it keys off `SatsChat-Regtest.xcscheme` existing).

Deployment target must stay **≥ iOS 15.0** (lightning-kmp minimum); the project is currently 18.2.
