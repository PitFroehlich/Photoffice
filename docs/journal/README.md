# Agent journal

One file per agent session, so parallel work on different machines never conflicts.

**File name:** `YYYY-MM-DD-<hostname>-<topic>.md` (e.g. `2026-10-08-fin-de-nb-0072-agent-setup.md`)

Keep it short. Template:

```markdown
# <Topic>

- **Date:** YYYY-MM-DD
- **Machine / Agent:** <hostname> / <Claude Code, Junie, human, ...>
- **Issue / PR:** #<nr> / #<nr>
- **Branch:** <branch>

## Goal
What was supposed to be achieved.

## Done
What was actually changed (files, behaviour).

## Open / Next steps
What is left, what the next agent should pick up.

## Pitfalls
Surprises, things that did not work, environment quirks.
```
