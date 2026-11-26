"""
Cookie, l'esthéticienne magique de Biscuit — invocation GeckoLib (Geo-Fx).

Petite poupée-masseuse (≈ 1,3 bloc) qui flotte à côté de Biscuit : grosse tête ronde, visage doux aux yeux fermés
en arc et joues roses, cheveux roses en carré avec un serre-tête blanc à nœud, tenue d'esthéticienne rose à col
blanc et tablier, serviette pliée sur le bras, flacon de lotion dans la main ; pas de jambes : le bas de la robe
s'arrondit et flotte. Animations : `appear`, `idle` (flotte et se balance, les mains massent en rythme).
"""
from __future__ import annotations

import math

from chibi import FaceGrid, cloth, crown_lock, flat, scalp, strand_paint
from hairkit import curtain
from geolib import Model, material, rgb, shade

SKIN = rgb('#fbe3d4')
HAIR = rgb('#d85a94')
HAIR_L = rgb('#f498c0')
DRESS = rgb('#ffb6d0')
WHITE = rgb('#fffaf6')
INK = rgb('#4a2232')


def skin(k=1.0):
    return material(shade(SKIN, k), top=1.05, bottom=0.9, edge=0.85, noise=2, grad=0.1)


def hair(k=1.0):
    return material(shade(HAIR, k), top=1.12, bottom=0.84, edge=0.8, noise=3, grad=0.16)


def face_paint(ctx):
    if ctx.face != 'north':
        hair(0.9)(ctx)
        return
    skin(1.02)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 14, HAIR)
    for x0 in (12, 38):                                            # yeux fermés en arc souriant
        f.poly((x0, 32, x0 + 8, 26, x0 + 16, 32, x0 + 16, 34, x0 + 8, 28, x0, 34), INK)
    f.rect(6, 38, 15, 43, rgb('#f6a0b0'))
    f.rect(51, 38, 60, 43, rgb('#f6a0b0'))
    f.poly((28, 46, 38, 46, 36, 49, 30, 49), rgb('#d0606a'))


def build():
    m = Model('summon_cookie', density=4, tex_width=512)
    dress = cloth(DRESS, shade(DRESS, 1.06), shade(DRESS, 0.78), ao_bottom=0.08, folds=3, seed=600)
    m.bone('body', [0, 0, 0])
    m.bone('head', [0, 10, 0], 'body')
    # bas de robe arrondi qui flotte
    for k, (y, w) in enumerate(((0.6, 4.4), (2.0, 5.6), (3.6, 6.2), (5.2, 5.4))):
        m.cube('body', [-w / 2, y, -w / 2 * 0.85], [w, 1.8, w * 0.85], dress)
    m.cube('body', [-1.8, 1.4, -2.75], [3.6, 5.0, 0.3], cloth(WHITE, seed=601))             # tablier
    m.cube('body', [-2.4, 6.8, -1.8], [4.8, 3.4, 3.6], dress)                              # buste
    m.cube('body', [-2.7, 9.2, -2.1], [5.4, 1.0, 4.2], flat(WHITE, 0.9))                   # col
    # tête ronde (cube de face + arrondis)
    m.cube('head', [-3.8, 10, -3.6], [7.6, 7, 7.2], face_paint)
    m.cube('head', [-3.2, 9.6, -3.0], [6.4, 7.8, 6.4], face_paint)
    # cheveux roses en carré, serre-tête et nœud
    mk = lambda lit: (lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#ffe0ee')))
    m.bone('hair', [0, 17, 0], 'head')
    c, R = (0.0, 13.9, 0.3), 5.0
    scalp(m, c, R, lambda k: mk(k)(0.5), skip_front=46, front_phi=34)
    for i, az in enumerate(range(-150, 151, 25)):
        crown_lock(m, f'c_m{i}', c, R + 0.2, az, 88, 6.0, 3.8, mk(0.16 + 0.06 * math.cos(math.radians(az))), flat=0.45,
                   turn=120, tip=0.35, segs=4)
    curtain(m, 'c_c', c, R - 0.3, range(-124, 125, 16), 0.4, 4.8, 3.4, lambda i: mk(0.06 + 0.05 * (i % 2)),
            inward=0, flip=-18, tip=0.3, segs=3)
    for j, az in enumerate((152, 168, 192, 208)):
        crown_lock(m, f'c_f{j}', c, R + 0.2, az, 70, 4.2, 3.2, mk(0.3), flat=0.45, turn=96, tip=0.25, segs=3)
    m.cube('head', [-4.4, 16.0, -3.6], [8.8, 0.8, 1.0], flat(WHITE, 0.9), pivot=[0, 16.4, -3.1], rotation=[-25, 0, 0])
    for sx in (-1, 1):
        m.cube('head', [sx * 2.2 - 1.2, 16.8, -3.4], [2.4, 1.8, 0.8], flat(WHITE, 0.9), pivot=[sx * 0.8, 17.7, -3.2],
               rotation=[0, 0, -sx * 20])
    m.cube('head', [-0.6, 17.1, -3.5], [1.2, 1.2, 0.9], flat(shade(WHITE, 0.92), 0.9))
    # bras : serviette sur le bras gauche, flacon dans la main droite
    for sx, side in ((-1, 'r'), (1, 'l')):
        m.bone(f'arm_{side}', [sx * 2.6, 9.4, 0], 'body', rotation=[-40, 0, -sx * 16])
        m.cube(f'arm_{side}', [sx * 2.6 - 0.7, 5.6, -0.7], [1.4, 4.0, 1.4], dress)
        m.cube(f'arm_{side}', [sx * 2.6 - 0.75, 4.6, -0.75], [1.5, 1.2, 1.5], skin(1.0))
    m.cube('arm_l', [2.0, 6.2, -1.2], [1.6, 0.6, 2.4], flat(WHITE, 0.9))
    m.cube('arm_r', [-3.2, 2.6, -0.5], [1.2, 2.2, 1.2], flat(rgb('#a8e0f0'), 0.95))
    m.cube('arm_r', [-2.95, 4.8, -0.25], [0.7, 0.6, 0.7], flat(rgb('#f0f0f0'), 0.95))
    m.animations = {
        'appear': {'loop': False, 'animation_length': 0.4, 'bones': {
            'body': {'scale': {'0.0': [0.1, 0.1, 0.1], '0.25': [1.12, 1.12, 1.12], '0.4': [1, 1, 1]}}}},
        'idle': {'loop': True, 'animation_length': 2.0, 'bones': {
            'body': {'position': {'0.0': [0, 0, 0], '1.0': [0, 0.6, 0], '2.0': [0, 0, 0]},
                     'rotation': {'0.0': [0, 0, -3], '1.0': [0, 0, 3], '2.0': [0, 0, -3]}},
            'head': {'rotation': {'0.0': [0, 0, 4], '1.0': [0, 0, -4], '2.0': [0, 0, 4]}},
            'arm_r': {'rotation': {'0.0': [0, 0, 0], '0.5': [-20, 0, 0], '1.0': [0, 0, 0], '1.5': [-20, 0, 0], '2.0': [0, 0, 0]}},
            'arm_l': {'rotation': {'0.0': [-20, 0, 0], '0.5': [0, 0, 0], '1.0': [-20, 0, 0], '1.5': [0, 0, 0], '2.0': [-20, 0, 0]}}}},
    }
    return m


if __name__ == '__main__':
    build().save()
