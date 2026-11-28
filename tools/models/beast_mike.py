"""
Mike, le chien géant du domaine Zoldyck — modèle GeckoLib `beast_mike`.

Un chien de garde colossal, haut sur pattes : pelage gris-brun sombre et ras, poitrail et collerette plus fournis,
longue tête de loup au museau gris clair, oreilles pointues dressées, et surtout les grands yeux ronds et vides
(blanc entier, minuscule pupille noire) qui fixent l'intrus sans expression. Longue queue droite.
Hauteur ≈ 2,8 blocs (boîte 2,0 × 2,8).
"""
from __future__ import annotations

from beastkit import fur, on_face, quad_animations, quad_leg
from chibi import _fin, flat
from geolib import Model, layered, only, rgb, shade

COAT = rgb('#5a5049')
COAT_D = rgb('#3a332e')
LIGHT = rgb('#a49a90')
NOSE = rgb('#1a1614')
WHITE = rgb('#f4f2ec')


def empty_eyes(ctx):
    d, W, H = ctx.draw, ctx.w, ctx.h
    for a, b in ((0.04, 0.36), (0.64, 0.96)):
        box = [round(a * W), round(0.18 * H), round(b * W) - 1, round(0.62 * H) - 1]
        d.ellipse([box[0] - 1, box[1] - 1, box[2] + 1, box[3] + 1], fill=NOSE)
        d.ellipse(box, fill=WHITE)
        cx, cy = (box[0] + box[2]) / 2, (box[1] + box[3]) / 2
        r = max(1.0, (box[2] - box[0]) * 0.09)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=NOSE)


def build():
    m = Model('beast_mike', density=3, tex_width=512)
    m.bone('root', [0, 0, 0])
    m.bone('body', [0, 26, 0], 'root')
    coat = fur(COAT, streaks=0.6, dark=COAT_D, seed=1)
    coat2 = fur(shade(COAT, 0.9), streaks=0.8, dark=COAT_D, seed=2)
    under = layered(coat, only(('down',), fur(LIGHT, noise=5)))
    # tronc long, poitrail profond, taille fine
    m.cube('body', [-10, 19, -20], [20, 17, 18], under)
    m.cube('body', [-8.5, 21, -2], [17, 13, 22], under)
    m.cube('body', [-8, 33, -18], [16, 3.5, 34], coat2)                              # ligne du dos
    m.cube('body', [-7, 17, -21], [14, 6, 10], fur(LIGHT, noise=5, streaks=0.4))     # poitrail
    # collerette épaisse autour du cou
    m.cube('body', [-11.5, 24, -24], [23, 14, 7], coat2)
    for sx in (-1, 1):
        m.cube('body', [sx * 11 - 2, 22, -23], [4, 12, 6], coat, pivot=[sx * 11, 28, -20], rotation=[0, sx * 10, sx * 12])

    paw = fur(COAT_D, noise=6)
    nails = flat(rgb('#2a2422'), 0.8)
    quad_leg(m, 'leg_fl', 6.5, 22, -13, 6, 22, 6, coat, paw, claws=nails, paw_len=8)
    quad_leg(m, 'leg_fr', -6.5, 22, -13, 6, 22, 6, coat, paw, claws=nails, paw_len=8)
    quad_leg(m, 'leg_bl', 6.5, 22, 14, 6, 22, 6, coat, paw, claws=nails, paw_len=7.5)
    quad_leg(m, 'leg_br', -6.5, 22, 14, 6, 22, 6, coat, paw, claws=nails, paw_len=7.5)
    for sx, leg in ((1, 'leg_bl'), (-1, 'leg_br')):
        m.cube(leg, [sx * 6.5 - 3.8, 13, 10], [7.6, 10, 9], coat2)                    # cuisses

    # grosse tête allongée
    m.bone('head', [0, 34, -24], 'body')
    m.cube('head', [-8, 30, -36], [16, 14, 13], on_face('north', fur(COAT, seed=4, streaks=0.4), empty_eyes))
    m.cube('head', [-8.6, 31, -32], [17.2, 9, 8], coat2)                               # joues
    m.cube('head', [-4.6, 29.5, -46], [9.2, 7.6, 11], layered(fur(LIGHT, noise=4), only(('up',), fur(COAT, seed=5))))
    m.cube('head', [-3.6, 36.4, -44.5], [7.2, 2.4, 9.5], fur(COAT, seed=6), pivot=[0, 37, -38], rotation=[-8, 0, 0])
    m.cube('head', [-3.2, 29.6, -48.5], [6.4, 6.2, 2.6], fur(LIGHT, noise=4))
    m.cube('head', [-2.0, 33.4, -49.6], [4.0, 2.6, 1.6], flat(NOSE, 0.8))
    m.bone('jaw', [0, 30, -36], 'head')
    m.cube('jaw', [-4.0, 26.8, -47], [8.0, 3.0, 11.5], fur(LIGHT, noise=4))
    m.cube('jaw', [-3.8, 29.6, -46.4], [7.6, 0.4, 10.5], flat(rgb('#7a2622'), 0.9))
    for sx in (-1, 1):
        m.cube('head', [sx * 2.6 - 0.5, 28.8, -48.6], [1.0, 1.6, 1.0], flat(WHITE, 0.9))
        # oreilles pointues dressées
        m.bone(f'ear{sx}', [sx * 5, 43.6, -28], 'head', rotation=[-4, -sx * 6, -sx * 10])
        _fin(m, f'ear{sx}', sx * 5, 43.6, -28, 9.0, 6.2, 2.4, lambda t: fur(shade(COAT, 1.0 - 0.15 * t), noise=6),
             ((0.0, 0.6, 0.9, 1.0), (0.6, 1.0, 0.6, 0.8)))
        _fin(m, f'ear{sx}', sx * 5, 44.2, -29.3, 6.4, 3.8, 0.2, lambda t: flat(LIGHT, 0.9), ((0.0, 1.0, 0.4, 1.0),))

    # longue queue
    m.bone('tail', [0, 32, 19], 'body', rotation=[38, 0, 0])
    m.cube('tail', [-2.5, 29.5, 18], [5, 5, 10], coat)
    m.cube('tail', [-2.2, 29.8, 27], [4.4, 4.4, 9], coat2)
    m.cube('tail', [-1.6, 30.4, 35], [3.2, 3.2, 5], coat)

    m.animations = quad_animations(swing=24, walk_len=1.0, attack='bite', tail_wag=6, head_bob=2)
    return m


if __name__ == '__main__':
    build().save()
