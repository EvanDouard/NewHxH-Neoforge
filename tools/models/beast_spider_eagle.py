"""
Aigle-araignée (falaises du Mont Split, épreuve des œufs) — modèle GeckoLib `beast_spider_eagle`.

Un grand rapace debout : plumage brun sombre strié, tête et cou blancs, bec crochu jaune à pointe sombre, œil
jaune perçant sous une arcade marquée, larges ailes repliées aux rémiges presque noires, queue en éventail, et de
longues pattes jaunes fines et articulées (d'où son nom) terminées par de grandes serres. En vol les ailes
s'ouvrent en grand. Hauteur ≈ 2 blocs debout (boîte 1,2 × 2,0).
"""
from __future__ import annotations

from beastkit import biped_animations, fur, on_face
from chibi import flat
from geolib import Model, layered, material, only, rgb, shade

BROWN = rgb('#5e4430')
BROWN_D = rgb('#33241a')
BROWN_L = rgb('#8a6a4a')
WHITE = rgb('#f2efe6')
YELLOW = rgb('#e8b830')
BEAK_D = rgb('#3a3026')


def feathers(base, seed=0, rows=4, dark=None):
    """Plumes : rangées d'écailles en chevrons."""
    dark = rgb(dark) if dark else shade(rgb(base), 0.7)
    mat = material(base, top=1.1, bottom=0.8, edge=0.8, noise=6, grad=0.2, seed=seed)

    def paint(ctx):
        mat(ctx)
        if ctx.face in ('up', 'down'):
            return
        d = ctx.draw
        step = max(2, ctx.h // max(1, rows * 2))
        for y in range(step, ctx.h, step):
            off = (y // step) % 2 * step
            for x in range(-off, ctx.w, step * 2):
                d.line([x, y, x + step, y - step // 2, x + step * 2, y], fill=dark, width=1)
    return paint


def eagle_face(side):
    def draw(ctx):
        d, W, H = ctx.draw, ctx.w, ctx.h
        x = round(W * (0.3 if side == 'west' else 0.7))
        y = round(H * 0.42)
        r = max(2, W // 7)
        d.ellipse([x - r, y - r, x + r, y + r], fill=YELLOW)
        d.ellipse([x - r // 2, y - r // 2, x + r // 2, y + r // 2], fill=(20, 14, 10, 255))
        d.line([x - r - 2, y - r - 1, x + r + 2, y - r - 2], fill=BROWN_D, width=2)
    return draw


def build():
    m = Model('beast_spider_eagle', density=4, tex_width=512)
    m.bone('root', [0, 0, 0])
    m.bone('body', [0, 14, 0], 'root', rotation=[-18, 0, 0])
    body = feathers(BROWN, seed=1, rows=5, dark=BROWN_D)
    m.cube('body', [-5.5, 12, -4.5], [11, 13, 9], layered(body, only(('north',), feathers(BROWN_L, seed=2, rows=5))))
    m.cube('body', [-4.5, 11, -5.2], [9, 8, 2], feathers(BROWN_L, seed=3, rows=4))          # poitrine
    m.cube('body', [-4, 24, -3.5], [8, 3, 7], feathers(WHITE, seed=4, dark='#c8c2b2'))      # base du cou blanche
    # tête blanche
    m.bone('head', [0, 27, -1], 'body', rotation=[18, 0, 0])
    head = feathers(WHITE, seed=5, rows=3, dark='#cfc8b8')
    head = on_face('east', on_face('west', head, eagle_face('west')), eagle_face('east'))
    m.cube('head', [-3.5, 26, -5], [7, 7, 7.5], head)
    for sx in (-1, 1):
        m.cube('head', [sx * 2.6 - 1.2, 31.6, -5.4], [2.4, 1.2, 4], flat(shade(WHITE, 0.82), 0.85),
               pivot=[sx * 2.6, 32, -4], rotation=[0, 0, sx * 10])                       # arcades
    # bec crochu
    m.cube('head', [-1.6, 27.6, -8.8], [3.2, 3.2, 4], flat(YELLOW, 0.85))
    m.cube('head', [-1.2, 26.4, -10.2], [2.4, 3.6, 1.8], flat(YELLOW, 0.85), pivot=[0, 30, -9], rotation=[-25, 0, 0])
    m.cube('head', [-0.8, 25.4, -10.6], [1.6, 1.4, 1.2], flat(BEAK_D, 0.85), pivot=[0, 30, -9], rotation=[-25, 0, 0])
    m.bone('jaw', [0, 27.4, -5], 'head')
    m.cube('jaw', [-1.3, 26.6, -8.4], [2.6, 1.0, 3.6], flat(shade(YELLOW, 0.9), 0.85))

    # ailes repliées (bord d'attaque en haut, rémiges sombres vers le bas et l'arrière)
    for sx, name in ((1, 'wing_l'), (-1, 'wing_r')):
        m.bone(name, [sx * 5.5, 24, 0], 'body')
        x0 = 5.5 if sx > 0 else -7.5
        m.cube(name, [x0, 13, -4], [2, 11.5, 9], feathers(BROWN, seed=6, rows=4, dark=BROWN_D))
        m.cube(name, [x0 + (0.3 if sx > 0 else -0.3), 6, -2], [1.4, 8, 9], feathers(BROWN_D, seed=7, rows=3, dark='#1a120c'))
        m.cube(name, [x0 + (0.6 if sx > 0 else -0.6), 3, 1], [0.8, 4, 6], flat(BROWN_D, 0.85))
        m.cube(name, [x0 - (0.3 if sx > 0 else -0.3), 21, -4.5], [2.4, 3.5, 9.5], feathers(BROWN_L, seed=8, rows=2))

    # queue en éventail
    m.bone('tail', [0, 13, 4], 'body', rotation=[30, 0, 0])
    for k, ry in enumerate((-22, -8, 8, 22)):
        m.cube('tail', [-1.6, 3, 3.5], [3.2, 10, 1.2], feathers(BROWN_D if k % 2 else BROWN, seed=9 + k, rows=3),
               pivot=[0, 13, 4], rotation=[0, 0, ry])

    # longues pattes fines articulées, serres
    leg = material(YELLOW, top=1.05, bottom=0.85, edge=0.75, noise=4, grad=0.1)
    talon = flat(rgb('#1e1a16'), 0.8)
    for sx, name in ((1, 'leg_l'), (-1, 'leg_r')):
        x = sx * 2.6
        m.bone(name, [x, 13, 0], 'root')
        m.cube(name, [x - 1.8, 9.5, -2.2], [3.6, 4.5, 4.4], feathers(BROWN, seed=13, rows=2))   # culotte
        m.cube(name, [x - 0.7, 1.2, -0.7], [1.4, 9, 1.4], leg)
        m.cube(name, [x - 0.9, 5.5, -0.9], [1.8, 1.2, 1.8], leg)                               # articulation
        for k, ry in enumerate((-28, 0, 28)):
            m.cube(name, [x - 0.5, 0, -4.4], [1.0, 1.2, 4], leg, pivot=[x, 0.6, 0], rotation=[0, ry, 0])
            m.cube(name, [x - 0.4, 0, -5.2], [0.8, 1.4, 1.0], talon, pivot=[x, 0.6, 0], rotation=[0, ry, 0])
        m.cube(name, [x - 0.5, 0, 0], [1.0, 1.2, 2.6], leg)

    m.animations = biped_animations(flap=60, walk_len=0.8, step=20, attack='peck')
    return m


if __name__ == '__main__':
    build().save()
