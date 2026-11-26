"""
Le clown de Kite (Crazy Slots) — invocation GeckoLib (Geo-Fx).

Pantin de clown (≈ 1,3 bloc) qui jaillit au-dessus de Kite : visage blanc grimé, nez rouge, grand sourire
moqueur, losanges sous les yeux, chapeau de bouffon à trois pointes à grelots ; collerette à volants ; le corps est
un tambour de machine à sous avec la roulette des chiffres 1 à 9 sur le ventre et une manette sur le côté ; il
finit en ressort (diable à ressort). Animations : `appear` (il jaillit du ressort), `idle` (il se dandine en riant,
la roulette tourne), `strike` (on tire la manette : il s'agite).
"""
from __future__ import annotations

from chibi import FaceGrid, flat
from geolib import Model, material, rgb, shade

WHITE = rgb('#fbf8f2')
RED = rgb('#e0303a')
PINK = rgb('#ff6fb5')
BLUE = rgb('#3a6ad8')
YELLOW = rgb('#ffd23a')
INK = rgb('#1a1418')
METAL = rgb('#c8ccd4')


def mat(c, k=1.0):
    return material(shade(c, k), top=1.08, bottom=0.86, edge=0.82, noise=2, grad=0.12)


def face_paint(ctx):
    mat(WHITE, 1.02)(ctx)
    if ctx.face != 'north':
        return
    f = FaceGrid(ctx)
    for x0 in (12, 38):
        f.poly((x0 + 8, 18, x0 + 16, 27, x0 + 8, 36, x0, 27), BLUE)    # losanges
        f.poly((x0 + 3, 26, x0 + 13, 26, x0 + 11, 29, x0 + 5, 29), INK)  # yeux plissés de rire
    f.rect(29, 32, 37, 39, RED)                                         # nez
    f.poly((10, 42, 56, 42, 48, 54, 18, 54), RED)                       # grand sourire
    f.poly((16, 44, 50, 44, 44, 51, 22, 51), INK)
    f.rect(22, 44, 44, 46, (255, 255, 255))


def slot_paint(ctx):
    mat(PINK, 1.0)(ctx)
    if ctx.face != 'north':
        return
    f = FaceGrid(ctx)
    f.rect(8, 14, 58, 46, (252, 250, 240))
    f.rect(8, 14, 58, 16, INK)
    f.rect(8, 44, 58, 46, INK)
    for k, x in enumerate((12, 28, 44)):                                 # trois rouleaux « 7 »
        f.rect(x, 20, x + 10, 23, RED)
        f.poly((x + 7, 23, x + 10, 23, x + 5, 40, x + 2, 40), RED)


def build():
    m = Model('summon_clown', density=6, tex_width=512)
    m.bone('body', [0, 0, 0])
    m.bone('spring', [0, 0, 0], 'body')
    for k in range(5):                                                   # ressort
        m.cube('spring', [-2.2, k * 1.2, -2.2], [4.4, 0.6, 4.4], flat(METAL, 0.9), pivot=[0, k * 1.2, 0],
               rotation=[0, k * 20, 0])
    m.bone('torso', [0, 6, 0], 'body')
    m.cube('torso', [-4.5, 6, -3.5], [9, 8, 7], slot_paint)              # tambour à roulette
    m.cube('torso', [-4.8, 5.6, -3.8], [9.6, 1, 7.6], flat(YELLOW, 0.9))
    m.cube('torso', [-4.8, 13.6, -3.8], [9.6, 1, 7.6], flat(YELLOW, 0.9))
    m.bone('lever', [4.8, 10, 0], 'torso')
    m.cube('lever', [4.6, 9.4, -0.5], [1.2, 1.2, 1.0], flat(METAL, 0.9))
    m.cube('lever', [5.4, 9.8, -0.4], [0.8, 5.0, 0.8], flat(METAL, 0.95))
    m.cube('lever', [5.1, 14.6, -0.7], [1.4, 1.4, 1.4], flat(RED, 0.95))
    # collerette à volants
    for i in range(10):
        m.cube('torso', [-1.2, 14.2, -5.0], [2.4, 1.4, 3.4], mat(WHITE if i % 2 else BLUE, 1.0), pivot=[0, 14.8, 0],
               rotation=[0, i * 36, 0])
    # tête et chapeau à trois pointes
    m.bone('head', [0, 15, 0], 'torso')
    m.cube('head', [-3.8, 15.4, -3.6], [7.6, 7.4, 7.2], face_paint)
    m.cube('head', [-4.2, 22.4, -3.9], [8.4, 1.2, 7.8], mat(PINK, 0.9))
    for k, (rz, rx, c) in enumerate(((-55, 0, PINK), (0, -15, BLUE), (55, 0, YELLOW))):
        m.cube('head', [-1.6, 23.2, -1.6], [3.2, 6.0, 3.2], mat(c), pivot=[0, 23.2, 0], rotation=[rx, 0, rz])
        m.cube('head', [-0.9, 28.6, -0.9], [1.8, 1.8, 1.8], flat(YELLOW, 0.95), pivot=[0, 23.2, 0], rotation=[rx, 0, rz])
    # petits bras gantés
    for sx, side in ((-1, 'r'), (1, 'l')):
        m.bone(f'arm_{side}', [sx * 4.5, 13, 0], 'torso', rotation=[0, 0, sx * 70])
        m.cube(f'arm_{side}', [sx * 4.5 - 0.8, 8.4, -0.8], [1.6, 4.6, 1.6], mat(BLUE))
        m.cube(f'arm_{side}', [sx * 4.5 - 1.2, 7.0, -1.2], [2.4, 2.0, 2.4], mat(WHITE))
    m.animations = {
        'appear': {'loop': False, 'animation_length': 0.5, 'bones': {
            'torso': {'position': {'0.0': [0, -8, 0], '0.2': [0, 3, 0], '0.35': [0, -1, 0], '0.5': [0, 0, 0]}},
            'spring': {'scale': {'0.0': [1, 0.1, 1], '0.2': [1, 1.6, 1], '0.35': [1, 0.85, 1], '0.5': [1, 1, 1]}}}},
        'idle': {'loop': True, 'animation_length': 1.0, 'bones': {
            'torso': {'rotation': {'0.0': [0, 0, -8], '0.5': [0, 0, 8], '1.0': [0, 0, -8]}},
            'head': {'rotation': {'0.0': [0, 0, 6], '0.5': [0, 0, -6], '1.0': [0, 0, 6]}},
            'arm_r': {'rotation': {'0.0': [0, 0, 0], '0.5': [0, 0, -25], '1.0': [0, 0, 0]}},
            'arm_l': {'rotation': {'0.0': [0, 0, 25], '0.5': [0, 0, 0], '1.0': [0, 0, 25]}}}},
        'strike': {'loop': False, 'animation_length': 0.6, 'bones': {
            'lever': {'rotation': {'0.0': [0, 0, 0], '0.15': [90, 0, 0], '0.4': [90, 0, 0], '0.6': [0, 0, 0]}},
            'torso': {'position': {'0.0': [0, 0, 0], '0.2': [0, 1.5, 0], '0.35': [0, -0.5, 0], '0.6': [0, 0, 0]}}}},
    }
    return m


if __name__ == '__main__':
    build().save()
