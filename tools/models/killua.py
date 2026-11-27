"""
Killua Zoldyck — même style que Gon (référence d'Evan). Traits iconiques : touffe de cheveux blancs ébouriffés
(mèches courtes dans tous les sens, frange qui tombe sur le front), yeux bleus en amande de chat, petit sourire en
coin, tenue de l'Examen (haut clair ample sur manches longues bleu nuit, short bleu nuit, baskets violettes),
posture nonchalante : épaules basses, mains dans les poches.
"""
from __future__ import annotations

from chibi import (HEAD, HEAD_Y, FaceGrid, arched, chamfered_cap, cloth, flat, hair_paint, lock, skeleton, skin_mat,
                   strand_paint)
from geolib import Model, humanoid_animations, material, mix, rgb, shade

# ---------------------------------------------------------------------------------------------------------------
# Palette
SKIN = rgb('#fbe6d7')
HAIR_LIGHT = rgb('#fbfcff')          # faces éclairées : presque blanc
HAIR_SHADOW = rgb('#bec4da')         # faces à l'ombre : gris bleuté
HAIR_RIM = rgb('#dcdcf7')            # reflet légèrement violacé
EYE = rgb('#2a7ff0')
EYE_DARK = rgb('#0c1a3d')
LINE = rgb('#2a2230')
TOP = rgb('#eceaf2')                 # haut clair ample
TOP_SHADOW = rgb('#c4c0d6')
UNDER = rgb('#232a5a')               # manches longues bleu nuit
UNDER_LIGHT = rgb('#36407e')
SHORTS = rgb('#36417c')
SHOE = rgb('#7b5cc2')
SHOE_LIGHT = rgb('#9a7fdb')
SOLE = rgb('#f2f2f4')


def hair_for(lit):
    return lambda t: hair_paint(HAIR_SHADOW, HAIR_LIGHT, HAIR_RIM, lit=lit + 0.25 * t, seed=int(lit * 100) + 3)


# ---------------------------------------------------------------------------------------------------------------
# Étape 1 — cheveux (itération 3 : mèches continues). Trois niveaux : 1) masse qui épouse le crâne ; 2) 9 grandes
# mèches continues (base large, pointe effilée, courbure et torsion propres, bases décalées en hauteur et en
# profondeur) ; 3) 6 petites mèches de relief. Côtés relevés vers le haut-arrière (pas de couronne), espaces entre
# les mèches de l'arrière, frange en mèches indépendantes qui dégagent les sourcils.

def mk(lit):
    return lambda t: strand_paint(HAIR_SHADOW, HAIR_LIGHT, t, lit=lit, rim=HAIR_RIM)


def build_hair(m):
    """Itération 5 (organique) : masse aux arêtes adoucies, puis trois niveaux de mèches organiques (`lock` :
    tronçons qui se courbent, racine évasée qui sort de la masse, aplaties ou volumineuses selon la mèche)."""
    m.bone('hair', [0, 25.0, 0], 'head')
    cap = hair_for(0.2)(0.3)
    back = hair_for(0.0)(0.2)
    m.cube('hair', [-5.0, 24.7, -4.75], [10.0, 1.5, 9.5], cap)                # calotte (au-dessus du front)
    m.cube('hair', [-5.0, 22.6, -2.9], [10.0, 2.4, 7.6], cap)                 # pourtour, en retrait du front
    m.cube('hair', [-4.0, 26.0, -3.4], [8.0, 1.0, 6.8], hair_for(0.35)(0.4))   # dessus bombé
    m.cube('hair', [-5.0, 18.8, 1.5], [10.0, 4.8, 3.4], back)                 # arrière du crâne
    m.cube('hair', [-4.6, 17.6, 2.6], [9.2, 1.4, 2.2], back)                  # nuque
    for sx in (-1, 1):                                                         # tempes, oreilles dégagées
        m.cube('hair', [sx * 5.0 - 0.5, 21.9, -3.7], [1.0, 1.6, 5.4], cap)
        m.cube('hair', [sx * 5.0 - 0.5, 19.0, 0.6], [1.0, 3.0, 1.2], back)

    # (nom, x, y, z, longueur, largeur, rx, rz, ry, aplatissement, courbure, éclairage, options)
    # Itération 6 : moins de pointes, bases plus larges, corps plus épais (belly), formes déjetées (skew) et
    # extrémités pas toujours très pointues (tip). Côtés et arrière orientés de façon cohérente : vers le haut et
    # l'arrière.
    # Itération 7 — hiérarchie : 5 grandes mèches nettement plus longues et plus larges (et un peu plus claires)
    # portent la silhouette ; 5 intermédiaires épaisses font les transitions sommet / côtés / arrière ; 4 petites
    # seulement. Directions variées (pas de couronne radiale), côtés moins écartés, plus tournés vers l'arrière.
    dominant = [
        ('k_top', 0.4, 25.8, -0.6, 11.2, 6.1, -4, -10, 14, 0.68, 16, 0.1, dict(skew=-0.1, tip=0.08, root=1.2)),
        ('k_top_r', -2.6, 25.6, 0.6, 10.0, 5.7, 20, 26, -22, 0.55, 18, 0.06, dict(skew=0.12, tip=0.12, root=1.2)),
        ('k_top_l', 2.8, 25.2, 0.0, 9.0, 5.4, 8, -32, 28, 0.78, 14, 0.04, dict(skew=-0.08, tip=0.14, root=1.2)),
        ('k_back', 0.6, 24.8, 2.6, 10.4, 6.1, 52, -10, 6, 0.55, 18, 0.0, dict(skew=0.08, tip=0.1, root=1.2)),
        ('k_side_r', -4.1, 24.0, 0.8, 7.8, 5.1, 44, 40, 16, 0.58, 18, 0.04, dict(skew=0.1, tip=0.16, root=1.25)),
    ]
    intermediate = [
        ('k_i_front', -0.8, 25.6, -2.8, 5.0, 3.9, -22, 18, -12, 0.72, 10, 0.06, dict(tip=0.18, skew=0.06)),
        ('k_i1', 2.2, 25.4, -1.8, 4.4, 3.7, -6, -30, 6, 0.75, 12, 0.04, dict(tip=0.2)),
        ('k_i_side_l', 4.1, 23.8, 1.0, 4.8, 3.9, 48, -42, -20, 0.7, 14, 0.0, dict(skew=-0.1, tip=0.18, root=1.25)),
        ('k_i3', -2.8, 23.6, 3.2, 5.0, 4.0, 70, 26, 24, 0.62, 12, -0.04, dict(skew=0.08, tip=0.16)),
        ('k_i4', 3.0, 23.4, 3.0, 4.6, 3.9, 64, -30, -24, 0.62, 12, -0.06, dict(skew=-0.08, tip=0.16)),
    ]
    small = [
        ('k_s1', -4.9, 22.4, -3.0, 3.0, 2.5, -6, 150, 0, 0.55, 8, 0.02, dict(tip=0.18)),   # encadrent le visage
        ('k_s2', 4.9, 22.2, -2.8, 2.6, 2.4, -6, -148, 0, 0.55, 8, -0.02, dict(tip=0.18)),
        # arrière : deux couches plates qui retombent vers la nuque, continuité avec le sommet
        ('k_bl1', -1.6, 25.4, 4.2, 6.4, 5.1, 158, 10, 0, 0.36, 10, -0.06, dict(tip=0.16)),
        ('k_bl2', 2.0, 24.2, 4.4, 5.4, 4.8, 162, -14, 0, 0.36, 10, -0.1, dict(tip=0.18)),
    ]
    arched(m, dominant, mk, segs=3)
    arched(m, intermediate, mk, segs=2)
    arched(m, small, mk, segs=2)
    # frange simplifiée : 3 mèches principales bien identifiables, longueurs différentes, qui se chevauchent
    # (profondeurs décalées) et retombent en se courbant ; sourcils dégagés
    for name, x, z, L, W, rz, arc, skew in (('kb_1', -2.5, -4.92, 3.0, 4.0, -18, 10, 0.12),
                                             ('kb_2', 0.2, -5.18, 3.9, 4.2, 6, 12, -0.08),
                                             ('kb_3', 2.9, -5.0, 2.5, 3.6, 24, 10, -0.12)):
        lock(m, name, [x, 25.8, z], L, W, 168, rz, mk(0.1), flat=0.6, curve=(-arc, arc * (1 if rz > 0 else -1) * 0.5),
             segs=2, root=1.15, tip=0.1, skew=skew)


# ---------------------------------------------------------------------------------------------------------------
# Étape 2 — visage : grands yeux bleus en amande (coin extérieur relevé), sourcils fins dégagés, petite bouche.
# Deux expressions : « malicieux » (habituel, sourire en coin) et « froid » (regard d'assassin : paupières
# baissées, iris sombre, pupille réduite à un point, ombre sur les yeux) dans la texture killua_froid.png.

FACE_D = 7


def face_paint(ctx, mode='malicieux'):
    cold = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 54, 66, 60, shade(SKIN, 0.93))
    f.rect(0, 50, 3, 60, shade(SKIN, 0.9))
    f.rect(63, 50, 66, 60, shade(SKIN, 0.9))
    f.rect(0, 0, 66, 8, HAIR_SHADOW)                                    # racine des cheveux
    f.rect(0, 8, 66, 10, shade(SKIN, 0.9))                              # ombre portée de la frange
    f.poly((0, 8, 6, 8, 0, 24), HAIR_SHADOW)
    f.poly((66, 8, 60, 8, 66, 24), HAIR_SHADOW)
    brow = shade(HAIR_SHADOW, 0.78)
    if cold:                                                            # sourcils froncés vers le centre
        f.poly((10, 16, 27, 19, 27, 21, 10, 18), brow)
        f.poly((56, 16, 39, 19, 39, 21, 56, 18), brow)
        f.rect(8, 21, 58, 26, shade(SKIN, 0.84))                        # ombre sur le regard
    else:
        f.poly((10, 17, 26, 16, 26, 18, 10, 19), brow)
        f.poly((56, 17, 40, 16, 40, 18, 56, 19), brow)
    for x0, outer_left in ((9, True), (42, False)):
        x1 = x0 + 15
        top = 27 if cold else 23                                        # paupière baissée en mode froid
        white = (236, 238, 244) if cold else (252, 252, 255)
        if outer_left:
            f.poly((x0, top, x1, top + 1, x1, 40, x0 + 2, 38), white)
        else:
            f.poly((x0, top + 1, x1, top, x1 - 2, 38, x0, 40), white)
        iw = 7 if cold else 9
        ix0 = x0 + (5 if outer_left else 1) + (1 if cold else 0)
        iris = shade(EYE, 0.55) if cold else EYE
        f.rect(ix0, top + 1, ix0 + iw, 39, iris)
        f.rect(ix0, 34, ix0 + iw, 39, shade(iris, 1.25) if not cold else shade(iris, 0.9))   # bas de l'iris plus clair
        f.rect(ix0, top + 1, ix0 + iw, top + 3, shade(iris, 0.6))       # ombre de la paupière
        f.rect(ix0 - 1, top + 1, ix0 - 1, 39, EYE_DARK)                 # contour de l'iris
        f.rect(ix0 + iw + 1, top + 1, ix0 + iw + 1, 39, EYE_DARK)
        if cold:
            f.rect(ix0 + 3, 32, ix0 + 4, 33, EYE_DARK)                  # pupille réduite à un point
        else:
            f.rect(ix0 + 3, 28, ix0 + 6, 36, EYE_DARK)                  # pupille nette
            f.rect(ix0 + 1, 26, ix0 + 3, 28, (255, 255, 255))           # reflets
            f.rect(ix0 + 6, 35, ix0 + 7, 36, (215, 236, 255))
        if outer_left:                                                  # paupière supérieure, pointe relevée
            f.poly((x0 - 3, top - 3, x1 + 1, top - 1, x1 + 1, top + 1, x0, top + 1), LINE)
        else:
            f.poly((x0 - 1, top - 1, x1 + 3, top - 3, x1, top + 1, x0 - 1, top + 1), LINE)
        f.rect(x0 + 3, 40, x1 - 2, 40, shade(SKIN, 0.7))                # cils inférieurs
    f.rect(32, 45, 33, 47, shade(SKIN, 0.82))                           # nez
    mouth = rgb('#7a4a40')
    if cold:
        f.rect(29, 51, 36, 51, mouth)
    else:                                                               # sourire en coin
        f.rect(29, 51, 34, 51, mouth)
        f.rect(34, 50, 37, 50, mouth)
        f.rect(37, 49, 38, 49, mouth)


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * 0.36)], fill=HAIR_SHADOW)
        ctx.draw.rectangle([int(ctx.w * 0.62), 0, ctx.w, int(ctx.h * 0.62)], fill=HAIR_SHADOW)   # vers la nuque
    elif ctx.face == 'south':
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * 0.85)], fill=HAIR_SHADOW)


def build_face(m):
    hx = HEAD[0] / 2
    m.head_cube = m.box('head', (0, 0), HEAD_Y, HEAD, head_paint, density=FACE_D)
    m.box('head', (0, 0), HEAD_Y - 0.6, (2.6, 0.7, 2.6), skin_mat(shade(SKIN, 0.92)))
    for side in (-1, 1):
        m.box('head', (side * (hx + 0.35), 0.2), 19.6, (0.7, 1.9, 1.3), skin_mat(shade(SKIN, 0.95)), density=FACE_D)
    m.cube('head', [-0.35, 25.4 - 46.5 * HEAD[1] / 60, -HEAD[2] / 2 - 0.25], [0.7, 0.42, 0.28],
           material(shade(SKIN, 0.97), noise=1, outline=False))


# ---------------------------------------------------------------------------------------------------------------
# Étapes 3 à 5 — t-shirt blanc ample, manches longues bleu nuit, short plus large, baskets violettes

def build_body(m):
    top = cloth(TOP, shade(TOP, 1.04), TOP_SHADOW, ao_top=0.06, ao_bottom=0.14, folds=3, seed=21)
    fold = cloth(shade(TOP, 0.9), seed=30)
    under = cloth(UNDER, UNDER_LIGHT, shade(UNDER, 0.65), ao_bottom=0.1, folds=2, seed=22)
    rib = cloth(UNDER_LIGHT, shade(UNDER_LIGHT, 1.1), shade(UNDER, 0.8), seed=25)
    # t-shirt : ample, s'évase sur les hanches ; plis discrets en relief
    m.cube('torso', [-4.05, 8.4, -2.45], [8.1, 8.2, 4.9], top)
    m.cube('torso', [-4.35, 7.7, -2.7], [8.7, 1.0, 5.4], cloth(shade(TOP, 0.94), TOP, TOP_SHADOW, seed=23))  # ourlet
    for x, y, h in ((-2.2, 8.8, 3.6), (1.4, 8.6, 2.8), (2.8, 9.2, 2.2)):
        m.cube('torso', [x, y, -2.6], [0.45, h, 0.2], fold)
    m.cube('torso', [-2.4, 8.7, 2.45], [0.45, 3.0, 0.18], fold)
    # col roulé bleu nuit qui dépasse de l'encolure
    m.cube('torso', [-2.3, 16.3, -1.9], [4.6, 0.9, 3.8], rib)
    m.cube('torso', [-2.7, 16.1, -2.6], [5.4, 0.55, 0.3], cloth(TOP_SHADOW, seed=24))      # bord de l'encolure
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 5.5
        m.box(f'{side}_arm', (cx, 0), 11.8, (3.1, 4.0, 3.1), under)
        m.box(f'{side}_arm', (cx, 0), 13.3, (3.8, 2.7, 3.8), top)                         # manche courte du t-shirt
        m.box(f'{side}_arm', (cx, 0), 13.1, (3.9, 0.5, 3.9), fold)                        # ourlet de la manche
        m.box(f'{side}_forearm', (cx, 0), 8.2, (3.0, 3.7, 3.0), under)
        m.box(f'{side}_forearm', (cx, 0), 7.7, (3.25, 0.9, 3.25), rib)                    # poignet côtelé
        m.box(f'{side}_hand', (cx, 0), 5.3, (2.8, 2.5, 2.8), skin_mat(SKIN))
    shorts = cloth(SHORTS, shade(SHORTS, 1.15), shade(SHORTS, 0.68), ao_top=0.22, folds=2, seed=26)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 2.05
        m.box(f'{side}_leg', (cx, 0), 4.3, (4.0, 3.8, 4.2), shorts)
        m.box(f'{side}_leg', (cx, 0), 3.85, (4.25, 0.75, 4.45), cloth(shade(SHORTS, 0.86), seed=27))  # bas évasé
        m.box(f'{side}_shin', (cx, 0), 1.9, (2.5, 2.2, 2.5), skin_mat(SKIN))
        # basket : corps, bout arrondi, talon, languette, semelle blanche épaisse, bande latérale, lacets
        shoe = cloth(SHOE, SHOE_LIGHT, shade(SHOE, 0.68), seed=28)
        m.box(f'{side}_shin', (cx, -0.3), 0.5, (3.3, 1.5, 4.2), shoe)
        m.box(f'{side}_shin', (cx, -2.15), 0.5, (3.0, 1.05, 1.0), cloth(SHOE_LIGHT, seed=31))   # bout
        m.box(f'{side}_shin', (cx, 1.45), 0.5, (3.1, 1.9, 0.9), cloth(shade(SHOE, 0.8), seed=32))  # talon
        m.box(f'{side}_shin', (cx, -0.9), 1.6, (1.8, 0.75, 1.9), cloth(SHOE_LIGHT, seed=33))    # languette
        m.box(f'{side}_shin', (cx, 0), 1.75, (2.9, 0.6, 2.9), cloth(shade(SHOE, 0.8), seed=29)) # col
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.5, 0.6, 4.95), flat(SOLE, 0.85))
        m.box(f'{side}_shin', (cx, -0.4), 0.55, (3.38, 0.25, 4.3), flat(SOLE, 0.95))            # bande latérale
        for k in range(2):
            m.cube(f'{side}_shin', [cx - 0.75, 1.1 + k * 0.4, -2.0], [1.5, 0.2, 0.14], flat(SOLE, 0.95))


# ---------------------------------------------------------------------------------------------------------------

def write_expression(model, mode):
    """Variante de texture où seul le visage change (mêmes UV) : textures/entity/npc_geo/killua_<mode>.png."""
    import os
    from geolib import ASSETS, PREVIEWS, FaceCtx
    x, y, w, h = model.head_cube.uv['north']
    ctx = FaceCtx('north', w, h, FACE_D)
    face_paint(ctx, mode)
    tex = model.texture.copy()
    tex.paste(ctx.img, (x, y))
    tex.save(os.path.join(ASSETS, 'textures', 'entity', 'npc_geo', f'{model.id}_{mode}.png'))
    both = model.texture.crop((x, y, x + w, y + h)).resize((w * 4, h * 4), 0)
    from PIL import Image
    out = Image.new('RGBA', (w * 8 + 8, h * 4), (40, 40, 48, 255))
    out.paste(both, (0, 0))
    out.paste(ctx.img.resize((w * 4, h * 4), 0), (w * 4 + 8, 0))
    out.save(os.path.join(PREVIEWS, f'{model.id}_expressions.png'))


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
    model = build()
    model.save()
    write_expression(model, 'froid')
