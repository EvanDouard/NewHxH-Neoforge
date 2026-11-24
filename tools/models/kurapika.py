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

from chibi import FaceGrid, cloth, flat, hair_paint, lock, skin_mat, strand_paint
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


# Proportions propres (plus élancé que Gon et Killua) : tête un peu plus petite, jambes plus longues, épaules plus
# étroites. Pieds à y = 0 ; hanches à 10 ; torse 10 → 18,6 ; tête 19,2 → 27,2.
HEAD = (8.6, 8.0, 8.0)
HEAD_Y = 19.2
HIP_Y = 10.0
KNEE_Y = 5.2
SHOULDER_Y = 17.8
ARM_X = 4.4


def skeleton(m):
    m.bone('body', [0, HIP_Y, 0])
    m.bone('torso', [0, HIP_Y + 1.6, 0], 'body')
    m.bone('head', [0, HEAD_Y, 0], 'torso')
    for side, sx in (('right', -1), ('left', 1)):
        m.bone(f'{side}_arm', [sx * (ARM_X - 0.4), SHOULDER_Y, 0], 'torso', rotation=[0, 0, -sx * 4])
        m.bone(f'{side}_forearm', [sx * ARM_X, 14.0, 0], f'{side}_arm')
        m.bone(f'{side}_hand', [sx * ARM_X, 10.2, 0], f'{side}_forearm')
        m.bone('item_main' if side == 'right' else 'item_off', [sx * ARM_X, 8.2, -1.0], f'{side}_hand')
        m.bone(f'{side}_leg', [sx * 1.75, HIP_Y, 0], 'body', rotation=[0, 0, -sx * 1.5])
        m.bone(f'{side}_shin', [sx * 1.75, KNEE_Y, 0], f'{side}_leg', rotation=[0, -sx * 5, 0])


def mk(lit):
    return lambda t: strand_paint(HAIR_DARK, HAIR_LIGHT, t, lit=lit, rim=HAIR_RIM)


# ---------------------------------------------------------------------------------------------------------------
# Étape 1 — cheveux : un carré lisse. Calotte arrondie (deux étages) puis des mèches plates et larges qui
# retombent tout autour de la tête, coupées net (extrémités larges : tip élevé), et qui se courbent vers
# l'intérieur en bas comme un vrai carré. Frange droite faite de mèches distinctes de longueurs un peu différentes.

def build_hair(m):
    """Refonte : chevelure arrondie en trois plans. 1) Coque intérieure sombre (cachée) qui épouse le crâne.
    2) Dôme : mèches plates couchées qui partent de l'épi au sommet et suivent l'arrondi de la tête jusqu'au bord.
    3) Mèches du carré qui retombent depuis ce bord (côtés qui encadrent le visage, arrière jusqu'à la nuque) en deux
    couches décalées, pointes effilées et irrégulières ; frange de mèches de longueurs et d'orientations
    différentes."""
    top = HEAD_Y + HEAD[1]
    hx, hz = HEAD[0] / 2, HEAD[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    inner = hair_paint(HAIR_DARK, HAIR_LIGHT, HAIR_RIM, lit=-0.2, seed=4)
    m.cube('hair', [-hx - 0.1, top - 6.6, -1.0], [HEAD[0] + 0.2, 4.8, hz + 1.1], inner)       # coque (cachée)
    # dôme arrondi : calotte puis plateaux de plus en plus en retrait (arrondi « voxel », comme un crâne de
    # Minecraft adouci) ; les mèches du carré partent du bord et recouvrent les premières marches
    w, d = HEAD[0] + 0.6, HEAD[2] + 0.6
    m.cube('hair', [-w / 2, top - 2.2, -d / 2], [w, 2.2, d], inner)
    y = top
    for inset, h, k in ((0.3, 0.5, 0.25), (0.8, 0.45, 0.3), (1.6, 0.4, 0.36), (2.8, 0.3, 0.42)):
        m.cube('hair', [-w / 2 + inset, y, -d / 2 + inset - 0.15], [w - 2 * inset, h, d - 2 * inset],
               hair_paint(HAIR_DARK, HAIR_LIGHT, HAIR_RIM, lit=k, seed=7))
        y += h

    side = dict(flat=0.5, segs=3, root=1.08, taper_pow=0.3, belly=1.05)
    for sx, sd in ((-1, 'r'), (1, 'l')):
        x = sx * (hx + 0.25)
        # couche extérieure : trois mèches larges (devant, qui encadre le visage jusqu'au menton ; milieu, sur
        # l'oreille ; arrière), longueurs et pointes irrégulières ; elles tombent droit puis rentrent
        for j, (z, L, W, k, tip, sk) in enumerate(((-2.7, 8.6, 4.4, 0.24, 0.3, -0.12), (0.2, 7.4, 4.8, 0.12, 0.5, 0.1),
                                                   (3.0, 6.6, 4.6, 0.04, 0.4, -0.06))):
            lock(m, f'hs_{sd}{j}', [x, top - 0.7, z], L, W, 0, -sx * 177, mk(k if sx < 0 else k - 0.08),
                 curve=(0, -sx * 10), tip=tip, skew=sk * sx, **side)
        # couche intérieure plus sombre, décalée : se voit entre les mèches (profondeur)
        lock(m, f'hi_{sd}0', [x - sx * 0.3, top - 0.6, -1.2], 7.4, 4.2, 0, -sx * 178, mk(-0.08),
             curve=(0, -sx * 8), tip=0.5, **side)
    # arrière : trois mèches larges vers la nuque, longueurs variées, et une couche intérieure
    for j, (x, L, W, rz, k, tip, sk) in enumerate(((-2.9, 7.6, 4.6, 3, -0.02, 0.36, 0.1), (0.1, 8.2, 4.8, 0, -0.06, 0.24, -0.06),
                                                   (3.0, 7.4, 4.6, -3, -0.04, 0.42, 0.08))):
        lock(m, f'hb_{j}', [x, top - 0.5, hz + 0.45], L, W, 182, rz, mk(k), curve=(-10, 0), tip=tip, skew=sk, **side)
    lock(m, 'hbi_0', [-1.4, top - 0.6, hz + 0.1], 6.8, 4.6, 181, 0, mk(-0.14), curve=(-8, 0), tip=0.5, **side)
    lock(m, 'hbi_1', [1.6, top - 0.6, hz + 0.1], 6.4, 4.4, 181, 0, mk(-0.14), curve=(-8, 0), tip=0.5, **side)
    # frange : mèches distinctes ; la plus longue passe entre les yeux, légèrement décalée à gauche
    for j, (x, z, L, W, rz, k, tip) in enumerate(((-3.3, -4.5, 2.8, 3.2, -14, 0.28, 0.3),
                                                  (-1.2, -4.7, 3.5, 3.4, -5, 0.34, 0.2),
                                                  (0.9, -4.55, 4.3, 3.4, 5, 0.38, 0.12),
                                                  (3.1, -4.7, 3.1, 3.2, 14, 0.3, 0.26))):
        lock(m, f'hf_{j}', [x, top + 0.1, z], L, W, 186, rz, mk(k), flat=0.5, segs=2, root=1.1, tip=tip,
             taper_pow=0.6, curve=(-10, rz * 0.4), skew=-0.04 * (1 if rz > 0 else -1))


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
    hx = HEAD[0] / 2
    m.cube('head', [hx - 0.05, HEAD_Y + 0.9, -0.3], [0.3, 0.5, 0.3], flat(GOLD, 0.9))
    m.cube('head', [hx - 0.15, HEAD_Y - 0.3, -0.4], [0.5, 1.2, 0.5], flat(RUBY, 0.9))


# ---------------------------------------------------------------------------------------------------------------
# Étapes 3 et 4 — tenue. Base commune : tunique blanche à manches longues amples, pantalon blanc, chaussures.
# Version A (« tunique ») : le tabard bleu du clan Kurta par-dessus, comme un vrai vêtement : panneaux épais devant
# et derrière qui s'écartent du corps en descendant, côtés ouverts sur la tunique, épaules qui enveloppent, grand
# col relevé ouvert devant, bordures dorées en relief, broderie centrale en relief, plis.
# Version B (« sans tunique ») : la tunique blanche seule, plus longue, avec col montant bordé de bleu, ceinture
# bleue nouée sur le côté, pans qui s'évasent sous la ceinture, fente sur les côtés.

TORSO_W, TORSO_D = 6.4, 3.6
TORSO_Y0, TORSO_Y1 = HIP_Y, 18.8


def tabard_paint(ctx):
    """Tissu bleu, broderie dorée (losanges sur la bande centrale), plis et ombres."""
    cloth(TABARD, TABARD_LIGHT, TABARD_DARK, ao_top=0.05, ao_bottom=0.12, folds=2, seed=60)(ctx)
    if ctx.face not in ('north', 'south'):
        return
    f = FaceGrid(ctx)
    f.rect(25, 0, 41, 60, TABARD_DARK)
    for y in range(3, 60, 11):
        f.poly((33, y, 39, y + 5.5, 33, y + 11, 27, y + 5.5), GOLD)
        f.poly((33, y + 3, 35.5, y + 5.5, 33, y + 8, 30.5, y + 5.5), TABARD_DARK)
        f.rect(26, y + 5, 28, y + 6, GOLD_DARK)
        f.rect(38, y + 5, 40, y + 6, GOLD_DARK)


def build_base(m, long_tunic=False):
    tunic = cloth(TUNIC, shade(TUNIC, 1.03), TUNIC_SHADE, ao_top=0.06, ao_bottom=0.12, folds=3, seed=61)
    fold = cloth(TUNIC_SHADE, seed=66)
    # torse : tunique blanche, un peu plus large aux épaules qu'à la taille
    m.cube('torso', [-TORSO_W / 2 + 0.3, TORSO_Y0, -TORSO_D / 2], [TORSO_W - 0.6, 4.6, TORSO_D], tunic)
    m.cube('torso', [-TORSO_W / 2, TORSO_Y0 + 4.4, -TORSO_D / 2 - 0.1], [TORSO_W, 4.4, TORSO_D + 0.2], tunic)
    m.cube('torso', [-1.4, TORSO_Y1 - 0.1, -1.3], [2.8, 0.8, 2.6], skin_mat(shade(SKIN, 0.93)))       # cou
    # manches amples : épaule arrondie, haut du bras, avant-bras évasé, poignet resserré, main
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * ARM_X
        m.box(f'{side}_arm', (cx, 0), 16.4, (2.9, 2.4, 3.2), tunic)                      # épaule
        m.box(f'{side}_arm', (cx, 0), 13.8, (2.6, 2.8, 2.8), tunic)
        m.cube(f'{side}_arm', [cx - 1.32, 14.2, -0.3], [0.15, 2.0, 0.6], fold)           # pli du coude
        m.box(f'{side}_forearm', (cx, 0), 11.2, (2.8, 2.9, 2.9), tunic)                  # avant-bras évasé
        m.box(f'{side}_forearm', (cx, 0), 10.3, (3.0, 1.0, 3.0), cloth(shade(TUNIC, 0.95), seed=67))
        m.box(f'{side}_hand', (cx, 0), 7.9, (2.4, 2.3, 2.4), skin_mat(SKIN))
    # pantalon blanc (jambes plus longues que Gon et Killua), chaussures bleu foncé
    pants = cloth(PANTS, shade(PANTS, 1.03), shade(PANTS, 0.78), ao_top=0.15, folds=2, seed=64)
    m.cube('body', [-3.0, HIP_Y - 1.2, -1.75], [6.0, 1.6, 3.5], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 1.75
        m.box(f'{side}_leg', (cx, 0), KNEE_Y - 0.2, (2.9, HIP_Y - KNEE_Y + 0.2, 3.1), pants)
        m.box(f'{side}_shin', (cx, 0), 1.4, (2.7, KNEE_Y - 1.4, 2.9), pants)
        m.box(f'{side}_shin', (cx, 0), 1.2, (3.0, 0.7, 3.2), cloth(shade(PANTS, 0.9), seed=68))         # revers
        shoe = cloth(SHOE, shade(SHOE, 1.2), shade(SHOE, 0.7), seed=65)
        m.box(f'{side}_shin', (cx, -0.4), 0.3, (3.0, 1.2, 4.2), shoe)
        m.box(f'{side}_shin', (cx, -1.9), 0.3, (2.7, 0.9, 1.0), cloth(shade(SHOE, 1.15), seed=69))
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.15, 0.4, 4.45), flat(shade(SHOE, 0.55), 0.9))


def build_tabard(m):
    gold = cloth(GOLD, shade(GOLD, 1.12), GOLD_DARK, seed=62)
    blue_in = cloth(TABARD_DARK, seed=70)
    T = 0.7
    for sz in (-1, 1):                                   # sz = -1 devant, +1 derrière
        zf = sz * (TORSO_D / 2 + 0.1 + T / 2)
        # partie haute (torse), légèrement plus large en bas
        m.cube('torso', [-2.9, TORSO_Y0 + 0.4, zf - T / 2], [5.8, 8.0, T], tabard_paint)
        for ex in (-1, 1):                                                               # filets en relief
            m.cube('torso', [ex * 1.05 - 0.13, TORSO_Y0 + 0.4, zf + sz * T / 2 - (0.12 if sz > 0 else 0.0)],
                   [0.26, 8.0, 0.12], gold)
        for ex in (-1, 1):                                                               # bordures latérales
            m.cube('torso', [ex * 2.9 - 0.25, TORSO_Y0 + 0.4, zf - T / 2 - 0.08], [0.5, 8.0, T + 0.16], gold)
        # pan (bassin → genou) : s'écarte du corps en descendant
        bone = f'tabard_{"front" if sz < 0 else "back"}'
        m.bone(bone, [0, TORSO_Y0 + 0.6, zf], 'body', rotation=[-sz * 7, 0, 0])
        m.cube(bone, [-3.0, 4.2, zf - T / 2], [6.0, TORSO_Y0 + 0.8 - 4.2, T], tabard_paint)
        for ex in (-1, 1):
            m.cube(bone, [ex * 1.05 - 0.13, 4.2, zf + sz * T / 2 - (0.12 if sz > 0 else 0.0)],
                   [0.26, TORSO_Y0 + 0.8 - 4.2, 0.12], gold)
        for ex in (-1, 1):
            m.cube(bone, [ex * 3.0 - 0.25, 4.2, zf - T / 2 - 0.08], [0.5, TORSO_Y0 + 0.8 - 4.2, T + 0.16], gold)
        m.cube(bone, [-3.25, 3.75, zf - T / 2 - 0.08], [6.5, 0.5, T + 0.16], gold)          # ourlet doré
        m.cube(bone, [-2.9, 4.25, zf - sz * (T / 2 + 0.02)], [5.8, TORSO_Y0 + 0.5 - 4.25, 0.04], blue_in)  # doublure
        for x in (-1.9, 1.6):                                                              # plis en relief
            m.cube(bone, [x, 4.3, zf + sz * (T / 2) - (0.1 if sz > 0 else 0)], [0.25, 4.0, 0.1],
                   cloth(TABARD_DARK, seed=71))
    # épaules : le tabard passe par-dessus et enveloppe l'épaule (devant ↔ dos)
    for sx in (-1, 1):
        m.cube('torso', [sx * 2.2 - 1.0, TORSO_Y1 - 0.4, -TORSO_D / 2 - 0.8], [2.0, 0.75, TORSO_D + 1.6],
               cloth(TABARD, TABARD_LIGHT, TABARD_DARK, seed=72))
        m.cube('torso', [sx * 3.25 - 0.2, TORSO_Y1 - 0.4, -TORSO_D / 2 - 0.85], [0.4, 0.8, TORSO_D + 1.7], gold)
    # grand col relevé, ouvert devant sur le col blanc de la tunique
    collar = cloth(TABARD, TABARD_LIGHT, TABARD_DARK, seed=73)
    m.cube('torso', [-2.6, TORSO_Y1 + 0.1, TORSO_D / 2 - 0.2], [5.2, 2.2, 0.7], collar)                   # arrière
    for sx in (-1, 1):
        m.cube('torso', [sx * 2.4 - 0.35, TORSO_Y1 + 0.1, -TORSO_D / 2 - 0.4], [0.7, 2.0, TORSO_D + 0.5], collar)
        m.cube('torso', [sx * 1.25 - 0.55, TORSO_Y1 + 0.1, -TORSO_D / 2 - 0.85], [1.1, 1.3, 0.55], collar,
               pivot=[sx * 1.25, TORSO_Y1 + 0.6, -TORSO_D / 2 - 0.6], rotation=[0, 0, sx * 22])     # revers
    m.cube('torso', [-2.8, TORSO_Y1 + 2.1, -TORSO_D / 2 - 0.45], [5.6, 0.3, TORSO_D + 1.0], gold)         # liseré
    m.cube('torso', [-1.1, TORSO_Y1 - 0.2, -TORSO_D / 2 - 0.25], [2.2, 1.4, 0.3],
           cloth(TUNIC, seed=74))                                                        # col blanc visible
    # poignets : revers bleu et or sur les manches blanches
    for side, sx in (('right', -1), ('left', 1)):
        m.box(f'{side}_forearm', (sx * ARM_X, 0), 10.25, (3.1, 0.6, 3.1), cloth(TABARD, seed=75))
        m.box(f'{side}_forearm', (sx * ARM_X, 0), 10.8, (3.12, 0.2, 3.12), gold)


def build_plain(m):
    """Version sans tabard : tunique blanche longue, col montant bordé de bleu, ceinture nouée, pans évasés."""
    tunic = cloth(TUNIC, shade(TUNIC, 1.03), TUNIC_SHADE, ao_top=0.04, ao_bottom=0.14, folds=3, seed=80)
    blue = cloth(TABARD, TABARD_LIGHT, TABARD_DARK, seed=81)
    gold = cloth(GOLD, shade(GOLD, 1.12), GOLD_DARK, seed=82)
    # col montant bordé de bleu et ouverture croisée (bande bleue en diagonale sur la poitrine)
    m.cube('torso', [-1.9, TORSO_Y1 - 0.1, -1.7], [3.8, 1.1, 3.4], tunic)
    m.cube('torso', [-1.95, TORSO_Y1 + 0.8, -1.75], [3.9, 0.3, 3.5], blue)
    m.cube('torso', [-0.25, TORSO_Y0 + 4.6, -TORSO_D / 2 - 0.22], [0.5, 4.6, 0.14], blue,
           pivot=[0, TORSO_Y0 + 6.9, -TORSO_D / 2], rotation=[0, 0, -24])
    for y in (TORSO_Y0 + 5.6, TORSO_Y0 + 7.4):                                           # attaches dorées
        m.cube('torso', [0.6, y, -TORSO_D / 2 - 0.3], [0.5, 0.4, 0.2], gold)
    # ceinture bleue nouée sur le côté gauche, pans du nœud qui retombent
    m.cube('torso', [-TORSO_W / 2 + 0.15, TORSO_Y0 + 0.2, -TORSO_D / 2 - 0.2], [TORSO_W - 0.3, 1.2, TORSO_D + 0.4], blue)
    m.cube('torso', [TORSO_W / 2 - 0.9, TORSO_Y0 - 0.1, -TORSO_D / 2 - 0.45], [1.1, 1.6, 0.5], blue)       # nœud
    for k, (dx, L) in enumerate(((0.0, 3.4), (0.55, 2.6))):
        m.cube('torso', [TORSO_W / 2 - 0.8 + dx, TORSO_Y0 - L, -TORSO_D / 2 - 0.42], [0.45, L, 0.2], blue,
               pivot=[TORSO_W / 2 - 0.6 + dx, TORSO_Y0, -TORSO_D / 2 - 0.3], rotation=[0, 0, 6 + 8 * k])
    # pans de la tunique sous la ceinture : devant et derrière, qui s'évasent, fendus sur les côtés, bord bleu
    for sz in (-1, 1):
        bone = f'skirt_{"front" if sz < 0 else "back"}'
        zf = sz * (TORSO_D / 2 + 0.05)
        m.bone(bone, [0, TORSO_Y0, zf], 'body', rotation=[-sz * 9, 0, 0])
        m.cube(bone, [-3.1, 6.6, zf - 0.3], [6.2, TORSO_Y0 - 6.6, 0.6], tunic)
        m.cube(bone, [-3.15, 6.3, zf - 0.33], [6.3, 0.45, 0.66], blue)
        m.cube(bone, [-0.2, 6.75, zf + sz * 0.3 - (0.1 if sz > 0 else 0)], [0.4, TORSO_Y0 - 7.0, 0.1],
               cloth(TUNIC_SHADE, seed=83))                                              # pli central
    # poignets bordés de bleu
    for side, sx in (('right', -1), ('left', 1)):
        m.box(f'{side}_forearm', (sx * ARM_X, 0), 10.25, (3.1, 0.5, 3.1), blue)


def build_chains(m):
    """Cinq chaînes qui pendent des doigts de la main droite, chacune avec son embout."""
    cx, y0 = -ARM_X, 7.9
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


def build_head_preview():
    m = Model('kurapika_tete', density=4, tex_width=512)
    skeleton(m)
    build_face(m)
    build_hair(m)
    return m


def build(outfit='tunique'):
    ident = 'kurapika' if outfit == 'tunique' else 'kurapika_sans_tunique'
    m = Model(ident, density=4, tex_width=512)
    skeleton(m)
    build_face(m)
    build_hair(m)
    build_base(m)
    if outfit == 'tunique':
        build_tabard(m)
    else:
        build_plain(m)
    build_chains(m)
    m.animations = humanoid_animations(arm_swing=20, leg_swing=24, breath=1.3)
    return m


def build_plain_outfit():
    return build('sans_tunique')


if __name__ == '__main__':
    for outfit in ('tunique', 'sans_tunique'):
        model = build(outfit)
        model.save()
        write_expression(model, 'froid')
