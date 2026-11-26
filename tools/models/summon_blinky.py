"""
Blinky, l'aspirateur de Shizuku — invocation GeckoLib (Geo-Fx).

Vieil aspirateur-traîneau doté d'un visage (≈ 1 bloc de haut) : réservoir bombé violet avec deux gros yeux
ronds à paupières et une large bouche (l'embouchure) aux lèvres épaisses, petites roues, poignée sur le dessus ;
tuyau annelé qui serpente jusqu'au manche tenu par Shizuku, embout à brosse. Animations : `appear`, `idle` (les yeux
clignent, la bouche aspire : elle se gonfle et se creuse), `strike` (grande aspiration).
"""
from __future__ import annotations

from chibi import FaceGrid, flat
from geolib import Model, material, rgb, shade

BODY = rgb('#7a5ab8')
BODY_L = rgb('#a888e0')
METAL = rgb('#c8ccd4')
INK = rgb('#141018')
LIP = rgb('#e85a7a')


def body(k=1.0):
    return material(shade(BODY, k), top=1.12, bottom=0.82, edge=0.78, noise=2, grad=0.14)


def face_paint(ctx):
    body(1.04)(ctx)
    if ctx.face != 'north':
        return
    f = FaceGrid(ctx)
    for x0 in (8, 38):                                                # gros yeux ronds à paupières
        f.rect(x0, 10, x0 + 20, 30, (252, 252, 252))
        f.rect(x0 + 7, 16, x0 + 14, 26, INK)
        f.rect(x0 + 8, 17, x0 + 10, 19, (255, 255, 255))
        f.rect(x0, 8, x0 + 20, 13, shade(BODY, 0.8))
    f.poly((10, 38, 56, 38, 50, 54, 16, 54), LIP)                      # bouche aux lèvres épaisses
    f.poly((16, 41, 50, 41, 46, 51, 20, 51), INK)


def build():
    m = Model('summon_blinky', density=6, tex_width=512)
    m.bone('body', [0, 0, 0])
    m.bone('tank', [0, 2, 0], 'body')
    # réservoir bombé (face avant = visage)
    m.cube('tank', [-5, 2, -4.5], [10, 9, 9], face_paint)
    m.cube('tank', [-5.6, 3, -3.8], [11.2, 7, 7.6], body(0.96))
    m.cube('tank', [-4.2, 1.4, -3.8], [8.4, 10.2, 7.6], body(0.98))
    m.cube('tank', [-1.6, 11.2, -1.0], [3.2, 2.0, 2.0], flat(METAL, 0.9))           # poignée
    m.cube('tank', [-2.6, 12.6, -1.0], [5.2, 1.0, 2.0], flat(METAL, 0.9))
    for sx in (-1, 1):                                                                 # petites roues
        for z in (-2.6, 2.6):
            m.cube('tank', [sx * 5.0 - 0.8, 0, z - 1.4], [1.6, 2.8, 2.8], flat(INK, 0.9))
    # tuyau annelé qui serpente vers l'arrière-haut, puis manche et embout
    m.bone('hose', [0, 7, 4.5], 'tank')
    pts = [(0, 7, 5), (0.6, 8.4, 7), (1.4, 10.2, 8.4), (2.0, 12.4, 9.2), (2.4, 14.6, 9.4), (2.6, 16.8, 9.0)]
    for k, (x, y, z) in enumerate(pts):
        m.cube('hose', [x - 1.1, y - 1.1, z - 1.1], [2.2, 2.2, 2.2], flat(shade(METAL, 0.7 if k % 2 else 0.85), 0.9))
    m.cube('hose', [2.0, 17, 8.4], [1.2, 6, 1.2], flat(METAL, 0.95))
    m.cube('hose', [0.8, 22.6, 7.6], [3.6, 1.2, 2.8], flat(INK, 0.9))
    m.animations = {
        'appear': {'loop': False, 'animation_length': 0.4, 'bones': {
            'body': {'scale': {'0.0': [0.2, 0.2, 0.2], '0.3': [1.1, 1.1, 1.1], '0.4': [1, 1, 1]}}}},
        'idle': {'loop': True, 'animation_length': 1.0, 'bones': {
            'tank': {'scale': {'0.0': [1, 1, 1], '0.5': [1.06, 0.95, 1.06], '1.0': [1, 1, 1]},
                     'rotation': {'0.0': [0, -4, 0], '0.5': [0, 4, 0], '1.0': [0, -4, 0]}},
            'hose': {'rotation': {'0.0': [0, 0, 0], '0.5': [-6, 0, 4], '1.0': [0, 0, 0]}}}},
        'strike': {'loop': False, 'animation_length': 0.6, 'bones': {
            'tank': {'scale': {'0.0': [1, 1, 1], '0.2': [1.25, 0.85, 1.25], '0.45': [0.9, 1.1, 0.9], '0.6': [1, 1, 1]}}}},
    }
    return m


if __name__ == '__main__':
    build().save()
