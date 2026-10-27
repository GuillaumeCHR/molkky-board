#!/usr/bin/env python3
"""Effets sonores maison de Molkky Board, synthétisés de zéro (aucun échantillon tiers : libres de droits).

Usage : python3 -I tools/sounds/generate.py core/designsystem/src/main/res/raw
Résultat déterministe (graine fixe) : relancer le script redonne les mêmes fichiers.
"""
import math
import random
import struct
import sys
import wave

RATE = 22050


def envelope(t, attack, decay):
    return min(1.0, t / attack) * math.exp(-t / decay) if attack > 0 else math.exp(-t / decay)


def write(path, samples):
    peak = max(1e-9, max(abs(s) for s in samples))
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(32767 * 0.85 * s / peak)) for s in samples))


def impact():
    """Choc boisé : basse qui descend + court bruit filtré (le mölkky dans les quilles)."""
    rnd = random.Random(12)
    out, low = [], 0.0
    for i in range(int(RATE * 0.35)):
        t = i / RATE
        freq = 140 * math.exp(-t * 6) + 60
        body = math.sin(2 * math.pi * freq * t) * envelope(t, 0.002, 0.08)
        low += 0.25 * (rnd.uniform(-1, 1) - low)
        out.append(body + 0.6 * low * envelope(t, 0.001, 0.025))
    return out


def chime(notes, length, decay):
    """Notes claires superposées (sinus + harmonique), chacune avec son départ."""
    out = [0.0] * int(RATE * length)
    for start, freq in notes:
        s0 = int(RATE * start)
        for i in range(s0, len(out)):
            t = (i - s0) / RATE
            env = envelope(t, 0.005, decay)
            out[i] += env * (math.sin(2 * math.pi * freq * t) + 0.3 * math.sin(2 * math.pi * 2 * freq * t))
    return out


def eliminated():
    """« Boing » qui descend, ton humoristique sans agressivité."""
    out = []
    phase = 0.0
    for i in range(int(RATE * 0.6)):
        t = i / RATE
        freq = 420 * math.exp(-t * 2.2) + 90 + 12 * math.sin(2 * math.pi * 9 * t)
        phase += 2 * math.pi * freq / RATE
        out.append(math.sin(phase) * envelope(t, 0.005, 0.22))
    return out


def main(target):
    write(f"{target}/sound_impact.wav", impact())
    write(f"{target}/sound_twelve.wav", chime([(0.0, 1046.5), (0.08, 1568.0)], 0.6, 0.18))
    write(f"{target}/sound_victory.wav", chime([(0.0, 523.25), (0.12, 659.25), (0.24, 783.99), (0.36, 1046.5)], 1.3, 0.35))
    write(f"{target}/sound_eliminated.wav", eliminated())


if __name__ == "__main__":
    main(sys.argv[1])
