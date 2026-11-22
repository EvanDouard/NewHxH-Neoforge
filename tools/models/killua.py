"""
Killua Zoldyck — même style que Gon (référence d'Evan). Traits iconiques : touffe de cheveux blancs ébouriffés
(mèches courtes dans tous les sens, frange qui tombe sur le front), yeux bleus en amande de chat, petit sourire en
coin, tenue de l'Examen (haut clair ample sur manches longues bleu nuit, short bleu nuit, baskets violettes),
posture nonchalante : épaules basses, mains dans les poches.
"""
from __future__ import annotations

from chibi import (HEAD, HEAD_Y, FaceGrid, blade, cloth, flat, hair_paint, skeleton, skin_mat)
from geolib import Model, humanoid_animations, material, mix, rgb, shade

# ---------------------------------------------------------------------------------------------------------------
# Palette
SKIN = rgb('#f8dcc6')
HAIR_LIGHT = rgb('#f7f8fc')          # faces éclairées : presque blanc
HAIR_SHADOW = rgb('#b4bdd2')         # faces à l'ombre : gris bleuté
HAIR_RIM = rgb('#ffffff')
EYE = rgb('#3d86d9')
EYE_DARK = rgb('#13284f')
LINE = rgb('#2a2230')
TOP = rgb('#eceaf2')                 # haut clair ample
TOP_SHADOW = rgb('#c4c0d6')
UNDER = rgb('#2b3366')               # manches longues bleu nuit
UNDER_LIGHT = rgb('#3c4685')
SHORTS = rgb('#343e74')
SHOE = rgb('#7b5cc2')
SHOE_LIGHT = rgb('#9a7fdb')
SOLE = rgb('#f2f2f4')


def hair_for(lit):
    return lambda t: hair_paint(HAIR_SHADOW, HAIR_LIGHT, HAIR_RIM, lit=lit + 0.25 * t, seed=int(lit * 100) + 3)


# ---------------------------------------------------------------------------------------------------------------
# Étape 1 — cheveux : touffe arrondie et ébouriffée, mèches plus courtes et plus nombreuses que Gon, qui partent
# dans tous les sens (y compris vers le bas) ; frange qui couvre le front.

def build_hair(m):
    m.bone('hair', [0, 25.0, 0], 'head')
    cap = hair_for(0.15)(0.3)
    m.cube('hair', [-5.6, 22.6, -5.0], [11.2, 4.6, 10.4], cap)                # calotte, large et ronde
    m.cube('hair', [-5.7, 18.0, 1.2], [11.4, 6.8, 4.0], hair_for(0.0)(0.2))   # arrière, très fourni
    for sx in (-1, 1):
        m.cube('hair', [sx * 5.3 - 0.9, 19.0, -3.8], [1.8, 5.2, 6.6], cap)    # côtés
    m.cube('hair', [-5.0, 26.6, -4.0], [10.0, 1.6, 8.8], hair_for(0.3)(0.4))  # dessus bombé

    # touffes courtes, larges et épaisses : la silhouette reste une boule ébouriffée, pas un oursin
    tufts = [
        # (nom, x, y, z, longueur, largeur, rx, rz, éclairage)
        ('k_top', 0.4, 27.4, -0.8, 4.6, 6.4, -12, -6, 0.32),
        ('k_top_l', -3.0, 27.0, 0.2, 4.4, 6.0, -2, 34, 0.22),
        ('k_top_r', 3.2, 27.0, 0.6, 4.6, 6.0, 4, -38, 0.2),
        ('k_top_b', 0.0, 26.6, 3.0, 4.4, 6.4, 40, 4, 0.06),
        ('k_crown', -0.6, 27.0, -3.0, 3.8, 5.6, -38, 10, 0.34),
        ('k_side_r', -5.2, 24.8, -0.6, 4.4, 5.6, -4, 80, 0.24),
        ('k_side_l', 5.2, 24.6, 0.4, 4.6, 5.6, 6, -82, 0.12),
        ('k_side_r2', -5.0, 22.4, 1.8, 3.6, 5.0, 16, 112, 0.06),
        ('k_side_l2', 5.0, 22.2, 1.4, 3.4, 5.0, 14, -114, 0.02),
        ('k_cheek_r', -5.2, 22.4, -3.2, 3.6, 3.6, -8, 154, 0.26),
        ('k_cheek_l', 5.2, 22.2, -3.2, 3.2, 3.4, -8, -152, 0.2),
        ('k_back', 0.4, 23.0, 4.6, 4.2, 6.6, 70, -6, -0.05),
        ('k_back_r', -3.4, 21.2, 4.4, 3.6, 5.4, 92, 30, -0.05),
        ('k_back_l', 3.4, 21.6, 4.4, 3.6, 5.4, 88, -28, -0.05),
        ('k_nape', 0.2, 19.2, 4.6, 2.8, 5.6, 124, 4, -0.1),
    ]
    for name, x, y, z, L, W, rx, rz, lit in tufts:
        blade(m, name, [x, y, z], L, W, rx, rz, hair_for(lit), depth=W * 0.62, step=1.5, taper=1.15, min_w=1.2)
    # frange : mèches qui tombent sur le front, irrégulières, laissent les yeux visibles
    for name, x, L, W, rz in (('kb_1', -3.5, 3.4, 3.6, -18), ('kb_2', -1.2, 4.2, 3.8, -4), ('kb_3', 1.3, 3.8, 3.8, 9),
                              ('kb_4', 3.6, 3.0, 3.4, 22)):
        blade(m, name, [x, 25.6, -4.9], L, W, 172, rz, hair_for(0.35), depth=1.3)


# ---------------------------------------------------------------------------------------------------------------
# Étape 2 — visage : yeux bleus en amande de chat (coin extérieur relevé), sourcils fins presque cachés par la
# frange, petit sourire en coin.

FACE_D = 7


def face_paint(ctx):
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 54, 66, 60, shade(SKIN, 0.93))
    f.rect(0, 50, 4, 60, shade(SKIN, 0.89))
    f.rect(62, 50, 66, 60, shade(SKIN, 0.89))
    f.rect(0, 0, 66, 15, HAIR_SHADOW)                                   # cheveux sur le haut du front
    f.rect(0, 15, 66, 18, shade(SKIN, 0.9))                             # ombre de la frange
    f.poly((0, 14, 9, 14, 0, 34), HAIR_SHADOW)
    f.poly((66, 14, 57, 14, 66, 34), HAIR_SHADOW)
    f.poly((12, 21, 25, 22, 25, 23, 12, 22), shade(HAIR_SHADOW, 0.8))   # sourcils fins
    f.poly((54, 21, 41, 22, 41, 23, 54, 22), shade(HAIR_SHADOW, 0.8))
    for x0, outer_left in ((12, True), (42, False)):
        x1 = x0 + 12
        # blanc de l'œil en amande : plus haut côté nez, coin extérieur relevé
        if outer_left:
            f.poly((x0, 27, x1, 28, x1, 39, x0 + 2, 37), (250, 250, 252))
        else:
            f.poly((x0, 28, x1, 27, x1 - 2, 37, x0, 39), (250, 250, 252))
        ix0 = x0 + (4 if outer_left else 1)
        f.rect(ix0, 28, ix0 + 7, 38, EYE)
        f.rect(ix0, 28, ix0 + 7, 30, shade(EYE, 0.7))                   # ombre de la paupière sur l'iris
        f.rect(ix0 + 2, 31, ix0 + 5, 37, EYE_DARK)
        f.rect(ix0 + 1, 30, ix0 + 3, 32, (255, 255, 255))
        f.rect(ix0 + 5, 35, ix0 + 6, 36, (220, 238, 255))
        if outer_left:                                                  # paupière supérieure, pointe relevée
            f.poly((x0 - 3, 24, x1 + 1, 26, x1 + 1, 28, x0, 28), LINE)
        else:
            f.poly((x0 - 1, 26, x1 + 3, 24, x1, 28, x0 - 1, 28), LINE)
        f.rect(x0 + 2, 39, x1 - 1, 39, shade(SKIN, 0.72))
    f.rect(32, 44, 33, 46, shade(SKIN, 0.82))
    # sourire en coin : trait qui remonte à droite de l'image
    f.rect(28, 51, 35, 51, rgb('#7a4a40'))
    f.rect(35, 50, 37, 50, rgb('#7a4a40'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (0.55 if ctx.face != 'south' else 0.8))], fill=HAIR_SHADOW)


def build_face(m):
    hx = HEAD[0] / 2
    m.box('head', (0, 0), HEAD_Y, HEAD, head_paint, density=FACE_D)
    m.box('head', (0, 0), HEAD_Y - 0.6, (2.8, 0.7, 2.8), skin_mat(shade(SKIN, 0.92)))
    for side in (-1, 1):
        m.box('head', (side * (hx + 0.35), 0.4), 19.6, (0.7, 1.9, 1.3), skin_mat(shade(SKIN, 0.96)), density=FACE_D)
    m.cube('head', [-0.35, 25.4 - 46.5 / FACE_D, -HEAD[2] / 2 - 0.28], [0.7, 0.45, 0.3],
           material(shade(SKIN, 0.97), noise=1, outline=False))


# ---------------------------------------------------------------------------------------------------------------
# Étapes 3 à 5 — haut clair ample sur manches longues bleu nuit, short, baskets violettes

def build_body(m):
    top = cloth(TOP, shade(TOP, 1.04), TOP_SHADOW, ao_top=0.08, ao_bottom=0.15, folds=3, seed=21)
    under = cloth(UNDER, UNDER_LIGHT, shade(UNDER, 0.7), ao_bottom=0.1, folds=1, seed=22)
    # haut : ample (un peu plus large que le torse), descend sur les hanches
    m.cube('torso', [-3.85, 8.2, -2.2], [7.7, 8.2, 4.4], top)
    m.cube('torso', [-4.0, 8.0, -2.35], [8.0, 0.8, 4.7], cloth(TOP_SHADOW, seed=23))       # ourlet
    # col : col roulé bleu nuit qui dépasse de l'encolure large
    m.cube('torso', [-2.3, 16.0, -1.9], [4.6, 1.0, 3.8], under)
    m.cube('torso', [-2.6, 15.6, -2.42], [5.2, 0.7, 0.3], cloth(TOP_SHADOW, seed=24))      # bord de l'encolure
    # bras : manches longues bleu nuit, épaule du haut clair par-dessus
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 5.5
        m.box(f'{side}_arm', (cx, 0), 11.8, (3.2, 4.0, 3.2), under)
        m.box(f'{side}_arm', (cx, 0), 13.6, (3.6, 2.3, 3.6), top)                         # mancheron clair
        m.box(f'{side}_forearm', (cx, 0), 8.0, (3.1, 3.8, 3.1), under)
        m.box(f'{side}_forearm', (cx, 0), 7.9, (3.3, 0.8, 3.3), cloth(shade(UNDER, 0.8), seed=25))  # poignet
        m.box(f'{side}_hand', (cx, 0), 5.4, (2.9, 2.5, 2.9), skin_mat(SKIN))
    # short bleu nuit (dépasse sous le haut), jambes, baskets violettes à semelle blanche
    shorts = cloth(SHORTS, shade(SHORTS, 1.15), shade(SHORTS, 0.7), ao_top=0.2, folds=2, seed=26)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 2.0
        m.box(f'{side}_leg', (cx, 0), 4.2, (3.6, 3.8, 3.8), shorts)
        m.box(f'{side}_leg', (cx, 0), 3.9, (3.8, 0.7, 4.0), cloth(shade(SHORTS, 0.85), seed=27))
        m.box(f'{side}_shin', (cx, 0), 1.8, (2.6, 2.3, 2.6), skin_mat(SKIN))
        m.box(f'{side}_shin', (cx, -0.4), 0.45, (3.3, 1.7, 4.3), cloth(SHOE, SHOE_LIGHT, shade(SHOE, 0.7), seed=28))
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.45, 0.55, 4.5), flat(SOLE, 0.85))
        m.box(f'{side}_shin', (cx, 0), 1.7, (3.0, 0.7, 2.9), cloth(shade(SHOE, 0.85), seed=29))   # col de la basket
        for k in range(2):                                                                      # lacets blancs
            m.cube(f'{side}_shin', [cx - 0.8, 1.0 + k * 0.45, -2.62], [1.6, 0.22, 0.14], flat(SOLE, 0.95))


# ---------------------------------------------------------------------------------------------------------------

def build():
    m = Model('killua', density=4, tex_width=512)
    # posture nonchalante : bras un peu en arrière, avant-bras pliés vers l'avant (mains dans les poches)
    skeleton(m, arm_out=4, leg_out=2.5, feet_out=6, arm_rot=[-8, 0, 0], forearm_rot=[16, 0, 0])
    build_face(m)
    build_hair(m)
    build_body(m)
    anims = humanoid_animations(arm_swing=10, leg_swing=26, breath=1.2)
    m.animations = anims
    return m


if __name__ == '__main__':
    build().save()
