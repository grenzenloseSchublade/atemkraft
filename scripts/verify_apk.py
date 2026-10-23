#!/usr/bin/env python3
"""Prüft fertige APKs gegen docs/SECURITY.md (Automatische Prüfung, Werkzeug ``verify-apk``).

Checks: apk-cert, apk-permissions, apk-exported, apk-hardening, apk-16kb, apk-provenance (mit
``--tag``), apk-licenses, apk-dex-deny. Allowlists liegen unter config/security/.

Aufrufe (über scripts/verify-apk.sh):
  scripts/verify-apk.sh atemkraft-v2.5.0.apk [weitere.apk …] [--tag v2.5.0]
  scripts/verify-apk.sh app-universal-release-unsigned.apk --allow-unsigned   # Prüf-Build

Werkzeuge: apksigner, aapt2 und zipalign aus den Android-Build-Tools (neueste Version unter
ANDROID_HOME, ANDROID_SDK_ROOT oder sdk.dir aus local.properties; sonst ``--build-tools DIR``).
"""
from __future__ import annotations

import argparse
import os
import re
import struct
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SECURITY = ROOT / "config/security"
# Release-Zertifikat (docs/SECURITY.md, „Echtheit einer APK prüfen“).
RELEASE_CERT_SHA256 = "75022defe65aa24e223f3e79880f09f423456757002ff0b33ba2ae29b9f41890"
# ID des verschlüsselten Abhängigkeits-Blocks im APK Signing Block (SEC-BUILD-03).
DEPENDENCY_INFO_BLOCK_ID = 0x504B4453
PAGE_16K = 16384
DEX_DENY = [
    re.compile(rb"Lcom/google/android/gms/"),
    re.compile(rb"Lcom/google/firebase/"),
    re.compile(rb"crashlytics", re.I),
    re.compile(rb"L[\w/$]*/analytics/"),
]


def fail(errors: list[str], check: str, msg: str) -> None:
    errors.append(f"{check}: {msg}")


def read_allow(name: str) -> list[list[str]]:
    rows = []
    for line in (SECURITY / name).read_text(encoding="utf-8").splitlines():
        if line.strip() and not line.lstrip().startswith("#"):
            rows.append(line.split())
    return rows


def sdk_dir() -> Path | None:
    for var in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(var):
            return Path(os.environ[var])
    props = ROOT / "local.properties"
    if props.is_file():
        m = re.search(r"^sdk\.dir=(.+)$", props.read_text(encoding="utf-8"), re.M)
        if m:
            return Path(m.group(1).strip())
    return None


def find_build_tools(explicit: str | None) -> Path:
    if explicit:
        return Path(explicit)
    sdk = sdk_dir()
    candidates = sorted(
        (d for d in (sdk / "build-tools").iterdir() if all((d / t).exists() for t in ("aapt2", "apksigner", "zipalign"))),
        key=lambda d: [int(x) if x.isdigit() else 0 for x in re.split(r"[.-]", d.name)],
    ) if sdk and (sdk / "build-tools").is_dir() else []
    if not candidates:
        sys.exit("verify-apk: keine Build-Tools gefunden (ANDROID_HOME setzen oder --build-tools DIR)")
    return candidates[-1]


def run(cmd: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, capture_output=True, text=True)


def check_cert(apk: str, tools: Path, args: argparse.Namespace, errors: list[str]) -> None:
    if is_unsigned(apk):
        if args.allow_unsigned:
            print("  HINWEIS apk-cert: unsigniert (--allow-unsigned, Prüf-Build – nicht verteilen)")
        else:
            fail(errors, "apk-cert", "APK ist nicht signiert")
        return
    res = run([str(tools / "apksigner"), "verify", "--print-certs", apk])
    if res.returncode != 0:
        fail(errors, "apk-cert", "Signatur ungültig: " + (res.stderr or res.stdout).strip()[:300])
        return
    digests = re.findall(r"certificate SHA-256 digest: ([0-9a-f]{64})", res.stdout)
    dns = re.findall(r"certificate DN: (.+)", res.stdout)
    if any("CN=Android Debug" in dn for dn in dns):
        if args.allow_debug_cert:
            print("  HINWEIS apk-cert: Debug-Zertifikat (--allow-debug-cert, nur Testbuild)")
            return
        fail(errors, "apk-cert", "mit dem öffentlichen Debug-Schlüssel signiert")
        return
    if digests != [RELEASE_CERT_SHA256]:
        fail(errors, "apk-cert", f"Zertifikat {digests} statt {RELEASE_CERT_SHA256}")


def manifest_tree(apk: str, tools: Path) -> list[str]:
    res = run([str(tools / "aapt2"), "dump", "xmltree", "--file", "AndroidManifest.xml", apk])
    if res.returncode != 0:
        sys.exit(f"verify-apk: aapt2 xmltree fehlgeschlagen: {res.stderr.strip()}")
    return res.stdout.splitlines()


def elements(tree: list[str]) -> list[tuple[str, dict[str, str]]]:
    """(Elementname, Attribute) in Dokumentreihenfolge aus `aapt2 dump xmltree`."""
    out: list[tuple[str, dict[str, str]]] = []
    for line in tree:
        s = line.strip()
        if s.startswith("E: "):
            out.append((s[3:].split(" ", 1)[0], {}))
        elif s.startswith("A: ") and out:
            m = re.match(r"A: (?:[^=]*:)?([\w]+)(?:\(0x[0-9a-f]+\))?=(.*)$", s)
            if m:
                value = m.group(2)
                q = re.match(r'"(.*?)"', value)
                out[-1][1][m.group(1)] = q.group(1) if q else value.split(" ", 1)[0]
    return out


def check_manifest(apk: str, tools: Path, errors: list[str]) -> None:
    els = elements(manifest_tree(apk, tools))
    used = {a.get("name") for e, a in els if e == "uses-permission"}
    declared = {a.get("name") for e, a in els if e == "permission"}
    allowed = {r[0] for r in read_allow("permissions.allow")}
    for p in sorted(used - allowed):
        fail(errors, "apk-permissions", f"nicht erlaubt: {p}")
    for p in sorted(allowed - used):
        fail(errors, "apk-permissions", f"in der Allowlist, aber nicht im Manifest: {p}")
    for p in sorted(declared - allowed):
        fail(errors, "apk-permissions", f"deklarierte Berechtigung nicht erlaubt: {p}")

    exported_allow = {r[0]: (r[1] if len(r) > 1 else None) for r in read_allow("exported.allow")}
    for e, a in els:
        if e in ("activity", "activity-alias", "service", "receiver", "provider") and a.get("exported") == "true":
            name = a.get("name", "?")
            if name not in exported_allow:
                fail(errors, "apk-exported", f"exportiert, nicht erlaubt: {name}")
            elif exported_allow[name] and a.get("permission") != exported_allow[name]:
                fail(errors, "apk-exported", f"{name} ohne Berechtigung {exported_allow[name]}")

    app = next((a for e, a in els if e == "application"), {})
    for attr in ("debuggable", "testOnly", "usesCleartextTraffic"):
        if app.get(attr) == "true":
            fail(errors, "apk-hardening", f"android:{attr}=true")


def signing_block_ids(apk: str) -> list[int]:
    """IDs im APK Signing Block (leer bei unsignierten APKs)."""
    data = Path(apk).read_bytes()
    eocd = data.rfind(b"PK\x05\x06")
    if eocd < 0:
        return []
    cd_offset = struct.unpack_from("<I", data, eocd + 16)[0]
    if data[cd_offset - 16:cd_offset] != b"APK Sig Block 42":
        return []
    size = struct.unpack_from("<Q", data, cd_offset - 24)[0]
    pos, end, ids = cd_offset - size - 8 + 8, cd_offset - 24, []
    while pos < end:
        length = struct.unpack_from("<Q", data, pos)[0]
        ids.append(struct.unpack_from("<I", data, pos + 8)[0])
        pos += 8 + length
    return ids


def is_unsigned(apk: str) -> bool:
    """Weder APK Signing Block (v2/v3) noch JAR-Signatur (v1)."""
    with zipfile.ZipFile(apk) as z:
        v1 = any(re.fullmatch(r"META-INF/[^/]+\.(SF|RSA|DSA|EC)", n) for n in z.namelist())
    return not v1 and not signing_block_ids(apk)


def load_alignments(elf: bytes) -> list[int]:
    """p_align aller PT_LOAD-Segmente einer ELF-Datei (32 oder 64 Bit, little endian)."""
    is64 = elf[4] == 2
    if is64:
        phoff, = struct.unpack_from("<Q", elf, 0x20)
        phentsize, phnum = struct.unpack_from("<HH", elf, 0x36)
    else:
        phoff, = struct.unpack_from("<I", elf, 0x1C)
        phentsize, phnum = struct.unpack_from("<HH", elf, 0x2A)
    aligns = []
    for i in range(phnum):
        off = phoff + i * phentsize
        if struct.unpack_from("<I", elf, off)[0] == 1:  # PT_LOAD
            aligns.append(struct.unpack_from("<Q", elf, off + 0x30)[0] if is64 else struct.unpack_from("<I", elf, off + 0x1C)[0])
    return aligns


def check_contents(apk: str, tools: Path, errors: list[str]) -> None:
    if DEPENDENCY_INFO_BLOCK_ID in signing_block_ids(apk):
        fail(errors, "apk-hardening", "Signing Block enthält verschlüsselte Abhängigkeits-Metadaten (0x504b4453)")
    allowed_libs = {r[0] for r in read_allow("native-libs.allow")}
    repo_licenses = {p.name for p in (ROOT / "app/src/main/assets/licenses").glob("*") if p.is_file()}
    with zipfile.ZipFile(apk) as z:
        names = z.namelist()
        for n in names:
            if n.startswith("lib/") and n.endswith(".so"):
                if n not in allowed_libs:
                    fail(errors, "apk-hardening", f"Native Lib nicht erlaubt: {n}")
                bad = [a for a in load_alignments(z.read(n)) if a < PAGE_16K]
                if bad:
                    fail(errors, "apk-16kb", f"{n}: LOAD-Ausrichtung {min(bad):#x} < 16 KB")
        in_apk = {n.rsplit("/", 1)[1] for n in names if n.startswith("assets/licenses/") and not n.endswith("/")}
        for missing in sorted(repo_licenses - in_apk):
            fail(errors, "apk-licenses", f"Lizenztext fehlt in der APK: assets/licenses/{missing}")
        if not in_apk:
            fail(errors, "apk-licenses", "keine Lizenztexte unter assets/licenses/")
        for n in names:
            if re.fullmatch(r"classes\d*\.dex", n):
                dex = z.read(n)
                for pat in DEX_DENY:
                    if pat.search(dex):
                        fail(errors, "apk-dex-deny", f"{n} enthält {pat.pattern.decode()}")
    res = run([str(tools / "zipalign"), "-c", "-P", "16", "4", apk])
    if res.returncode != 0:
        fail(errors, "apk-16kb", "zipalign -c -P 16 4 schlägt fehl: " + (res.stdout + res.stderr).strip()[-300:])


def check_provenance(apk: str, tools: Path, tag: str, errors: list[str]) -> None:
    res = run([str(tools / "aapt2"), "dump", "badging", apk])
    m = re.search(r"versionName='([^']*)'", res.stdout)
    if not m or f"v{m.group(1)}" != tag:
        fail(errors, "apk-provenance", f"versionName {m.group(1) if m else '?'} passt nicht zum Tag {tag}")
    res = run(["git", "-C", str(ROOT), "rev-parse", "--verify", "--quiet", f"{tag}^{{commit}}"])
    commit = res.stdout.strip() if res.returncode == 0 else ""
    if not commit:
        fail(errors, "apk-provenance", f"Tag {tag} gibt es in diesem Klon nicht")
        return
    with zipfile.ZipFile(apk) as z:
        try:
            info = z.read("META-INF/version-control-info.textproto").decode()
        except KeyError:
            fail(errors, "apk-provenance", "META-INF/version-control-info.textproto fehlt")
            return
    rev = re.search(r'revision:\s*"([0-9a-f]{40})"', info)
    if not rev:
        # AGP erkennt in einem `git worktree` kein Git (generate_error_reason: NO_VALID_GIT_FOUND).
        fail(errors, "apk-provenance", "keine Revision in version-control-info.textproto (in einem git worktree gebaut?)")
    elif rev.group(1) != commit:
        fail(errors, "apk-provenance", f"Revision {rev.group(1)} ist nicht der Tag-Commit {commit}")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("apks", nargs="+")
    ap.add_argument("--tag", help="Release-Tag vX.Y.Z: versionName und Revision prüfen (apk-provenance)")
    ap.add_argument("--allow-debug-cert", action="store_true", help="Debug-Zertifikat dulden (CI-Testbuilds)")
    ap.add_argument("--allow-unsigned", action="store_true", help="unsignierte APK dulden (Prüf-Build, nie Release)")
    ap.add_argument("--build-tools", help="Ordner der Android-Build-Tools")
    args = ap.parse_args()
    tools = find_build_tools(args.build_tools)
    print(f"verify-apk: Build-Tools {tools}")
    failed = 0
    for apk in args.apks:
        errors: list[str] = []
        print(f"== {apk}")
        check_cert(apk, tools, args, errors)
        check_manifest(apk, tools, errors)
        check_contents(apk, tools, errors)
        if args.tag:
            check_provenance(apk, tools, args.tag, errors)
        for e in errors:
            print(f"  FEHLER  {e}")
        print("  ok" if not errors else f"  {len(errors)} Fehler")
        failed += bool(errors)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
