"""
Singe à visage humain (marais de Numele, le faux Satotz de l'Examen) — modèle GeckoLib `beast_man_faced_ape`.

Un grand singe maigre et voûté : pelage gris-brun hirsute et terne, très longs bras qui descendent sous les genoux
(grandes mains aux doigts crochus), jambes courtes et arquées, gros pieds préhensiles. Ce qui dérange, c'est le
visage : une face d'homme glabre et pâle, nez droit, lèvres minces au sourire figé, yeux humains enfoncés au regard
fixe, encadrée d'une collerette de poils. Hauteur ≈ 1,95 bloc (boîte 0,6 × 1,95).
"""
from __future__ import annotations

from beastkit import biped_animations, fur, on_face
from chibi import FaceGrid, flat
from geolib import Model, layered, material, only, rgb, shade

COAT = rgb('#6a6058')
COAT_D = rgb('#423a34')
FACE = rgb('#d9c8b6')
FACE_D = rgb('#a89482')
PALM = rgb('#4a3e38')


def human_face(ctx):
    g = FaceGrid(ctx)
    d = ctx.draw
    # orbites creuses et yeux humains fixes
    for x0 in (12, 38):
        g.rect(x0 - 1, 16, x0 + 17, 28, shade(FACE_D, 0.9))
        g.rect(x0 + 2, 19, x0 + 14, 25, '#f2eee6')
        g.rect(x0 + 6, 19, x0 + 10, 25, '#3a2c22')
        g.rect(x0 + 7, 20, x0 + 9, 22, '#0e0a08')
        g.rect(x0, 14, x0 + 16, 16, COAT_D)                          # sourcils
    # nez droit
    g.rect(30, 26, 36, 40, shade(FACE, 0.94))
    g.rect(27, 38, 39, 42, FACE_D)
    g.rect(28, 40, 31, 42, '#5a3e34')
    g.rect(35, 40, 38, 42, '#5a3e34')
    # sourire figé, lèvres minces
    g.poly([16, 48, 50, 48, 46, 51, 20, 51], '#7a4a42')
    g.rect(16, 46, 18, 48, '#7a4a42')
    g.rect(48, 46, 50, 48, '#7a4a42')
    # rides des joues
    g.rect(14, 38, 16, 46, FACE_D)
    g.rect(50, 38, 52, 46, FACE_D)


def build():
    m = Model('beast_man_faced_ape', density=4, tex_width=512)
    m.bone('root', [0, 0, 0])
    m.bone('body', [0, 13, 0], 'root', rotation=[-22, 0, 0])
    coat = fur(COAT, streaks=1.0, dark=COAT_D, seed=1)
    coat2 = fur(shade(COAT, 0.9), streaks=1.2, dark=COAT_D, seed=2)
    m.cube('body', [-4.5, 12, -2.6], [9, 13, 5.2], coat)
    m.cube('body', [-5.5, 21, -3.2], [11, 6, 6.4], coat2)                # épaules voûtées
    m.cube('body', [-3.5, 14, -3.2], [7, 7, 1.0], flat(shade(FACE_D, 0.85), 0.9))   # ventre peu poilu
    # tête
    m.bone('head', [0, 27, -1], 'body', rotation=[18, 0, 0])
    m.cube('head', [-4, 26.5, -5], [8, 8.5, 7.5], layered(coat, only(('north',), material(FACE, noise=3, grad=0.08))))
    m.cube('head', [-3.6, 26.8, -5.2], [7.2, 7.6, 0.3], on_face('north', material(FACE, noise=3, grad=0.08), human_face))
    # collerette de poils autour du visage
    m.cube('head', [-5, 25.5, -4.6], [10, 1.6, 7], coat2)
    for sx in (-1, 1):
        m.cube('head', [sx * 4.6 - 0.9, 26, -4.8], [1.8, 9, 6.5], coat2)
    m.cube('head', [-4.6, 34.4, -4.8], [9.2, 1.8, 7.5], coat2)
    m.bone('jaw', [0, 27, -3], 'head')

    # très longs bras, grandes mains crochues
    for sx, name in ((1, 'arm_l'), (-1, 'arm_r')):
        x = sx * 6.5
        m.bone(name, [x, 25, 0], 'body', rotation=[24, 0, sx * 6])
        m.cube(name, [x - 1.6, 13, -1.6], [3.2, 13, 3.2], coat)
        m.cube(name, [x - 1.4, 6.5, -1.4], [2.8, 7.5, 2.8], coat2, pivot=[x, 13.5, 0], rotation=[10, 0, 0])
        m.cube(name, [x - 1.8, 2.5, -2.4], [3.6, 4.6, 3.4], flat(PALM, 0.85), pivot=[x, 13.5, 0], rotation=[10, 0, 0])
        for f in range(3):
            m.cube(name, [x - 1.5 + f * 1.1, 0.8, -2.6], [0.8, 2.4, 0.9], flat(PALM, 0.8), pivot=[x, 13.5, 0],
                   rotation=[-18, 0, 0])

    # jambes courtes arquées, pieds préhensiles
    for sx, name in ((1, 'leg_l'), (-1, 'leg_r')):
        x = sx * 2.6
        m.bone(name, [x, 12.5, 0], 'root', rotation=[0, 0, -sx * 6])
        m.cube(name, [x - 1.8, 6, -1.8], [3.6, 7, 3.6], coat)
        m.cube(name, [x - 1.5, 1.4, -1.0], [3.0, 5.5, 3.0], coat2, pivot=[x, 6.5, 0], rotation=[14, 0, 0])
        m.cube(name, [x - 2.0, 0, -4.0], [4.0, 1.6, 5.4], flat(PALM, 0.85))
        m.cube(name, [x + sx * 2.0 - 0.6, 0, -2.8], [1.2, 1.4, 2.2], flat(PALM, 0.85))      # pouce du pied

    m.animations = biped_animations(walk_len=0.8, step=20, attack='claw', arms=True, wings=('arm_l', 'arm_r'), fly=False)
    return m


if __name__ == '__main__':
    build().save()
