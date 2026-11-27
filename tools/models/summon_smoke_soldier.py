"""
Soldat de fumée de Morel (Deep Purple) — invocation GeckoLib (Geo-Fx).

Petit soldat (≈ 1,7 bloc) fait de fumée violet-gris : tout en boules de nuage superposées (tête, torse, bras,
jambes qui s'effilochent en volutes), deux yeux ronds sombres et une bouche en O, casque de fumée à visière ;
il tient une lance de fumée. Rendu translucide. Animations : `appear` (il se forme en gonflant), `idle` (les
boules respirent et ondulent), `strike` (coup de lance).
"""
from __future__ import annotations

from chibi import FaceGrid, flat
from geolib import Model, material, rgb, shade

SMOKE = rgb('#b8aec8')
SMOKE_D = rgb('#7a6e90')
INK = rgb('#2a2236')


def puff(k=1.0):
    """Fumée : fond doux, volutes en spirale plus claires et plus sombres, bords effilochés (pixels transparents
    au hasard sur le pourtour) pour casser l'effet de pierre."""
    base = material(shade(SMOKE, k), top=1.1, bottom=0.85, edge=0.9, noise=6, grad=0.18)

    def paint(ctx):
        import math
        import random
        base(ctx)
        w, h = ctx.w, ctx.h
        rnd = random.Random(int(k * 1000) + w * 31 + h * 17 + hash(ctx.face) % 97)
        px = ctx.img.load()
        for _ in range(max(1, (w * h) // 160)):              # volutes
            cx, cy = rnd.uniform(0, w), rnd.uniform(0, h)
            r0 = rnd.uniform(2, max(3, min(w, h) * 0.45))
            col = shade(SMOKE, k * rnd.choice((1.22, 1.15, 0.82)))
            turns = rnd.uniform(1.2, 2.2)
            for i in range(90):
                t = i / 90
                a = t * turns * 2 * math.pi + rnd.random() * 0.05
                r = r0 * (1 - t)
                x, y = int(cx + r * math.cos(a)), int(cy + r * math.sin(a))
                if 0 <= x < w and 0 <= y < h:
                    px[x, y] = tuple(int(v) for v in col[:3]) + (235,)
        for x in range(w):                                    # bords effilochés
            for y in range(h):
                d = min(x, y, w - 1 - x, h - 1 - y) / max(1, min(w, h))
                if d < 0.14 and rnd.random() < (0.14 - d) * 5.5:
                    px[x, y] = (0, 0, 0, 0)
                elif d < 0.2:
                    p = px[x, y]
                    px[x, y] = (p[0], p[1], p[2], int(p[3] * 0.75))
    return paint


def face_paint(ctx):
    puff(1.06)(ctx)
    if ctx.face != 'north':
        return
    f = FaceGrid(ctx)
    for x0 in (14, 40):
        f.rect(x0, 24, x0 + 12, 36, INK)
    f.rect(28, 42, 38, 52, INK)


def ball(m, bone, c, s, k=1.0, paint=None):
    """Boule de nuage : trois boîtes croisées (arrondi)."""
    p = paint or puff(k)
    x, y, z = c
    m.cube(bone, [x - s / 2, y - s * 0.4, z - s * 0.4], [s, s * 0.8, s * 0.8], p)
    m.cube(bone, [x - s * 0.4, y - s / 2, z - s * 0.4], [s * 0.8, s, s * 0.8], puff(k * 0.97))
    m.cube(bone, [x - s * 0.4, y - s * 0.4, z - s / 2], [s * 0.8, s * 0.8, s], puff(k * 0.94))


def build():
    m = Model('summon_smoke_soldier', density=4, tex_width=512)
    m.bone('body', [0, 0, 0])
    m.bone('head', [0, 20, 0], 'body')
    # jambes en volutes qui s'effilochent vers le bas
    for sx, side in ((-1, 'r'), (1, 'l')):
        b = f'leg_{side}'
        m.bone(b, [sx * 2, 11, 0], 'body')
        for k, (y, s) in enumerate(((9, 4.6), (5.6, 3.8), (2.6, 3.0), (0.6, 2.0))):
            ball(m, b, (sx * 2 + sx * 0.3 * k, y, 0.3 * k), s, 1.0 - 0.06 * k)
    # torse : grosses boules
    import random
    rnd = random.Random(7)
    for (x, y, z, s) in ((0, 13, 0, 8.2), (-2.6, 16.8, 0, 6.4), (2.6, 16.8, 0, 6.4), (0, 19, 0, 5.4)):
        ball(m, 'body', (x, y, z), s, 1.0)
    for _ in range(9):                                          # petites volutes autour du corps
        x, y, z = rnd.uniform(-4, 4), rnd.uniform(10, 20), rnd.uniform(-3, 3)
        ball(m, 'body', (x, y, z), rnd.uniform(2.4, 3.6), rnd.uniform(0.9, 1.1))
    # tête et casque à visière
    m.cube('head', [-3.6, 20, -3.6], [7.2, 7, 7.2], face_paint)
    ball(m, 'head', (0, 24, 0.6), 7.6, 0.98)
    m.cube('head', [-4.2, 26.4, -4.2], [8.4, 2.4, 8.4], flat(SMOKE_D, 0.8))
    m.cube('head', [-3.4, 26.6, -6.4], [6.8, 0.8, 2.6], flat(SMOKE_D, 0.8))
    m.cube('head', [-1.0, 28.6, -1.0], [2, 2.4, 2], flat(SMOKE_D, 0.8))
    # bras et lance
    for sx, side in ((-1, 'r'), (1, 'l')):
        b = f'arm_{side}'
        m.bone(b, [sx * 4.6, 18, 0], 'body', rotation=[-20 if sx < 0 else 0, 0, -sx * 12])
        for k, (y, s) in enumerate(((16.6, 3.6), (13.6, 3.2), (11.0, 2.8))):
            ball(m, b, (sx * 4.8, y, 0), s, 1.0 - 0.04 * k)
    m.cube('arm_r', [-5.3, 4, -0.5], [1.0, 22, 1.0], flat(SMOKE_D, 0.85))
    m.cube('arm_r', [-5.8, 25.5, -1.0], [2.0, 3.5, 2.0], flat(shade(SMOKE, 1.15), 0.85), pivot=[-4.8, 27, 0],
           rotation=[0, 45, 0])
    puffs = {}
    for name in ('body', 'head'):
        puffs[name] = {'scale': {'0.0': [1, 1, 1], '1.0': [1.04, 0.97, 1.04], '2.0': [1, 1, 1]}}
    m.animations = {
        'appear': {'loop': False, 'animation_length': 0.5, 'bones': {
            'body': {'scale': {'0.0': [0.2, 0.05, 0.2], '0.35': [1.15, 1.1, 1.15], '0.5': [1, 1, 1]}}}},
        'idle': {'loop': True, 'animation_length': 2.0, 'bones': {
            **puffs,
            'leg_r': {'rotation': {'0.0': [0, 0, 0], '1.0': [6, 0, 4], '2.0': [0, 0, 0]}},
            'leg_l': {'rotation': {'0.0': [0, 0, 0], '1.0': [-6, 0, -4], '2.0': [0, 0, 0]}}}},
        'strike': {'loop': False, 'animation_length': 0.5, 'bones': {
            'arm_r': {'rotation': {'0.0': [0, 0, 0], '0.15': [30, 0, 0], '0.3': [-70, 0, 0], '0.5': [0, 0, 0]}}}},
    }
    return m


if __name__ == '__main__':
    build().save()
