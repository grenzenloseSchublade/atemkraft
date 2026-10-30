#!/usr/bin/env python3
"""Statische Stil-Checks zu docs/STYLEGUIDE.md (erzeugte Tabelle in docs/CHECKS.md),
Sicherheits-Checks (``sec-*``) aus docs/SECURITY.md (Automatische Prüfung) und Doku-Lints
(``rule-length``, ``check-table``, ``catalog-sync``, ``version-sync``).

Jeder Check zählt Treffer pro Datei. Bekannte Altlasten stehen in
config/style-baseline.txt (``check<TAB>datei<TAB>anzahl<TAB>S-ID``):
  * mehr Treffer als Baseline       → Fehler (neuer Verstoß, bei SOLL nur Warnung)
  * weniger Treffer als Baseline    → Fehler „Baseline senken“ (baseline-stale, PROZ-02)
  * Zeile ohne S-ID                 → Fehler (baseline-ids, §13.2: Altlast ≠ Ausnahme)
Kommentarzeilen (``//``, ``/*``, ``*``) zählen nicht, außer bei todo-unlinked; ebenso Zeilen
mit ``// Abweichung <ID>:`` oder ``// dekorativ:``. Die ``sec-*``-Checks haben keine Baseline
(Null-Toleranz): Jeder Treffer ist ein Fehler, ein Baseline-Eintrag für sie ebenfalls.

Aufrufe:
  scripts/check-style.sh                     # prüfen
  scripts/check-style.sh --self-test         # jeder Check gegen config/style-fixtures/<check>.{pos,neg} (Pflicht)
  scripts/check-style.sh --update-baseline   # Baseline auf den Ist-Stand setzen (nur zum Senken!)
  scripts/check-style.sh --list              # Check-Tabelle (Markdown) wie in docs/CHECKS.md
  scripts/check-style.sh --update-check-table  # Tabelle in docs/CHECKS.md neu schreiben
  scripts/check-style.sh --ci-range A..B     # commit-msg je Commit, guide-sync über den Bereich (CI conventions)
"""
from __future__ import annotations

import argparse
import fnmatch
import hashlib
import os
import re
import subprocess
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = "app/src/main/java/app/atemkraft/"
RES = "app/src/main/res/"
MANIFEST = "app/src/main/AndroidManifest.xml"
JAVA = "app/src/main/java/"
BASELINE = ROOT / "config/style-baseline.txt"
STYLEGUIDE = ROOT / "docs/STYLEGUIDE.md"
# Die erzeugte Check-Tabelle steht außerhalb des Guides, damit der Regeltext kurz bleibt (STYLEGUIDE §13.3).
CHECKS_DOC = ROOT / "docs/CHECKS.md"
CHECK_TABLE_START = "<!-- check-table:start -->"
CHECK_TABLE_END = "<!-- check-table:end -->"
BACKLOG_ID = re.compile(r"^(S|A)-\d+$")
BINARIES_ALLOW = "config/security/binaries.allow"  # relativ: Self-Test-Fixtures setzen ROOT um
FIXTURES = ROOT / "config/style-fixtures"

MARKER = re.compile(r"//\s*(Abweichung [A-Z][A-Z0-9]*(?:-[A-Z]+)?-\d+:|dekorativ:)")
COMMENT = re.compile(r"^\s*(//|/\*|\*)")
IMPORT = re.compile(r"^\s*import\s")


@dataclass
class Check:
    name: str
    rule: str
    include: list[str]
    patterns: list[str] = field(default_factory=list)
    exclude: list[str] = field(default_factory=list)
    level: str = "MUSS"            # MUSS: Fehler · SOLL: nur Warnung
    multiline: bool = False        # Muster über die ganze Datei statt zeilenweise
    skip_imports: bool = False
    skip_comments: bool = True
    per_file: bool = False         # höchstens 1 Treffer pro Datei (Datei-Bedingung)
    custom: object = None          # Funktion(files) -> {datei: anzahl}
    baseline: bool = True          # False: Null-Toleranz, kein Eintrag in der Baseline (sec-*)
    scope: str = ""                # Sonder-Checks: Geltungsbereich für die Check-Tabelle (--list)
    fixture: object = None         # Sonder-Checks: Funktion(text) -> Trefferzahl für den Self-Test

    def scope_text(self) -> str:
        """Geltungsbereich für die Check-Tabelle: Include-Muster in Kurzform, Excludes dahinter."""
        if self.scope:
            return self.scope
        def short(g: str) -> str:
            return g.replace(SRC, "src/").replace(RES, "res/").replace(JAVA, "java/")
        text = ", ".join(short(g) for g in self.include)
        if self.exclude:
            text += " ohne " + ", ".join(short(g) for g in self.exclude)
        return text

    def compiled(self) -> list[re.Pattern[str]]:
        flags = re.MULTILINE if self.multiline else 0
        return [re.compile(p, flags) for p in self.patterns]


def files_for(include: list[str], exclude: list[str]) -> list[str]:
    tracked = subprocess.run(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard"],
        cwd=ROOT, capture_output=True, text=True, check=True,
    ).stdout.splitlines()
    out = []
    for f in tracked:
        if not (ROOT / f).is_file():
            continue
        if any(fnmatch.fnmatch(f, g) for g in include) and not any(fnmatch.fnmatch(f, g) for g in exclude):
            out.append(f)
    return sorted(out)


def prepared_lines(text: str, check: Check) -> list[str]:
    """Zeilen, die zählen; ausgeblendete werden zu Leerzeilen (Zeilennummern bleiben)."""
    lines = []
    for line in text.split("\n"):
        hidden = (
            (check.skip_comments and COMMENT.match(line))
            # Stil-Marker sind Ausnahmen mit Begründung; Null-Toleranz-Checks (sec-*) kennen keine.
            or (check.baseline and MARKER.search(line))
            or (check.skip_imports and IMPORT.match(line))
        )
        lines.append("" if hidden else line)
    return lines


def count_text(text: str, check: Check) -> list[int]:
    """Zeilennummern (1-basiert) aller Treffer in einem Text."""
    lines = prepared_lines(text, check)
    hits: list[int] = []
    if check.multiline:
        joined = "\n".join(lines)
        for pat in check.compiled():
            for m in pat.finditer(joined):
                hits.append(joined.count("\n", 0, m.start()) + 1)
    else:
        pats = check.compiled()
        for i, line in enumerate(lines, 1):
            hits.extend(i for p in pats if p.search(line))
    if check.per_file and hits:
        return hits[:1]
    return hits


# ---------------------------------------------------------------------------
# Sonder-Checks, die sich nicht als Zeilenmuster ausdrücken lassen
# ---------------------------------------------------------------------------

def theme_tokens(_: Check) -> dict[str, list[int]]:
    """FARBE-06: implizite M3-Rollen gesetzt, jedes Token außerhalb seiner Datei genutzt."""
    hits: dict[str, list[int]] = {}
    theme = ROOT / SRC / "ui/theme/Theme.kt"
    text = theme.read_text(encoding="utf-8")
    for role in ("outline", "error", "onError", "primaryContainer"):
        if not re.search(rf"\b{role}\s*=", text):
            hits.setdefault(str(theme.relative_to(ROOT)), []).append(0)
    sources = {f: (ROOT / f).read_text(encoding="utf-8") for f in files_for([SRC + "*.kt"], [])}
    for token_file in ("ui/theme/Color.kt", "ui/theme/Dimens.kt", "ui/theme/Sizes.kt", "ui/theme/Motion.kt"):
        path = SRC + token_file
        if path not in sources:
            continue
        for m in re.finditer(r"^\s*(?:const\s+)?val\s+(\w+)", sources[path], re.MULTILINE):
            name = m.group(1)
            used = any(
                re.search(rf"\b{name}\b", txt) for f, txt in sources.items() if f != path
            )
            if not used:
                line = sources[path].count("\n", 0, m.start()) + 1
                hits.setdefault(path, []).append(line)
    return hits


def dark_only_dirs(_: Check) -> dict[str, list[int]]:
    """PRIN-01: keine values-night-Variante (App ist dark-only)."""
    night = [d for d in (ROOT / RES).glob("values-night*") if d.is_dir()]
    return {str(d.relative_to(ROOT)): [0] for d in night}


def locale_config(_: Check) -> dict[str, list[int]]:
    """TEXT-03: Build mit localeFilters "de", Manifest mit localeConfig."""
    hits: dict[str, list[int]] = {}
    build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    if not re.search(r'localeFilters[^\n]*"de"', build):
        hits["app/build.gradle.kts"] = [0]
    if "android:localeConfig" not in (ROOT / MANIFEST).read_text(encoding="utf-8"):
        hits[MANIFEST] = [0]
    return hits


def section_header_heading(_: Check) -> dict[str, list[int]]:
    """A11Y-01: Abschnitts-, Seiten- und Kartenköpfe (CardTitle in TextBlocks.kt) sind Überschriften für TalkBack."""
    hits = {}
    # ScreenHeader nutzt den Titel der gemeinsamen Kopfzeile (HeaderTitle in PushHeader.kt).
    for name in ("SectionHeader", "PushHeader", "TextBlocks"):
        path = SRC + f"ui/components/{name}.kt"
        if "heading()" not in (ROOT / path).read_text(encoding="utf-8"):
            hits[path] = [0]
    return hits


def pane_title(_: Check) -> dict[str, list[int]]:
    """A11Y-07: Kopfzeile aller Seiten (HeaderBar in PushHeader.kt, auch für ScreenHeader) und
    Session-Overlay sind Bereiche mit paneTitle; TalkBack sagt sonst keinen Seitenwechsel an."""
    hits = {}
    for name in ("PushHeader", "ScreenHeader", "OverlayChrome"):
        path = SRC + f"ui/components/{name}.kt"
        if "paneTitle" not in (ROOT / path).read_text(encoding="utf-8"):
            hits[path] = [0]
    return hits


UI_LITERAL_LINE = re.compile(r"Text\(|contentDescription\s*=|stateDescription\s*=|onClickLabel\s*=|listOf\(")
STRING_LITERAL = re.compile(r'"((?:[^"\\]|\\.)*)"')


def has_letter_literal(text: str) -> bool:
    for m in STRING_LITERAL.finditer(text):
        content = re.sub(r"\$\{[^}]*\}|\$\w+", "", m.group(1))
        if re.search(r"[^\W\d_]", content):
            return True
    return False


def ui_literal_line(line: str) -> bool:
    """TEXT-01: sichtbarer oder vorgelesener Text als Literal statt aus strings.xml."""
    return bool(UI_LITERAL_LINE.search(line)) and has_letter_literal(line)


def ui_literal_hits(lines: list[str]) -> list[int]:
    """Wie ui_literal_line, aber über die ganze Anweisung: Ein `contentDescription = if (…) {`
    mit den Literalen in den Folgezeilen zählt genauso (sonst „behebt“ Umformatieren den Fund)."""
    hits = []
    for i, line in enumerate(lines):
        if not UI_LITERAL_LINE.search(line):
            continue
        statement, depth = line, line.count("(") + line.count("{") - line.count(")") - line.count("}")
        j = i + 1
        while depth > 0 and j < len(lines) and j - i <= 8:
            statement += "\n" + lines[j]
            depth += lines[j].count("(") + lines[j].count("{") - lines[j].count(")") - lines[j].count("}")
            j += 1
        if has_letter_literal(statement):
            hits.append(i + 1)
    return hits


# Feature-Pakete unter ui/ greifen nur auf Gemeinsames zu; Ausnahmen mit Begründung in CODE-01.
UI_SHARED = {"theme", "components", "navigation"}
UI_FEATURE_EXCEPTIONS = {("own", "app.atemkraft.ui.log.LogHistory")}  # Verlauf (Wochenpunkte, Bilanz) im Tab „Eigene“
UI_FEATURE_IMPORT = re.compile(r"^import (app\.atemkraft\.ui\.(\w+)\.[\w.]+)", re.MULTILINE)


def ui_feature_import(check: Check) -> dict[str, list[int]]:
    """CODE-01: `ui/<feature>` importiert aus `ui/` nur theme, components, navigation und sich selbst."""
    hits: dict[str, list[int]] = {}
    for f in files_for(check.include, check.exclude):
        feature = f[len(SRC + "ui/"):].split("/")[0]
        if feature in UI_SHARED or "/" not in f[len(SRC + "ui/"):]:
            continue
        text = (ROOT / f).read_text(encoding="utf-8")
        for m in UI_FEATURE_IMPORT.finditer(text):
            target = m.group(2)
            if target in UI_SHARED or target == feature or (feature, m.group(1)) in UI_FEATURE_EXCEPTIONS:
                continue
            hits.setdefault(f, []).append(text.count("\n", 0, m.start()) + 1)
    return hits


SCREEN_PARAMS_MAX = 8
SCREEN_FUN = re.compile(r"^(?:internal\s+|public\s+)?fun\s+(\w+(?:Screen|Overlay))\s*\(", re.MULTILINE)


def top_level_params(text: str, start: int) -> list[str]:
    """Parameter (ohne Kommentare) einer Funktion, deren Liste bei [start] (nach „(“) beginnt."""
    depth, cur, params, i = 1, "", [], start
    while i < len(text) and depth:
        c = text[i]
        if text.startswith("//", i):
            i = text.index("\n", i)
            continue
        if c in "([{<":
            depth += 1
        elif c in ")]}>" and not text.startswith("->", i - 1):
            depth -= 1
        if depth == 1 and c == ",":
            params.append(cur.strip())
            cur = ""
        elif depth:
            cur += c
        i += 1
    if cur.strip():
        params.append(cur.strip())
    return params


def screen_params(check: Check) -> dict[str, list[int]]:
    """CODE-02: nicht private `…Screen`/`…Overlay`-Composables mit mehr als 8 Parametern (ohne `modifier`)."""
    hits: dict[str, list[int]] = {}
    for f in files_for(check.include, check.exclude):
        text = (ROOT / f).read_text(encoding="utf-8")
        for m in SCREEN_FUN.finditer(text):
            params = [p for p in top_level_params(text, m.end()) if not p.startswith("modifier:")]
            if len(params) > SCREEN_PARAMS_MAX:
                hits.setdefault(f, []).append(text.count("\n", 0, m.start()) + 1)
    return hits


def ui_literal(check: Check) -> dict[str, list[int]]:
    hits: dict[str, list[int]] = {}
    for f in files_for(check.include, check.exclude):
        found = ui_literal_hits(prepared_lines((ROOT / f).read_text(encoding="utf-8"), check))
        if found:
            hits[f] = found
    return hits


# ---------------------------------------------------------------------------
# Doku- und Katalog-Checks (STYLEGUIDE §0, §5.1, §13.3 → docs/CHECKS.md; PROCESS PROZ-05)
# ---------------------------------------------------------------------------

PUBLIC_FUN = re.compile(r"^(?:@\w+(?:\([^)]*\))?\s+)*fun\s+(?:[\w<>, ]+\.)?(\w+)\s*[(<]", re.M)
BACKTICKED = re.compile(r"`([^`]+)`")


def public_funs(text: str) -> list[str]:
    """Namen aller öffentlichen Top-Level-Funktionen einer Kotlin-Datei (Spalte 0, nicht private/internal)."""
    return [m.group(1) for m in PUBLIC_FUN.finditer(text)]


def catalog_missing(kotlin: str, guide: str) -> list[str]:
    """KOMP-01: öffentliche `fun` aus ui/components, die nicht als `Name` im Guide vorkommen."""
    names = set()
    # Code-Blöcke (```…```, ASCII-Skizzen in §4.5) vorher entfernen, sonst verschiebt ein Zaun die Paare.
    guide = re.sub(r"```.*?```", "", guide, flags=re.S)
    for token in BACKTICKED.findall(guide):
        for part in re.split(r"[^\w]+", token):
            if part:
                names.add(part)
    return [n for n in public_funs(kotlin) if n not in names]


def catalog_sync(check: Check) -> dict[str, list[int]]:
    guide = STYLEGUIDE.read_text(encoding="utf-8")
    hits: dict[str, list[int]] = {}
    for f in files_for(check.include, check.exclude):
        text = (ROOT / f).read_text(encoding="utf-8")
        missing = catalog_missing(text, guide)
        if missing:
            hits[f] = [text[: m.start()].count("\n") + 1 for m in PUBLIC_FUN.finditer(text) if m.group(1) in missing]
    return hits


def catalog_fixture(text: str) -> int:
    """Fixture: Kotlin-Teil und Guide-Teil durch eine Zeile `---` getrennt."""
    kotlin, _, guide = text.partition("\n---\n")
    return len(catalog_missing(kotlin, guide))


VERSION_FILES = ("docs/STYLEGUIDE.md", "docs/SECURITY.md", "docs/PRIVACY.md", "THIRD_PARTY_LICENSES.md", "docs/STORE.md")
VERSION_HEAD_LINES = 6


def version_problems(version_name: str, version_code: str, heads: dict[str, str], existing: set[str]) -> list[str]:
    """PROZ-05: versionName in den Kopfzeilen jeder Stand-Datei; Release-Notes und Changelog vorhanden."""
    out = []
    for path, head in heads.items():
        if not re.search(rf"(?<![\d.]){re.escape(version_name)}(?![\d.])", head):
            out.append(path)
    if f"docs/release-notes/v{version_name}.md" not in existing:
        out.append(f"docs/release-notes/v{version_name}.md")
    if f"fastlane/metadata/android/de-DE/changelogs/{version_code}.txt" not in existing:
        out.append(f"fastlane/metadata/android/de-DE/changelogs/{version_code}.txt")
    return out


def gradle_version() -> tuple[str, str]:
    build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    name = re.search(r'versionName\s*=\s*"([^"]+)"', build)
    code = re.search(r"versionCode\s*=\s*(\d+)", build)
    return (name.group(1) if name else "?", code.group(1) if code else "?")


def version_sync(_: Check) -> dict[str, list[int]]:
    name, code = gradle_version()
    heads = {f: "\n".join((ROOT / f).read_text(encoding="utf-8").splitlines()[:VERSION_HEAD_LINES]) for f in VERSION_FILES if (ROOT / f).is_file()}
    existing = set(files_for(["docs/release-notes/*.md", "fastlane/metadata/android/de-DE/changelogs/*.txt"], []))
    return {path: [0] for path in version_problems(name, code, heads, existing)}


def version_fixture(text: str) -> int:
    """Fixture: erste Zeile `version code`, danach je Zeile `datei: kopftext`; `existiert: pfad` listet Dateien."""
    lines = text.splitlines()
    name, code = lines[0].split()
    heads, existing = {}, set()
    for line in lines[1:]:
        key, _, value = line.partition(": ")
        if key == "existiert":
            existing.add(value)
        elif key:
            heads[key] = value
    return len(version_problems(name, code, heads, existing))


def check_table_rows() -> list[str]:
    """Zeilen der Check-Tabelle (docs/CHECKS.md) aus CHECKS: Check | Regel | Scope | Stufe."""
    rows = ["| Check | Regel | Scope | Stufe |", "|---|---|---|---|"]
    for c in CHECKS:
        level = c.level if c.baseline else "MUSS, ohne Baseline"
        rows.append(f"| `{c.name}` | {c.rule} | {c.scope_text()} | {level} |")
    rows.append("| `baseline-stale` / `baseline-ids` | PROZ-02, §13.2 | `config/style-baseline.txt` | MUSS |")
    rows.append("| `guide-sync` (CI) | PROZ-02 | Push-Bereich | MUSS |")
    rows.append("| `commit-msg` (CI, Hook) | PROZ-01 | Push-Bereich, `.githooks/commit-msg` | MUSS |")
    return rows


def check_table_text() -> str:
    return "\n".join(check_table_rows())


def table_in_doc(doc: str) -> str | None:
    start, end = doc.find(CHECK_TABLE_START), doc.find(CHECK_TABLE_END)
    if start < 0 or end < 0:
        return None
    return doc[start + len(CHECK_TABLE_START):end].strip()


def check_table(_: Check) -> dict[str, list[int]]:
    """§13.3: Die Tabelle in docs/CHECKS.md entspricht `--list`."""
    current = table_in_doc(CHECKS_DOC.read_text(encoding="utf-8"))
    return {} if current == check_table_text() else {"docs/CHECKS.md": [0]}


def check_table_fixture(text: str) -> int:
    """Fixture: `{TABLE}` steht für die aktuelle Tabelle, so bleibt das Negativ-Fixture bei jeder Check-Änderung gültig."""
    return 0 if table_in_doc(text.replace("{TABLE}", check_table_text())) == check_table_text() else 1


def update_check_table() -> int:
    doc = CHECKS_DOC.read_text(encoding="utf-8")
    start, end = doc.find(CHECK_TABLE_START), doc.find(CHECK_TABLE_END)
    if start < 0 or end < 0:
        print("FEHLER  Marker check-table:start/end fehlen in docs/CHECKS.md")
        return 1
    new = doc[: start + len(CHECK_TABLE_START)] + "\n" + check_table_text() + "\n" + doc[end:]
    CHECKS_DOC.write_text(new, encoding="utf-8")
    print(f"Check-Tabelle geschrieben: {len(CHECKS)} Checks")
    return 0


# ---------------------------------------------------------------------------
# Sicherheits-Checks (docs/SECURITY.md, Automatische Prüfung): Datei-Bedingungen
# ---------------------------------------------------------------------------

def read(path: str) -> str:
    f = ROOT / path
    return f.read_text(encoding="utf-8") if f.is_file() else ""


SEC_LINES = Check("", "", [], baseline=False)


def code_lines(path: str) -> list[str]:
    """Zeilen ohne Kommentare für die Sonder-Checks sec-*; Stil-Marker blenden nichts aus."""
    return prepared_lines(read(path), SEC_LINES)


def sec_privacy_link(_: Check) -> dict[str, list[int]]:
    """SEC-PRIV-05: Datenschutzerklärung existiert und ist im Über-Screen verlinkt."""
    about = SRC + "ui/about/AboutScreen.kt"
    if not (ROOT / "docs/PRIVACY.md").is_file():
        return {"docs/PRIVACY.md": [0]}
    return {} if "blob/main/docs/PRIVACY.md" in read(about) else {about: [0]}


def sec_tls_config(_: Check) -> dict[str, list[int]]:
    """SEC-NET-01: network_security_config verbietet Cleartext und ist im Manifest eingetragen."""
    hits: dict[str, list[int]] = {}
    config = RES + "xml/network_security_config.xml"
    if 'cleartextTrafficPermitted="false"' not in read(config):
        hits[config] = [0]
    if 'android:networkSecurityConfig="@xml/network_security_config"' not in read(MANIFEST):
        hits[MANIFEST] = [0]
    return hits


def sec_download(_: Check) -> dict[str, list[int]]:
    """SEC-NET-02: jede Verbindung mit Timeouts und eigenem User-Agent (ohne Gerätedaten)."""
    hits: dict[str, list[int]] = {}
    for f in files_for([JAVA + "*.kt"], []):
        code = "\n".join(code_lines(f))
        if "openConnection(" in code and not all(k in code for k in ("connectTimeout", "readTimeout", '"User-Agent"')):
            hits[f] = [0]
    return hits


BACKUP_DOMAINS = ("root", "file", "database", "sharedpref", "external",
                  "device_root", "device_file", "device_database", "device_sharedpref")


def excludes_all(block: str) -> bool:
    return "<include" not in block and all(
        re.search(rf'<exclude\s+domain="{d}"\s+path="\."', block) for d in BACKUP_DOMAINS
    )


def sec_backup(_: Check) -> dict[str, list[int]]:
    """SEC-STORE-02: kein Cloud-Backup; Gerät-zu-Gerät ohne die nachladbaren Stimmen."""
    hits: dict[str, list[int]] = {}
    if 'android:allowBackup="false"' not in read(MANIFEST):
        hits[MANIFEST] = [0]
    backup = RES + "xml/backup_rules.xml"
    if not excludes_all(read(backup)):
        hits[backup] = [0]
    rules = RES + "xml/data_extraction_rules.xml"
    text = read(rules)
    cloud = re.search(r"<cloud-backup>(.*?)</cloud-backup>", text, re.S)
    transfer = re.search(r"<device-transfer>(.*?)</device-transfer>", text, re.S)
    if not cloud or not excludes_all(cloud.group(1)) or not transfer \
            or not re.search(r'<exclude\s+domain="file"\s+path="tts"', transfer.group(1)):
        hits[rules] = [0]
    return hits


def sec_room(_: Check) -> dict[str, list[int]]:
    """SEC-STORE-03: Room-Schema exportiert und eingecheckt, keine destruktive Migration,
    DataStore mit corruptionHandler."""
    hits: dict[str, list[int]] = {}
    present = set(files_for(["app/schemas/*.json"], []))
    for f in files_for([JAVA + "*.kt"], []):
        lines = code_lines(f)
        found = [i for i, line in enumerate(lines, 1) if "fallbackToDestructiveMigration" in line]
        code = "\n".join(lines)
        db = re.search(r"@Database\((.*?)\)\s*(?:abstract\s+)?class\s+(\w+)", code, re.S)
        if db:
            version = re.search(r"version\s*=\s*(\d+)", db.group(1))
            package = re.search(r"^package\s+([\w.]+)", code, re.M)
            schema = f"app/schemas/{package.group(1) if package else ''}.{db.group(2)}/{version.group(1) if version else '?'}.json"
            if not re.search(r"exportSchema\s*=\s*true", db.group(1)) or schema not in present:
                found.append(code.count("\n", 0, db.start()) + 1)
        if "preferencesDataStore(" in code and "corruptionHandler" not in code:
            found.append(0)
        if found:
            hits[f] = found
    return hits


def sec_repos(_: Check) -> dict[str, list[int]]:
    """SEC-SUP-01: kein mavenLocal/http, Projekt-Repos verboten; google(), mavenCentral() und
    gradlePluginPortal() nur mit Content-Filter; jedes weitere Repo nur in `exclusiveContent`
    mit `includeGroup` und SHA-256-Pin je Gruppe in gradle/verification-metadata.xml."""
    path = "settings.gradle.kts"
    lines = code_lines(path)
    found = [i for i, line in enumerate(lines, 1)
             if re.search(r"mavenLocal|http://|\b(google|mavenCentral|gradlePluginPortal)\s*\(\s*\)", line)]
    code = "\n".join(lines)
    if "FAIL_ON_PROJECT_REPOS" not in code:
        found.append(0)
    extra_repos = len(re.findall(r"\bmaven\s*[({]", code))
    if extra_repos:
        metadata = read("gradle/verification-metadata.xml")
        groups = re.findall(r'includeGroup\(\s*"([^"]+)"', code)
        if code.count("exclusiveContent") < extra_repos or not groups or any(
            not re.search(rf'group="{re.escape(g)}"[\s\S]*?<sha256', metadata) for g in groups
        ):
            found.append(0)
    return {path: found} if found else {}


def sha256_of(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def sec_pinned_inputs(_: Check) -> dict[str, list[int]]:
    """SEC-SUP-02, SEC-REPO-01: Wrapper gepinnt; Binärdateien nur laut Allowlist, mit gültiger
    .sha256-Datei und demselben Hash im Herkunftsnachweis von SECURITY.md; ebenso jede in
    gradle/verification-metadata.xml gepinnte Bibliothek (AAR/JAR)."""
    hits: dict[str, list[int]] = {}
    wrapper = "gradle/wrapper/gradle-wrapper.properties"
    if not re.search(r"^distributionSha256Sum=[0-9a-f]{64}$", read(wrapper), re.M):
        hits[wrapper] = [0]
    allow: dict[str, bool] = {}
    for line in read(BINARIES_ALLOW).splitlines():
        if line.strip() and not line.startswith("#"):
            parts = line.split()
            allow[parts[0]] = "sha256" in parts[1:]
    binaries = files_for(["*." + ext for ext in ("aar", "jar", "so", "dex", "jks", "keystore", "p12", "apk", "aab")], [])
    security = read("docs/SECURITY.md")
    for f in binaries:
        if f not in allow or (allow[f] and not (ROOT / (f + ".sha256")).is_file()):
            hits[f] = [0]
    for f in files_for(["*.sha256"], []):
        entry = read(f).split()
        target = (ROOT / f).parent / entry[1].lstrip("*") if len(entry) >= 2 else None
        if target is None or not target.is_file() or sha256_of(target) != entry[0] or f"`{entry[0]}`" not in security:
            hits[f] = [0]
    metadata = "gradle/verification-metadata.xml"
    if (ROOT / metadata).is_file():
        pinned = re.findall(r'<artifact name="[^"]+\.(?:aar|jar)">\s*<sha256 value="([0-9a-f]{64})"', read(metadata))
        if any(f"`{h}`" not in security for h in pinned):
            hits[metadata] = [0]
    return hits


def sec_gradle_flags(_: Check) -> dict[str, list[int]]:
    """SEC-BUILD-03: keine Abhängigkeits-Metadaten in APK/AAB (blockiert sonst F-Droid)."""
    path = "app/build.gradle.kts"
    return {} if re.search(r"includeInApk\s*=\s*false", "\n".join(code_lines(path))) else {path: [0]}


def sec_secrets(_: Check) -> dict[str, list[int]]:
    """SEC-BUILD-02: keine Signatur-Passwörter in getrackten oder neuen (nicht ignorierten) Dateien."""
    hits: dict[str, list[int]] = {}
    pattern = re.compile(rb"^(store|key)Password\s*[=:]", re.M)  # Properties-Format
    for f in files_for(["*"], []):
        data = (ROOT / f).read_bytes()
        found = [data.count(b"\n", 0, m.start()) + 1 for m in pattern.finditer(data)]
        if found:
            hits[f] = found
    return hits


GITIGNORE_SAMPLES = ("x.jks", "x.p12", "x.keystore", "sub/keystore.properties", "x.idsig", ".claude/settings.local.json")


def sec_gitignore(_: Check) -> dict[str, list[int]]:
    """SEC-REPO-01: .gitignore deckt Keystores, Passwörter, Signaturen und lokale Agent-Einstellungen."""
    missing = [s for s in GITIGNORE_SAMPLES
               if subprocess.run(["git", "check-ignore", "-q", s], cwd=ROOT).returncode != 0]
    return {".gitignore": [0] * len(missing)} if missing else {}


def sec_actions(_: Check) -> dict[str, list[int]]:
    """SEC-REPO-03, SEC-SUP-03: Actions per Commit-SHA, Rechte nur lesend, keine Token im Checkout;
    Dependabot monatlich und gruppiert für Gradle und Actions."""
    hits: dict[str, list[int]] = {}
    for f in files_for([".github/workflows/*.yml", ".github/workflows/*.yaml"], []):
        text = read(f)
        found = [i for i, line in enumerate(text.split("\n"), 1)
                 if re.search(r"^\s*-?\s*uses:\s*(?!\./)\S+@(?![0-9a-f]{40}\b)", line)]
        if not re.search(r"^permissions:", text, re.M):
            found.append(0)
        if text.count("actions/checkout@") > text.count("persist-credentials: false"):
            found.append(0)
        if found:
            hits[f] = found
    dependabot = ".github/dependabot.yml"
    text = read(dependabot)
    if not all(re.search(rf"package-ecosystem:\s*{e}\b", text) for e in ("gradle", "github-actions")) \
            or text.count("interval: monthly") < 2 or text.count("groups:") < 2:
        hits[dependabot] = [0]
    return hits


SEC_JAVA = [JAVA + "*.kt", JAVA + "*.java"]

CHECKS: list[Check] = [
    Check("color-source", "FARBE-01", [SRC + "*.kt"], [r"Color\(0x", r"\b(NeonMagenta|NeonCyan|DarkSurface|DarkBackground|OnNeon)\b"],
          exclude=[SRC + "ui/theme/*"], skip_imports=True),
    Check("color-source-res", "FARBE-01", [RES + "*.xml"], [r"#[0-9A-Fa-f]{6,8}\b"],
          exclude=[RES + "values*/colors.xml", RES + "drawable*/ic_launcher_*", RES + "mipmap*/*", RES + "drawable*/ic_*.xml"]),
    Check("color-source-icons", "FARBE-01", [RES + "drawable*/ic_*.xml"], [r"#(?!FF000000\b|FFFFFFFF\b)[0-9A-Fa-f]{6,8}\b"],
          exclude=[RES + "drawable*/ic_launcher_*"]),
    Check("alpha-literal", "FARBE-02", [SRC + "ui/*.kt", SRC + "MainActivity.kt"],
          [r"alpha\s*=\s*(?:if\s*\([^)]*\)[^,)\n]*?)?\d*\.\d+f?"], exclude=[SRC + "ui/theme/*"]),
    Check("theme-tokens", "FARBE-06", [], custom=theme_tokens, scope="`ui/theme/`"),
    Check("type-literal", "TYPO-01", [SRC + "*.kt"], [r"\b\d+(\.\d+)?f?\.sp\b|FontWeight\.|(fontSize|letterSpacing|lineHeight)\s*="],
          exclude=[SRC + "ui/theme/*"], skip_imports=True),
    Check("layout-literal", "LAYOUT-01", [SRC + "*.kt"], [r"\b([1-9]\d*|\d*\.\d+)f?\.dp\b", r"(RoundedCorner|CutCorner)Shape\(", r"\bDp\.Hairline\b"],
          exclude=[SRC + "ui/theme/*"]),
    Check("weight-nofill", "LAYOUT-03", [SRC + "ui/*.kt"], [r"weight\([^)]*fill\s*=\s*false"]),
    Check("m3-size-const", "LAYOUT-04", [SRC + "*.kt"],
          [r"\b(ButtonDefaults\.(MinHeight|MinWidth|IconSize)|FilterChipDefaults\.Height|SwitchDefaults\.IconSize|SegmentedButtonDefaults\.IconSize)\b"],
          exclude=[SRC + "ui/theme/*"], skip_imports=True),
    Check("control-scale", "LAYOUT-04", [SRC + "ui/*.kt", SRC + "MainActivity.kt"], [r"\.scale\(|\bscale[XY]\s*="],
          exclude=[SRC + "ui/theme/*"]),
    Check("touch-order", "LAYOUT-04", [SRC + "ui/*.kt"],
          [r"\.(size|height|width|requiredSize|requiredHeight|requiredWidth)\((?:[^()]|\([^()]*\))*\)\s*\.minimumInteractiveComponentSize\(\)"],
          multiline=True),
    Check("control-direct", "LAYOUT-04", [SRC + "ui/*.kt", SRC + "MainActivity.kt"],
          [r"(?<![\w.])(Button|TextButton|FilledTonalButton|OutlinedButton|ElevatedButton|IconButton|FilledTonalIconButton|FilledIconButton|"
           r"OutlinedIconButton|FilterChip|InputChip|SegmentedButton|Switch|RadioButton|Checkbox|Slider)\("],
          exclude=[SRC + "ui/components/*"], skip_imports=True),
    Check("platform-override", "LAYOUT-03", [SRC + "*.kt", MANIFEST],
          [r"screenOrientation|requestedOrientation", r"LocalMinimumInteractiveComponentSize\s+provides"]),
    Check("dark-only", "PRIN-01", [SRC + "*.kt", RES + "*.xml"],
          [r"Theme\.Material\.Light|DayNight|lightColorScheme|dynamic(Dark|Light)ColorScheme|enableEdgeToEdge\(\s*\)"], skip_imports=True),
    Check("dark-only-dirs", "PRIN-01", [], custom=dark_only_dirs, scope="`res/values-night*/`"),
    Check("neon-fill", "PRIN-02", [SRC + "ui/*.kt", SRC + "MainActivity.kt"],
          [r"(?s)(?<![A-Za-z])Button\(.*(?<![A-Za-z])Button\(",
           r"containerColor\s*=\s*(MaterialTheme\.colorScheme\.(primary|secondary|tertiary)\b|Neon\w+)"],
          exclude=[SRC + "ui/components/*"], multiline=True, per_file=True),
    Check("no-fab-toast", "PRIN-03", [SRC + "*.kt"], [r"FloatingActionButton|Toast\.|Snackbar"], skip_imports=True),
    Check("card-style", "KOMP-02", [SRC + "ui/*.kt", SRC + "MainActivity.kt"],
          [r"Card\((?:(?!\)\s*\{)[\s\S])*?\.clickable", r"HorizontalDivider\((?!(?:[^()]|\([^()]*\))*?color\s*=\s*DividerSubtle\b)"], multiline=True),
    Check("exception-message-ui", "MUSTER-04", [SRC + "ui/*.kt", SRC + "cue/tts/*.kt"],
          [r"\b(e|it|t|ex|err|error|throwable)\.message\b"], level="SOLL"),
    Check("icon-source", "ICON-01", ["*.kts", "gradle/*.toml", SRC + "*.kt"],
          [r"material-icons-extended", r"Icons\.(Outlined|Rounded|Sharp|TwoTone)\.", r'Text\(\s*(text\s*=\s*)?"\s*[−+›→‹←]\s*"'],
          multiline=True, skip_imports=True),
    Check("cd-style", "ICON-02", [SRC + "*.kt"],
          [r"IconButton\([^)]*semantics\s*\{\s*contentDescription", r"contentDescription\s*=\s*null(?!.*//\s*dekorativ:)"]),
    Check("motion-literal", "MOTION-04", [SRC + "ui/*.kt", SRC + "MainActivity.kt"],
          [r"tween\(\s*(durationMillis\s*=\s*)?\d", r"val\s+\w*(dur|Duration|Ms|Millis)\w*\s*=\s*\d"],
          exclude=[SRC + "ui/theme/*"], level="SOLL"),
    Check("motion-literal-delay", "MOTION-04", [SRC + "ui/components/*.kt"], [r"delay\(\s*\d"], level="SOLL"),
    Check("player-in-composable", "AUDIO-04", [SRC + "ui/*.kt"], [r"remember\s*\{\s*(Haptic|ToneCue|Continuous)\w*Player\("]),
    # Über die ganze Datei mit Klammerausgleich: role darf in einer Folgezeile stehen, auch nach
    # Argumenten mit eigenen Klammern wie remember { MutableInteractionSource() }.
    # Ebenso M3 Surface(onClick): Es setzt keine Rolle; sie kommt per semantics { role = … } dazu.
    Check("semantics-required", "A11Y-01", [SRC + "ui/*.kt"],
          [r"\.clickable\s*(?:\((?!(?:[^()]|\([^()]*\))*?\brole\s*=)|\{)",
           r"(?<![\w.])Surface\((?=(?:[^()]|\([^()]*\))*?\bonClick\s*=)(?!(?:[^()]|\([^()]*\))*?\brole\s*=)"],
          multiline=True),
    Check("semantics-heading", "A11Y-01", [], custom=section_header_heading, scope="`ui/components/` (SectionHeader, PushHeader, TextBlocks)"),
    Check("pane-title", "A11Y-07", [], custom=pane_title, scope="`ui/components/` (PushHeader, ScreenHeader, OverlayChrome)"),
    Check("a11y-ratchet", "A11Y-02", [SRC + "ui/*.kt"], [r"clearAndSetSemantics", r"indication\s*=\s*null"], skip_imports=True),
    Check("switch-unlabeled", "A11Y-03", [SRC + "ui/*.kt"],
          [r"\bSwitch\((?:[^()]|\([^()]*\))*?onCheckedChange\s*=(?!\s*null)"], multiline=True),
    Check("ui-literal", "TEXT-01", [SRC + "ui/*.kt"], custom=ui_literal),
    # Köpfe weiß (FARBE-07): Kopf- und Kartentitel bekommen keine Farbe; nur NavCard färbt über headingColor.
    Check("head-color", "FARBE-07", [SRC + "ui/*.kt"],
          [r"(?<![\w.])(SectionHeader|CollapsibleSection|GroupLabel|CardTitle|CardHeader|ExpanderSection|ExpanderCard)\((?:[^()]|\([^()]*\))*?\b(color|titleColor)\s*=(?!\s*titleColor\b)"],
          exclude=[SRC + "ui/components/SectionHeader.kt", SRC + "ui/components/TextBlocks.kt", SRC + "ui/components/CardHeader.kt",
                   SRC + "ui/components/ExpanderSection.kt", SRC + "ui/components/NavCard.kt", SRC + "ui/home/*.kt"],
          multiline=True),
    # Auswahl ohne Häkchen (A11Y-03): kein Icon an Segment oder Chip, kein Check-Icon.
    Check("no-check-icon", "A11Y-03", [SRC + "ui/*.kt"],
          [r"(?<![\w.])(SegmentedButton|FilterChip|SuggestionChip|InputChip)\((?:[^()]|\([^()]*\))*?\bicon\s*=(?!\s*(?:\{\s*\}|null))",
           r"Icons\.(Filled|Default)\.Check\b"],
          multiline=True, skip_imports=True),
    # Layout-Grammatik (§4.5): Overlays nur in der Titelzeile einer Karte (CardHeader).
    Check("anatomy-overlay", "KOMP-02", [SRC + "ui/*.kt"], [r"\.align\(\s*Alignment\.(TopEnd|TopStart|BottomEnd|BottomStart)\s*\)"],
          exclude=[SRC + "ui/components/CardHeader.kt"]),
    # Bottom Sheet (MUSTER-11): Gerüst nur über AppBottomSheet, damit die Anatomie an einer Stelle lebt;
    # auch der Zustand – geschlossen vor einer Aktion wird nur über AppBottomSheetScope.closeThen.
    Check("sheet-direct", "MUSTER-11", [SRC + "ui/*.kt"], [r"(?<![\w.])ModalBottomSheet\(", r"(?<![\w.])rememberModalBottomSheetState\("],
          exclude=[SRC + "ui/components/AppBottomSheet.kt"], skip_imports=True),
    Check("catalog-sync", "KOMP-01", [SRC + "ui/components/*.kt"], custom=catalog_sync, fixture=catalog_fixture),
    # Doku-Lint (§0): Regelzellen (dritte Spalte jeder Zeile mit ID) höchstens 400 Zeichen; Spezifikation und
    # Historie wandern aus. Ein maskiertes `\|` gehört zur Zelle und beendet den Treffer nicht.
    Check("rule-length", "§0", ["docs/STYLEGUIDE.md", "docs/SECURITY.md", "docs/PROCESS.md", "docs/BACKLOG.md"],
          [r"^\| [A-Z][A-Z0-9]*(?:-[A-Z]+)?-\d+ \| [^|]+ \| (?:[^|\\]|\\.){401,} \|"], skip_comments=False),
    Check("check-table", "§13.3", [], custom=check_table, fixture=check_table_fixture, scope="`docs/CHECKS.md`"),
    Check("version-sync", "PROZ-05", [], custom=version_sync, fixture=version_fixture,
          scope="`app/build.gradle.kts`, Stand-Zeilen, `docs/release-notes/`, `fastlane/…/changelogs/`"),
    Check("locale", "TEXT-03", [SRC + "ui/*.kt"], [r"Locale\.(getDefault|GERMAN|GERMANY)\b|SimpleDateFormat"], skip_imports=True),
    Check("locale-config", "TEXT-03", [], custom=locale_config),
    Check("typo-chars-strings", "TEXT-10", [RES + "values*/strings.xml"], [r"„[^“<]*\"", r"\\'", r"”", r"—", r"~"]),
    Check("typo-chars-src", "TEXT-10", [SRC + "*.kt"], [r'\\"', r"”"]),
    # „~“ nur innerhalb eines String-Literals (gerade Zahl von " davor, dann ein offenes "):
    # Code-Kommentare hinter einer Anweisung („repeat = 60, // ~10 min“) bleiben erlaubt.
    Check("typo-chars-content", "TEXT-10", [SRC + "data/*.kt", SRC + "domain/*.kt"],
          [r"—", r'^(?:[^"\n]*"[^"\n]*")*[^"\n]*"[^"\n]*~']),
    Check("layer-import-domain", "CODE-01", [SRC + "domain/*.kt"], [r"^import app\.atemkraft\.(?!domain\b)"]),
    # domain/ soll als KMP-commonMain übersetzbar bleiben (docs/IOS_PLAN.md §4): keine JVM-APIs; der
    # einzige offene Fall (Normalizer der Suche) ist als expect/actual-Kandidat dokumentiert.
    Check("domain-platform", "CODE-01", [SRC + "domain/*.kt"],
          [r"^import java\.(?!text\.Normalizer$)", r"\bjavaClass\b", r"\.format\(", r"\bLocale\b", r"\b(Math|System|Character)\."]),
    Check("layer-import-data", "CODE-01", [SRC + "data/*.kt"], [r"^import app\.atemkraft\.(ui|cue|playback)\b"]),
    Check("layer-import-cue", "CODE-01", [SRC + "cue/*.kt"], [r"^import app\.atemkraft\.(?!(cue|domain)\b|(BuildConfig|R)$)"]),
    Check("layer-import-components", "CODE-01", [SRC + "ui/components/*.kt"], [r"^import app\.atemkraft\.ui\.(?!theme\b|components\b|navigation\.(LocalTabBarState|TabBarState)\b)"]),
    Check("layer-import-playback", "CODE-01", [SRC + "playback/*.kt"], [r"^import app\.atemkraft\.ui\b"]),
    # Das Wurzelpaket (Application, Container, Activity, NavGraph) steckt alles zusammen; nur es selbst
    # kennt seine Klassen, die Schichten darunter sehen davon nur BuildConfig und R.
    Check("layer-import-root", "CODE-01", [SRC + "data/*.kt", SRC + "playback/*.kt", SRC + "ui/*.kt"],
          [r"^import app\.atemkraft\.(?!(BuildConfig|R)$)[A-Z]\w*$"]),
    # State-Holder (CODE-02): Repositories, Controller und Scopes nur in ViewModels, Destinations und
    # dem Export/Import-Holder; Screens bekommen Werte und Lambdas.
    Check("ui-state-holder", "CODE-02", [SRC + "ui/*.kt"],
          [r"^import app\.atemkraft\.data\.\w*Repository$", r"^import app\.atemkraft\.playback\.\w*Controller$",
           r"^import kotlinx\.coroutines\.CoroutineScope$"],
          exclude=[SRC + "ui/*Destination.kt", SRC + "ui/*ViewModel.kt", SRC + "ui/settings/PatternTransfer.kt"]),
    Check("screen-params", "CODE-02", [SRC + "ui/*.kt"], custom=screen_params),
    Check("ui-feature-import", "CODE-01", [SRC + "ui/*.kt"], custom=ui_feature_import),
    Check("ordinal-persist", "CODE-05", [SRC + "data/*.kt"], [r"\.ordinal\b"]),
    Check("todo-unlinked", "CODE-06", [SRC + "*.kt"], [r"//\s*TODO(?!\((S|A)-\d+\))"], skip_comments=False),
    # Leistung (PERF-01): Datenbank auf dem Main-Thread und blockierendes Warten frieren die UI ein.
    Check("perf-main", "PERF-01", [JAVA + "*.kt"], [r"\ballowMainThreadQueries\b", r"\brunBlocking\b"], skip_imports=True, level="SOLL"),
    # Sicherheit (docs/SECURITY.md, Automatische Prüfung): Null-Toleranz, ohne Baseline.
    Check("sec-deny-deps", "SEC-PRIV-01", ["gradle/libs.versions.toml", "*.gradle.kts"],
          [r"(?i)firebase|crashlytics|play-services|\bgms\b|admob|analytics|sentry|bugsnag|appcenter|facebook"], baseline=False),
    Check("sec-net-endpoints", "SEC-PRIV-02", SEC_JAVA, [r"https?://"],
          exclude=[SRC + "domain/VoiceCatalog.kt", SRC + "ui/about/AboutScreen.kt"], baseline=False),
    Check("sec-net-client", "SEC-NET-02", SEC_JAVA, [r"openConnection\(|\bSocket\(|\bokhttp|\bretrofit|\bktor\b"],
          exclude=[SRC + "cue/tts/VoiceModelManager.kt"], baseline=False),
    Check("sec-device-leaks", "SEC-PRIV-03", SEC_JAVA,
          [r"ANDROID_ID|AdvertisingIdClient|getDeviceId|getImei|Build\.SERIAL",
           r"\bLog\.[vdiwe]\(|\bprintln\(|printStackTrace\(|\bTimber\.", r"ClipboardManager|setPrimaryClip"], baseline=False),
    Check("sec-privacy-link", "SEC-PRIV-05", [], custom=sec_privacy_link, baseline=False, scope="`docs/PRIVACY.md`, `ui/about/AboutScreen.kt`"),
    Check("sec-wakelock", "SEC-PERM-02", SEC_JAVA,
          [r"newWakeLock\((?!\s*(PowerManager\.)?PARTIAL_WAKE_LOCK)", r"\bacquire\(\s*\)"], baseline=False),
    Check("sec-tls", "SEC-NET-01", ["app/src/main/*.kt", "app/src/main/*.java", "app/src/main/*.xml"],
          [r"http://(?!schemas\.android\.com)", r"X509TrustManager|HostnameVerifier|SSLSocketFactory|setDefaultSSL"],
          exclude=["app/src/main/assets/licenses/*"], baseline=False),
    Check("sec-tls-config", "SEC-NET-01", [], custom=sec_tls_config, baseline=False, scope="`res/xml/network_security_config.xml`, Manifest"),
    Check("sec-download", "SEC-NET-02", [], custom=sec_download, baseline=False, scope="`java/`"),
    Check("sec-components", "SEC-PLAT-01", SEC_JAVA + [MANIFEST],
          [r"<receiver\b", r"(?<!un)registerReceiver\((?!.*RECEIVER_NOT_EXPORTED)"], baseline=False),
    Check("sec-intents", "SEC-PLAT-02", SEC_JAVA + [MANIFEST],
          [r"\bintent\.(data|extras)\b|\bget\w*Extra\(", r"<data\s", r"FLAG_MUTABLE(?!.*//\s*mutable:)"], baseline=False),
    Check("sec-no-dyncode", "SEC-PLAT-03", SEC_JAVA + [RES + "*.xml"],
          [r"\b(WebView|DexClassLoader|PathClassLoader|InMemoryDexClassLoader)\b|System\.load\("], baseline=False),
    Check("sec-storage", "SEC-STORE-01", SEC_JAVA, [r"getExternal\w*Dir|getExternalStorage|MediaStore|MODE_WORLD_"], baseline=False),
    Check("sec-backup", "SEC-STORE-02", [], custom=sec_backup, baseline=False, scope="Manifest, `res/xml/backup_rules.xml`, `data_extraction_rules.xml`"),
    Check("sec-room", "SEC-STORE-03", [], custom=sec_room, baseline=False, scope="`java/`, `app/schemas/`"),
    Check("sec-repos", "SEC-SUP-01", [], custom=sec_repos, baseline=False, scope="`settings.gradle.kts`, `gradle/verification-metadata.xml`"),
    Check("sec-pinned-inputs", "SEC-SUP-02", [], custom=sec_pinned_inputs, baseline=False, scope="Wrapper, Binärdateien, `.sha256`, `verification-metadata.xml`, SECURITY.md"),
    Check("sec-gradle-flags", "SEC-BUILD-03", [], custom=sec_gradle_flags, baseline=False, scope="`app/build.gradle.kts`"),
    Check("sec-secrets", "SEC-BUILD-02", [], custom=sec_secrets, baseline=False, scope="alle Dateien"),
    Check("sec-gitignore", "SEC-REPO-01", [], custom=sec_gitignore, baseline=False, scope="`.gitignore`"),
    Check("sec-actions", "SEC-REPO-03", [], custom=sec_actions, baseline=False, scope="`.github/workflows/`, `dependabot.yml`"),
]


def run_check(check: Check) -> dict[str, list[int]]:
    if check.custom:
        return check.custom(check)
    hits: dict[str, list[int]] = {}
    for f in files_for(check.include, check.exclude):
        found = count_text((ROOT / f).read_text(encoding="utf-8"), check)
        if found:
            hits[f] = found
    return hits


def baseline_line_problem(line: str) -> str | None:
    """§13.2: `check⇥datei⇥anzahl⇥S-ID` – ohne Backlog-ID ist eine Zeile keine Altlast, sondern ein Schlupfloch."""
    parts = line.split("\t")
    if len(parts) != 4:
        return "Format check⇥datei⇥anzahl⇥S-ID"
    if not parts[2].isdigit():
        return "Anzahl keine Zahl"
    if not BACKLOG_ID.match(parts[3]):
        return "ohne Backlog-ID (S-nn/A-nn)"
    return None


def baseline_fixture(text: str) -> int:
    return sum(1 for line in text.splitlines() if line.strip() and not line.startswith("#") and baseline_line_problem(line))


def load_baseline() -> dict[tuple[str, str], tuple[int, str]]:
    """(check, datei) → (anzahl, S-ID); kaputte Zeilen meldet check_all über baseline_problems()."""
    base: dict[tuple[str, str], tuple[int, str]] = {}
    for line in baseline_lines():
        if baseline_line_problem(line):
            continue
        name, path, n, backlog = line.split("\t")
        base[(name, path)] = (int(n), backlog)
    return base


def baseline_lines() -> list[str]:
    if not BASELINE.exists():
        return []
    return [ln for ln in BASELINE.read_text(encoding="utf-8").splitlines() if ln.strip() and not ln.startswith("#")]


def baseline_problems() -> list[str]:
    return [f"baseline-ids (§13.2) {ln.split(chr(9))[0]} {ln.split(chr(9))[1] if chr(9) in ln else ''}: {why}"
            for ln in baseline_lines() if (why := baseline_line_problem(ln))]


def check_all() -> int:
    base = load_baseline()
    errors, warnings = baseline_problems(), []
    seen: set[tuple[str, str]] = set()
    for check in CHECKS:
        for path, lines in run_check(check).items():
            key = (check.name, path)
            seen.add(key)
            allowed = base.get(key, (0, ""))[0] if check.baseline else 0
            if len(lines) > allowed:
                where = ", ".join(str(n) for n in lines) if lines != [0] else "Datei"
                msg = f"{check.name} ({check.rule}) {path}: {len(lines)} Treffer, Baseline {allowed} – Zeilen {where}"
                (warnings if check.level == "SOLL" else errors).append(msg)
            elif len(lines) < allowed:
                errors.append(f"baseline-stale (PROZ-02) {check.name} {path}: {len(lines)} statt {allowed} – Baseline senken")
    no_baseline = {c.name for c in CHECKS if not c.baseline}
    for key in base:
        if key[0] in no_baseline:
            errors.append(f"baseline-verboten {key[0]} {key[1]}: Sicherheits-Checks haben keine Baseline (SECURITY.md)")
    for key, (n, _) in base.items():
        if key not in seen and n > 0 and key[0] not in no_baseline:
            errors.append(f"baseline-stale (PROZ-02) {key[0]} {key[1]}: 0 statt {n} – Baseline senken")
    for w in warnings:
        print(f"WARNUNG {w}")
    for e in errors:
        print(f"FEHLER  {e}")
    total = sum(n for n, _ in base.values())
    print(f"check-style: {len(CHECKS)} Checks, {len(errors)} Fehler, {len(warnings)} Warnungen, {total} Altlasten in der Baseline")
    return 1 if errors else 0


def update_baseline() -> int:
    old = load_baseline()
    rows, raised, unknown = [], [], []
    for check in (c for c in CHECKS if c.baseline):
        for path, lines in sorted(run_check(check).items()):
            n = len(lines)
            was, backlog = old.get((check.name, path), (0, "S-??"))
            if n > was and old:
                raised.append(f"{check.name} {path}: {was} → {n}")
            if backlog == "S-??":
                unknown.append(f"{check.name} {path}")
            rows.append(f"{check.name}\t{path}\t{n}\t{backlog}")
    header = (
        "# Eingefrorene Altlasten der Stil-Checks (docs/STYLEGUIDE.md §13.2).\n"
        "# check<TAB>datei<TAB>anzahl<TAB>S-ID – darf nur sinken; jede Zeile nennt den Backlog-Punkt, der sie behebt.\n"
        "# Erzeugt mit scripts/check-style.sh --update-baseline (neue Zeilen tragen S-?? und müssen zugeordnet werden).\n"
    )
    BASELINE.write_text(header + "\n".join(rows) + "\n", encoding="utf-8")
    print(f"Baseline geschrieben: {len(rows)} Einträge, {sum(int(r.split(chr(9))[2]) for r in rows)} Treffer")
    for u in unknown:
        print(f"ACHTUNG ohne Backlog-ID (S-?? eintragen oder beheben): {u}")
    for r in raised:
        print(f"ACHTUNG gestiegen: {r}")
    return 0


def fixture_tree(text: str) -> dict[str, str]:
    """Fixture eines Sonder-Checks über Dateien: je Datei eine Zeile `=== pfad`, darunter ihr Inhalt.
    Eine Inhaltszeile, die mit `\\` beginnt, verliert es – so liegt im Repo selbst kein Fund
    (z. B. `\\storePassword=…` für sec-secrets, das alle Dateien liest)."""
    files: dict[str, list[str]] = {}
    current = None
    for line in text.split("\n"):
        if line.startswith("=== "):
            current = line[4:].strip()
            files[current] = []
        elif current is not None:
            files[current].append(line[1:] if line.startswith("\\") else line)
    return {path: "\n".join(lines) for path, lines in files.items()}


def tree_hits(check: Check, files: dict[str, str]) -> int:
    """Führt einen Sonder-Check auf einem eigenen Git-Repo aus [files] aus (ROOT zeigt solange dorthin).
    Ohne globale Git-Konfiguration, globale Ignore-Datei und GIT_*-Variablen eines Hooks: Nur die Fixture zählt."""
    global ROOT
    saved_root, saved_env = ROOT, dict(os.environ)
    with tempfile.TemporaryDirectory() as tmp:
        for key in [k for k in os.environ if k.startswith("GIT_")]:
            del os.environ[key]
        # Auch die globale Ignore-Datei (~/.config/git/ignore) bleibt draußen, sonst deckt sie sec-gitignore.
        os.environ.update(GIT_CONFIG_GLOBAL=os.devnull, GIT_CONFIG_NOSYSTEM="1", XDG_CONFIG_HOME=os.devnull)
        try:
            subprocess.run(["git", "init", "-q", tmp], check=True, capture_output=True)
            for path, content in files.items():
                target = Path(tmp) / path
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(content, encoding="utf-8")
            ROOT = Path(tmp)
            return sum(len(lines) for lines in check.custom(check).values())
        finally:
            ROOT = saved_root
            os.environ.clear()
            os.environ.update(saved_env)


def self_test() -> int:
    """Jeder Check braucht `<name>.pos` (mindestens ein Treffer) und `<name>.neg` (keiner); ein
    fehlendes Fixture ist ein Fehler. Sonder-Checks ohne eigene Fixture-Funktion laufen auf einem
    Dateibaum aus dem Fixture ([fixture_tree]) – so kann keiner unbemerkt ins Leere prüfen."""
    failed = 0
    fixture_checks = list(CHECKS) + [Check("baseline-ids", "§13.2", [], fixture=baseline_fixture)]
    for check in fixture_checks:
        for kind in ("pos", "neg"):
            fixture = FIXTURES / f"{check.name}.{kind}"
            if not fixture.exists():
                print(f"FEHLER  Fixture fehlt: {fixture.relative_to(ROOT)}")
                failed += 1
                continue
            text = fixture.read_text(encoding="utf-8")
            if check.fixture is not None:
                n = check.fixture(text)
            elif check.name == "ui-literal":
                n = len(ui_literal_hits(prepared_lines(text, check)))
            elif check.custom:
                n = tree_hits(check, fixture_tree(text))
            else:
                n = len(count_text(text, Check(**{**check.__dict__, "per_file": False})))
            ok = n > 0 if kind == "pos" else n == 0
            if not ok:
                print(f"FEHLER  {check.name}.{kind}: {n} Treffer")
                failed += 1
            if kind == "pos" and not check.baseline and not check.custom:
                # Gegenprobe: ein angehängter Stil-Marker darf keinen sec-Treffer verstecken.
                marked = "\n".join(f"{line} // dekorativ: x // Abweichung SEC-1: x" if line.strip() else line
                                    for line in text.split("\n"))
                m = len(count_text(marked, Check(**{**check.__dict__, "per_file": False})))
                if m != n:
                    print(f"FEHLER  {check.name}.pos mit Stil-Marker: {m} statt {n} Treffer")
                    failed += 1
    if prepared_lines('Log.d("a", t) // dekorativ: x', SEC_LINES) == [""]:
        print("FEHLER  code_lines blendet Stil-Marker aus")
        failed += 1
    # guide-sync: Fixture ist ein Diff-Text; pos = Token geändert ohne STYLEGUIDE, neg = mit STYLEGUIDE im Bereich.
    for kind in ("pos", "neg"):
        fixture = FIXTURES / f"guide-sync.{kind}"
        if not fixture.exists():
            print(f"FEHLER  Fixture fehlt: {fixture.relative_to(ROOT)}")
            failed += 1
            continue
        hit = guide_sync_problem(fixture.read_text(encoding="utf-8"), "")
        if hit != (kind == "pos"):
            print(f"FEHLER  guide-sync.{kind}: {'Treffer' if hit else 'kein Treffer'}")
            failed += 1
    print(f"self-test: {'ok' if not failed else f'{failed} Fehler'}")
    return 1 if failed else 0


def commit_subject_ok(subject: str) -> bool:
    """PROZ-01: Betreff beginnt mit vX.Y.Z:, build:, docs: oder fix: (kein feat:, kein Scope)."""
    return re.match(r"^(v\d+\.\d+\.\d+|build|docs|fix): \S", subject) is not None


def commit_msg(path: str) -> int:
    """commit-msg-Hook (.githooks/commit-msg): prüft den Betreff vor dem Commit wie das CI."""
    lines = [ln for ln in Path(path).read_text(encoding="utf-8").splitlines() if not ln.startswith("#")]
    subject = next((ln.strip() for ln in lines if ln.strip()), "")
    # Von Git selbst erzeugte Betreffs (fixup!/squash! beim Umbauen) gehen durch; sie werden
    # vor dem Push zusammengelegt, und dann prüft --ci-range den echten Betreff.
    if commit_subject_ok(subject) or subject.startswith(("fixup! ", "squash! ", "amend! ")):
        return 0
    print(f"FEHLER  commit-msg (PROZ-01): „{subject}“ – erlaubt sind fix:, docs:, build: und vX.Y.Z:")
    return 1


API_DIFF = re.compile(
    rf"^diff --git a/{re.escape(SRC)}ui/theme/[^\n]*\n(?:(?!diff --git)[^\n]*\n)*?[+-]\s*(const\s+)?val\s"
    rf"|^diff --git a/{re.escape(SRC)}ui/components/[^\n]*\n(?:(?!diff --git)[^\n]*\n)*?[+-]\s*(@Composable\s+)?fun\s",
    re.M,
)


def guide_sync_problem(diff: str, bodies: str) -> bool:
    """PROZ-02: Der ganze Bereich ändert Theme-Tokens oder öffentliche Komponenten, aber nirgends den STYLEGUIDE."""
    return bool(API_DIFF.search(diff)) and "diff --git a/docs/STYLEGUIDE.md" not in diff and "Guide: n/a" not in bodies


def ci_range(rev_range: str) -> int:
    """commit-msg je Commit (PROZ-01) und guide-sync über den ganzen Bereich (PROZ-02): Code-Commits
    und ein Doku-Commit im selben Push brauchen keinen Alibi-Trailer mehr."""
    errors = []
    log = subprocess.run(["git", "log", "--format=%H%x00%s%x00%B%x01", rev_range],
                         cwd=ROOT, capture_output=True, text=True, check=True).stdout
    bodies = []
    for entry in filter(None, (e.strip("\n") for e in log.split("\x01"))):
        sha, subject, body = entry.split("\x00", 2)
        bodies.append(body)
        if not commit_subject_ok(subject):
            errors.append(f"commit-msg (PROZ-01) {sha[:8]}: „{subject}“")
    diff = subprocess.run(["git", "diff", "-U0", rev_range], cwd=ROOT, capture_output=True, text=True, check=True).stdout
    if guide_sync_problem(diff, "\n".join(bodies)):
        errors.append(f"guide-sync (PROZ-02) {rev_range}: Theme-Token oder öffentliche Komponente geändert, aber kein Commit des Bereichs ändert docs/STYLEGUIDE.md (Trailer „Guide: n/a – <Grund>“ nur für echte Ausnahmen)")
    for e in errors:
        print(f"FEHLER  {e}")
    print(f"ci-range {rev_range}: {len(errors)} Fehler")
    return 1 if errors else 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--update-baseline", action="store_true")
    ap.add_argument("--ci-range")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--update-check-table", action="store_true")
    ap.add_argument("--commit-msg", metavar="DATEI")
    a = ap.parse_args()
    if a.self_test:
        return self_test()
    if a.update_baseline:
        return update_baseline()
    if a.ci_range:
        return ci_range(a.ci_range)
    if a.list:
        print(check_table_text())
        return 0
    if a.update_check_table:
        return update_check_table()
    if a.commit_msg:
        return commit_msg(a.commit_msg)
    return check_all()


if __name__ == "__main__":
    sys.exit(main())
