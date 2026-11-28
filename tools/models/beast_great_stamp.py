"""
Great Stamp, cochon géant de la forêt de Biska (épreuve de Buhara) — modèle GeckoLib `beast_great_stamp`.

Un énorme cochon tout en masse : corps rond et lourd sur de courtes pattes à sabots, peau rose-beige aux soies
clairsemées, crête de soies brunes sur l'échine, tête énorme portée bas dont le museau et le front sont couverts
d'une épaisse plaque osseuse grise (le « nez blindé » qui encaisse les coups de face), deux larges narines, petites
défenses, petits yeux enfoncés et méchants, oreilles tombantes, petite queue en tire-bouchon.
Hauteur ≈ 1,9 bloc (boîte 1,9 × 1,9).
"""
from __future__ import annotations

from beastkit import fur, on_face, quad_animations, quad_leg
from chibi import flat
from geolib import Model, layered, material, only, rgb, shade

HIDE = rgb('#d79a86')
HIDE_D = rgb('#a86a5a')
BRISTLE = rgb('#6a4030')
BONE = rgb('#9a958c')
BONE_D = rgb('#6c675f')
HOOF = rgb('#3a2a26')
TUSK = rgb('#efe6cf')


def hide(seed=0, k=1.0):
    return fur(shade(HIDE, k), noise=6, streaks=0.18, dark=HIDE_D, seed=seed)


def armor(seed=0):
    base = material(BONE, top=1.15, bottom=0.8, edge=0.7, noise=10, grad=0.22, seed=seed)

    def paint(ctx):
        base(ctx)
        d = ctx.draw
        for i in range(1, 4):       # fissures / rides de la plaque
            y = round(ctx.h * i / 4)
            d.line([0, y, ctx.w - 1, y + (1 if i % 2 else -1)], fill=BONE_D, width=1)
    return paint


def small_eyes(ctx):
    d, W, H = ctx.draw, ctx.w, ctx.h
    for a, b in ((0.06, 0.24), (0.76, 0.94)):
        box = [round(a * W), round(0.2 * H), round(b * W), round(0.33 * H)]
        d.rectangle(box, fill=(30, 14, 10, 255))
        d.rectangle([box[0] + 1, box[1] + 1, box[0] + 2, box[1] + 1], fill=(220, 60, 40, 255))
        d.line([box[0] - 1, box[1] - 2, box[2] + 1, box[1]], fill=HIDE_D, width=2)


def build():
    m = Model('beast_great_stamp', density=3, tex_width=512)
    m.bone('root', [0, 0, 0])
    m.bone('body', [0, 18, 0], 'root')
    # corps rond : plusieurs pavés emboîtés
    m.cube('body', [-14, 9, -14], [28, 20, 26], layered(hide(1), only(('down',), hide(2, 0.9))))
    m.cube('body', [-12, 7, -12], [24, 24, 22], hide(3))
    m.cube('body', [-12.5, 10, 12], [25, 17, 5], hide(4, 0.96))          # croupe
    m.cube('body', [-12.5, 10, -17], [25, 18, 4], hide(5))               # épaules
    # crête de soies sur l'échine
    for i in range(9):
        z = -16 + i * 3.6
        m.cube('body', [-1.5, 30.5, z], [3, 2.6 - 0.12 * abs(i - 4), 3], fur(BRISTLE, noise=8, streaks=1.2, seed=10 + i),
               pivot=[0, 31, z + 1.5], rotation=[-20, 0, 0])

    hoof = flat(HOOF, 0.8)
    quad_leg(m, 'leg_fl', 8.5, 11, -9, 7, 11, 7, hide(6), hoof)
    quad_leg(m, 'leg_fr', -8.5, 11, -9, 7, 11, 7, hide(6), hoof)
    quad_leg(m, 'leg_bl', 8.5, 11, 10, 7.5, 11, 7.5, hide(7), hoof)
    quad_leg(m, 'leg_br', -8.5, 11, 10, 7.5, 11, 7.5, hide(7), hoof)

    # tête énorme, portée bas
    m.bone('head', [0, 20, -17], 'body')
    m.cube('head', [-10, 9, -29], [20, 17, 13], on_face('north', hide(8), small_eyes))
    for sx in (-1, 1):
        m.cube('head', [sx * 10.5 - 2.5, 10, -26], [5, 12, 9], hide(9, 0.95))                 # bajoues
        # oreilles tombantes
        m.cube('head', [sx * 8 - 3, 24, -22], [6, 1.5, 8], hide(11, 0.92), pivot=[sx * 7, 25, -20],
               rotation=[25, 0, sx * 40])
    # plaque osseuse : front + museau (le « nez blindé »)
    m.cube('head', [-6.5, 22.5, -30.2], [13, 4.5, 2.4], armor(1), pivot=[0, 23, -29], rotation=[-20, 0, 0])
    m.cube('head', [-7, 9.5, -37], [14, 10.5, 9], layered(hide(12), only(('up', 'east', 'west'), armor(2))))
    m.cube('head', [-7.5, 9, -39.5], [15, 11.5, 2.8], armor(3))
    for sx in (-1, 1):
        m.cube('head', [sx * 3.4 - 1.6, 12, -39.9], [3.2, 4.2, 0.6], flat(rgb('#2a1612'), 0.85))   # narines
    # mâchoire et défenses
    m.bone('jaw', [0, 11, -28], 'head')
    m.cube('jaw', [-6.5, 7, -38], [13, 4, 10], hide(13, 0.9))
    for sx in (-1, 1):
        m.cube('jaw', [sx * 6.2 - 0.8, 9.5, -38.5], [1.6, 5, 1.6], flat(TUSK, 0.85), pivot=[sx * 6.2, 10, -38],
               rotation=[-12, 0, -sx * 16])

    # petite queue en tire-bouchon
    m.bone('tail', [0, 26, 17], 'body')
    for k in range(4):
        m.cube('tail', [-0.8 + (k % 2) * 1.0 - 0.5, 26 - k * 1.3, 17 + k * 0.7], [1.6, 1.6, 1.6], hide(14 + k, 0.9))

    m.animations = quad_animations(swing=20, walk_len=0.9, attack='charge', tail_wag=14)
    return m


if __name__ == '__main__':
    build().save()
