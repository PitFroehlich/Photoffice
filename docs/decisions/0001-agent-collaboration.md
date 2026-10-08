# 0001: Repository as shared memory for multiple agents

- **Status:** Accepted
- **Date:** 2026-10-08

## Context
Several coding agents (Claude Code, Junie) work on Photoffice from different machines.
Their local memories are not shared, so they cannot know what the others have done.

## Decision
- The repository and GitHub are the only shared memory.
- `AGENTS.md` in the root holds stable rules incl. the collaboration protocol; `CLAUDE.md` and `.junie/AGENTS.md` point to it.
- Tasks are GitHub Issues; the label `in-progress` marks claimed work, `blocked` marks waiting work.
- One branch per issue, PRs against `main`, never push directly to `main`.
- Session log in `docs/journal/` (one file per session to avoid merge conflicts), decisions in `docs/decisions/`.
- A Claude Code `SessionStart` hook (`.claude/hooks/session-start.sh`) shows sync state, open issues and the latest journal entries at session start.

## Consequences
- Every session costs a little overhead (claiming issues, writing a journal entry).
- Knowledge survives across machines and agents and is reviewable in Git history.
- Requires `gh` to be installed and authenticated on every machine.
