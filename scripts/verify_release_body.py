#!/usr/bin/env python3
"""Prüft Release-Commit, Notes und Release-Body gegen docs/SECURITY.md (Release-Checkliste
Schritte 2 und 7, Check ``release-verify``, Teil ``release-body``).

Checks:
  release-version    versionName des Release-Commits = Tag, versionCode = Vorgänger-Commit + 1
                     (PROZ-05: der Bump ist ein eigener Commit, also zählt der Elter-Commit;
                     nicht der vorige Tag, weil nicht jede Version veröffentlicht wird)
  release-changelog  docs/release-notes/vX.Y.Z.md und fastlane/…/changelogs/<versionCode>.txt
                     liegen im Release-Commit und sind nicht leer
  release-claims     keine Heilversprechen in Notes, Changelog und Body (dieselbe Wortliste
                     config/health-claims.txt und Ausnahmen config/health-claims.allow wie
                     ContentRulesTest health-claims)
  release-notes      jede Zeile der Notes-Datei steht im Body       (nur mit --body)
  release-sha256     je APK eine Zeile „<sha256>  atemkraft-vX.Y.Z….apk“ im Body; mit APK-Dateien
                     stimmt der Hash mit der Datei überein            (nur mit --body)
  release-fingerprint  Signatur-Fingerprint aus verify_apk.py im Body (nur mit --body)

Aufrufe (über scripts/verify-release-body.sh):
  scripts/verify-release-body.sh --tag v2.9.0                    # Schritt 2: Notes, Changelog, Version
  scripts/verify-release-body.sh --tag v2.6.0 --body body.md atemkraft-v2.6.0*.apk   # Schritt 7/9

Der Release-Commit ist der Tag, sonst der Commit mit dem Betreff „vX.Y.Z: …“ (vor dem Tag).
"""
from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(Path(__file__).resolve().parent))
from verify_apk import RELEASE_CERT_SHA256  # noqa: E402  (eine Quelle für den Fingerprint)

ABIS = ["", "-arm64-v8a", "-armeabi-v7a"]  # SEC-BUILD-05: Asset-Namen je Release
CHANGELOGS = "fastlane/metadata/android/de-DE/changelogs"
# Ein SHA-256-Fingerprint als Ganzes: 32 Hex-Paare, wahlweise mit „:“ oder Leerzeichen getrennt.
FINGERPRINT = re.compile(r"(?<![0-9A-Fa-f:])((?:[0-9A-Fa-f]{2}[: ]?){31}[0-9A-Fa-f]{2})(?![0-9A-Fa-f:])")


def git(*args: str) -> str:
    return subprocess.run(["git", "-C", str(ROOT), *args], capture_output=True, text=True, check=True).stdout


def release_commit(tag: str) -> str | None:
    try:
        return git("rev-parse", "-q", "--verify", f"refs/tags/{tag}^{{commit}}").strip()
    except subprocess.CalledProcessError:
        pass
    hits = git("log", "--all", "--format=%H %s").splitlines()
    for line in hits:
        sha, _, subject = line.partition(" ")
        if subject.startswith(f"{tag}: "):
            return sha
    return None


def show(rev: str, path: str) -> str | None:
    try:
        return git("show", f"{rev}:{path}")
    except subprocess.CalledProcessError:
        return None


def version_of(gradle: str | None) -> tuple[int | None, str | None]:
    if gradle is None:
        return None, None
    code = re.search(r"^\s*versionCode\s*=\s*(\d+)", gradle, re.M)
    name = re.search(r'^\s*versionName\s*=\s*"([^"]+)"', gradle, re.M)
    return (int(code.group(1)) if code else None), (name.group(1) if name else None)


def health_rules() -> tuple[list[re.Pattern[str]], list[str]]:
    """Wie ContentRulesTest: Muster an Wortgrenzen (Buchstabe/Ziffer), ohne Groß-/Kleinschreibung."""
    claims = [
        re.compile(rf"(?<![^\W_])(?:{line})(?![^\W_])", re.I)
        for line in (s.strip() for s in (ROOT / "config/health-claims.txt").read_text().splitlines())
        if line and not line.startswith("#")
    ]
    allow = [a for a in (ROOT / "config/health-claims.allow").read_text().splitlines() if a.strip() and not a.lstrip().startswith("#")]
    return claims, allow


def claim_findings(name: str, text: str) -> list[str]:
    claims, allow = health_rules()
    out = []
    for i, line in enumerate(text.splitlines(), 1):
        allowed = [(m.start(), m.end()) for a in allow for m in re.finditer(re.escape(a), line)]
        for rule in claims:
            for m in rule.finditer(line):
                if not any(s <= m.start() and m.end() <= e for s, e in allowed):
                    out.append(f"{name}:{i}: „{m.group(0)}“")
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--tag", required=True, help="Release-Tag vX.Y.Z")
    ap.add_argument("--body", help="Release-Body (Datei); ohne nur Version, Notes und Changelog")
    ap.add_argument("apks", nargs="*", help="Release-Assets: ihre SHA-256 müssen im Body stehen")
    args = ap.parse_args()
    tag = args.tag
    if not re.fullmatch(r"v\d+\.\d+\.\d+", tag):
        print(f"verify-release-body: Tag „{tag}“ ist nicht vX.Y.Z", file=sys.stderr)
        return 2
    if args.apks and not args.body:
        print("verify-release-body: APKs nur zusammen mit --body", file=sys.stderr)
        return 2

    errors: list[str] = []

    def fail(check: str, msg: str) -> None:
        errors.append(f"{check}: {msg}")

    rev = release_commit(tag)
    print(f"verify-release-body: {tag}, Release-Commit {rev[:10] if rev else '–'}")
    if rev is None:
        print(f"verify-release-body: weder Tag {tag} noch Commit „{tag}: …“ gefunden", file=sys.stderr)
        return 2

    # release-version
    code, name = version_of(show(rev, "app/build.gradle.kts"))
    prev_code, _ = version_of(show(f"{rev}^", "app/build.gradle.kts"))
    if name != tag[1:]:
        fail("release-version", f"versionName {name} ≠ {tag[1:]}")
    if code is None or prev_code is None or code != prev_code + 1:
        fail("release-version", f"versionCode {code} ≠ Vorgänger {prev_code} + 1")

    # release-changelog
    notes_path = f"docs/release-notes/{tag}.md"
    notes = show(rev, notes_path)
    changelog_path = f"{CHANGELOGS}/{code}.txt"
    changelog = show(rev, changelog_path) if code is not None else None
    for path, text in ((notes_path, notes), (changelog_path, changelog)):
        if not (text or "").strip():
            fail("release-changelog", f"{path} fehlt oder ist leer im Release-Commit")

    # release-claims
    texts = [(notes_path, notes or ""), (changelog_path, changelog or "")]
    body = None
    if args.body:
        body = Path(args.body).read_text(encoding="utf-8").replace("\r\n", "\n")
        texts.append(("Body", body))
    for path, text in texts:
        for f in claim_findings(path, text):
            fail("release-claims", f"Heilversprechen {f} (SEC-LEGAL-01)")

    if body is not None:
        # release-notes
        body_lines = {line.strip() for line in body.splitlines()}
        missing = [line for line in (notes or "").splitlines() if line.strip() and line.strip() not in body_lines]
        if missing:
            fail("release-notes", f"{len(missing)} Zeile(n) der Notes fehlen im Body, z. B. „{missing[0][:60]}…“")

        # release-sha256
        listed = {m.group(2): m.group(1).lower() for m in re.finditer(r"(?m)^([0-9a-fA-F]{64})\s+\*?(\S+\.apk)\s*$", body)}
        for abi in ABIS:
            asset = f"atemkraft-{tag}{abi}.apk"
            if asset not in listed:
                fail("release-sha256", f"keine Zeile „<sha256>  {asset}“ im Body")
        for apk in args.apks:
            p = Path(apk)
            digest = hashlib.sha256(p.read_bytes()).hexdigest()
            if p.name not in listed:
                fail("release-sha256", f"{p.name} hat keine SHA-256-Zeile im Body")
            elif listed[p.name] != digest:
                fail("release-sha256", f"{p.name}: Body {listed[p.name][:12]}… ≠ Datei {digest[:12]}…")

        # release-fingerprint: nur ganze Fingerprints (64 Hex, auch in Paaren mit „:“ oder Leerzeichen)
        # in Zeilen, die „Fingerprint“ oder „SHA-256“ nennen. Nicht den ganzen Body zu Hex
        # zusammenschieben: Sonst könnte der Wert aus Bruchstücken quer durch den Text entstehen.
        fingerprints = {
            re.sub(r"[: ]", "", m.group(1)).lower()
            for line in body.splitlines()
            if re.search(r"fingerprint|sha-?256", line, re.I)
            for m in FINGERPRINT.finditer(line)
        }
        if RELEASE_CERT_SHA256 not in fingerprints:
            fail("release-fingerprint", f"Signatur-Fingerprint {RELEASE_CERT_SHA256[:8]}…{RELEASE_CERT_SHA256[-4:]} fehlt im Body")

    for e in errors:
        print(f"  FEHLER  {e}")
    checks = "Version, Notes, Changelog, Heilversprechen" + (", Body, SHA-256, Fingerprint" if body is not None else "")
    print(f"  ok ({checks})" if not errors else f"  {len(errors)} Fehler")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
