#!/usr/bin/env python3
"""Statische Stil-Checks aus docs/STYLEGUIDE.md §13.3 und Sicherheits-Checks (``sec-*``) aus
docs/SECURITY.md (Automatische Prüfung).

Jeder Check zählt Treffer pro Datei. Bekannte Altlasten stehen in
config/style-baseline.txt (``check<TAB>datei<TAB>anzahl``):
  * mehr Treffer als Baseline       → Fehler (neuer Verstoß, bei SOLL nur Warnung)
  * weniger Treffer als Baseline    → Fehler „Baseline senken“ (baseline-stale, META-01)
Kommentarzeilen (``//``, ``/*``, ``*``) zählen nicht, außer bei todo-unlinked; ebenso Zeilen
mit ``// Abweichung <ID>:`` oder ``// dekorativ:``. Die ``sec-*``-Checks haben keine Baseline
(Null-Toleranz): Jeder Treffer ist ein Fehler, ein Baseline-Eintrag für sie ebenfalls.

Aufrufe:
  scripts/check-style.sh                     # prüfen
  scripts/check-style.sh --self-test         # jedes Muster gegen config/style-fixtures/<check>.{pos,neg}
  scripts/check-style.sh --update-baseline   # Baseline auf den Ist-Stand setzen (nur zum Senken!)
  scripts/check-style.sh --ci-range A..B     # nur guide-sync und commit-msg über A..B (CI-Job conventions)
"""
from __future__ import annotations

import argparse
import fnmatch
import hashlib
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = "app/src/main/java/app/atemkraft/"
RES = "app/src/main/res/"
MANIFEST = "app/src/main/AndroidManifest.xml"
JAVA = "app/src/main/java/"
BASELINE = ROOT / "config/style-baseline.txt"
BINARIES_ALLOW = ROOT / "config/security/binaries.allow"
FIXTURES = ROOT / "config/style-fixtures"

MARKER = re.compile(r"//\s*(Abweichung [A-Z]+-\d+:|dekorativ:)")
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
    """A11Y-01: Abschnitts- und Seitenköpfe sind Überschriften für TalkBack."""
    hits = {}
    for name in ("SectionHeader", "ScreenHeader", "PushHeader"):
        path = SRC + f"ui/components/{name}.kt"
        if "heading()" not in (ROOT / path).read_text(encoding="utf-8"):
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


def ui_literal(check: Check) -> dict[str, list[int]]:
    hits: dict[str, list[int]] = {}
    for f in files_for(check.include, check.exclude):
        found = ui_literal_hits(prepared_lines((ROOT / f).read_text(encoding="utf-8"), check))
        if found:
            hits[f] = found
    return hits


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
    """SEC-SUP-01: kein mavenLocal/http, Projekt-Repos verboten; jedes weitere Repo nur mit
    Content-Filter und SHA-256-Pin in gradle/verification-metadata.xml."""
    path = "settings.gradle.kts"
    lines = code_lines(path)
    found = [i for i, line in enumerate(lines, 1) if re.search(r"mavenLocal|http://", line)]
    code = "\n".join(lines)
    if "FAIL_ON_PROJECT_REPOS" not in code:
        found.append(0)
    if re.search(r"\bmaven\s*[({]", code):
        metadata = read("gradle/verification-metadata.xml")
        groups = re.findall(r'includeGroup\(\s*"([^"]+)"', code)
        if "content {" not in code or not groups or any(
            not re.search(rf'group="{re.escape(g)}"[\s\S]*?<sha256', metadata) for g in groups
        ):
            found.append(0)
    return {path: found} if found else {}


def sha256_of(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def sec_pinned_inputs(_: Check) -> dict[str, list[int]]:
    """SEC-SUP-02, SEC-REPO-01: Wrapper gepinnt; Binärdateien nur laut Allowlist, mit gültiger
    .sha256-Datei und demselben Hash im Herkunftsnachweis von SECURITY.md."""
    hits: dict[str, list[int]] = {}
    wrapper = "gradle/wrapper/gradle-wrapper.properties"
    if not re.search(r"^distributionSha256Sum=[0-9a-f]{64}$", read(wrapper), re.M):
        hits[wrapper] = [0]
    allow: dict[str, bool] = {}
    for line in read(str(BINARIES_ALLOW.relative_to(ROOT))).splitlines():
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
    Check("theme-tokens", "FARBE-06", [], custom=theme_tokens),
    Check("type-literal", "TYPO-01", [SRC + "*.kt"], [r"\b\d+(\.\d+)?f?\.sp\b|FontWeight\.|(fontSize|letterSpacing|lineHeight)\s*="],
          exclude=[SRC + "ui/theme/*"], skip_imports=True),
    Check("layout-literal", "LAYOUT-01", [SRC + "*.kt"], [r"\b([1-9]\d*|\d*\.\d+)f?\.dp\b", r"(RoundedCorner|CutCorner)Shape\("],
          exclude=[SRC + "ui/theme/*"]),
    Check("weight-nofill", "LAYOUT-03", [SRC + "ui/*.kt"], [r"weight\([^)]*fill\s*=\s*false"]),
    Check("m3-size-const", "LAYOUT-04", [SRC + "*.kt"],
          [r"\b(ButtonDefaults\.(MinHeight|MinWidth|IconSize)|FilterChipDefaults\.Height|SwitchDefaults\.IconSize|SegmentedButtonDefaults\.IconSize)\b"],
          exclude=[SRC + "ui/theme/*"], skip_imports=True),
    Check("control-scale", "LAYOUT-06", [SRC + "ui/*.kt", SRC + "MainActivity.kt"], [r"\.scale\(|\bscale[XY]\s*="],
          exclude=[SRC + "ui/theme/*"]),
    Check("touch-order", "LAYOUT-06", [SRC + "ui/*.kt"],
          [r"\.(size|height|width|requiredSize|requiredHeight|requiredWidth)\((?:[^()]|\([^()]*\))*\)\s*\.minimumInteractiveComponentSize\(\)"],
          multiline=True),
    Check("control-direct", "KOMP-04", [SRC + "ui/*.kt", SRC + "MainActivity.kt"],
          [r"(?<![\w.])(Button|TextButton|FilledTonalButton|OutlinedButton|ElevatedButton|IconButton|FilledTonalIconButton|FilledIconButton|"
           r"OutlinedIconButton|FilterChip|InputChip|SegmentedButton|Switch|RadioButton|Checkbox|Slider)\("],
          exclude=[SRC + "ui/components/*"], skip_imports=True),
    Check("platform-override", "LAYOUT-03", [SRC + "*.kt", MANIFEST],
          [r"screenOrientation|requestedOrientation", r"LocalMinimumInteractiveComponentSize\s+provides"]),
    Check("dark-only", "PRIN-01", [SRC + "*.kt", RES + "*.xml"],
          [r"Theme\.Material\.Light|DayNight|lightColorScheme|dynamic(Dark|Light)ColorScheme|enableEdgeToEdge\(\s*\)"], skip_imports=True),
    Check("dark-only-dirs", "PRIN-01", [], custom=dark_only_dirs),
    Check("neon-fill", "PRIN-02", [SRC + "ui/*.kt", SRC + "MainActivity.kt"],
          [r"(?s)(?<![A-Za-z])Button\(.*(?<![A-Za-z])Button\(",
           r"containerColor\s*=\s*(MaterialTheme\.colorScheme\.(primary|secondary|tertiary)\b|Neon\w+)"],
          exclude=[SRC + "ui/components/*"], multiline=True, per_file=True),
    Check("no-fab-toast", "PRIN-03", [SRC + "*.kt"], [r"FloatingActionButton|Toast\.|Snackbar"], skip_imports=True),
    Check("card-style", "KOMP-02", [SRC + "ui/*.kt", SRC + "MainActivity.kt"],
          [r"Card\((?:(?!\)\s*\{)[\s\S])*?\.clickable", r"HorizontalDivider\((?:[^()]|\([^()]*\))*?color\s*="], multiline=True),
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
    Check("semantics-required", "A11Y-01", [SRC + "ui/*.kt"], [r"\.clickable\s*(\((?![^)]*role\s*=)|\{)"]),
    Check("semantics-heading", "A11Y-01", [], custom=section_header_heading),
    Check("a11y-ratchet", "A11Y-02", [SRC + "ui/*.kt"], [r"clearAndSetSemantics", r"indication\s*=\s*null"], skip_imports=True),
    Check("switch-unlabeled", "A11Y-03", [SRC + "ui/*.kt"],
          [r"\bSwitch\((?:[^()]|\([^()]*\))*?onCheckedChange\s*=(?!\s*null)"], multiline=True),
    Check("ui-literal", "TEXT-01", [SRC + "ui/*.kt"], custom=ui_literal),
    Check("locale", "TEXT-03", [SRC + "ui/*.kt"], [r"Locale\.(getDefault|GERMAN|GERMANY)\b|SimpleDateFormat"], skip_imports=True),
    Check("locale-config", "TEXT-03", [], custom=locale_config),
    Check("typo-chars-strings", "TEXT-10", [RES + "values*/strings.xml"], [r"„[^“<]*\"", r"\\'", r"”", r"—"]),
    Check("typo-chars-src", "TEXT-10", [SRC + "*.kt"], [r'\\"', r"”"]),
    Check("typo-chars-content", "TEXT-10", [SRC + "data/*.kt", SRC + "domain/*.kt"], [r"—"]),
    Check("layer-import-domain", "CODE-01", [SRC + "domain/*.kt"], [r"^import app\.atemkraft\.(ui|data)\b"]),
    Check("layer-import-data", "CODE-01", [SRC + "data/*.kt"], [r"^import app\.atemkraft\.ui\b"]),
    Check("layer-import-components", "CODE-01", [SRC + "ui/components/*.kt"], [r"^import app\.atemkraft\.ui\.(?!theme\b|components\b)"]),
    Check("ordinal-persist", "CODE-05", [SRC + "data/*.kt"], [r"\.ordinal\b"]),
    Check("todo-unlinked", "CODE-06", [SRC + "*.kt"], [r"//\s*TODO(?!\((S|A)-\d+\))"], skip_comments=False),
    # Sicherheit (docs/SECURITY.md, Automatische Prüfung): Null-Toleranz, ohne Baseline.
    Check("sec-deny-deps", "SEC-PRIV-01", ["gradle/libs.versions.toml", "*.gradle.kts"],
          [r"(?i)firebase|crashlytics|play-services|\bgms\b|admob|analytics|sentry|bugsnag|appcenter|facebook"], baseline=False),
    Check("sec-net-endpoints", "SEC-PRIV-02", SEC_JAVA, [r"https?://"],
          exclude=[SRC + "cue/tts/VoiceCatalog.kt", SRC + "ui/about/AboutScreen.kt"], baseline=False),
    Check("sec-net-client", "SEC-NET-02", SEC_JAVA, [r"openConnection\(|\bSocket\(|\bokhttp|\bretrofit|\bktor\b"],
          exclude=[SRC + "cue/tts/VoiceModelManager.kt"], baseline=False),
    Check("sec-device-leaks", "SEC-PRIV-03", SEC_JAVA,
          [r"ANDROID_ID|AdvertisingIdClient|getDeviceId|getImei|Build\.SERIAL",
           r"\bLog\.[vdiwe]\(|\bprintln\(|printStackTrace\(|\bTimber\.", r"ClipboardManager|setPrimaryClip"], baseline=False),
    Check("sec-privacy-link", "SEC-PRIV-05", [], custom=sec_privacy_link, baseline=False),
    Check("sec-wakelock", "SEC-PERM-02", SEC_JAVA,
          [r"newWakeLock\((?!\s*(PowerManager\.)?PARTIAL_WAKE_LOCK)", r"\bacquire\(\s*\)"], baseline=False),
    Check("sec-tls", "SEC-NET-01", ["app/src/main/*.kt", "app/src/main/*.java", "app/src/main/*.xml"],
          [r"http://(?!schemas\.android\.com)", r"X509TrustManager|HostnameVerifier|SSLSocketFactory|setDefaultSSL"],
          exclude=["app/src/main/assets/licenses/*"], baseline=False),
    Check("sec-tls-config", "SEC-NET-01", [], custom=sec_tls_config, baseline=False),
    Check("sec-download", "SEC-NET-02", [], custom=sec_download, baseline=False),
    Check("sec-components", "SEC-PLAT-01", SEC_JAVA + [MANIFEST],
          [r"<receiver\b", r"registerReceiver\((?!.*RECEIVER_NOT_EXPORTED)"], baseline=False),
    Check("sec-intents", "SEC-PLAT-02", SEC_JAVA + [MANIFEST],
          [r"\bintent\.(data|extras)\b|\bget\w*Extra\(", r"<data\s", r"FLAG_MUTABLE(?!.*//\s*mutable:)"], baseline=False),
    Check("sec-no-dyncode", "SEC-PLAT-03", SEC_JAVA + [RES + "*.xml"],
          [r"\b(WebView|DexClassLoader|PathClassLoader|InMemoryDexClassLoader)\b|System\.load\("], baseline=False),
    Check("sec-storage", "SEC-STORE-01", SEC_JAVA, [r"getExternal\w*Dir|getExternalStorage|MediaStore|MODE_WORLD_"], baseline=False),
    Check("sec-backup", "SEC-STORE-02", [], custom=sec_backup, baseline=False),
    Check("sec-room", "SEC-STORE-03", [], custom=sec_room, baseline=False),
    Check("sec-repos", "SEC-SUP-01", [], custom=sec_repos, baseline=False),
    Check("sec-pinned-inputs", "SEC-SUP-02", [], custom=sec_pinned_inputs, baseline=False),
    Check("sec-gradle-flags", "SEC-BUILD-03", [], custom=sec_gradle_flags, baseline=False),
    Check("sec-secrets", "SEC-BUILD-02", [], custom=sec_secrets, baseline=False),
    Check("sec-gitignore", "SEC-REPO-01", [], custom=sec_gitignore, baseline=False),
    Check("sec-actions", "SEC-REPO-03", [], custom=sec_actions, baseline=False),
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


def load_baseline() -> dict[tuple[str, str], int]:
    base: dict[tuple[str, str], int] = {}
    if BASELINE.exists():
        for line in BASELINE.read_text(encoding="utf-8").splitlines():
            if not line.strip() or line.startswith("#"):
                continue
            name, path, n = line.split("\t")
            base[(name, path)] = int(n)
    return base


def check_all() -> int:
    base = load_baseline()
    errors, warnings = [], []
    seen: set[tuple[str, str]] = set()
    for check in CHECKS:
        for path, lines in run_check(check).items():
            key = (check.name, path)
            seen.add(key)
            allowed = base.get(key, 0) if check.baseline else 0
            if len(lines) > allowed:
                where = ", ".join(str(n) for n in lines) if lines != [0] else "Datei"
                msg = f"{check.name} ({check.rule}) {path}: {len(lines)} Treffer, Baseline {allowed} – Zeilen {where}"
                (warnings if check.level == "SOLL" else errors).append(msg)
            elif len(lines) < allowed:
                errors.append(f"baseline-stale (META-01) {check.name} {path}: {len(lines)} statt {allowed} – Baseline senken")
    no_baseline = {c.name for c in CHECKS if not c.baseline}
    for key in base:
        if key[0] in no_baseline:
            errors.append(f"baseline-verboten {key[0]} {key[1]}: Sicherheits-Checks haben keine Baseline (SECURITY.md)")
    for key, n in base.items():
        if key not in seen and n > 0 and key[0] not in no_baseline:
            errors.append(f"baseline-stale (META-01) {key[0]} {key[1]}: 0 statt {n} – Baseline senken")
    for w in warnings:
        print(f"WARNUNG {w}")
    for e in errors:
        print(f"FEHLER  {e}")
    total = sum(base.values())
    print(f"check-style: {len(CHECKS)} Checks, {len(errors)} Fehler, {len(warnings)} Warnungen, {total} Altlasten in der Baseline")
    return 1 if errors else 0


def update_baseline() -> int:
    old = load_baseline()
    rows, raised = [], []
    for check in (c for c in CHECKS if c.baseline):
        for path, lines in sorted(run_check(check).items()):
            n = len(lines)
            if n > old.get((check.name, path), 0) and old:
                raised.append(f"{check.name} {path}: {old.get((check.name, path), 0)} → {n}")
            rows.append(f"{check.name}\t{path}\t{n}")
    header = (
        "# Eingefrorene Altlasten der Stil-Checks (docs/STYLEGUIDE.md §13.2).\n"
        "# check<TAB>datei<TAB>anzahl – darf nur sinken. Erzeugt mit scripts/check-style.sh --update-baseline.\n"
    )
    BASELINE.write_text(header + "\n".join(rows) + "\n", encoding="utf-8")
    print(f"Baseline geschrieben: {len(rows)} Einträge, {sum(int(r.split(chr(9))[2]) for r in rows)} Treffer")
    for r in raised:
        print(f"ACHTUNG gestiegen: {r}")
    return 0


def self_test() -> int:
    failed = 0
    for check in CHECKS:
        if check.custom and check.name != "ui-literal":
            continue
        for kind in ("pos", "neg"):
            fixture = FIXTURES / f"{check.name}.{kind}"
            if not fixture.exists():
                print(f"FEHLER  Fixture fehlt: {fixture.relative_to(ROOT)}")
                failed += 1
                continue
            text = fixture.read_text(encoding="utf-8")
            if check.name == "ui-literal":
                n = len(ui_literal_hits(prepared_lines(text, check)))
            else:
                n = len(count_text(text, Check(**{**check.__dict__, "per_file": False})))
            ok = n > 0 if kind == "pos" else n == 0
            if not ok:
                print(f"FEHLER  {check.name}.{kind}: {n} Treffer")
                failed += 1
            if kind == "pos" and not check.baseline:
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
    print(f"self-test: {'ok' if not failed else f'{failed} Fehler'}")
    return 1 if failed else 0


def ci_range(rev_range: str) -> int:
    """guide-sync und commit-msg (META-01, CODE-08) über einen Push-Bereich."""
    errors = []
    log = subprocess.run(["git", "log", "--format=%H%x00%s%x00%B%x01", rev_range],
                         cwd=ROOT, capture_output=True, text=True, check=True).stdout
    for entry in filter(None, (e.strip("\n") for e in log.split("\x01"))):
        sha, subject, body = entry.split("\x00", 2)
        if not re.match(r"^(v\d+\.\d+\.\d+|build|docs|fix): \S", subject):
            errors.append(f"commit-msg (CODE-08) {sha[:8]}: „{subject}“")
        files = subprocess.run(["git", "diff-tree", "--no-commit-id", "-r", "-U0", "-p", sha],
                               cwd=ROOT, capture_output=True, text=True, check=True).stdout
        touched_api = re.search(
            rf"^diff --git a/{re.escape(SRC)}ui/theme/[^\n]*\n(?:(?!diff --git)[^\n]*\n)*?[+-]\s*(const\s+)?val\s", files, re.M,
        ) or re.search(
            rf"^diff --git a/{re.escape(SRC)}ui/components/[^\n]*\n(?:(?!diff --git)[^\n]*\n)*?[+-]\s*(@Composable\s+)?fun\s", files, re.M,
        )
        if touched_api and "docs/STYLEGUIDE.md" not in files and "Guide: n/a" not in body:
            errors.append(f"guide-sync (META-01) {sha[:8]}: Theme-Token oder öffentliche Komponente geändert ohne STYLEGUIDE (Trailer „Guide: n/a“ mit Grund möglich)")
    for e in errors:
        print(f"FEHLER  {e}")
    print(f"ci-range {rev_range}: {len(errors)} Fehler")
    return 1 if errors else 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--update-baseline", action="store_true")
    ap.add_argument("--ci-range")
    a = ap.parse_args()
    if a.self_test:
        return self_test()
    if a.update_baseline:
        return update_baseline()
    if a.ci_range:
        return ci_range(a.ci_range)
    return check_all()


if __name__ == "__main__":
    sys.exit(main())
