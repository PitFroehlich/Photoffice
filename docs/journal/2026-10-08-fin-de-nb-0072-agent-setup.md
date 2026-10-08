# Fork und Struktur für Multi-Agenten-Zusammenarbeit

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0072 / Claude Code
- **Issue / PR:** #1
- **Branch:** chore/agent-collaboration

## Goal
Eigener Fork für die Weiterentwicklung und eine Struktur, mit der Agenten auf verschiedenen Rechnern wissen, was die anderen gemacht haben.

## Done
- Fork `PitFroehlich/Photoffice` angelegt; `origin` = Fork, `upstream` = `stargazer74/Photoffice`.
- Im Fork `master` → `main` umbenannt (Default-Branch). Upstream nutzt weiterhin `master`.
- Issues im Fork aktiviert, Labels `in-progress` und `blocked` angelegt.
- `.junie/AGENTS.md` → `AGENTS.md` (Root) verschoben, Abschnitt 0 „Collaboration Protocol“ ergänzt; `CLAUDE.md` und `.junie/AGENTS.md` verweisen darauf.
- `docs/journal/` und `docs/decisions/` (inkl. ADR 0001) angelegt.
- Claude-Code-`SessionStart`-Hook `.claude/hooks/session-start.sh` (Git-Stand, offene Issues/PRs, letzte Journal-Einträge).

## Open / Next steps
- Konkrete Aufgaben als Issues anlegen.
- Auf weiteren Rechnern: `gh` installieren, `gh auth login`, `gh repo clone PitFroehlich/Photoffice`.

## Pitfalls
- Bei Forks sind Issues standardmäßig deaktiviert (`gh repo edit --enable-issues`).
- `gh repo fork --remote` schlägt fehl, wenn `origin` schon existiert – Remotes manuell umbenennen.
- Beim Klonen ohne `gh` fehlt `upstream`: `git remote add upstream https://github.com/stargazer74/Photoffice.git`.
