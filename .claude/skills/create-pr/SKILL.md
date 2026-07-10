---
name: create-pr
description: Open a GitHub pull request from the current branch against the correct protected base branch (usually the repo default — main/master/dev/develop), with a structured, KMP-aware title and description. Refuses to open a PR from a protected branch, reuses an existing open PR instead of duplicating, and previews the title/body/base before creating. Use when the user says create-pr, /create-pr, "open a PR", or "raise a pull request". Accepts an optional base-branch override, e.g. /create-pr dev.
---

# create-pr

**First read `.claude/skills/_conventions/CONVENTIONS.md` and follow it** (protected branches,
guard ladder, KMP scope map, PR body template, and the "no Claude attribution" rule).

Goal: open a well-formed PR from the current feature branch to the right base branch, using
`gh`.

## Steps

1. **Preflight.** Guard ladder G1, G3, G5, G6 (must be a repo, a real branch, have a remote, and
   be `gh`-authenticated).
2. **Current branch:** `git symbolic-ref --short -q HEAD`. If it is a protected branch, **stop**:
   tell the user to create a feature branch first (run `/push-code`).
3. **Ensure the branch is on the remote with an upstream.** Check
   `git rev-parse --abbrev-ref --symbolic-full-name @{u}` and `git status -sb`. If there is no
   upstream or local is ahead, push first with `git push -u origin HEAD` (**confirm** — outward).
4. **Determine the base branch:**
   - Default: `gh repo view --json defaultBranchRef -q .defaultBranchRef.name`.
   - Honor an optional override argument (e.g. `/create-pr dev`).
   - Guard: base ≠ head, and the base must exist on the remote
     (`git ls-remote --heads origin <base>`). If the default branch isn't on the remote yet
     (fresh repo), tell the user to push the baseline first (CONVENTIONS §7).
5. **Reuse check:** `gh pr list --head <branch> --state open --json number,url`. If a PR is
   already open for this branch, offer to view/update it instead of creating a duplicate.
6. **Gather context** for the body:
   `git log --oneline <base>..HEAD`, `git diff --stat <base>...HEAD` (three-dot), and which KMP
   surfaces changed (android / ios / shared / build via the §3 path map).
7. **Compose** the title (Conventional Commits, same style as the commit) and the body from the
   PR template (§6). Drop the iOS or Android test/screenshot rows when that surface is untouched.
   No "Generated with Claude Code" footer.
8. **PREVIEW + CONFIRM (gate — outward/irreversible).** Show base, head, title, and full body.
   Wait for an explicit "yes".
9. **Create:** write the body to a temp file and
   `gh pr create --base <base> --head <branch> --title "<title>" --body-file <tmpfile>`
   (add `--draft` only if the user asked).
10. **Report** the PR URL and number. When invoked by `/pcr`, surface the number as the final
    output so the orchestrator can pass it to `/review-pr`.
