#!/usr/bin/env bash
# SessionStart hook: give the agent a quick overview of what other agents/machines have done.
# Output on stdout is added to Claude's context. Never fail the session start.

cd "${CLAUDE_PROJECT_DIR:-$(dirname "$0")/../..}" || exit 0

echo "## Team-Kontext (automatisch, siehe AGENTS.md Abschnitt 0)"
echo "Maschine: $(hostname)"

echo
echo "### Git"
timeout 15 git fetch --all --prune --quiet 2>/dev/null || echo "(git fetch fehlgeschlagen/offline)"
branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
echo "Branch: $branch"
if git rev-parse --abbrev-ref '@{u}' >/dev/null 2>&1; then
  read -r behind ahead < <(git rev-list --left-right --count '@{u}...HEAD' 2>/dev/null)
  echo "Gegenüber Upstream-Branch: $behind hinter, $ahead voraus"
  [ "${behind:-0}" -gt 0 ] && echo "ACHTUNG: Branch ist nicht aktuell – zuerst 'git pull'."
fi
if git rev-parse --verify --quiet origin/main >/dev/null; then
  echo "Neueste Commits auf origin/main:"
  git log origin/main --oneline -5 2>/dev/null | sed 's/^/  /'
fi
changes=$(git status --porcelain 2>/dev/null | wc -l)
[ "$changes" -gt 0 ] && echo "Lokale, nicht committete Änderungen: $changes Datei(en)"

echo
echo "### Offene Issues"
if command -v gh >/dev/null 2>&1; then
  timeout 15 gh issue list --state open --limit 20 \
    --json number,title,labels,assignees \
    --template '{{if not .}}  (keine){{"\n"}}{{end}}{{range .}}  #{{.number}} {{.title}}{{range .labels}} [{{.name}}]{{end}}{{range .assignees}} @{{.login}}{{end}}{{"\n"}}{{end}}' \
    2>/dev/null || echo "(gh issue list fehlgeschlagen)"
  echo "Offene PRs:"
  timeout 15 gh pr list --state open --limit 10 --json number,title,headRefName \
    --template '{{if not .}}  (keine){{"\n"}}{{end}}{{range .}}  #{{.number}} {{.title}} ({{.headRefName}}){{"\n"}}{{end}}' 2>/dev/null || echo "(gh pr list fehlgeschlagen)"
else
  echo "(gh nicht installiert)"
fi

echo
echo "### Letzte Journal-Einträge (docs/journal/)"
ls -1 docs/journal/ 2>/dev/null | grep -v '^README.md$' | sort -r | head -5 | sed 's/^/  /'
echo
echo "Vor Arbeitsbeginn: relevante Journal-Einträge lesen, Issue claimen (Label in-progress)."
exit 0
