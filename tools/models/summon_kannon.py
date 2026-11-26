"""
Bodhisattva aux mille mains (Hyakushiki Kannon) de Netero — invocation GeckoLib (Geo-Fx).

Statue dorée qui surgit derrière Netero, vue du bassin jusqu'au chignon : visage serein aux yeux mi-clos, urna au
front, longs lobes, haut chignon (ushnisha) ceint d'une couronne à joyaux ; écharpe drapée, colliers ; les deux
mains jointes en prière devant la poitrine (elle copie la prière de Netero) ; dans le dos, un grand éventail de
bras (11 de chaque côté) qui finissent par des paumes ouvertes, devant une auréole et un halo derrière la tête ;
le tout posé sur une couronne de pétales de lotus.

Taille de référence : 4 blocs de haut (64 px) ; le rendu la met à l'échelle demandée par la technique (9 à 12 blocs).
Animations : `appear` (elle monte du sol), `idle` (les bras ondulent en vague), `strike` (une paume géante part
vers l'avant et frappe : « Main n° 3 »), `zero` (Main zéro : les bras se referment devant, la bouche s'ouvre).
"""
from __future__ import annotations

import math

from chibi import FaceGrid, flat
from geolib import Model, material, rgb, shade

GOLD = rgb('#e6b443')
GOLD_L = rgb('#ffd978')
GOLD_D = rgb('#a8741e')
JEWEL = rgb('#e8463a')
JADE = rgb('#4ac0a0')


def gold(k=1.0, edge=0.8):
    return material(shade(GOLD, k), top=1.18, bottom=0.78, edge=edge, noise=3, grad=0.2)


def face_paint(ctx):
    gold(1.05)(ctx)
    if ctx.face != 'north':
        return
    f = FaceGrid(ctx)
    ink = GOLD_D
    f.rect(30, 14, 36, 19, JEWEL)                                 # urna
    for x0, x1 in ((10, 28), (38, 56)):                           # sourcils en arc
        f.poly((x0, 22, (x0 + x1) / 2, 18, x1, 22, x1, 24, (x0 + x1) / 2, 20, x0, 24), ink)
    for x0 in (12, 40):                                           # yeux mi-clos (paupières baissées)
        f.poly((x0, 30, x0 + 14, 30, x0 + 12, 33, x0 + 2, 33), shade(GOLD_D, 0.8))
        f.rect(x0 + 1, 29, x0 + 13, 30, ink)
    f.poly((31, 30, 35, 30, 37, 42, 29, 42), shade(GOLD, 1.12))   # nez
    f.rect(29, 42, 37, 43, ink)
    f.poly((25, 48, 33, 50, 41, 48, 41, 49, 33, 52, 25, 49), shade(GOLD_D, 0.9))   # sourire serein


def build():
    m = Model('summon_kannon', density=2, tex_width=512)
    m.bone('body', [0, 0, 0])
    m.bone('torso', [0, 8, 0], 'body')
    m.bone('head', [0, 33, 0], 'torso')

    # couronne de pétales de lotus (socle)
    m.bone('lotus', [0, 0, 0], 'body')
    for i in range(14):
        a = i * 360 / 14
        r = math.radians(a)
        x, z = 13 * math.sin(r), 13 * math.cos(r) * 0.8
        m.cube('lotus', [x - 4, 0, z - 1], [8, 9, 2], gold(0.95 if i % 2 else 1.05), pivot=[x, 0, z],
               rotation=[-22 * math.cos(r), a, 0])
    m.cube('lotus', [-12, 0, -9], [24, 6, 18], gold(0.85))

    # buste : robe, ventre, poitrine, épaules arrondies
    m.cube('torso', [-8, 5, -5], [16, 12, 10], gold(0.95))
    m.cube('torso', [-9, 16, -5.5], [18, 10, 11], gold(1.0))
    m.cube('torso', [-10, 23, -5], [20, 6, 10], gold(1.02))
    for sx in (-1, 1):
        m.cube('torso', [sx * 10 - 3, 22, -4.5], [6, 7, 9], gold(1.05), pivot=[sx * 10, 25, 0], rotation=[0, 0, -sx * 20])
    m.cube('torso', [-3, 28, -3], [6, 6, 6], gold(1.0))           # cou
    # écharpe drapée en travers et colliers
    m.cube('torso', [-2, 6, -6.2], [4, 24, 1.2], gold(1.15), pivot=[0, 18, -6], rotation=[0, 0, 32])
    for k, (y, w) in enumerate(((27, 12), (24.5, 9))):
        m.cube('torso', [-w / 2, y, -6.0], [w, 1.2, 1.0], flat(GOLD_L, 0.9))
        m.cube('torso', [-1, y - 2.0, -6.4], [2, 2, 1], flat(JEWEL if k == 0 else JADE, 0.9))
    m.cube('torso', [-8.5, 15, -6.0], [17, 2, 1.0], gold(0.8))    # ceinture

    # tête : visage, lobes, chignon, couronne
    m.cube('head', [-6, 34, -5.5], [12, 13, 11], face_paint)
    for sx in (-1, 1):
        m.cube('head', [sx * 6.5 - 1, 35, -1.5], [2, 7, 3], gold(1.0))          # longs lobes
        m.cube('head', [sx * 6.7 - 0.8, 34, -1.0], [1.6, 2, 2], flat(JEWEL, 0.9))
    m.cube('head', [-6.5, 43, -5.5], [13, 5, 11.5], gold(0.75))                 # cheveux (or sombre)
    m.cube('head', [-7, 44.5, -6.2], [14, 2.5, 12.4], gold(1.15))              # couronne
    for k, x in enumerate((-5, 0, 5)):
        m.cube('head', [x - 1.5, 45.5, -7.0], [3, 4.5 if k == 1 else 3.5, 1], gold(1.2))
        m.cube('head', [x - 0.8, 46.5, -7.3], [1.6, 1.6, 0.6], flat(JEWEL if k == 1 else JADE, 0.9))
    for k, (w, h) in enumerate(((9, 4), (7, 4), (5, 3.5), (3, 3))):           # haut chignon
        y = 48 + sum(hh for _, hh in ((9, 4), (7, 4), (5, 3.5), (3, 3))[:k])
        m.cube('head', [-w / 2, y, -w / 2 + 0.5], [w, h, w], gold(0.75 + 0.05 * k))
    m.cube('head', [-1, 62.5, -0.5], [2, 2, 2], flat(JEWEL, 0.9))

    # halo derrière la tête et grande auréole derrière le buste (anneaux de segments)
    m.bone('halo', [0, 40, 8], 'torso')
    for i in range(16):
        a = math.radians(i * 360 / 16)
        x, y = 11 * math.sin(a), 40 + 11 * math.cos(a)
        m.cube('halo', [x - 2.4, y - 0.9, 8], [4.8, 1.8, 1], gold(1.2), pivot=[x, y, 8], rotation=[0, 0, -i * 360 / 16])
    m.cube('halo', [-7, 33, 8.5], [14, 14, 0.6], flat(shade(GOLD_L, 1.05), 0.95))
    for i in range(26):
        a = math.radians(-130 + i * 260 / 25)
        x, y = 27 * math.sin(a), 28 + 27 * math.cos(a)
        m.cube('halo', [x - 3.6, y - 0.8, 11], [7.2, 1.6, 1], gold(1.1), pivot=[x, y, 11],
               rotation=[0, 0, -math.degrees(a)])

    # éventail des mille mains : 11 bras par côté, du bas vers le haut, qui partent du dos
    for sx, side in ((-1, 'r'), (1, 'l')):
        for i in range(11):
            ang = -50 + i * 13                     # 0 = horizontal, > 0 vers le haut
            name = f'fan_{side}{i}'
            sh = [sx * 7, 22 + i * 0.6, 6 + 0.25 * i]
            m.bone(name, sh, 'torso', rotation=[0, 0, -sx * (90 - ang)])
            L = 15 + 4 * math.cos(math.radians(ang)) + (i % 2) * 2
            x0, y0, z0 = sh
            m.cube(name, [x0 - 1.4, y0, z0 - 1.4], [2.8, L * 0.55, 2.8], gold(0.95))
            m.cube(name, [x0 - 1.2, y0 + L * 0.5, z0 - 1.2], [2.4, L * 0.5, 2.4], gold(1.0))
            m.cube(name, [x0 - 1.7, y0 + L * 0.5 - 0.6, z0 - 1.7], [3.4, 1.2, 3.4], flat(GOLD_L, 0.9))   # bracelet
            # paume ouverte (face vers l'avant) et doigts
            m.cube(name, [x0 - 2.2, y0 + L, z0 - 0.9], [4.4, 4, 1.8], gold(1.1))
            for f in range(4):
                m.cube(name, [x0 - 2.0 + f * 1.1, y0 + L + 4, z0 - 0.6], [0.9, 2.6 - 0.4 * abs(f - 1.5), 1.2], gold(1.12))
            m.cube(name, [x0 + sx * 2.0 - (1 if sx < 0 else 0), y0 + L + 1, z0 - 0.6], [1.0, 2.2, 1.2], gold(1.12),
                   pivot=[x0 + sx * 2.2, y0 + L + 1, z0], rotation=[0, 0, sx * 30])

    # bras avant, mains jointes en prière devant la poitrine
    for sx, side in ((-1, 'r'), (1, 'l')):
        m.bone(f'arm_{side}', [sx * 10, 25, 0], 'torso', rotation=[-10, 0, sx * 6])
        m.cube(f'arm_{side}', [sx * 10 - 2.5, 14, -2.5], [5, 12, 5], gold(1.0))
        m.bone(f'forearm_{side}', [sx * 10, 15, 0], f'arm_{side}', rotation=[-48, 0, sx * 37])
        m.cube(f'forearm_{side}', [sx * 10 - 2.2, 15, -2.2], [4.4, 12, 4.4], gold(1.02))
        m.cube(f'forearm_{side}', [sx * 10 - 2.6, 23, -2.6], [5.2, 1.4, 5.2], flat(GOLD_L, 0.9))
        m.cube(f'forearm_{side}', [sx * 10 - 2.6, 26.5, -1.0], [5.2, 7, 2.0], gold(1.12))   # paume dressée
    # paume géante de frappe : repliée derrière l'auréole au repos, elle part vers l'avant (strike)
    m.bone('palm', [6, 28, 9.5], 'torso', rotation=[62, 0, -14])
    m.cube('palm', [3.5, 28, 7], [5, 24, 5], gold(0.98))
    m.cube('palm', [1.5, 52, 6], [9, 9, 3], gold(1.1))
    for f in range(4):
        m.cube('palm', [1.8 + f * 2.2, 61, 6.5], [1.8, 5 - abs(f - 1.5), 2], gold(1.12))
    m.cube('palm', [10.0, 53, 6.5], [2, 5, 2], gold(1.12), pivot=[10, 53, 7.5], rotation=[0, 0, -35])

    fan = [f'fan_{s}{i}' for s in 'rl' for i in range(11)]
    idle_bones = {}
    for name in fan:                      # vague qui parcourt l'éventail (sinus échantillonné, boucle de 4 s)
        sx = -1 if name[4] == 'r' else 1
        ph = int(name[5:]) / 11
        keys = {}
        for k in range(9):
            t = k * 0.5
            w = math.sin(2 * math.pi * (t / 4 + ph))
            keys[f'{t:.1f}'] = [round(-6 * w, 2), 0, round(sx * 5 * w, 2)]
        idle_bones[name] = {'rotation': keys}
    idle_bones['body'] = {'position': {'0.0': [0, 0, 0], '2.0': [0, 1.2, 0], '4.0': [0, 0, 0]}}
    idle_bones['halo'] = {'rotation': {'0.0': [0, 0, 0], '4.0': [0, 0, 8]}}
    m.animations = {
        'appear': {'loop': False, 'animation_length': 0.7, 'bones': {
            'body': {'position': {'0.0': [0, -40, 0], '0.5': [0, 2, 0], '0.7': [0, 0, 0]},
                     'scale': {'0.0': [0.6, 0.6, 0.6], '0.5': [1.04, 1.04, 1.04], '0.7': [1, 1, 1]}},
            **{n: {'rotation': {'0.0': [0, 0, (-1 if n[4] == 'r' else 1) * 70], '0.6': [0, 0, 0]}} for n in fan}}},
        'idle': {'loop': True, 'animation_length': 4.0, 'bones': idle_bones},
        'strike': {'loop': False, 'animation_length': 0.9, 'bones': {
            'palm': {'rotation': {'0.0': [0, 0, 0], '0.25': [0, 0, 10], '0.4': [-150, 0, 14], '0.7': [-150, 0, 14],
                                  '0.9': [0, 0, 0]},
                     'scale': {'0.0': [1, 1, 1], '0.4': [1.6, 1.6, 1.6], '0.7': [1.6, 1.6, 1.6], '0.9': [1, 1, 1]}},
            'torso': {'rotation': {'0.0': [0, 0, 0], '0.4': [-8, -10, 0], '0.9': [0, 0, 0]}}}},
        'zero': {'loop': False, 'animation_length': 1.6, 'bones': {
            **{n: {'rotation': {'0.0': [0, 0, 0], '0.6': [-70, (-1 if n[4] == 'r' else 1) * -40, 0],
                                '1.6': [-70, (-1 if n[4] == 'r' else 1) * -40, 0]}} for n in fan},
            'head': {'rotation': {'0.0': [0, 0, 0], '0.6': [-10, 0, 0], '1.6': [-10, 0, 0]}}}},
    }
    return m


if __name__ == '__main__':
    build().save()
