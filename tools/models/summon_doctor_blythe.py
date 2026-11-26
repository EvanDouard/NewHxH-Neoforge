"""
Docteur Blythe, la poupée-chirurgienne de Neferpitou — invocation GeckoLib (Geo-Fx).

Grande poupée (≈ 3 blocs) qui flotte au-dessus du patient : tête de porcelaine ronde trop grosse, recousue
(coutures en croix sur la joue et le front), yeux-boutons noirs, joues roses, bouche cousue ; coiffe d'infirmière
à croix rouge ; robe-cloche blanche bouffante avec tablier et ceinture rouges ; quatre bras, deux qui finissent
en mains de poupée tenant une aiguille et un fil, deux plus longs terminés par des instruments (pince et
scalpel) ; pas de jambes : le bas de la robe flotte, d'où pend le cordon qui la relie à la nuque de Pitou
(cordon dessiné par l'effet).
Animations : `appear`, `idle` (elle opère : les quatre bras cousent en boucle, la tête suit, le corps flotte).
"""
from __future__ import annotations

from chibi import FaceGrid, cloth, flat
from geolib import Model, material, rgb, shade

PORC = rgb('#f6e8e0')
DRESS = rgb('#f4f1ea')
RED = rgb('#d8303a')
INK = rgb('#141414')
STEEL = rgb('#cfd6de')


def porcelain(k=1.0):
    return material(shade(PORC, k), top=1.06, bottom=0.88, edge=0.84, noise=2, grad=0.1)


def face_paint(ctx):
    porcelain(1.02)(ctx)
    if ctx.face != 'north':
        return
    f = FaceGrid(ctx)
    for x0 in (13, 39):                                           # yeux-boutons
        f.rect(x0, 24, x0 + 14, 38, INK)
        f.rect(x0 + 3, 26, x0 + 6, 29, (240, 240, 240))
        f.rect(x0 + 6, 30, x0 + 8, 32, rgb('#3a3a3a'))
    f.rect(8, 40, 16, 45, rgb('#f2a0a8'))
    f.rect(50, 40, 58, 45, rgb('#f2a0a8'))
    f.rect(24, 49, 42, 50, INK)                                   # bouche cousue
    for x in range(25, 42, 4):
        f.rect(x, 47, x + 1, 52, INK)
    for (x0, y0, x1, y1) in ((44, 8, 60, 10), (50, 4, 52, 16), (4, 44, 12, 46), (6, 40, 8, 50)):   # coutures
        f.rect(x0, y0, x1, y1, rgb('#6a4a4a'))


def build():
    m = Model('summon_doctor_blythe', density=4, tex_width=512)
    dress = cloth(DRESS, DRESS, shade(DRESS, 0.8), ao_bottom=0.1, folds=4, seed=500)
    m.bone('body', [0, 0, 0])
    m.bone('torso', [0, 20, 0], 'body')
    m.bone('head', [0, 32, 0], 'torso')
    # robe-cloche : tranches qui s'élargissent vers le bas, ourlet festonné
    for k, (y, w, h) in enumerate(((20, 12, 4), (16, 15, 4), (12, 18, 4), (8, 20, 4), (5, 21, 3))):
        m.cube('body', [-w / 2, y, -w / 2 * 0.85], [w, h, w * 0.85], dress)
    for i in range(8):
        x = -9.5 + i * 2.7
        m.cube('body', [x, 3.8, -9.4], [2.2, 1.4, 0.6], flat(shade(DRESS, 0.9), 0.85))
    m.cube('body', [-6, 6, -9.2], [12, 14, 0.6], cloth(shade(DRESS, 1.02), seed=501))      # tablier
    m.cube('body', [-6.6, 18.5, -5.6], [13.2, 1.6, 11.2], flat(RED, 0.9))                  # ceinture
    m.cube('body', [-1.5, 17.6, 4.8], [3, 3, 1.2], flat(RED, 0.9))                          # nœud dans le dos
    # buste et col
    m.cube('torso', [-5, 20, -4], [10, 10, 8], dress)
    m.cube('torso', [-6, 28.5, -4.6], [12, 2, 9.2], flat(shade(DRESS, 0.94), 0.85))
    m.cube('torso', [-1, 26, -4.4], [2, 2, 0.6], flat(RED, 0.9))
    # tête de porcelaine trop grosse, arrondie
    m.cube('head', [-7, 32, -6.8], [14, 13, 13], face_paint)
    m.cube('head', [-6, 31, -6.0], [12, 15, 12], porcelain(0.97))
    m.cube('head', [-7.8, 33.5, -6.0], [15.6, 10, 12], porcelain(0.96))
    m.cube('head', [-6, 33, -6.6], [12, 11, 13.6], porcelain(0.99))
    # coiffe d'infirmière et croix
    m.cube('head', [-6.5, 45, -5.5], [13, 3, 11], flat(rgb('#ffffff'), 0.85))
    m.cube('head', [-5.5, 47.6, -4.5], [11, 2, 7], flat(rgb('#ffffff'), 0.85))
    m.cube('head', [-2.4, 45.8, -5.9], [4.8, 1.4, 0.5], flat(RED, 0.9))
    m.cube('head', [-0.7, 44.4, -5.9], [1.4, 4.4, 0.5], flat(RED, 0.9))
    # quatre bras : deux mains (aiguille, fil), deux longs bras à instruments
    arms = (('r0', -1, 28, 32, 'needle'), ('l0', 1, 28, 32, 'thread'), ('r1', -1, 24, 58, 'pliers'),
            ('l1', 1, 24, 58, 'scalpel'))
    for name, sx, y, splay, tool in arms:
        b = f'arm_{name}'
        m.bone(b, [sx * 5.5, y, 0], 'torso', rotation=[50, 0, -sx * splay])
        long_ = tool in ('pliers', 'scalpel')
        L = 13 if long_ else 9
        m.cube(b, [sx * 5.5 - 1.4, y - L, -1.4], [2.8, L, 2.8], dress if not long_ else porcelain(0.94))
        m.cube(b, [sx * 5.5 - 1.6, y - L - 0.4, -1.6], [3.2, 1.0, 3.2], flat(RED, 0.9))
        hy = y - L - 3
        if tool == 'needle':
            m.cube(b, [sx * 5.5 - 1.5, hy, -1.5], [3, 3, 3], porcelain(1.0))
            m.cube(b, [sx * 5.5 - 0.2, hy - 6, -0.2], [0.4, 6, 0.4], flat(STEEL, 1.0))
        elif tool == 'thread':
            m.cube(b, [sx * 5.5 - 1.5, hy, -1.5], [3, 3, 3], porcelain(1.0))
            m.cube(b, [sx * 5.5 - 1.2, hy - 2.5, -1.2], [2.4, 2.5, 2.4], flat(rgb('#c84a5a'), 0.9))   # bobine
        elif tool == 'pliers':
            for k in (-1, 1):
                m.cube(b, [sx * 5.5 - 0.3 + k * 0.6, hy - 2, -0.3], [0.6, 5, 0.6], flat(STEEL, 1.0),
                       pivot=[sx * 5.5, hy + 3, 0], rotation=[0, 0, k * 10])
        else:
            m.cube(b, [sx * 5.5 - 0.4, hy, -0.4], [0.8, 3, 0.8], flat(rgb('#3a3a44'), 0.9))
            m.cube(b, [sx * 5.5 - 0.15, hy - 4, -1.0], [0.3, 4, 2.0], flat(STEEL, 1.0))
    m.cube('body', [-1, 0, 2], [2, 5, 2], flat(rgb('#c06a8a'), 0.9))             # départ du cordon

    sew = {}
    for i, (name, sx, *_rest) in enumerate(arms):
        o = 0.25 * i
        sew[f'arm_{name}'] = {'rotation': {'0.0': [0, 0, 0], f'{0.2 + o:.2f}': [-25, 0, sx * 10],
                                           f'{0.45 + o:.2f}': [10, 0, -sx * 6], '1.2': [0, 0, 0]}}
    sew['head'] = {'rotation': {'0.0': [8, 0, 0], '0.6': [12, 12, 4], '1.2': [8, 0, 0]}}
    m.animations = {
        'appear': {'loop': False, 'animation_length': 0.5, 'bones': {
            'body': {'scale': {'0.0': [0.05, 0.05, 0.05], '0.35': [1.1, 1.1, 1.1], '0.5': [1, 1, 1]}}}},
        # au repos, elle opère : les quatre bras cousent en boucle, la tête suit, le corps flotte
        'idle': {'loop': True, 'animation_length': 1.2, 'bones': {
            **sew, 'body': {'position': {'0.0': [0, 0, 0], '0.6': [0, 0.6, 0], '1.2': [0, 0, 0]}}}},
    }
    return m


if __name__ == '__main__':
    build().save()
