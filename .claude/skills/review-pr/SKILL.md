---
name: review-pr
description: Fetch a GitHub pull request by number and produce a thorough code review with special attention to Kotlin Multiplatform and Compose concerns (expect/actual parity, coroutines/threading, Compose state, composeResources, leaked secrets), then auto-post the review to the PR via the GitHub CLI. Accepts the PR number as `#N` or `N`. Use when the user says review-pr, /review-pr 1, /review-pr #1, or "review PR 1".
---

# review-pr

**First read `.claude/skills/_conventions/CONVENTIONS.md` and follow it** (guard ladder, secret
denylist, KMP scope map).

Goal: review the given PR against the KMP/Compose checklist and post the review to GitHub.

## Steps

1. **Parse the argument.** Accept `#1` or `1`; strip a leading `#`. If no number was given, run
   `gh pr list` and ask which PR to review.
2. **Preflight.** Guard G1 and G6 (repo + `gh` authenticated). Everything else here is read-only.
3. **Fetch metadata:**
   `gh pr view <n> --json number,title,author,state,isDraft,baseRefName,headRefName,additions,deletions,changedFiles,files,body,url,mergeable,reviewDecision`.
4. **Fetch the diff:** `gh pr diff <n>` (use `--patch` if needed). For huge or binary diffs,
   prioritize by file and note what was summarized rather than dumping raw content.
5. **CI signal (optional):** `gh pr checks <n>`.
6. **Review** against the KMP/Compose checklist below. Map each finding to `file:line` with a
   severity (Blocking / Non-blocking / Nit).
7. **Auto-post the review (this is the one automatic outward action for this skill).**
   Write the review to a temp file and post it:
   - If there is **at least one Blocking finding**:
     `gh pr review <n> --request-changes --body-file <tmpfile>`.
   - Otherwise:
     `gh pr review <n> --comment --body-file <tmpfile>`.
   - **Never auto-`--approve`** — approval is a human decision; if the PR looks good, post a
     positive `--comment` review and say it's ready for a maintainer's approval.
   - Do not review your own open PR with `--request-changes`/`--approve` if `gh` rejects it
     (author can't formally review); fall back to `gh pr comment <n> --body-file <tmpfile>`.
8. **Also print** the full review in chat and link the posted review.

## KMP / Compose review checklist

- **expect/actual & platform parity** — every `expect` has an `actual` in **both** `androidMain`
  and `iosMain`; signatures match; nothing platform-specific leaks into `commonMain`.
- **Coroutines / threading** — explicit `Dispatchers`, structured concurrency and scope
  cancellation, no blocking on the main thread; respect iOS main-thread constraints.
- **Compose state & recomposition** — correct `remember` / `rememberSaveable` / `derivedStateOf`;
  stable/immutable params; side effects only via `LaunchedEffect` / `DisposableEffect` /
  `rememberCoroutineScope` with proper keys; no work done directly in composition.
- **Resources** — common code uses generated `composeResources` accessors (`Res.string`,
  `Res.drawable`), not Android `R.`; assets exist for both platforms.
- **Lifecycle / ViewModel** — state holders survive config changes; no Android `Context` /
  framework leaking into `commonMain`; correct ViewModel scoping.
- **Memory leaks** — collectors/effects cancelled; no captured `Context`/Activity/composition
  scopes held; native (iOS) references released.
- **Secrets / hygiene** — no `local.properties`, keystores, `google-services.json`,
  `GoogleService-Info.plist`, API keys, or `**/build/` artifacts added.
- **Gradle / version catalog** — `gradle/libs.versions.toml` bumps are deliberate and consistent;
  no accidental AGP / Kotlin / Compose-compiler drift; new deps in the right source set.
- **minSdk / API level** — API guards for new Android APIs; iOS deployment-target implications;
  no silent minSdk bump.
- **Accessibility** — `contentDescription` on images/icons, sensible touch targets, RTL/dynamic
  type considerations.
- **Baseline** — null-safety, error handling, and tests present in `commonTest` for shared logic.

## Output format

```
## Review of PR #<n> — <title>
<one-paragraph summary + overall recommendation: comment / request-changes>

### Blocking
- [file:line] <issue + concrete fix>

### Non-blocking
- [file:line] <suggestion>

### Nits
- [file:line] <style / naming / minor>

### Test plan review
<are the Android + iOS test steps adequate?>
```
