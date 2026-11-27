"""
Leorio Paladiknight — même style que Gon, Killua et Kurapika (« Minecraft anime », mèches organiques).
Traits iconiques : grand (≈ 1,95 bloc), longues jambes, épaules larges ; cheveux noirs courts et hérissés vers le
haut et l'arrière (front dégagé) ; visage allongé, mâchoire carrée, sourcils épais, petites lunettes rondes aux
verres bleutés, grand sourire ; costume trois pièces bleu marine (veste ouverte à revers, pantalon à pli), chemise
blanche, cravate bleue, chaussures noires ; mallette de médecin à la main droite.
Expression « froid » : colère (sourcils froncés, bouche ouverte en criant), en combat.
"""
from __future__ import annotations

import math
import os

from chibi import FaceGrid, arched, cloth, flat, hair_paint, lock, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f2cfb0')
HAIR = rgb('#141418')
HAIR_LIT = rgb('#2c2f3c')
HAIR_RIM = rgb('#4a5066')
LINE = rgb('#231a16')
EYE = rgb('#3d2a20')
LENS = rgb('#5b86c9')
LENS_DARK = rgb('#2e4c86')
FRAME = rgb('#1c1c22')
SUIT = rgb('#2f3a5e')
SUIT_LIGHT = rgb('#405083')
SUIT_DARK = rgb('#1c2440')
SHIRT = rgb('#f1f2f5')
SHIRT_SHADE = rgb('#c8cbd6')
TIE = rgb('#3d6fd1')
TIE_DARK = rgb('#26479a')
SHOE = rgb('#1b1b1f')
LEATHER = rgb('#7a4a26')
LEATHER_DARK = rgb('#4f2e16')
BRASS = rgb('#d6b04a')

# Proportions (pieds à 0) : grand et élancé
HEAD = (7.8, 9.2, 7.8)
HEAD_Y = 22.6
HIP_Y = 12.2
KNEE_Y = 6.2
SHOULDER_Y = 21.0
ARM_X = 5.0
TORSO_W, TORSO_D = 7.6, 4.0


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_LIT, t, lit=lit, rim=HAIR_RIM)


def skeleton(m):
    m.bone('body', [0, HIP_Y, 0])
    m.bone('torso', [0, HIP_Y + 1.6, 0], 'body')
    m.bone('head', [0, HEAD_Y, 0], 'torso')
    for side, sx in (('right', -1), ('left', 1)):
        m.bone(f'{side}_arm', [sx * (ARM_X - 0.4), SHOULDER_Y, 0], 'torso', rotation=[0, 0, -sx * 4])
        m.bone(f'{side}_forearm', [sx * ARM_X, 16.6, 0], f'{side}_arm')
        m.bone(f'{side}_hand', [sx * ARM_X, 12.2, 0], f'{side}_forearm')
        m.bone('item_main' if side == 'right' else 'item_off', [sx * ARM_X, 10.0, -1.0], f'{side}_hand')
        m.bone(f'{side}_leg', [sx * 2.0, HIP_Y, 0], 'body', rotation=[0, 0, -sx * 1.5])
        m.bone(f'{side}_shin', [sx * 2.0, KNEE_Y, 0], f'{side}_leg', rotation=[0, -sx * 6, 0])


# ---------------------------------------------------------------------------------------------------------------
# Cheveux : courts, hérissés vers le haut et l'arrière. Masse qui épouse le crâne (dégagée sur le front et les
# tempes), puis des mèches courtes et épaisses, plus hautes devant (houppe), couchées vers l'arrière ; côtés
# courts, nuque nette.

def build_hair(m):
    top = HEAD_Y + HEAD[1]
    hx, hz = HEAD[0] / 2, HEAD[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    base = hair_paint(HAIR, HAIR_LIT, HAIR_RIM, lit=0.05, seed=3)
    m.cube('hair', [-hx - 0.2, top - 1.6, -hz + 0.6], [HEAD[0] + 0.4, 2.2, HEAD[2] - 0.4], base)     # calotte
    m.cube('hair', [-hx + 0.6, top + 0.4, -hz + 1.2], [HEAD[0] - 1.2, 0.9, HEAD[2] - 2.0], base)
    m.cube('hair', [-hx - 0.2, top - 4.4, 1.2], [HEAD[0] + 0.4, 3.0, hz - 0.8], base)               # arrière
    m.cube('hair', [-hx + 0.6, top - 5.4, hz - 0.2], [HEAD[0] - 1.2, 1.2, 0.5], base)               # nuque
    for sx in (-1, 1):                                                                               # côtés courts
        m.cube('hair', [sx * (hx + 0.1) - 0.3, top - 3.6, -1.6], [0.6, 2.4, 4.2], base)
    # (nom, x, y, z, longueur, largeur, rx, rz, ry, aplatissement, courbure, éclairage, options)
    spikes = [   # courtes, épaisses, couchées vers l'arrière ; la houppe avant est la plus haute
        ('l_f1', -1.8, top + 0.2, -3.2, 3.8, 3.4, 10, 18, 8, 0.6, 22, 0.24, dict(tip=0.18, skew=0.08)),
        ('l_f2', 0.5, top + 0.4, -3.4, 4.6, 3.6, 6, -6, -6, 0.62, 24, 0.3, dict(tip=0.14, skew=-0.06)),
        ('l_f3', 2.7, top + 0.1, -3.0, 3.4, 3.2, 14, -24, 10, 0.6, 20, 0.2, dict(tip=0.2)),
        ('l_f4', -3.4, top - 0.3, -2.6, 2.4, 3.2, 10, 26, 0, 0.6, 16, 0.18, dict(tip=0.22)),
        ('l_t1', -0.6, top + 0.5, -0.8, 4.2, 4.0, 40, 10, 12, 0.6, 18, 0.12, dict(tip=0.16)),
        ('l_t2', 2.0, top + 0.4, 0.2, 2.8, 4.0, 56, -14, -10, 0.6, 16, 0.08, dict(tip=0.18)),
        ('l_t3', -2.6, top + 0.3, 0.6, 2.8, 3.8, 58, 18, 6, 0.6, 16, 0.06, dict(tip=0.18)),
        ('l_b1', 0.2, top - 0.5, 2.6, 2.8, 4.4, 84, 2, 0, 0.55, 12, -0.02, dict(tip=0.2)),
        ('l_b2', -2.6, top - 1.1, 2.8, 2.4, 3.8, 92, 22, 0, 0.55, 10, -0.04, dict(tip=0.22)),
        ('l_b3', 2.8, top - 1.1, 2.6, 2.4, 3.8, 90, -24, 0, 0.55, 10, -0.06, dict(tip=0.22)),
        ('l_s1', -hx, top - 1.4, -0.4, 2.0, 3.0, 50, 80, 0, 0.55, 8, 0.06, dict(tip=0.25)),
        ('l_s2', hx, top - 1.4, 0.0, 1.9, 3.0, 54, -82, 0, 0.55, 8, 0.0, dict(tip=0.25)),
        ('l_x1', 1.2, top + 0.4, -1.8, 3.8, 3.2, 24, -30, 14, 0.6, 22, 0.16, dict(tip=0.12, skew=-0.1)),
        ('l_x2', -2.0, top + 0.2, 1.8, 3.4, 3.4, 70, 30, -8, 0.6, 18, 0.04, dict(tip=0.14, skew=0.1)),
    ]
    arched(m, spikes, mk, segs=2)


# ---------------------------------------------------------------------------------------------------------------
# Visage : allongé, mâchoire carrée, sourcils épais, petites lunettes rondes bleutées (en relief), grand sourire

FACE_D = 7


def face_paint(ctx, mode='souriant'):
    angry = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 50, 66, 60, shade(SKIN, 0.92))                   # mâchoire
    f.rect(0, 46, 5, 60, shade(SKIN, 0.88))
    f.rect(61, 46, 66, 60, shade(SKIN, 0.88))
    f.rect(0, 0, 66, 6, HAIR)
    f.rect(0, 6, 66, 8, shade(SKIN, 0.88))
    f.poly((0, 6, 4, 6, 0, 22), HAIR)
    f.poly((66, 6, 62, 6, 66, 22), HAIR)
    if angry:
        f.poly((9, 14, 28, 21, 28, 24, 9, 18), LINE)
        f.poly((57, 14, 38, 21, 38, 24, 57, 18), LINE)
    else:
        f.poly((9, 16, 28, 17, 28, 20, 9, 20), LINE)
        f.poly((57, 16, 38, 17, 38, 20, 57, 20), LINE)
    # yeux derrière les verres (les verres sont en relief, voir build_face) : petits, sombres
    for x0 in (14, 40):
        f.rect(x0, 26, x0 + 11, 33, (238, 240, 246))
        f.rect(x0 + 3, 26, x0 + 8, 33, EYE)
        f.rect(x0 + 4, 27, x0 + 6, 29, (255, 255, 255))
        f.rect(x0 - 1, 24, x0 + 12, 26, LINE)
    f.rect(32, 36, 34, 44, shade(SKIN, 0.84))                 # nez plus long
    f.rect(30, 44, 35, 45, shade(SKIN, 0.82))
    if angry:                                                  # bouche ouverte, il crie
        f.rect(25, 48, 41, 55, rgb('#5a1e1e'))
        f.rect(26, 48, 40, 50, (245, 245, 245))
    else:                                                      # grand sourire
        f.rect(23, 49, 43, 50, LINE)
        f.rect(25, 50, 41, 53, (245, 245, 245))
        f.rect(26, 53, 40, 54, LINE)
        f.rect(21, 47, 23, 49, LINE)
        f.rect(43, 47, 45, 49, LINE)


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * 0.3)], fill=HAIR)
        ctx.draw.rectangle([int(ctx.w * 0.62), 0, ctx.w, int(ctx.h * 0.62)], fill=HAIR)
    elif ctx.face == 'south':
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * 0.55)], fill=HAIR)


def build_face(m):
    hx, hz = HEAD[0] / 2, HEAD[2] / 2
    m.head_cube = m.box('head', (0, 0), HEAD_Y, HEAD, head_paint, density=FACE_D)
    m.box('head', (0, 0), HEAD_Y - 0.8, (3.0, 0.9, 3.0), skin_mat(shade(SKIN, 0.92)))       # cou
    for sx in (-1, 1):                                                                   # oreilles
        m.box('head', (sx * (hx + 0.3), 0.3), HEAD_Y + 2.8, (0.6, 2.0, 1.3), skin_mat(shade(SKIN, 0.95)),
              density=FACE_D)
    # arcade sourcilière marquée et mâchoire carrée plus large en bas
    m.cube('head', [-hx + 0.4, HEAD_Y + HEAD[1] * (1 - 21 / 60) - 0.3, -hz - 0.3], [HEAD[0] - 0.8, 0.6, 0.35], skin_mat(shade(SKIN, 0.9)))
    m.cube('head', [-hx - 0.15, HEAD_Y, -hz + 0.4], [HEAD[0] + 0.3, 1.8, HEAD[2] - 0.8], skin_mat(shade(SKIN, 0.93)))
    # nez en relief
    m.cube('head', [-0.45, HEAD_Y + 2.4, -hz - 0.35], [0.9, 1.6, 0.4], skin_mat(shade(SKIN, 0.97)))
    # lunettes rondes : verres teintés, monture fine, pont, branches jusqu'aux oreilles
    yl = HEAD_Y + HEAD[1] * (1 - 29.5 / 60)
    for sx in (-1, 1):
        cx = sx * 1.75
        m.cube('head', [cx - 1.15, yl - 0.75, -hz - 0.36], [2.3, 1.5, 0.12], flat(LENS, 0.95))
        m.cube('head', [cx - 0.75, yl - 1.15, -hz - 0.33], [1.5, 2.3, 0.12], flat(LENS, 0.95))
        fr = flat(FRAME, 0.9)                                                            # monture fine
        m.cube('head', [cx - 0.8, yl + 1.12, -hz - 0.42], [1.6, 0.18, 0.1], fr)
        m.cube('head', [cx - 0.8, yl - 1.3, -hz - 0.42], [1.6, 0.18, 0.1], fr)
        m.cube('head', [cx - 1.3, yl - 0.75, -hz - 0.42], [0.18, 1.5, 0.1], fr)
        m.cube('head', [cx + 1.12, yl - 0.75, -hz - 0.42], [0.18, 1.5, 0.1], fr)
        m.cube('head', [sx * (hx + 0.05) - 0.06, yl - 0.1, -hz - 0.2], [0.12, 0.2, hz + 0.4], flat(FRAME, 0.9))
    m.cube('head', [-0.6, yl + 0.2, -hz - 0.42], [1.2, 0.18, 0.1], flat(FRAME, 0.9))


# ---------------------------------------------------------------------------------------------------------------
# Costume : veste ouverte à revers (épaisseur, pans qui s'écartent en bas), gilet, chemise, cravate, pantalon à
# pli, chaussures ; mallette dans la main droite

def build_suit(m):
    suit = cloth(SUIT, SUIT_LIGHT, SUIT_DARK, ao_top=0.05, ao_bottom=0.12, folds=2, seed=90)
    suit_d = cloth(SUIT_DARK, SUIT, shade(SUIT_DARK, 0.8), seed=91)
    shirt = cloth(SHIRT, shade(SHIRT, 1.02), SHIRT_SHADE, folds=1, seed=92)
    tie = cloth(TIE, shade(TIE, 1.12), TIE_DARK, seed=93)
    y0, y1 = HIP_Y + 0.2, SHOULDER_Y + 0.6
    d2 = TORSO_D / 2
    # chemise (visible entre les revers), cou, col
    m.cube('torso', [-TORSO_W / 2 + 0.6, y0, -d2 + 0.1], [TORSO_W - 1.2, y1 - y0, TORSO_D - 0.2], shirt)
    for sx in (-1, 1):
        m.cube('torso', [sx * 0.9 - 0.7, y1 - 0.9, -d2 - 0.12], [1.4, 1.0, 0.3], shirt,
               pivot=[sx * 0.9, y1 - 0.4, -d2], rotation=[0, 0, -sx * 28])
    # cravate : nœud en relief, pan qui s'élargit
    m.cube('torso', [-0.65, y1 - 1.6, -d2 - 0.45], [1.3, 1.1, 0.5], tie)
    m.cube('torso', [-0.55, y1 - 6.0, -d2 - 0.22], [1.1, 4.6, 0.22], tie)
    m.cube('torso', [-0.35, y1 - 6.5, -d2 - 0.22], [0.7, 0.6, 0.22], tie, pivot=[0, y1 - 6.0, -d2], rotation=[0, 0, 45])
    # gilet sombre, boutonné
    m.cube('torso', [-TORSO_W / 2 + 0.7, y0 + 0.2, -d2 - 0.15], [2.0, 5.6, 0.3], suit_d)
    m.cube('torso', [TORSO_W / 2 - 2.7, y0 + 0.2, -d2 - 0.15], [2.0, 5.6, 0.3], suit_d)
    # veste : dos et côtés, devant ouvert (deux pans épais), épaules carrées
    m.cube('torso', [-TORSO_W / 2, y0 - 0.1, -d2 + 0.6], [TORSO_W, y1 - y0 + 0.1, TORSO_D - 0.4], suit)
    m.cube('torso', [-TORSO_W / 2 - 0.25, y1 - 1.3, -d2 - 0.1], [TORSO_W + 0.5, 1.4, TORSO_D + 0.4], suit)
    for sx in (-1, 1):
        x0 = sx * (TORSO_W / 2) - (2.3 if sx > 0 else 0)
        m.cube('torso', [x0 if sx < 0 else x0, y0 - 0.1, -d2 - 0.35], [2.3, y1 - y0 - 1.0, 0.95], suit)
        # revers en pointe, en relief
        m.cube('torso', [sx * 1.7 - 0.55, y1 - 5.0, -d2 - 0.55], [1.1, 4.2, 0.3], suit_d,
               pivot=[sx * 1.7, y1 - 1.0, -d2 - 0.4], rotation=[0, 0, sx * 14])
        m.cube('torso', [sx * 2.9 - 0.25, y0 + 2.2, -d2 - 0.42], [0.5, 0.2, 0.12], flat(SUIT_DARK, 0.9))  # poche
    # pans de la veste sur les hanches : s'écartent un peu (suivent le bassin)
    for sx in (-1, 1):
        bone = f'jacket_{"r" if sx < 0 else "l"}'
        m.bone(bone, [sx * 2.4, y0, -d2], 'body', rotation=[-6, 0, -sx * 4])
        m.cube(bone, [sx * 2.4 - 1.6, y0 - 3.2, -d2 - 0.35], [3.2, 3.4, 0.9], suit)
    m.bone('jacket_b', [0, y0, d2], 'body', rotation=[6, 0, 0])
    m.cube('jacket_b', [-TORSO_W / 2, y0 - 3.2, d2 - 0.55], [TORSO_W, 3.4, 0.9], suit)
    m.cube('jacket_b', [-0.12, y0 - 3.2, d2 + 0.32], [0.24, 2.4, 0.1], suit_d)                      # fente
    # bras : manches de veste, poignets de chemise, mains
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * ARM_X
        m.box(f'{side}_arm', (cx, 0), 17.0, (3.2, 4.4, 3.3), suit)
        m.box(f'{side}_arm', (cx, 0), 19.8, (3.5, 1.8, 3.5), suit)
        m.box(f'{side}_forearm', (cx, 0), 12.9, (3.1, 4.0, 3.2), suit)
        m.box(f'{side}_forearm', (cx, 0), 12.5, (2.9, 0.6, 3.0), shirt)
        m.box(f'{side}_hand', (cx, 0), 10.1, (2.7, 2.5, 2.7), skin_mat(SKIN))
    # pantalon à pli, chaussures de ville
    pants = cloth(SUIT, SUIT_LIGHT, SUIT_DARK, ao_top=0.1, folds=1, seed=94)
    m.cube('body', [-TORSO_W / 2 + 0.4, HIP_Y - 1.2, -1.9], [TORSO_W - 0.8, 1.4, 3.8], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 2.0
        m.box(f'{side}_leg', (cx, 0), KNEE_Y - 0.2, (3.3, HIP_Y - KNEE_Y + 0.2, 3.4), pants)
        m.cube(f'{side}_leg', [cx - 0.1, KNEE_Y, -1.82], [0.2, HIP_Y - KNEE_Y - 1.0, 0.12], suit_d)     # pli
        m.box(f'{side}_shin', (cx, 0), 1.2, (3.1, KNEE_Y - 1.2, 3.2), pants)
        m.cube(f'{side}_shin', [cx - 0.1, 1.4, -1.72], [0.2, KNEE_Y - 1.6, 0.12], suit_d)
        shoe = cloth(SHOE, shade(SHOE, 1.4), shade(SHOE, 0.8), seed=95)
        m.box(f'{side}_shin', (cx, -0.5), 0.3, (3.2, 1.1, 4.6), shoe)
        m.box(f'{side}_shin', (cx, -2.3), 0.3, (2.8, 0.8, 1.0), cloth(shade(SHOE, 1.3), seed=96))
        m.box(f'{side}_shin', (cx, -0.5), 0.0, (3.3, 0.35, 4.8), flat(shade(SHOE, 0.7), 0.9))


def build_briefcase(m):
    cx, y = -ARM_X, 10.2
    m.bone('briefcase', [cx, y, 0], 'right_hand')
    leather = cloth(LEATHER, shade(LEATHER, 1.15), LEATHER_DARK, folds=0, seed=97)
    m.cube('briefcase', [cx - 1.0, y - 7.6, -3.6], [2.0, 5.4, 7.2], leather)                     # corps
    m.cube('briefcase', [cx - 1.05, y - 3.0, -3.65], [2.1, 0.5, 7.3], cloth(LEATHER_DARK, seed=98))  # rabat
    m.cube('briefcase', [cx - 0.3, y - 2.2, -1.4], [0.6, 0.4, 2.8], cloth(LEATHER_DARK, seed=99))  # poignée
    for z in (-1.4, 1.0):
        m.cube('briefcase', [cx - 0.25, y - 2.4, z], [0.5, 0.5, 0.4], cloth(LEATHER_DARK, seed=100))
        m.cube('briefcase', [cx - 1.15, y - 3.6, z - 0.6], [2.3, 0.6, 0.6], flat(BRASS, 0.9))       # fermoirs
    m.cube('briefcase', [cx - 1.08, y - 7.7, -3.7], [2.16, 0.3, 7.4], cloth(LEATHER_DARK, seed=101))


def write_expression(model, mode):
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
    m = Model('leorio', density=4, tex_width=512)
    skeleton(m)
    build_face(m)
    build_hair(m)
    build_suit(m)
    build_briefcase(m)
    m.animations = humanoid_animations(arm_swing=18, leg_swing=24, breath=1.3)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, 'froid')
