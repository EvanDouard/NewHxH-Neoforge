"""
Kiriko (forêt avant Zaban, faux mari démasqué de l'Examen) — modèle GeckoLib `beast_kiriko`.

Bête magique haute et maigre, debout comme un homme : corps couvert de plumes gris-vert, ventre plus clair, longs
bras-ailes emplumés terminés par trois doigts griffus, longues pattes d'oiseau digitigrades, petite queue de plumes.
Tête ronde au long museau pointu, large sourire fendu plein de petites dents, petits yeux noirs ronds et rieurs,
huppe de plumes rabattue vers l'arrière. Hauteur ≈ 2,4 blocs (boîte 1,4 × 2,4).
"""
from __future__ import annotations

from beastkit import biped_animations, on_face
from beast_spider_eagle import feathers
from chibi import flat
from geolib import Model, layered, material, only, rgb, shade

GREEN = rgb('#6e7e58')
GREEN_D = rgb('#3e4a32')
BELLY = rgb('#c6c49a')
SKIN = rgb('#d8c49a')
CLAW = rgb('#2a2620')
MOUTH = rgb('#3a1814')
TOOTH = rgb('#f4f0e2')


def grin_face(ctx):
    """Face avant de la tête : deux petits yeux noirs rieurs."""
    d, W, H = ctx.draw, ctx.w, ctx.h
    for cx in (0.27, 0.73):
        x, y, r = round(cx * W), round(0.36 * H), max(2, W // 9)
        d.ellipse([x - r, y - r, x + r, y + r], fill=(16, 14, 12, 255))
        d.rectangle([x - r // 2, y - r // 2, x - r // 2 + 1, y - r // 2 + 1], fill=(255, 255, 255, 255))
        d.arc([x - r - 2, y - r - 3, x + r + 2, y + r - 1], 200, 340, fill=GREEN_D, width=2)


def smile(ctx):
    """Côtés du museau : la fente du sourire qui remonte vers les joues, avec les dents."""
    d, W, H = ctx.draw, ctx.w, ctx.h
    up = ctx.face == 'east'
    pts = [(0, round(H * 0.55)), (W - 1, round(H * 0.4))] if not up else [(0, round(H * 0.4)), (W - 1, round(H * 0.55))]
    d.line(pts, fill=MOUTH, width=max(2, H // 6))
    n = max(3, W // 3)
    for i in range(n):
        x = round((i + 0.5) * W / n)
        t = x / max(1, W - 1)
        y = pts[0][1] + (pts[1][1] - pts[0][1]) * t
        d.rectangle([x - 1, round(y) - 1, x, round(y) + 1], fill=TOOTH)


def build():
    m = Model('beast_kiriko', density=4, tex_width=512)
    m.bone('root', [0, 0, 0])
    m.bone('body', [0, 20, 0], 'root', rotation=[-6, 0, 0])
    plum = feathers(GREEN, seed=1, rows=3, dark=GREEN_D)
    headmat = material(GREEN, top=1.1, bottom=0.85, edge=0.8, noise=6, grad=0.15)
    m.cube('body', [-5, 18, -3.5], [10, 13, 7], layered(plum, only(('north',), feathers(BELLY, seed=2, rows=3, dark='#9a986e'))))
    m.cube('body', [-6, 27, -3.8], [12, 4.5, 7.6], plum)                                   # épaules
    m.cube('body', [-4, 15.5, -3], [8, 3.5, 6], plum)                                      # bassin
    m.cube('body', [-2, 31, -2], [4, 3, 4], feathers(GREEN_D, seed=3, rows=2))            # cou

    # tête ronde + long museau au sourire fendu
    m.bone('head', [0, 33.5, -0.5], 'body')
    m.cube('head', [-4.5, 33, -5], [9, 8, 8.5], on_face('north', headmat, grin_face))
    snout = on_face('east', on_face('west', material(SKIN, noise=4, grad=0.1), smile), smile)
    m.cube('head', [-2.6, 33.4, -11], [5.2, 4, 6.5], snout)
    m.cube('head', [-1.4, 34.2, -13.4], [2.8, 2.6, 2.6], material(SKIN, noise=4), pivot=[0, 35, -11], rotation=[8, 0, 0])
    m.bone('jaw', [0, 34, -5], 'head')
    m.cube('jaw', [-2.4, 32.2, -10.5], [4.8, 1.4, 6], material(shade(SKIN, 0.9), noise=4))
    # huppe rabattue en arrière
    for k, (x, rx) in enumerate(((-2, 50), (0, 58), (2, 50), (0, 70))):
        m.cube('head', [x - 0.9, 40, -2 + k * 0.6], [1.8, 6 - k * 0.5, 1.4], feathers(GREEN_D, seed=4 + k, rows=2),
               pivot=[x, 40.5, -1.5], rotation=[rx, 0, x * 4])

    # bras-ailes : rémiges qui pendent sous l'avant-bras, trois doigts griffus
    for sx, name in ((1, 'wing_l'), (-1, 'wing_r')):
        x = sx * 6
        m.bone(name, [x, 30, 0], 'body', rotation=[0, 0, sx * 4])
        m.cube(name, [x - 1.5, 18, -1.5], [3, 13, 3], plum)
        m.cube(name, [x - 0.6 * sx - 0.4, 9, -0.2], [0.8, 14, 6.5], feathers(GREEN_D, seed=8, rows=5, dark='#262e1e'))
        m.cube(name, [x - 1.2, 13.5, -1.2], [2.4, 5, 2.4], plum)
        for f, dz in enumerate((-1.0, 0.0, 1.0)):
            m.cube(name, [x - 0.4 + dz * 0.5, 10.5, -1.0 + dz * 0.4], [0.8, 3.4, 0.8], flat(SKIN, 0.85))
            m.cube(name, [x - 0.35 + dz * 0.5, 9.6, -1.2 + dz * 0.4], [0.7, 1.0, 0.7], flat(CLAW, 0.85))

    m.bone('tail', [0, 17, 3], 'body', rotation=[30, 0, 0])
    for k, rz in enumerate((-14, 0, 14)):
        m.cube('tail', [-1.4, 8, 2.5], [2.8, 9, 1.0], feathers(GREEN_D if k == 1 else GREEN, seed=12 + k, rows=3),
               pivot=[0, 17, 3], rotation=[0, 0, rz])

    # pattes d'oiseau digitigrades : cuisse emplumée, long tarse nu, trois doigts
    leg = material(SKIN, top=1.05, bottom=0.85, edge=0.78, noise=4, grad=0.1)
    for sx, name in ((1, 'leg_l'), (-1, 'leg_r')):
        x = sx * 2.6
        m.bone(name, [x, 17, 0], 'root')
        m.cube(name, [x - 2, 10, -2.5], [4, 8, 5], plum, pivot=[x, 17, 0], rotation=[-12, 0, 0])
        m.cube(name, [x - 0.8, 1.2, 0.4], [1.6, 10, 1.6], leg, pivot=[x, 10.5, 1.2], rotation=[12, 0, 0])
        for ry in (-26, 0, 26):
            m.cube(name, [x - 0.5, 0, -3.6], [1.0, 1.2, 4.2], leg, pivot=[x, 0.6, 0.6], rotation=[0, ry, 0])
            m.cube(name, [x - 0.4, 0, -4.4], [0.8, 1.3, 1.0], flat(CLAW, 0.85), pivot=[x, 0.6, 0.6], rotation=[0, ry, 0])

    m.animations = biped_animations(flap=50, walk_len=0.9, step=22, attack='claw', arms=True)
    return m


if __name__ == '__main__':
    build().save()
