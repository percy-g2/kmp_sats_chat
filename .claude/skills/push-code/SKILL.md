---
name: push-code
description: Commit and push the current working tree the safe way. NEVER commits or pushes to a protected branch (main, master, dev, develop, release/*) — when on one it derives a Conventional-Commits branch name from the diff, creates it, then commits and pushes; on a feature branch it commits and pushes there. Generates a proper commit title + body and previews everything before committing or pushing. Use when the user says push-code, /push-code, "commit and push", or "push my changes".
---

# push-code

**First read `.claude/skills/_conventions/CONVENTIONS.md` and follow it** (protected-branch
list, guard ladder, KMP scope map, secret denylist, branch naming, commit template, and the
"no Claude attribution" rule).

Goal: get the current working-tree changes onto a proper feature branch and pushed, with a
clean Conventional-Commits message — without ever touching a protected branch directly.

## Steps

1. **Preflight.** Run the guard ladder G1–G6 from CONVENTIONS.md §2. Handle the empty-repo case
   (§7) if `git rev-list --count --all` is 0.
2. **Current branch:** `git symbolic-ref --short -q HEAD`. On detached HEAD, create a branch at
   HEAD first (CONVENTIONS §4).
3. **Inspect + secret-scan, then stage.**
   - `git status --porcelain=v1`. If there is nothing to commit, stop and say so.
   - Check untracked + to-be-staged files against the secret denylist (§5). Refuse any match.
   - Stage: `git add -A`. Then verify with `git diff --cached --name-only` and **unstage any
     denylisted file that slipped through** (`git restore --staged <file>`). Never stage a
     `**/build/` path.
4. **Analyze the diff** for the message and (maybe) the branch:
   `git diff --cached --stat` and `git diff --cached` (summarize large diffs by file).
5. **Protected-branch decision.**
   - If the current branch is protected (`main`/`master`/`dev`/`develop`/`release/*`): derive a
     branch name (§4) from the diff and `git switch -c <branch>` (this carries the staged
     changes over). If the name exists, reuse it or suffix `-2`.
   - Otherwise: keep the current feature branch.
6. **Compose the commit** using the Conventional-Commits template (§6). No `Co-Authored-By` line.
7. **PREVIEW (gate A).** Show the target branch, the commit title, the full body, and the staged
   file list, then proceed — do not wait for confirmation.
8. **Commit:** write the message to a temp file and `git commit -F <tmpfile>`.
9. **Push (gate B — outward/irreversible; proceed after previewing, no confirmation).**
   - First push of the branch: `git push -u origin HEAD`.
   - Upstream already set: `git push`.
   - No-upstream error: fall back to `git push -u origin HEAD`.
   - Non-fast-forward rejection: `git fetch origin` then `git pull --rebase origin <branch>`; if
     clean, re-push; on conflicts, stop and hand back. Never `--force`; only `--force-with-lease`
     on explicit request.
10. **Report** the branch name and a compare/PR link (derive from `git remote get-url origin`).
    Offer to run `/create-pr` next.
