---
name: pcr
description: One-shot "push, PR, review" orchestrator — runs push-code, then create-pr, captures the new PR number, then review-pr on it. Previews each irreversible action (commit, push, PR creation) then proceeds without asking; auto-posts the review; never merges. Use when the user says pcr, /pcr, "push create and review", or "do the whole PR flow".
---

# pcr

**First read `.claude/skills/_conventions/CONVENTIONS.md` and follow it.** This skill runs the
`push-code`, `create-pr`, and `review-pr` skills in sequence — follow each one's `SKILL.md` in
full, including its previews (which proceed without confirmation).

Goal: take the current working-tree changes all the way to a reviewed PR in one flow.

## Steps

1. **Announce the plan** so the user knows the sequence: push-code → create-pr → review-pr.
2. **Stage 1 — push-code.** Execute the `push-code` skill, including its previews. If it
   stops for any reason (nothing to commit, unresolved conflict, auth failure),
   stop `pcr` and report where it halted.
3. **Stage 2 — create-pr.** Execute the `create-pr` skill on the branch push-code produced,
   including its confirm gate.
   - **Capture the PR number.** Prefer a deterministic query right after creation:
     `gh pr view --json number -q .number` (run on the current branch). Fallback:
     `gh pr list --head <branch> --state open --json number -q '.[0].number'`.
   - If create-pr reused an existing open PR, take that number.
4. **Stage 3 — review-pr.** Run the `review-pr` skill with the captured number. It auto-posts the
   review per its own policy (`--comment`, or `--request-changes` if there are Blocking findings).
   Present the review in chat.
5. **Stop.** Do **not** merge and do **not** take any further action. Report the branch, the PR
   URL/number, and the review outcome.

If the PR number can't be determined after Stage 2, stop before Stage 3 and ask the user for it
rather than reviewing the wrong PR.
