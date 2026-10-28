#!/usr/bin/env python3
"""
Contrôle « aucun traceur » de Molkky Board, à la manière d'Exodus Privacy : les noms de classes présents dans les
fichiers dex d'un APK sont comparés aux signatures de code des traceurs connus (liste publique d'Exodus Privacy,
copie datée dans exodus-trackers.json). Le script échoue (code 1) si une signature correspond.

    python3 -I tools/trackers/check_trackers.py app/build/outputs/apk/debug/app-debug.apk
    python3 -I tools/trackers/check_trackers.py --update      # rafraîchit la liste depuis l'API d'Exodus
    python3 -I tools/trackers/check_trackers.py --self-test   # vérifie que la détection fonctionne

À lancer sur un APK non minifié (debug) : les noms de classes y sont intacts.
"""
import json
import re
import struct
import sys
import urllib.request
import zipfile
from pathlib import Path

SIGNATURES = Path(__file__).with_name("exodus-trackers.json")
API = "https://reports.exodus-privacy.eu.org/api/trackers"


def uleb128(data: bytes, offset: int) -> tuple[int, int]:
    result, shift = 0, 0
    while True:
        byte = data[offset]
        offset += 1
        result |= (byte & 0x7F) << shift
        if byte < 0x80:
            return result, offset
        shift += 7


def dex_classes(dex: bytes) -> set[str]:
    """Noms de classes (forme pointée) tirés de la table des chaînes d'un fichier dex."""
    if not dex.startswith(b"dex\n"):
        return set()
    size, offset = struct.unpack_from("<II", dex, 0x38)
    classes = set()
    for i in range(size):
        (data_off,) = struct.unpack_from("<I", dex, offset + 4 * i)
        _, start = uleb128(dex, data_off)
        end = dex.index(b"\x00", start)
        text = dex[start:end].decode("utf-8", errors="replace")
        if text.startswith("L") and text.endswith(";"):
            classes.add(text[1:-1].replace("/", "."))
    return classes


def apk_classes(apk: Path) -> set[str]:
    with zipfile.ZipFile(apk) as archive:
        return set().union(
            *(dex_classes(archive.read(name)) for name in archive.namelist() if re.fullmatch(r"classes\d*\.dex", name))
        )


def load_trackers() -> list[dict]:
    return json.loads(SIGNATURES.read_text(encoding="utf-8"))["trackers"]


def find(classes: set[str], trackers: list[dict]) -> dict[str, list[str]]:
    found = {}
    for tracker in trackers:
        pattern = re.compile(tracker["code_signature"])
        hits = sorted(name for name in classes if pattern.search(name))
        if hits:
            found[tracker["name"]] = hits
    return found


def update() -> None:
    with urllib.request.urlopen(API, timeout=30) as response:
        data = json.load(response)["trackers"]
    trackers = sorted(
        ({"id": t["id"], "name": t["name"], "code_signature": t["code_signature"]} for t in data.values() if t["code_signature"]),
        key=lambda t: t["id"],
    )
    from datetime import date

    SIGNATURES.write_text(
        json.dumps({"source": API, "retrieved": date.today().isoformat(), "trackers": trackers}, ensure_ascii=False, indent=1),
        encoding="utf-8",
    )
    print(f"{len(trackers)} signatures enregistrées dans {SIGNATURES}")


def self_test(trackers: list[dict]) -> int:
    fake = {"com.google.firebase.analytics.FirebaseAnalytics", "com.facebook.appevents.AppEventsLogger", "com.yogistudio.molkkyboard.MainActivity"}
    found = find(fake, trackers)
    ok = "Google Firebase Analytics" in found and any("Facebook" in name for name in found) and len(found) >= 2
    print("auto-test :", "OK" if ok else f"ÉCHEC {found}")
    return 0 if ok else 1


def main(args: list[str]) -> int:
    if args == ["--update"]:
        update()
        return 0
    trackers = load_trackers()
    if args == ["--self-test"]:
        return self_test(trackers)
    if not args:
        print(__doc__)
        return 2
    status = 0
    for apk in map(Path, args):
        classes = apk_classes(apk)
        found = find(classes, trackers)
        if found:
            status = 1
            print(f"{apk} : {len(found)} traceur(s) détecté(s)")
            for name, hits in found.items():
                print(f"  - {name} : {', '.join(hits[:3])}{' …' if len(hits) > 3 else ''}")
        else:
            print(f"{apk} : 0 traceur ({len(classes)} classes, {len(trackers)} signatures Exodus du {json.loads(SIGNATURES.read_text())['retrieved']})")
    return status


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
