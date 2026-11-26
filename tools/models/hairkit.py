"""
Briques communes aux modèles « même style » : rideau de cheveux longs, lunettes rondes, tenue simple.
Les mèches restent des chibi.lock / chibi.crown_lock ; ces fonctions ne font que les placer.
"""
from __future__ import annotations

import math
import random

from chibi import flat, lock


def curtain(m, name, c, r, azs, y_off, length, width, paint, *, inward=12.0, flare=0.0, rnd=None, tip=0.12,
            flat_=0.38, belly=1.08, wave=0.0, segs=5, flip=0.0):
    """Mèches qui pendent d'un anneau autour du crâne (rayon r, hauteur c_y + y_off).
    length / width : float ou fonction de az ; inward : courbure vers le corps (dos) ; flare : écartement des pointes ;
    flip : pointes qui rebiquent vers l'extérieur ; wave : torsion aléatoire."""
    rnd = rnd or random.Random(len(name))
    for i, az in enumerate(azs):
        a = math.radians(az)
        L = length(az) if callable(length) else length
        W = width(az) if callable(width) else width
        bend = inward * math.cos(a) - flip * math.cos(a)
        side = flip * math.sin(a) + flare * az / 90
        lock(m, f'{name}{i}', [c[0] + r * math.sin(a), c[1] + y_off, c[2] + r * math.cos(a)], L + rnd.uniform(-0.5, 0.5),
             W, 178, flare * az * 0.05, paint(i), ry=rnd.uniform(-wave, wave), flat=flat_, curve=(bend, side),
             segs=segs, root=1.05, tip=tip, taper_pow=0.45, belly=belly, skew=rnd.uniform(-0.12, 0.12))


def glasses(m, b, frame, lens, *, gy=30.5, w=2.2, h=2.0, round_=False):
    """Lunettes en relief sur la face avant de la tête (gy : hauteur des yeux sur la grille 0..60)."""
    hx, hz = b.head[0] / 2, b.head[2] / 2
    yl = b.head_y + b.head[1] * (1 - gy / 60)
    fr = flat(frame, 0.9)
    for sx in (-1, 1):
        cx = sx * 1.75
        m.cube('head', [cx - w / 2, yl - h / 2, -hz - 0.36], [w, h, 0.1], flat(lens, 0.95))
        m.cube('head', [cx - w / 2 - 0.1, yl + h / 2 - 0.05, -hz - 0.42], [w + 0.2, 0.18, 0.1], fr)
        m.cube('head', [cx - w / 2 - 0.1, yl - h / 2 - 0.15, -hz - 0.42], [w + 0.2, 0.18, 0.1], fr)
        m.cube('head', [cx - w / 2 - 0.2, yl - h / 2, -hz - 0.42], [0.18, h, 0.1], fr)
        m.cube('head', [cx + w / 2 + 0.02, yl - h / 2, -hz - 0.42], [0.18, h, 0.1], fr)
        m.cube('head', [sx * (hx + 0.05) - 0.06, yl, -hz - 0.2], [0.12, 0.2, hz + 0.4], fr)
    m.cube('head', [-0.6, yl + 0.2, -hz - 0.42], [1.2, 0.18, 0.1], fr)
