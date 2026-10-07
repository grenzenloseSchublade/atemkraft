## Was und warum

<!-- Kurz: Was ändert sich für Nutzer? Warum? Issue verknüpfen mit „Fixes #123“ (GitHub schließt es dann beim Mergen). -->

## Checkliste

- [ ] `scripts/check.sh` ist lokal grün (Stil-Checks, ktlint, Unit- und Screenshot-Tests, Android Lint)
- [ ] Commit-Betreff nach CODE-08: `fix: …`, `docs: …` oder `build: …`, deutsch mit Umlauten
- [ ] Regeln aus [STYLEGUIDE.md](https://github.com/grenzenloseSchublade/atemkraft/blob/main/docs/STYLEGUIDE.md) eingehalten (Kurz-Checkliste am Anfang)
- [ ] Texte: du-Anrede, Laiensprache, keine Heil- oder Therapieversprechen (`config/health-claims.txt`), genannte Studien in `Refs.kt`
- [ ] Keine neue Netzverbindung, Berechtigung, Tracking-Bibliothek oder gespeicherten Daten – oder im selben Commit in [SECURITY.md](https://github.com/grenzenloseSchublade/atemkraft/blob/main/docs/SECURITY.md), README und [PRIVACY.md](https://github.com/grenzenloseSchublade/atemkraft/blob/main/docs/PRIVACY.md) nachgetragen
- [ ] Neue Abhängigkeit mit freier Lizenz und Eintrag in `THIRD_PARTY_LICENSES.md` (falls zutreffend)

## Screenshots

<!-- Bei sichtbaren Änderungen: vorher / nachher. Bilder aus `scripts/build.sh recordRoborazziDebug`
     (app/build/outputs/roborazzi/) oder vom Gerät. Keine persönlichen Daten zeigen. -->
