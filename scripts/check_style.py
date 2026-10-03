#!/usr/bin/env python3
"""Statische Stil-Checks aus docs/STYLEGUIDE.md §13.3.

Jeder Check zählt Treffer pro Datei. Bekannte Altlasten stehen in
config/style-baseline.txt (``check<TAB>datei<TAB>anzahl``):
  * mehr Treffer als Baseline       → Fehler (neuer Verstoß, bei SOLL nur Warnung)
  * weniger Treffer als Baseline    → Fehler „Baseline senken“ (baseline-stale, META-01)
Kommentarzeilen (``//``, ``/*``, ``*``) zählen nicht, außer bei todo-unlinked; ebenso Zeilen
mit ``// Abweichung <ID>:`` oder ``// dekorativ:``.

Aufrufe:
  scripts/check-style.sh                     # prüfen
  scripts/check-style.sh --self-test         # jedes Muster gegen config/style-fixtures/<check>.{pos,neg}
  scripts/check-style.sh --update-baseline   # Baseline auf den Ist-Stand setzen (nur zum Senken!)
  scripts/check-style.sh --ci-range A..B     # zusätzlich guide-sync und commit-msg über A..B
"""
from __future__ import annotations

import argparse
import fnmatch
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = "app/src/main/java/app/atemkraft/"
RES = "app/src/main/res/"
MANIFEST = "app/src/main/AndroidManifest.xml"
BASELINE = ROOT / "config/style-baseline.txt"
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
            or MARKER.search(line)
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
    """A11Y-01: Abschnittsköpfe sind Überschriften für TalkBack."""
    path = SRC + "ui/components/SectionHeader.kt"
    text = (ROOT / path).read_text(encoding="utf-8")
    return {} if "heading()" in text else {path: [0]}


UI_LITERAL_LINE = re.compile(r"Text\(|contentDescription\s*=|stateDescription\s*=|onClickLabel\s*=|listOf\(")
STRING_LITERAL = re.compile(r'"((?:[^"\\]|\\.)*)"')


def ui_literal_line(line: str) -> bool:
    """TEXT-01: sichtbarer oder vorgelesener Text als Literal statt aus strings.xml."""
    if not UI_LITERAL_LINE.search(line):
        return False
    for m in STRING_LITERAL.finditer(line):
        content = re.sub(r"\$\{[^}]*\}|\$\w+", "", m.group(1))
        if re.search(r"[^\W\d_]", content):
            return True
    return False


def ui_literal(check: Check) -> dict[str, list[int]]:
    hits: dict[str, list[int]] = {}
    for f in files_for(check.include, check.exclude):
        for i, line in enumerate(prepared_lines((ROOT / f).read_text(encoding="utf-8"), check), 1):
            if ui_literal_line(line):
                hits.setdefault(f, []).append(i)
    return hits


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
            allowed = base.get(key, 0)
            if len(lines) > allowed:
                where = ", ".join(str(n) for n in lines) if lines != [0] else "Datei"
                msg = f"{check.name} ({check.rule}) {path}: {len(lines)} Treffer, Baseline {allowed} – Zeilen {where}"
                (warnings if check.level == "SOLL" else errors).append(msg)
            elif len(lines) < allowed:
                errors.append(f"baseline-stale (META-01) {check.name} {path}: {len(lines)} statt {allowed} – Baseline senken")
    for key, n in base.items():
        if key not in seen and n > 0:
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
    for check in CHECKS:
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
                n = sum(ui_literal_line(line) for line in prepared_lines(text, check))
            else:
                n = len(count_text(text, Check(**{**check.__dict__, "per_file": False})))
            ok = n > 0 if kind == "pos" else n == 0
            if not ok:
                print(f"FEHLER  {check.name}.{kind}: {n} Treffer")
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
    rc = check_all()
    if a.ci_range:
        rc |= ci_range(a.ci_range)
    return rc


if __name__ == "__main__":
    sys.exit(main())
