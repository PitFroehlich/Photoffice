# 0009: Keycloak-Theme "photoffice" – nur Deutsch, Standard-Theme plus CSS

- **Status:** Accepted
- **Date:** 2026-10-08

## Context
Seit #24 bekommen neue Studio-Admins eine Einladung von Keycloak und legen ihr Passwort auf Keycloak-Seiten fest.
Seiten und Mails waren englisch und im Keycloak-Design ("Update Your Account", "Click here to proceed"). Für Studios
als zahlende Kunden wirkt das unfertig (#36). Keycloak-Themes lassen sich auf drei Arten anpassen: komplett eigene
Templates, das React-basierte Keycloakify oder ein Theme, das vom Standard-Theme erbt und nur Styles, Texte und
einzelne Templates überschreibt.

## Decision
- **Ein Theme `photoffice` als Code** in `infra/keycloak/themes/photoffice/` mit den Typen `login` und `email`; der
  Realm setzt `loginTheme` und `emailTheme`. Eingebunden per Volume (`docker-compose.yml`) bzw. Kopie
  (Testcontainers, `DevKeycloakContainer`).
- **Login: erbt von `keycloak.v2`** (PatternFly 5). Das Design entsteht nur über `css/photoffice.css`: PatternFly-
  Variablen auf die Photoffice-Farben (Petrol `#1F4E5F`, Kupfer `#C8794A` als Akzent oben an der Karte, warmes
  Neutral als Hintergrund), Schrift Inter (dieselbe Datei wie im Frontend, selbst gehostet, kein CDN), runde Buttons
  wie im Frontend, Logo = Kamera-Symbol (Material Symbols, wie in der App) + Realm-Name. Dark Mode aus, weil auch die
  App standardmäßig hell ist. Kein Build-Schritt, kein Keycloakify.
- **Nur ein Template kopiert:** `info.ftl` (Begrüßung "Willkommen bei Photoffice!" beim Einladungslink, Aktion als
  Button). Alle anderen Seiten bleiben Keycloak-Templates; Texte werden über `messages_de.properties` angepasst.
- **Nur Deutsch:** `supportedLocales` = `de`, `defaultLocale` = `de`. Mit Englisch als zweiter Sprache würde Keycloak
  bei englischem Browser englisch anzeigen und einen Sprachumschalter einblenden – die App selbst ist aber nur
  deutsch. Kommt eine zweite Sprache für die App, wird sie hier ergänzt (`messages_<lang>.properties`).
- **E-Mail: erbt von `base`**, eigenes Layout (`html/template.ftl`, nur Inline-Styles wegen Mail-Clients) und eigene
  Templates für `executeActions` und `password-reset` (HTML + Text). Die `executeActions`-Mail ist in Photoffice die
  **Einladung** eines neuen Studio-Admins (einziger Aufrufer: `StudioOnboarding`) und ist entsprechend formuliert.
  Absendername bleibt `smtpServer.fromDisplayName` = "Photoffice".

## Consequences
- Keycloak-Updates bringen Verbesserungen der Standard-Seiten automatisch mit. Risiko: geänderte CSS-Klassen oder
  Variablen in `keycloak.v2`, Änderungen an `base/login/info.ftl` → nach jedem Keycloak-Update die Demo
  `docs/demos/0036-keycloak-deutsch.md` einmal durchgehen.
- Element-IDs der Keycloak-Seiten (`#username`, `#kc-login`, `#password-new`, …) bleiben stabil; E2E-Tests nutzen sie.
- Der Studioname steht nicht in der Einladung: Keycloak kennt beim Versand nur Benutzer und Realm. Dafür bräuchte es
  ein Benutzerattribut (Declarative User Profile) oder eine eigene Mail aus dem Backend.
- Wird `execute-actions-email` später für andere Zwecke genutzt (z. B. Passwortänderung erzwingen), passt der
  Einladungstext nicht mehr – dann Text verallgemeinern oder unterscheiden.
- Deployment (#28): Das Theme-Verzeichnis muss mit Keycloak ausgeliefert werden (Volume oder eigenes Image mit
  `/opt/keycloak/themes/photoffice`). Im Produktionsmodus cacht Keycloak Themes; Änderungen brauchen einen Neustart.
