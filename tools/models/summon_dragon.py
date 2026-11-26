"""
Dragon d'aura de Zeno (Tête de dragon, Plongée du dragon) — invocation GeckoLib (Geo-Fx).

Dragon oriental serpentin fait d'aura dorée : tête allongée gueule ouverte (crocs, langue), deux longues cornes
rejetées en arrière, longues moustaches, crinière en pointes ; corps de 10 anneaux qui s'affinent, crête dorsale, deux paires de
petites pattes griffues, queue en flamme. Il regarde vers -Z (l'avant), le corps s'étire derrière lui.
Longueur de référence ≈ 4,5 blocs. Animations : `appear`, `idle` (le corps ondule en S, la gueule claque),
`strike` (la gueule s'ouvre en grand, coup de tête).
"""
from __future__ import annotations

import math

from chibi import flat, lock
from geolib import Model, material, rgb, shade

GOLD = rgb('#f2c24a')
GOLD_L = rgb('#fff0a0')
GOLD_D = rgb('#b07a1e')
MOUTH = rgb('#c0402a')
WHITE = rgb('#fffbe8')


def gold(k=1.0):
    return material(shade(GOLD, k), top=1.16, bottom=0.8, edge=0.78, noise=3, grad=0.18)


def build():
    m = Model('summon_dragon', density=4, tex_width=512)
    m.bone('root', [0, 6, 0])
    # tête
    m.bone('head', [0, 6, 0], 'root')
    m.cube('head', [-3.5, 3, -6], [7, 6, 8], gold(1.0))                       # crâne
    m.cube('head', [-2.6, 4.6, -12], [5.2, 3.2, 6.5], gold(1.05))             # museau
    m.cube('head', [-2.0, 7.6, -11.5], [4.0, 0.8, 5], gold(1.15))             # arête du museau
    for sx in (-1, 1):
        m.cube('head', [sx * 1.6 - 0.5, 6.8, -12.3], [1.0, 1.0, 0.6], flat(GOLD_D, 0.9))     # naseaux
        m.cube('head', [sx * 2.6 - 0.9, 7.2, -5.6], [1.8, 1.4, 1.0], flat(WHITE, 0.95))       # yeux
        m.cube('head', [sx * 2.6 - 0.4, 7.4, -5.8], [0.8, 1.0, 0.4], flat(MOUTH, 0.9))
        m.cube('head', [sx * 3.0 - 1.0, 8.2, -6.4], [2.0, 0.8, 2.6], gold(1.2))              # arcade
        # longues cornes rejetées en arrière
        m.cube('head', [sx * 2.2 - 0.7, 9, -3.2], [1.4, 9, 1.4], flat(GOLD_L, 0.9), pivot=[sx * 2.2, 9, -2.5],
               rotation=[62, 0, -sx * 22])
        # longues moustaches qui flottent vers l'arrière depuis le museau
        m.cube('head', [sx * 2.7 - 0.3, 5.0, -11.6], [0.6, 0.6, 13], flat(GOLD_L, 0.95), pivot=[sx * 2.7, 5.3, -11.3],
               rotation=[12, sx * 28, 0])
    # crinière en pointes derrière la tête
    for i, (x, y, rz) in enumerate(((-3, 8, 40), (-1.5, 9, 15), (0, 9.4, 0), (1.5, 9, -15), (3, 8, -40), (-3.6, 5, 70), (3.6, 5, -70))):
        m.cube('head', [x - 0.8, y, 0.5], [1.6, 4.5, 1.6], gold(0.9 + 0.04 * (i % 3)), pivot=[x, y, 1.3],
               rotation=[60, 0, rz])
    # mâchoire (s'ouvre) avec crocs et langue
    m.bone('jaw', [0, 4, -4], 'head', rotation=[-18, 0, 0])
    m.cube('jaw', [-2.4, 2.4, -11.5], [4.8, 1.8, 7.5], gold(0.95))
    m.cube('jaw', [-1.6, 3.9, -10.5], [3.2, 0.5, 6], flat(MOUTH, 0.9))
    for sx in (-1, 1):
        m.cube('jaw', [sx * 1.9 - 0.4, 4.0, -11.2], [0.8, 1.6, 0.8], flat(WHITE, 0.95))
        m.cube('head', [sx * 1.9 - 0.4, 3.4, -11.6], [0.8, 1.6, 0.8], flat(WHITE, 0.95))
    # corps : anneaux chaînés (chaque anneau est l'enfant du précédent pour l'ondulation)
    parent, z = 'root', 1.5
    segs = 10
    for i in range(segs):
        w = 6.2 - 0.42 * i
        L = 6.0
        name = f'seg{i}'
        m.bone(name, [0, 6, z], parent, rotation=[round(5 * math.cos(i * 0.8), 1), round(16 * math.sin(i * 0.8 + 0.6), 1), 0])
        m.cube(name, [-w / 2, 6 - w / 2, z], [w, w, L], gold(1.0 - 0.02 * (i % 2)))
        m.cube(name, [-w / 2 + 0.6, 6 - w / 2 - 0.3, z + 0.5], [w - 1.2, 0.5, L - 1], flat(GOLD_L, 0.9))   # ventre clair
        m.cube(name, [-0.6, 6 + w / 2 - 0.2, z + 1.5], [1.2, 2.2 - 0.12 * i, 2.6], gold(1.15), pivot=[0, 6 + w / 2, z + 2.8],
               rotation=[-25, 0, 0])                                                                 # crête
        if i in (1, 6):                                       # pattes griffues
            for sx in (-1, 1):
                m.cube(name, [sx * (w / 2 + 0.6) - 0.8, 6 - w / 2 - 2.2, z + 2], [1.6, 3.2, 1.6], gold(0.95),
                       pivot=[sx * w / 2, 6 - w / 2, z + 2.8], rotation=[20, 0, sx * 30])
                for f in range(3):
                    m.cube(name, [sx * (w / 2 + 1.6) - 0.3 + (f - 1) * 0.6, 6 - w / 2 - 3.4, z + 1.6], [0.5, 1.0, 0.5],
                           flat(WHITE, 0.95), pivot=[sx * w / 2, 6 - w / 2, z + 2.8], rotation=[20, 0, sx * 30])
        parent, z = name, z + L - 0.4
    # queue en flamme
    for k, (rx, L) in enumerate(((-20, 6), (10, 7), (35, 5))):
        lock(m, f'tail{k}', [0, 6, z + 0.5], L, 2.6, 90 + rx, (k - 1) * 25, lambda t: flat(GOLD_L, 0.9), flat=0.4,
             segs=3, root=1.0, tip=0.1, parent=parent)

    wave = {}
    for i in range(segs):
        keys = {}
        for k in range(9):
            t = k * 0.25
            keys[f'{t:.2f}'] = [round(4 * math.sin(2 * math.pi * (t / 2 - i * 0.12) + 1), 2),
                                round(12 * math.sin(2 * math.pi * (t / 2 - i * 0.12)), 2), 0]
        wave[f'seg{i}'] = {'rotation': keys}
    m.animations = {
        'appear': {'loop': False, 'animation_length': 0.3, 'bones': {
            'root': {'scale': {'0.0': [0.3, 0.3, 0.3], '0.3': [1, 1, 1]}}}},
        'idle': {'loop': True, 'animation_length': 2.0, 'bones': {
            **wave,
            'jaw': {'rotation': {'0.0': [0, 0, 0], '0.5': [-14, 0, 0], '1.0': [0, 0, 0], '1.5': [-14, 0, 0], '2.0': [0, 0, 0]}},
            'head': {'rotation': {'0.0': [0, -6, 0], '1.0': [0, 6, 0], '2.0': [0, -6, 0]}}}},
        'strike': {'loop': False, 'animation_length': 0.5, 'bones': {
            'jaw': {'rotation': {'0.0': [0, 0, 0], '0.15': [-40, 0, 0], '0.3': [6, 0, 0], '0.5': [0, 0, 0]}},
            'head': {'rotation': {'0.0': [0, 0, 0], '0.15': [-20, 0, 0], '0.3': [12, 0, 0], '0.5': [0, 0, 0]}}}},
    }
    return m


if __name__ == '__main__':
    build().save()
