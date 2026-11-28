"""
Mike, le chien de garde géant du domaine Zoldyck — modèle GeckoLib `beast_mike` (version de l'anime de 2011).

Sources (wiki Hunterpedia) : chien énorme ; fourrure pourpre de Tyr dans l'anime de 2011 (blanche dans le manga et
l'anime de 1999), langue rose (bleue dans le manga) ; silhouette fine aux longues pattes, oreilles pointues, museau
fin et allongé, longue queue touffue ; pattes avant aux longues griffes recourbées, plus proches d'une main humaine
que d'une patte de chien ; aucune émotion, regard de machine. Inspiré du barzoï.

D'où : poitrail très profond et taille très creusée, dos arqué, long cou, tête étroite en coin au museau très
long, grands yeux ronds et vides (blanc entier, pupille minuscule) qui fixent droit devant, franges de poils plus
clairs sous le ventre, derrière les pattes et sous la queue, mains griffues, langue rose qui dépasse.
Environ 3,5 blocs jusqu'aux oreilles (modèle à taille réelle, l'entité garde son échelle ×3).
"""
from __future__ import annotations

from beastkit import fur, quad_animations, seg
from chibi import _fin, flat
from geolib import Model, layered, material, only, rgb, shade

COAT = rgb('#6c3d64')       # pourpre de Tyr adouci, comme l'anime
COAT_D = rgb('#4a2744')
COAT_L = rgb('#93648a')     # franges et dessous
SKIN = rgb('#3a2236')       # truffe, babines, contour des yeux
CLAW = rgb('#e8ddc8')
WHITE = rgb('#f6f4ee')
TONGUE = rgb('#e2798a')


def coat(seed=0, k=1.0, streaks=0.7):
    return fur(shade(COAT, k), noise=5, streaks=streaks * 0.4, dark=shade(COAT, 0.8), seed=seed)


def fringe(seed=0):
    return fur(COAT_L, noise=5, streaks=0.8, dark=shade(COAT_L, 0.84), seed=seed)


def eye_paint(ctx):
    """Œil rond et vide : grand disque blanc cerclé de sombre, pupille minuscule au centre."""
    material(SKIN, noise=0, grad=0)(ctx)
    if ctx.face != 'north':
        return
    d, W, H = ctx.draw, ctx.w, ctx.h
    m = max(1, W // 10)
    d.ellipse([m, m, W - 1 - m, H - 1 - m], fill=WHITE)
    cx, cy = W // 2, H // 2
    d.rectangle([cx - 1, cy - 1, cx, cy], fill=(14, 10, 14, 255))


def hand(m, bone, x, z, sx):
    """Patte avant « main » : paume, quatre longs doigts, griffes recourbées, pouce."""
    m.cube(bone, [x - 3, 0.6, z - 3.5], [6, 2.6, 5.5], coat(20, 0.9, 0.3))
    for i in range(4):
        fx = x - 2.4 + i * 1.6
        L = 4.2 if i in (1, 2) else 3.4
        m.cube(bone, [fx - 0.65, 0.4, z - 3.5 - L], [1.3, 1.6, L], coat(21 + i, 0.88, 0.2))
        m.cube(bone, [fx - 0.45, 0.4, z - 3.5 - L - 1.6], [0.9, 1.1, 1.8], flat(CLAW, 0.85),
               pivot=[fx, 1.0, z - 3.5 - L], rotation=[25, 0, 0])
    tx = x + sx * 3.0
    m.cube(bone, [tx - 0.6, 1.2, z - 3.0], [1.2, 1.4, 2.6], coat(25, 0.88, 0.2), pivot=[tx, 1.8, z - 2],
           rotation=[0, -sx * 30, 0])
    m.cube(bone, [tx - 0.4, 0.8, z - 4.4], [0.8, 1.0, 1.6], flat(CLAW, 0.85), pivot=[tx, 1.8, z - 2],
           rotation=[0, -sx * 30, 0])


def build():
    m = Model('beast_mike', density=4, tex_width=1024)
    m.bone('root', [0, 0, 0])
    m.bone('body', [0, 34, 0], 'root')

    # --- tronc de lévrier : poitrail très profond, taille creusée, dos arqué ---
    m.cube('body', [-6.5, 23, -21], [13, 17, 16], layered(coat(1), only(('down',), fringe(2))))
    m.cube('body', [-5.5, 21, -19], [11, 3, 12], fringe(3))                                          # quille
    m.cube('body', [-5, 31, -6], [10, 9.5, 9], coat(4, 0.96))
    m.cube('body', [-4, 33.5, 3], [8, 7.5, 8], coat(5, 0.94))                                        # taille
    m.cube('body', [-5.8, 31, 10], [11.6, 10, 10], coat(6))                                          # hanches
    m.cube('body', [-4.5, 40, -18], [9, 2, 13], coat(7, 1.06, 0.5))                                  # garrot
    m.cube('body', [-3.8, 40.5, -6], [7.6, 2.2, 18], coat(8, 1.06, 0.5), pivot=[0, 41, 3], rotation=[-3, 0, 0])
    m.cube('body', [-4.6, 39.5, 11], [9.2, 2.5, 9], coat(9, 1.04, 0.5), pivot=[0, 41, 11], rotation=[8, 0, 0])
    for i, z in enumerate((-6, -2, 2, 6)):          # franges sous le ventre
        h = 3.6 - i * 0.6
        m.cube('body', [-3.6, 31 - h + (i * 0.9), z], [7.2, h, 4], fringe(10 + i))

    # --- long cou et jabot : du garrot jusque sous le crâne ---
    seg(m, 'body', 0, (37, -16), (51, -31), 8, 8.5, coat(14))
    seg(m, 'body', 0, (33, -19), (47, -31), 8.4, 5, fringe(15))
    for sx in (-1, 1):
        seg(m, 'body', sx * 3.6, (37, -17), (49, -29), 2.4, 8, coat(16, 0.94, 1.0))

    # --- tête étroite en coin ---
    m.bone('head', [0, 52, -31], 'body')
    m.cube('head', [-4.6, 48, -35], [9.2, 8, 9], coat(17, 1.0, 0.4))
    m.cube('head', [-4.0, 55.6, -34], [8.0, 1.4, 7.5], coat(18, 1.08, 0.3))
    for sx in (-1, 1):
        m.cube('head', [sx * 4.6 - 1.0, 48, -33], [2.0, 5.5, 6.5], coat(19, 0.92, 0.3))
    m.cube('head', [-2.8, 48.4, -46], [5.6, 4.8, 11.5], layered(coat(26, 1.0, 0.25), only(('down',), fringe(27))))
    m.cube('head', [-2.2, 48.6, -51], [4.4, 3.8, 5.5], coat(28, 1.0, 0.2))
    m.cube('head', [-2.4, 52.6, -45], [4.8, 1.6, 10], coat(29, 1.06, 0.2), pivot=[0, 53, -40], rotation=[-4, 0, 0])
    m.cube('head', [-1.5, 50.6, -52.2], [3.0, 2.0, 1.6], flat(SKIN, 0.8))                         # truffe
    m.bone('jaw', [0, 48.6, -36], 'head')
    m.cube('jaw', [-2.4, 46.6, -50], [4.8, 2.0, 14], coat(30, 0.9, 0.2))
    m.cube('jaw', [-2.0, 48.4, -49], [4.0, 0.4, 12], flat(shade(TONGUE, 0.7), 0.9))
    m.cube('jaw', [0.6, 44.2, -45], [2.0, 3.6, 2.6], material(TONGUE, top=1.1, bottom=0.8, edge=0.8, noise=3),
           pivot=[1.6, 48, -44], rotation=[0, 0, 12])                                              # langue
    for sx in (-1, 1):
        m.cube('head', [sx * 3.5 - 1.9, 51.2, -35.6], [3.8, 3.8, 0.6], eye_paint,
               pivot=[sx * 3.5, 53, -35.3], rotation=[0, sx * 22, 0])
        m.cube('head', [sx * 3.5 - 2.2, 54.9, -35.8], [4.4, 1.1, 1.4], coat(31, 0.85, 0.2),
               pivot=[sx * 3.5, 55, -35], rotation=[0, sx * 22, 0])                               # arcade
        m.bone(f'ear{sx}', [sx * 3.0, 56.6, -30.5], 'head', rotation=[-8, -sx * 8, -sx * 14])
        _fin(m, f'ear{sx}', sx * 3.0, 56.6, -30.5, 8.5, 5.2, 1.8,
             lambda t: fur(shade(COAT, 1.0 - 0.18 * t), noise=6), ((0.0, 0.6, 0.92, 1.0), (0.6, 1.0, 0.6, 0.85)))
        _fin(m, f'ear{sx}', sx * 3.0, 57.0, -31.5, 6.0, 3.2, 0.2, lambda t: flat(COAT_L, 0.9), ((0.0, 1.0, 0.4, 1.0),))

    # --- pattes de devant : longues et fines, franges derrière, mains griffues ---
    for sx, name in ((1, 'leg_fl'), (-1, 'leg_fr')):
        x = sx * 4.6
        m.bone(name, [x, 33, -14], 'root')
        m.cube(name, [x - 2.6, 22, -16.8], [5.2, 12, 5.6], coat(32))
        m.cube(name, [x - 1.7, 3.0, -15.6], [3.4, 19.5, 3.4], coat(33, 0.95, 0.5))
        m.cube(name, [x - 1.5, 6, -12.4], [3.0, 13, 2.2], fringe(34))
        m.cube(name, [x - 1.9, 2.0, -15.9], [3.8, 2.6, 3.8], coat(35, 0.9, 0.3))
        hand(m, name, x, -12.6, sx)

    # --- pattes arrière : cuisse puissante, genou, jarret haut, long métatarse ---
    for sx, name in ((1, 'leg_bl'), (-1, 'leg_br')):
        x = sx * 4.8
        hip, knee, hock, foot = (35, 15), (21, 12.5), (10, 20), (1.6, 18.5)
        m.bone(name, [x, hip[0], hip[1]], 'root')
        seg(m, name, x, hip, knee, 6, 9, coat(40))                                               # cuisse
        seg(m, name, x, (33, 19), (22, 17.5), 5.4, 3, fringe(41))                                 # culotte
        seg(m, name, x, knee, hock, 3.8, 4, coat(42, 0.95, 0.5))                                  # jambe
        seg(m, name, x, hock, foot, 3.2, 3.2, coat(43, 0.92, 0.4))                                # métatarse
        m.cube(name, [x - 2.4, 0, 14.0], [4.8, 2.4, 6.5], coat(44, 0.9, 0.3))                     # pied
        for i in range(3):
            fx = x - 1.6 + i * 1.6
            m.cube(name, [fx - 0.45, 0, 12.6], [0.9, 1.0, 1.6], flat(CLAW, 0.85))

    # --- longue queue touffue, basse puis relevée au bout ---
    m.bone('tail', [0, 39, 20], 'body', rotation=[62, 0, 0])
    m.cube('tail', [-1.8, 37.2, 19.5], [3.6, 3.6, 9], coat(50))
    m.cube('tail', [-1.4, 34.6, 20.5], [2.8, 3.0, 8], fringe(51))
    m.bone('tail2', [0, 39, 28], 'tail', rotation=[-24, 0, 0])
    m.cube('tail2', [-1.7, 37.3, 27.5], [3.4, 3.4, 9], coat(52))
    m.cube('tail2', [-1.5, 33.6, 28], [3.0, 4.0, 8.5], fringe(53))
    m.bone('tail3', [0, 39, 36], 'tail2', rotation=[-30, 0, 0])
    m.cube('tail3', [-1.5, 37.5, 35.5], [3.0, 3.0, 7], coat(54))
    m.cube('tail3', [-1.3, 34.4, 36], [2.6, 3.4, 6.5], fringe(55))

    anims = quad_animations(swing=24, walk_len=1.05, attack='bite', tail_wag=5, head_bob=2)
    # immobilité de machine : au repos la queue ne remue pas, seule la respiration bouge
    anims['idle']['bones']['tail'] = {'rotation': {'0.0': [0, 0, 0], '3.0': [0, 0, 0]}}
    anims['walk']['bones']['tail2'] = {'rotation': {'0.0': [0, -6, 0], '0.52': [0, 6, 0], '1.05': [0, -6, 0]}}
    m.animations = anims
    return m


if __name__ == '__main__':
    build().save()
