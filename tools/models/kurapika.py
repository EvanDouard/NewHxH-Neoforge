"""
Kurapika — même style que Gon et Killua (chibi « Minecraft anime », mèches organiques `lock`).
Traits iconiques : carré blond lisse (frange droite légèrement irrégulière, côtés et nuque coupés net à la
mâchoire, pointes qui rentrent vers l'intérieur), visage fin et calme, yeux gris-brun qui deviennent écarlates en
combat (texture kurapika_froid), boucle d'oreille rouge à gauche, tenue du clan Kurta : tunique blanche à manches
longues, grand tabard bleu brodé d'or (devant et dos jusqu'aux genoux, col large), pantalon blanc, chaussures bleu
foncé ; chaînes à la main droite (cinq, une par doigt, avec leurs embouts).
"""
from __future__ import annotations

import os

from chibi import FaceGrid, HEAD, HEAD_Y, cloth, flat, hair_paint, lock, skeleton, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

# ---------------------------------------------------------------------------------------------------------------
# Palette
SKIN = rgb('#fae3d2')
HAIR_LIGHT = rgb('#f7df90')
HAIR_DARK = rgb('#a9822f')
HAIR_RIM = rgb('#fff3c4')
EYE = rgb('#6c5c52')                 # gris-brun calme
EYE_DARK = rgb('#2a1f1a')
SCARLET = rgb('#e3202a')             # yeux écarlates
SCARLET_DARK = rgb('#7a0a10')
LINE = rgb('#3a2a1c')
TUNIC = rgb('#eef0f5')
TUNIC_SHADE = rgb('#c9ccd8')
TABARD = rgb('#2f5fb8')
TABARD_LIGHT = rgb('#4b7ad3')
TABARD_DARK = rgb('#1d3c7e')
GOLD = rgb('#e2b545')
GOLD_DARK = rgb('#a77b22')
PANTS = rgb('#e9e7e0')
SHOE = rgb('#243868')
CHAIN = rgb('#c8ccd4')
RUBY = rgb('#d0182a')


def mk(lit):
    return lambda t: strand_paint(HAIR_DARK, HAIR_LIGHT, t, lit=lit, rim=HAIR_RIM)


# ---------------------------------------------------------------------------------------------------------------
# Étape 1 — cheveux : un carré lisse. Calotte arrondie (deux étages) puis des mèches plates et larges qui
# retombent tout autour de la tête, coupées net (extrémités larges : tip élevé), et qui se courbent vers
# l'intérieur en bas comme un vrai carré. Frange droite faite de mèches distinctes de longueurs un peu différentes.

def build_hair(m):
    """Carré lisse : une calotte aux arêtes adoucies (pans à 45°), puis des pans de cheveux continus sur les
    côtés et à l'arrière, légèrement bombés vers l'extérieur en haut et qui rentrent vers la mâchoire en bas
    (deux tronçons inclinés). Quelques mèches plates posées dessus marquent les séparations sans faire « peigne ».
    Frange droite en mèches distinctes."""
    from chibi import chamfered_cap
    top = HEAD_Y + HEAD[1]
    m.bone('hair', [0, top, 0], 'head')
    lit = lambda k: hair_paint(HAIR_DARK, HAIR_LIGHT, HAIR_RIM, lit=k, seed=4)
    cap, side, back = lit(0.3), lit(0.15), lit(0.02)
    m.cube('hair', [-5.0, top - 2.4, -4.75], [10.0, 3.0, 9.5], cap)                 # calotte
    m.cube('hair', [-4.2, top + 0.6, -4.0], [8.4, 0.8, 8.0], cap)                   # dessus bombé
    chamfered_cap(m, cap, back, x0=-5.0, x1=5.0, y_top=top + 0.6, z0=-4.75, z1=4.75, s=1.6)
    # pans des côtés : partie haute un peu évasée, partie basse qui rentre vers la mâchoire
    for sx, sd in ((-1, 'r'), (1, 'l')):
        m.bone(f'hp_{sd}', [sx * 5.0, top - 0.6, 0], 'hair', rotation=[0, 0, -sx * 4])
        m.cube(f'hp_{sd}', [sx * 5.0 - 0.7, top - 4.6, -4.4], [1.4, 4.2, 8.6], side)
        m.bone(f'hp_{sd}2', [sx * 5.0, top - 4.4, 0], f'hp_{sd}', rotation=[0, 0, sx * 10])
        m.cube(f'hp_{sd}2', [sx * 5.0 - 0.7, top - 7.6, -4.2], [1.4, 3.4, 8.3], side)
    # pan arrière
    m.bone('hp_b', [0, top - 0.6, 4.75], 'hair', rotation=[-4, 0, 0])
    m.cube('hp_b', [-5.4, top - 4.6, 4.05], [10.8, 4.2, 1.4], back)
    m.bone('hp_b2', [0, top - 4.4, 4.75], 'hp_b', rotation=[10, 0, 0])
    m.cube('hp_b2', [-5.2, top - 7.6, 4.05], [10.4, 3.4, 1.4], back)

    # quelques mèches plates en surface : séparations et pointes un peu irrégulières en bas
    bob = dict(flat=0.35, segs=3, root=1.0, tip=0.5, taper_pow=0.3, belly=1.0)
    for sx, sd in ((-1, 'r'), (1, 'l')):
        for i, (z, L, k) in enumerate(((-3.2, 8.0, 0.22), (0.2, 8.4, 0.12), (3.0, 7.8, 0.04))):
            lock(m, f'hs_{sd}{i}', [sx * 5.55, top - 0.2, z], L, 3.2, 0, -sx * 176, mk(k if sx < 0 else k - 0.08),
                 curve=(0, sx * -12), parent='hair', **bob)
    for i, (x, L, k) in enumerate(((-3.0, 8.2, -0.02), (0.2, 8.6, -0.06), (3.2, 8.0, -0.08))):
        lock(m, f'hb_{i}', [x, top - 0.2, 5.3], L, 3.4, 176, 0, mk(k), curve=(12, 0), parent='hair', **bob)
    # frange : 5 mèches droites et distinctes qui s'arrêtent au-dessus des sourcils
    for i, (x, L, rz, k) in enumerate(((-3.7, 2.8, -4, 0.3), (-1.9, 3.3, -2, 0.34), (0.0, 3.0, 1, 0.36),
                                       (1.9, 3.4, 2, 0.32), (3.7, 2.7, 5, 0.28))):
        lock(m, f'hf_{i}', [x, top + 0.2, -4.85], L, 2.6, 186, rz, mk(k), flat=0.5, segs=2, root=1.05, tip=0.55,
             taper_pow=0.35, curve=(-6, 0))


# ---------------------------------------------------------------------------------------------------------------
# Étape 2 — visage : fin et calme. Yeux en amande, iris gris-brun (écarlate en combat), sourcils fins, petite
# bouche neutre.

FACE_D = 7


def face_paint(ctx, mode='calme'):
    red = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 54, 66, 60, shade(SKIN, 0.93))
    f.rect(0, 0, 66, 10, HAIR_DARK)
    f.rect(0, 10, 66, 12, shade(SKIN, 0.9))
    f.rect(0, 0, 5, 48, HAIR_DARK)                       # mèches des côtés qui encadrent le visage
    f.rect(61, 0, 66, 48, HAIR_DARK)
    brow = shade(HAIR_DARK, 0.85)
    if red:                                                # sourcils froncés
        f.poly((11, 18, 27, 21, 27, 23, 11, 20), brow)
        f.poly((55, 18, 39, 21, 39, 23, 55, 20), brow)
    else:
        f.poly((11, 19, 26, 18, 26, 20, 11, 21), brow)
        f.poly((55, 19, 40, 18, 40, 20, 55, 21), brow)
    iris = SCARLET if red else EYE
    for x0, outer_left in ((11, True), (42, False)):
        x1 = x0 + 13
        top = 27 if red else 25
        white = (250, 250, 252)
        if outer_left:
            f.poly((x0, top + 1, x1, top, x1, 38, x0 + 2, 37), white)
        else:
            f.poly((x0, top, x1, top + 1, x1 - 2, 37, x0, 38), white)
        ix = x0 + (4 if outer_left else 1)
        f.rect(ix, top + 1, ix + 8, 38, iris)
        f.rect(ix, 34, ix + 8, 38, shade(iris, 1.2 if not red else 1.1))
        f.rect(ix, top + 1, ix + 8, top + 3, shade(iris, 0.6))
        f.rect(ix + 2, 29, ix + 6, 36, SCARLET_DARK if red else EYE_DARK)
        f.rect(ix + 1, 28, ix + 3, 30, (255, 255, 255) if not red else (255, 200, 200))
        f.rect(x0 - 1, top - 2, x1 + 1, top + 1, LINE)    # paupière
        f.rect(x0 + 2, 39, x1 - 1, 39, shade(SKIN, 0.74))
    f.rect(32, 44, 33, 46, shade(SKIN, 0.83))
    f.rect(29, 51, 36, 51, rgb('#86564a'))
    if red:
        f.rect(8, 23, 58, 25, shade(SKIN, 0.86))           # ombre sur le regard


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * 0.85)], fill=HAIR_DARK)


def build_face(m):
    m.head_cube = m.box('head', (0, 0), HEAD_Y, HEAD, head_paint, density=FACE_D)
    m.box('head', (0, 0), HEAD_Y - 0.6, (2.5, 0.7, 2.5), skin_mat(shade(SKIN, 0.92)))
    m.cube('head', [-0.35, HEAD_Y + HEAD[1] - 45.5 * HEAD[1] / 60, -HEAD[2] / 2 - 0.25], [0.7, 0.42, 0.28],
           material(shade(SKIN, 0.97), noise=1, outline=False))
    # boucle d'oreille rouge à gauche (sous la mèche)
    m.cube('head', [5.05, HEAD_Y + 0.6, -0.5], [0.3, 0.5, 0.3], flat(GOLD, 0.9))
    m.cube('head', [4.95, HEAD_Y - 0.6, -0.6], [0.5, 1.2, 0.5], flat(RUBY, 0.9))


# ---------------------------------------------------------------------------------------------------------------
# Étape 3 — tenue Kurta : tunique blanche, tabard bleu brodé d'or devant et dos (la partie basse est portée par le
# bassin pour ne pas suivre les jambes), col large doré, pantalon blanc, chaussures bleu foncé

def tabard_paint(ctx):
    """Panneau bleu à broderie dorée : bande centrale de losanges, filets latéraux."""
    cloth(TABARD, TABARD_LIGHT, TABARD_DARK, ao_bottom=0.1, folds=1, seed=60)(ctx)
    if ctx.face not in ('north', 'south'):
        return
    f = FaceGrid(ctx)
    f.rect(26, 0, 40, 60, TABARD_DARK)
    for y in range(4, 60, 12):
        f.poly((33, y, 39, y + 6, 33, y + 12, 27, y + 6), GOLD)
        f.poly((33, y + 3, 36, y + 6, 33, y + 9, 30, y + 6), TABARD)
    f.rect(8, 0, 10, 60, GOLD_DARK)
    f.rect(56, 0, 58, 60, GOLD_DARK)


def build_body(m):
    tunic = cloth(TUNIC, shade(TUNIC, 1.03), TUNIC_SHADE, ao_top=0.06, ao_bottom=0.14, folds=2, seed=61)
    gold = cloth(GOLD, shade(GOLD, 1.1), GOLD_DARK, seed=62)
    m.cube('torso', [-3.6, 8.6, -2.1], [7.2, 7.8, 4.2], tunic)                       # tunique
    # col large doré (anneau) et col montant blanc
    m.cube('torso', [-2.4, 16.1, -1.9], [4.8, 0.9, 3.8], tunic)
    m.cube('torso', [-3.3, 15.6, -2.35], [6.6, 0.7, 4.7], gold)
    for side, sx in (('front', -1), ('back', 1)):
        z0 = -2.75 if sx < 0 else 2.15
        # tabard, partie haute (torse) : bleu brodé, bordé d'or
        m.cube('torso', [-2.8, 9.0, z0], [5.6, 6.7, 0.6], tabard_paint)
        m.cube('torso', [-3.0, 9.0, z0 - 0.05 * -sx], [0.35, 6.7, 0.7], gold)
        m.cube('torso', [2.65, 9.0, z0 - 0.05 * -sx], [0.35, 6.7, 0.7], gold)
        # partie basse (bassin) : descend jusqu'au genou
        zb = -2.95 if sx < 0 else 2.35
        m.cube('body', [-2.7, 3.4, zb], [5.4, 6.0, 0.6], tabard_paint)
        m.cube('body', [-2.9, 3.4, zb - 0.05], [0.35, 6.0, 0.7], gold)
        m.cube('body', [2.55, 3.4, zb - 0.05], [0.35, 6.0, 0.7], gold)
        m.cube('body', [-2.9, 3.0, zb - 0.05], [5.8, 0.45, 0.7], gold)                # ourlet doré
    # épaulières du tabard (relient devant et dos)
    for sx in (-1, 1):
        m.cube('torso', [sx * 2.8 - 1.0, 15.5, -2.75], [2.0, 0.6, 5.5], cloth(TABARD, seed=63))
    # bras : manches blanches longues, poignet doré, mains
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 5.5
        m.box(f'{side}_arm', (cx, 0), 11.8, (3.2, 4.0, 3.2), tunic)
        m.box(f'{side}_arm', (cx, 0), 14.0, (3.5, 1.8, 3.5), tunic)
        m.box(f'{side}_forearm', (cx, 0), 8.0, (3.1, 3.8, 3.1), tunic)
        m.box(f'{side}_forearm', (cx, 0), 7.9, (3.3, 0.7, 3.3), gold)
        m.box(f'{side}_hand', (cx, 0), 5.4, (2.8, 2.5, 2.8), skin_mat(SKIN))
    # pantalon blanc, chaussures bleu foncé
    pants = cloth(PANTS, shade(PANTS, 1.03), shade(PANTS, 0.78), ao_top=0.2, folds=2, seed=64)
    m.cube('body', [-3.6, 7.2, -2.0], [7.2, 1.8, 4.0], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 2.0
        m.box(f'{side}_leg', (cx, 0), 3.9, (3.5, 3.6, 3.6), pants)
        m.box(f'{side}_shin', (cx, 0), 1.3, (3.2, 2.7, 3.3), pants)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.4, 1.5, 4.4), cloth(SHOE, shade(SHOE, 1.2), shade(SHOE, 0.7), seed=65))
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.5, 0.45, 4.6), flat(shade(SHOE, 0.6), 0.9))


def build_chains(m):
    """Cinq chaînes qui pendent des doigts de la main droite, chacune avec son embout."""
    cx, y0 = -5.5, 5.4
    m.bone('chains', [cx, y0, 0], 'right_hand')
    steel = flat(CHAIN, 0.85)
    m.cube('chains', [cx - 1.5, y0 + 0.2, -1.5], [3.0, 0.35, 3.0], flat(shade(CHAIN, 0.8), 0.85))   # bagues
    ends = (('sphere', (0.8, 0.8, 0.8)), ('hook', (0.3, 0.9, 0.6)), ('cross', (0.8, 0.8, 0.25)),
            ('needle', (0.2, 1.1, 0.2)), ('sword', (0.35, 1.2, 0.2)))
    for i, (kind, size) in enumerate(ends):
        x = cx - 1.2 + i * 0.6
        z = -0.9 + (i % 2) * 0.5
        n = 3 + (i % 3)
        y = y0
        for k in range(n):
            w, d = (0.45, 0.18) if k % 2 == 0 else (0.18, 0.45)
            m.cube('chains', [x - w / 2, y - 0.75, z - d / 2], [w, 0.75, d], steel)
            y -= 0.6
        sx, sy, sz = size
        m.cube('chains', [x - sx / 2, y - sy, z - sz / 2], [sx, sy, sz], flat(shade(CHAIN, 1.05), 0.9))
        if kind == 'cross':
            m.cube('chains', [x - 0.12, y - 1.0, z - 0.12], [0.25, 1.0, 0.25], flat(CHAIN, 0.9))


# ---------------------------------------------------------------------------------------------------------------

def write_expression(model, mode):
    """Texture <id>_<mode> où seul le visage change (mêmes UV)."""
    from geolib import ASSETS, PREVIEWS, FaceCtx
    from PIL import Image
    x, y, w, h = model.head_cube.uv['north']
    ctx = FaceCtx('north', w, h, FACE_D)
    face_paint(ctx, mode)
    tex = model.texture.copy()
    tex.paste(ctx.img, (x, y))
    tex.save(os.path.join(ASSETS, 'textures', 'entity', 'npc_geo', f'{model.id}_{mode}.png'))
    out = Image.new('RGBA', (w * 8 + 8, h * 4), (40, 40, 48, 255))
    out.paste(model.texture.crop((x, y, x + w, y + h)).resize((w * 4, h * 4), 0), (0, 0))
    out.paste(ctx.img.resize((w * 4, h * 4), 0), (w * 4 + 8, 0))
    out.save(os.path.join(PREVIEWS, f'{model.id}_expressions.png'))


def build():
    m = Model('kurapika', density=4, tex_width=512)
    skeleton(m, arm_out=5, leg_out=2, feet_out=6)
    build_face(m)
    build_hair(m)
    build_body(m)
    build_chains(m)
    m.animations = humanoid_animations(arm_swing=20, leg_swing=26, breath=1.3)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, 'froid')
