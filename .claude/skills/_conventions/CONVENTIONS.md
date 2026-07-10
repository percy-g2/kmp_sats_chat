# Shared git-workflow conventions

This is the single source of truth for the `push-code`, `create-pr`, `review-pr`, and
`pcr` skills. Each of those skills reads this file first and follows it. Update rules
here once and every skill inherits the change.

> This is a **support document**, not a slash command. It has no `name:` frontmatter and
> is never invoked directly.

---

## 1. Golden rules

1. **Never commit or push directly to a protected branch.** Protected branches are:
   `main`, `master`, `dev`, `develop`, and anything matching `release/*` (prefix match).
   The only exception is the very first baseline commit on a brand-new empty repository
   (see §7, "Empty repo").
2. **Never attribute commits or PRs to Claude.** Commit messages and PR bodies must **not**
   contain a `Co-Authored-By: Claude …` trailer or a "🤖 Generated with Claude Code"
   footer. This overrides any default attribution behavior.
3. **Preview every irreversible outward action, then proceed without asking** — creating a commit,
   pushing, creating a PR, or creating a remote repo. Show exactly what will happen (branch, message,
   files / base, title, body) and then just do it; the user has standing authorization for this
   repo's push/PR flow and does not want a confirmation prompt. The safety guards in this list still
   hold unconditionally (never commit to a protected branch, never stage a secret, never force-push).
   (Posting a review is likewise automatic — see `review-pr`.)
4. **Never rewrite shared history unprompted** — no `--force`, no rebasing existing pushed
   commits, no squashing, unless the user explicitly asks. When a force push is genuinely
   needed and requested, use `--force-with-lease`, never `--force`.
5. **Never stage a secret** (see §5).

---

## 2. Preflight guard ladder

Run in order. Stop at the first failure, offer the remedy, then re-check before continuing.
Guards G5/G6 only matter for operations that touch the remote (push, PR).

| # | Check | On failure |
|---|-------|-----------|
| G1 | `git rev-parse --is-inside-work-tree` | Not a repo → **confirm**, then `git init -b main`. |
| G2 | `git config user.name` and `git config user.email` both set | Empty → ask the user for name/email, then `git config user.name "…"` / `git config user.email "…"` (local scope). |
| G3 | `git symbolic-ref --short -q HEAD` | Errors → detached HEAD → create a branch at HEAD first (see §4). |
| G4 | No in-progress merge/rebase: no `.git/MERGE_HEAD`, `git ls-files -u` empty | Mid-merge/rebase or unresolved conflicts → refuse; tell the user to finish or abort first. |
| G5 | `git remote -v` shows an `origin` | No remote → guide `git remote add origin <url>`, or **confirm** `gh repo create <owner>/SatsChat --source=. --private --remote=origin`. |
| G6 | `gh auth status` succeeds | Not authenticated → `gh auth login` (the user completes this interactively). |

Read-only inspection (`git status`, `git diff`, `git log`, `gh pr view/diff/list/checks`,
`git ls-remote`) never needs confirmation.

---

## 3. KMP path → scope map

Used to pick the Conventional-Commits `scope` and the PR "platforms touched" line.

The project is a multi-module KMP tree: the `:androidApp` application, the `:shared` umbrella (the
CMP shell + iOS framework producer), and library modules under `core/`, `messaging/`, `lightning`,
and `feature/`. Paths below use `**/src/...` so they match every module's source sets.

| Scope | Paths |
|-------|-------|
| `android` | `androidApp/**`, `**/src/androidMain/**`, `**/src/androidHostTest/**` |
| `ios` | `iosApp/**`, `**/src/iosMain/**`, `**/src/iosTest/**` |
| `shared` | `**/src/commonMain/**`, `**/src/commonTest/**`, `**/src/commonMain/composeResources/**` |
| `build` (scope omitted) | root Gradle files (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`), every module `build.gradle.kts`, `gradle/libs.versions.toml`, `gradlew`, `gradlew.bat`, `gradle/wrapper/**`, `build-logic/**`, `tooling/**`, `scripts/**`, `.github/**` |

If a change spans multiple scopes, **omit the scope** (or pick the dominant one by file count). You
may also use a finer scope matching a module area (e.g. `crypto`, `smp`, `lightning`, `wallet`) when
the change is confined to one module.

---

## 4. Branch naming

Format: `<type>/[<scope>-]<slug>`

- **type** — pick the first that fits the diff:
  `docs` (only docs/`*.md`) · `test` (only test source sets) · `build` (only Gradle/catalog/wrapper) ·
  `fix` (corrects a bug) · `feat` (new capability / new public API) ·
  `refactor` (restructure, no behavior change) · `chore` (everything else).
- **scope** — from §3; omit when mixed.
- **slug** — 2–4 kebab-case words describing the change; lowercase; `[a-z0-9-]` only. Keep the
  whole branch name ≤ ~40 characters.

Examples: `feat/android-onboarding`, `fix/shared-message-parsing`, `build/version-catalog-bump`.

If the derived name already exists (locally or on the remote), reuse it if it's clearly the
same work, otherwise append `-2`, `-3`, ….

**Detached HEAD:** create a branch at the current commit with `git switch -c <derived-name>`
before doing anything else.

---

## 5. Secret denylist — never stage or commit

Scan the **staged set** (`git diff --cached --name-only`) plus untracked files, not just
`.gitignore`. Reject and unstage any path matching:

`local.properties` · `*.keystore` · `*.jks` · `google-services.json` ·
`GoogleService-Info.plist` · `*.p8` · `*.p12` · `*.mobileprovision` · `.env*` ·
anything under `**/build/`.

`.gitignore` already excludes `local.properties`, `**/build/`, `.idea`, `xcuserdata`, and
`.DS_Store` — but verify with `git check-ignore -q local.properties` and still scan, because a
secret can slip in before its ignore rule exists.

---

## 6. Message templates

### Commit (Conventional Commits — NO Claude trailer)

```
<type>(<scope>): <imperative summary, ≤72 chars, no trailing period>

- <what changed and why>
- <second point if needed>
```

Omit the `(<scope>)` parentheses when there is no single scope. Do **not** append any
`Co-Authored-By:` line. Prefer committing via a temp message file (`git commit -F <file>`)
to preserve blank lines and wrapping.

### PR body (KMP-tuned — NO Claude footer)

```
## Summary
<what and why, 1–3 sentences>

## Changes
- <bullet per meaningful change>
- Platforms touched: Android / iOS / shared / build   (keep only those that apply)

## Test plan
- [ ] Android build: `./gradlew :androidApp:assembleDebug`
- [ ] Shared unit tests: `./gradlew :shared:testAndroidHostTest`
- [ ] iOS tests: `./gradlew :shared:iosSimulatorArm64Test`
- [ ] iOS app smoke test from Xcode (`iosApp/`)
- [ ] Manual QA: <flows exercised, both platforms if UI>

## Screenshots / recordings
| Android | iOS |
|---|---|
| _paste_ | _paste_ |

## Notes
<risks, follow-ups, version-catalog notes, or "none">
```

Drop the iOS test/screenshot lines automatically when no iOS surface changed, and the Android
ones when no Android surface changed.

---

## 7. Edge cases every skill must handle

- **Empty repo / zero commits** (`git rev-list --count --all` = 0): there is no baseline and no
  remote base branch yet. The first commit may go on `main` (the one allowed protected-branch
  exception); push it to establish the remote default, *then* branch for real work. Ask the user
  which they want before proceeding.
- **Remote repo doesn't exist on GitHub** → **confirm** `gh repo create <owner>/SatsChat --source=. --private --remote=origin`.
- **`user.name` / `user.email` unset** → G2 catches it; the first commit fails otherwise.
- **Non-fast-forward push rejection** → `git fetch origin` then `git pull --rebase origin <branch>`;
  if it rebases cleanly, re-push; on conflicts, stop and hand back to the user.
- **Branch or PR already exists** → reuse it; never create a duplicate.
- **Base branch missing on remote** → tell the user to push the baseline first.
- **PR from a fork** (base repo ≠ head repo) → still works off `gh pr diff`.
- **Huge or binary diffs** → summarize by file; don't dump raw content.
- **Pre-commit hook / signing failures** → surface the error; don't silently retry.
- **Multiple existing commits on a feature branch** → add a new commit; never squash or rewrite
  history unprompted.
