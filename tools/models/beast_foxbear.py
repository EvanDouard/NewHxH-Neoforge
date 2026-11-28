"""
Renard-ours (île de la Baleine) — bête du bestiaire, modèle GeckoLib `beast_foxbear`.

Un ours massif à tête de renard : corps lourd au garrot bossu, épaisse fourrure roux-brun, poitrail et museau
crème, tête de renard au museau long et pointu (truffe noire, crocs), grandes oreilles triangulaires dressées aux
pointes sombres, petits yeux jaunes féroces sous des arcades froncées, grosses pattes griffues, queue de renard
touffue au bout crème. Hauteur ≈ 1,9 bloc (boîte 1,8 × 1,9).
"""
from __future__ import annotations

from beastkit import eyes_drawer, fur, on_face, quad_animations, quad_leg
from chibi import _fin, flat
from geolib import Model, layered, material, only, rgb, shade

RUST = rgb('#9a5a2c')
RUST_D = rgb('#6a3a1c')
CREAM = rgb('#e8d2a8')
DARK = rgb('#2a1a12')
CLAW = rgb('#e8e0cc')


def build():
    m = Model('beast_foxbear', density=3, tex_width=512)
    m.bone('root', [0, 0, 0])
    m.bone('body', [0, 16, 0], 'root')
    coat = fur(RUST, streaks=0.9, dark=RUST_D, seed=1)
    coat2 = fur(shade(RUST, 0.92), streaks=1.0, dark=RUST_D, seed=2)
    chest = layered(coat, only(('down',), fur(CREAM, noise=5)))
    # tronc : épaules hautes (bosse), croupe plus basse
    m.cube('body', [-12, 11, -16], [24, 17, 16], chest)                 # avant-train
    m.cube('body', [-6.5, 11.5, -18.5], [13, 11, 3], layered(coat, only(('north',), fur(CREAM, noise=5, streaks=0.4))))  # poitrail
    m.cube('body', [-9, 26, -14], [18, 4, 11], coat2)                    # bosse du garrot
    m.cube('body', [-11, 11.5, 0], [22, 14.5, 16], chest)                # arrière-train
    m.cube('body', [-9.5, 24, 1], [19, 2.5, 13], coat2)                  # dos
    # touffes du cou
    for sx in (-1, 1):
        m.cube('body', [sx * 11 - 1.5, 15, -17], [3, 9, 6], coat2, pivot=[sx * 11, 20, -14], rotation=[0, sx * 12, sx * 10])

    # pattes (avant épaisses)
    paw = fur(RUST_D, streaks=0.6, dark=DARK, seed=3)
    claws = flat(CLAW, 0.85)
    quad_leg(m, 'leg_fl', 8, 14, -10, 7, 14, 7, coat, paw, parent='root', claws=claws, paw_len=8.5)
    quad_leg(m, 'leg_fr', -8, 14, -10, 7, 14, 7, coat, paw, parent='root', claws=claws, paw_len=8.5)
    quad_leg(m, 'leg_bl', 8, 14, 10, 6.5, 14, 7, coat, paw, parent='root', claws=claws, paw_len=8)
    quad_leg(m, 'leg_br', -8, 14, 10, 6.5, 14, 7, coat, paw, parent='root', claws=claws, paw_len=8)
    for sx in (-1, 1):   # cuisses arrière plus larges
        m.cube('leg_bl' if sx > 0 else 'leg_br', [sx * 8 - 4, 9, 6], [8, 8, 9], coat2)

    # tête de renard
    m.bone('head', [0, 22, -18], 'body')
    face = fur(RUST, streaks=0.5, dark=RUST_D, seed=4)
    brow = DARK
    m.cube('head', [-6.5, 17, -27], [13, 11, 10],
           on_face('north', face, eyes_drawer('#f2d44a', '#d8a020', '#1a0e08', y0=0.22, y1=0.48, x0=0.06, x1=0.3,
                                              brow=brow)))
    # joues crème
    for sx in (-1, 1):
        m.cube('head', [sx * 5.2 - 1.8, 16.6, -27.4], [3.6, 5, 6], layered(fur(RUST, seed=12), only(('down', 'north'), fur(CREAM, noise=4))), pivot=[sx * 6, 19, -23],
               rotation=[0, 0, sx * 14])
    # museau long et pointu
    snout = layered(fur(CREAM, noise=4), only(('up', 'east', 'west'), fur(RUST, streaks=0.4, dark=RUST_D, seed=5)))
    m.cube('head', [-3.5, 18, -36], [7, 6, 9.5], snout)
    m.cube('head', [-2.5, 21.6, -34], [5, 2.6, 7], fur(RUST, seed=6), pivot=[0, 22, -30], rotation=[-8, 0, 0])
    m.cube('head', [-1.6, 21.2, -37.2], [3.2, 2.4, 1.6], flat(DARK, 0.8))                        # truffe
    # mâchoire et crocs
    m.bone('jaw', [0, 18, -27], 'head')
    m.cube('jaw', [-3, 15.4, -35], [6, 2.8, 8.5], fur(CREAM, noise=4))
    m.cube('jaw', [-2.6, 18.0, -34.6], [5.2, 0.4, 7.5], flat(rgb('#8a2a24'), 0.9))
    for sx in (-1, 1):
        m.cube('head', [sx * 2.4 - 0.4, 16.8, -35.6], [0.8, 1.6, 0.8], claws)
        m.cube('jaw', [sx * 2.2 - 0.4, 17.6, -34.8], [0.8, 1.4, 0.8], claws)
    # grandes oreilles triangulaires (empilement qui s'affine) aux pointes sombres
    for sx in (-1, 1):
        m.bone(f'ear{sx}', [sx * 4.2, 27.4, -21.6], 'head', rotation=[-6, -sx * 10, -sx * 16])
        earfur = lambda t: fur(shade(RUST, 1.0 - 0.15 * t), noise=6) if t < 0.62 else flat(DARK, 0.85)
        _fin(m, f'ear{sx}', sx * 4.2, 27.4, -21.6, 8.0, 6.6, 2.2, earfur, ((0.0, 0.55, 0.9, 1.0), (0.55, 1.0, 0.6, 0.8)))
        _fin(m, f'ear{sx}', sx * 4.2, 27.9, -22.8, 5.6, 4.0, 0.2, lambda t: flat(CREAM, 0.9), ((0.0, 1.0, 0.4, 1.0),))
        # arcades froncées
        m.cube('head', [sx * 3.4 - 2.2, 25.6, -27.6], [4.4, 1.3, 2.5], fur(RUST_D, seed=9),
               pivot=[sx * 3.4, 26, -27], rotation=[0, 0, sx * 14])

    # queue de renard touffue, bout crème
    m.bone('tail', [0, 22, 16], 'body', rotation=[42, 0, 0])
    m.cube('tail', [-3.5, 18.5, 15], [7, 7, 7], coat2)
    m.cube('tail', [-4.5, 18, 21], [9, 8, 8], coat)
    m.cube('tail', [-3.5, 18.5, 28.5], [7, 7, 5], fur(CREAM, noise=5))
    m.cube('tail', [-2, 19.5, 33], [4, 5, 2.5], fur(CREAM, noise=5))

    m.animations = quad_animations(swing=22, walk_len=1.1, attack='swipe', tail_wag=8)
    return m


if __name__ == '__main__':
    build().save()
