"""
Démon d'aura de Razor (les 14 Démons, Jeu de la balle) — invocation GeckoLib (Geo-Fx).

Petit diablotin trapu (≈ 1,4 bloc) : grosse tête ronde rouge sombre, deux cornes courbes, grands yeux jaunes
en amande, énorme sourire plein de dents, oreilles pointues ; corps rond avec un numéro peint sur le ventre,
petits bras griffus levés (prêt à smasher), jambes courtes, queue fourchue ; petites ailes de chauve-souris.
Animations : `appear`, `idle` (sautille, ailes qui battent), `strike` (smash : il saute et frappe des deux mains).
"""
from __future__ import annotations

from chibi import FaceGrid, flat
from geolib import Model, material, rgb, shade

RED = rgb('#b02a30')
RED_D = rgb('#6a1418')
HORN = rgb('#ece0c8')
INK = rgb('#140808')
EYE = rgb('#ffd23a')


def red(k=1.0):
    return material(shade(RED, k), top=1.1, bottom=0.82, edge=0.8, noise=3, grad=0.16)


def face_paint(ctx):
    red(1.04)(ctx)
    if ctx.face != 'north':
        return
    f = FaceGrid(ctx)
    for x0, slope in ((10, 1), (38, -1)):
        f.poly((x0, 20 + 4 * (slope < 0), x0 + 18, 20 + 4 * (slope > 0), x0 + 16, 32, x0 + 2, 32), EYE)
        f.rect(x0 + 7, 23, x0 + 11, 31, INK)
    f.poly((8, 40, 58, 40, 50, 54, 16, 54), INK)                      # énorme sourire
    for x in range(10, 56, 6):
        f.poly((x, 40, x + 5, 40, x + 2.5, 45), (250, 246, 236))
        f.poly((x + 3, 54, x + 8, 54, x + 5.5, 49), (250, 246, 236))


def belly_paint(ctx):
    red(0.98)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.rect(20, 14, 46, 46, rgb('#f2e6d0'))                         # plastron avec numéro « 7 »
        f.rect(25, 19, 41, 23, INK)
        f.poly((37, 23, 41, 23, 32, 42, 28, 42), INK)


def build():
    m = Model('summon_demon', density=6, tex_width=512)
    m.bone('body', [0, 0, 0])
    m.bone('head', [0, 12, 0], 'body')
    m.cube('body', [-4.5, 4, -3.5], [9, 9, 7], belly_paint)                           # corps rond
    m.cube('body', [-5.2, 5, -3.0], [10.4, 7, 6], red(0.95))
    m.cube('head', [-5.5, 12.5, -5.0], [11, 10, 10], face_paint)
    m.cube('head', [-6.2, 13.5, -4.2], [12.4, 8, 8.4], red(0.97))
    m.cube('head', [-4.8, 12.0, -4.2], [9.6, 11.2, 8.4], red(0.99))
    for sx in (-1, 1):
        # cornes courbes en deux segments
        m.cube('head', [sx * 3.6 - 0.9, 21.5, -1.6], [1.8, 3.4, 1.8], flat(HORN, 0.9), pivot=[sx * 3.6, 21.5, -0.7],
               rotation=[0, 0, -sx * 25])
        m.cube('head', [sx * 4.9 - 0.6, 24.2, -1.3], [1.2, 2.6, 1.2], flat(HORN, 0.9), pivot=[sx * 4.9, 24.2, -0.7],
               rotation=[0, 0, sx * 20])
        # oreilles pointues
        m.cube('head', [sx * 6.4 - 0.8, 16, -0.8], [1.6, 3.2, 1.0], red(0.9), pivot=[sx * 6.2, 16, 0],
               rotation=[0, 0, -sx * 50])
        # petites ailes
        m.bone(f'wing_{side_name(sx)}', [sx * 3.0, 11, 3.2], 'body', rotation=[0, -sx * 30, 0])
        m.cube(f'wing_{side_name(sx)}', [sx * 3.0 - (5.5 if sx < 0 else 0), 7, 3.0], [5.5, 6, 0.4], flat(RED_D, 0.85))
        # bras griffus levés
        b = f'arm_{side_name(sx)}'
        m.bone(b, [sx * 4.6, 11, 0], 'body', rotation=[0, 0, sx * 130])
        m.cube(b, [sx * 4.6 - 1.1, 6, -1.1], [2.2, 5, 2.2], red(1.0))
        for k in range(3):
            m.cube(b, [sx * 4.6 - 1.0 + k * 0.8, 5.0, -0.3], [0.5, 1.2, 0.6], flat(HORN, 0.9))
        # jambes courtes
        l = f'leg_{side_name(sx)}'
        m.bone(l, [sx * 2.2, 4, 0], 'body')
        m.cube(l, [sx * 2.2 - 1.3, 0, -1.3], [2.6, 4.2, 2.6], red(0.9))
        m.cube(l, [sx * 2.2 - 1.4, 0, -2.4], [2.8, 1.0, 3.4], flat(RED_D, 0.9))
    # queue fourchue
    m.cube('body', [-0.5, 4.5, 3.4], [1.0, 1.0, 5.0], red(0.92), pivot=[0, 5, 3.5], rotation=[-30, 0, 0])
    m.cube('body', [-1.4, 7.6, 7.2], [2.8, 1.6, 0.6], flat(RED_D, 0.9), pivot=[0, 5, 3.5], rotation=[-30, 0, 0])
    m.animations = {
        'appear': {'loop': False, 'animation_length': 0.4, 'bones': {
            'body': {'scale': {'0.0': [0.1, 0.1, 0.1], '0.25': [1.15, 1.15, 1.15], '0.4': [1, 1, 1]}}}},
        'idle': {'loop': True, 'animation_length': 0.8, 'bones': {
            'body': {'position': {'0.0': [0, 0, 0], '0.4': [0, 1.4, 0], '0.8': [0, 0, 0]}},
            'wing_r': {'rotation': {'0.0': [0, 0, 0], '0.2': [0, -40, 0], '0.4': [0, 0, 0], '0.6': [0, -40, 0], '0.8': [0, 0, 0]}},
            'wing_l': {'rotation': {'0.0': [0, 0, 0], '0.2': [0, 40, 0], '0.4': [0, 0, 0], '0.6': [0, 40, 0], '0.8': [0, 0, 0]}}}},
        'strike': {'loop': False, 'animation_length': 0.5, 'bones': {
            'body': {'position': {'0.0': [0, 0, 0], '0.2': [0, 6, 0], '0.5': [0, 0, 0]}},
            'arm_r': {'rotation': {'0.0': [0, 0, 0], '0.2': [0, 0, 20], '0.3': [-120, 0, -40], '0.5': [0, 0, 0]}},
            'arm_l': {'rotation': {'0.0': [0, 0, 0], '0.2': [0, 0, -20], '0.3': [-120, 0, 40], '0.5': [0, 0, 0]}}}},
    }
    return m


def side_name(sx):
    return 'r' if sx < 0 else 'l'


if __name__ == '__main__':
    build().save()
