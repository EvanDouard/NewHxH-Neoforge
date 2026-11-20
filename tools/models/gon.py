"""
Gon Freecss — modèle d'après la référence d'Evan (style « Minecraft anime » : grosse tête cubique, cheveux en
grandes mèches prismatiques, veste verte à fermeture et motifs rouges, ceinture, short ample, chaussures sombres).
Construit par étapes : 1 cheveux, 2 visage, 3 veste, 4 ceinture et short, 5 chaussures et posture.
"""
from __future__ import annotations

import random

from geolib import Model, humanoid_animations, layered, material, only, rgb, shade, mix

# ---------------------------------------------------------------------------------------------------------------
# Palette (relevée sur la référence)
SKIN = rgb('#f6cfa8')
HAIR = rgb('#121214')
HAIR_LIT = rgb('#1d3a24')          # vert très foncé sur les faces éclairées
JACKET = rgb('#3ea64c')
JACKET_LIGHT = rgb('#55c062')
JACKET_DARK = rgb('#226b2e')
COLLAR = rgb('#14301a')
RED = rgb('#c9481f')                # fermeture, bandes de manches
RED_DARK = rgb('#8f2a16')           # motifs de poitrine
CUFF = rgb('#171a17')               # poignets sombres
BELT = rgb('#141414')
BUCKLE = rgb('#b9bcc0')
BUCKLE_LIGHT = rgb('#f2f2f2')
SHORTS = rgb('#3caa4b')
SHOE = rgb('#141414')
SHOE_TOP = rgb('#1c3a20')
SHOE_DETAIL = rgb('#e8e8e8')
EYE_IRIS = rgb('#5b3216')
LINE = rgb('#1a1110')

RND = random.Random(7)


# ---------------------------------------------------------------------------------------------------------------
# Étape 1 — cheveux

def hair_paint(lit=0.0):
    """Mèche : noir très sombre ; les faces éclairées (avant, côté gauche de l'image, dessus) tirent vers le vert
    très foncé, plus fort vers la pointe ; dessous et faces à l'ombre presque noirs. `lit` varie d'une mèche à
    l'autre pour différencier les mèches."""
    def paint(ctx):
        px = ctx.img.load()
        lit_face = {'north': 0.55, 'west': 0.75, 'up': 0.9, 'east': 0.18, 'south': 0.12, 'down': 0.0}[ctx.face]
        for y in range(ctx.h):
            t = 1 - y / max(1, ctx.h - 1)   # 1 en haut de la face (vers la pointe)
            for x in range(ctx.w):
                k = lit_face * (0.35 + 0.65 * t) * (0.75 + lit)
                c = mix(HAIR, HAIR_LIT, min(1, k))
                if ctx.face in ('north', 'west') and x == 1 and t > 0.3:
                    c = mix(c, rgb('#2e5a37'), 0.5 * lit_face)        # liseré de reflet
                if ctx.face not in ('up', 'down') and (x == 0 or x == ctx.w - 1):
                    c = shade(c, 0.7)
                d = RND.randint(-3, 3)
                px[x, y] = (max(0, c[0] + d), max(0, c[1] + d), max(0, c[2] + d), 255)
    return paint


def spike(m, name, base, length, width, rx, rz, ry=0, lit=0.0, depth=None, parent='hair'):
    """Grande mèche en lame triangulaire « voxel » : tranches empilées qui rétrécissent jusqu'à la pointe (bords en
    escalier, comme sur la référence), avec une vraie épaisseur. L'os oriente la lame (rx > 0 : vers l'arrière ;
    rz > 0 : vers la gauche de l'image, rz < 0 : vers la droite)."""
    m.bone(name, base, parent, rotation=[rx, ry, rz])
    x, y, z = base
    depth = depth if depth is not None else max(1.6, width * 0.42)
    n = max(3, round(length / 1.7))
    step = length / n
    for i in range(n):
        t = i / n
        w = max(0.9, width * (1 - t))
        d = max(0.8, depth * (1 - 0.5 * t))
        m.cube(name, [x - w / 2, y + i * step - 0.4, z - d / 2], [w, step + 0.4, d], hair_paint(lit + 0.25 * t))


def build_hair(m):
    m.bone('hair', [0, 25.0, 0], 'head')
    # calotte et volume de base (dépasse largement le crâne sur les côtés et derrière)
    cap = hair_paint(0.1)
    m.cube('hair', [-5.1, 23.4, -4.7], [10.2, 3.4, 9.6], cap)
    m.cube('hair', [-5.3, 18.2, 2.2], [10.6, 6.0, 2.6], cap)                 # arrière de la tête
    for sx in (-1, 1):
        m.cube('hair', [sx * 5.0 - 0.75, 19.6, -2.6], [1.5, 4.6, 5.8], cap)  # côtés (au-dessus des oreilles)
    m.cube('hair', [-4.4, 25.8, -3.4], [8.8, 2.4, 7.6], cap)                 # masse sous les mèches

    # (nom, base x, y, z, longueur, largeur, rx, rz, éclairage) — peu de mèches mais grandes
    spikes = [
        # dessus : grandes pointes dominantes, larges à la base, longueurs et directions variées
        ('s_top', 1.0, 25.8, -0.4, 9.5, 6.0, -6, -8, 0.25),
        ('s_top_l', -2.6, 25.6, 0.2, 8.5, 5.6, 2, 24, 0.10),
        ('s_top_r', 3.4, 25.2, 0.6, 7.2, 5.2, 4, -36, 0.05),
        ('s_front', -1.4, 25.6, -2.6, 6.0, 5.0, -24, 12, 0.3),
        # côtés : vers la gauche et la droite, élargissent la silhouette
        ('s_side_r', -4.4, 24.0, -0.2, 7.5, 5.2, -4, 66, 0.15),
        ('s_side_l', 4.5, 24.2, 0.6, 8.0, 5.2, 6, -62, 0.05),
        # encadrent le visage et dépassent derrière les oreilles
        ('s_temple_r', -4.9, 23.2, -2.2, 5.0, 3.6, -8, 128, 0.2),
        ('s_temple_l', 4.9, 23.0, -2.0, 4.6, 3.4, -10, -132, 0.15),
        ('s_ear_r', -4.8, 21.6, 1.6, 5.4, 4.4, 10, 92, 0.0),
        ('s_ear_l', 4.8, 21.2, 2.0, 5.0, 4.2, 16, -96, -0.05),
        # arrière : volume en profondeur
        ('s_back', 0.4, 22.4, 3.8, 7.0, 6.0, 66, -4, -0.1),
        ('s_back_r', -2.8, 20.6, 3.6, 5.6, 5.0, 84, 30, -0.1),
        ('s_back_l', 2.8, 21.0, 3.6, 5.4, 5.0, 80, -26, -0.1),
        ('s_nape', 0.6, 19.2, 3.8, 4.2, 4.6, 118, 4, -0.15),
    ]
    for name, x, y, z, L, W, rx, rz, lit in spikes:
        spike(m, name, [x, y, z], L, W, rx, rz, lit=lit)

    # frange : mèches épaisses et irrégulières qui pointent vers le bas (pointe au milieu du front), sourcils
    # visibles
    for name, x, L, W, rz in (('b_1', -3.0, 2.8, 3.6, -18), ('b_2', -0.4, 4.0, 4.0, 2),
                              ('b_3', 2.4, 3.0, 3.6, -12)):
        spike(m, name, [x, 25.6, -4.55], L, W, 174, rz, lit=0.3, depth=1.2)


# ---------------------------------------------------------------------------------------------------------------
# Étape 2 — visage

FACE_D = 7  # densité du visage (texels par pixel de modèle)


def face_paint(ctx):
    """Face avant de la tête (9,4 × 8,6 px → 66 × 60 texels) : yeux grands et rectangulaires, iris brun foncé,
    sourcils épais inclinés vers le centre, petit nez, bouche en trait neutre, mâchoire carrée."""
    skin = material(SKIN, grad=0.06, noise=2, outline=False)
    skin(ctx)
    d = ctx.draw
    sx, sy = ctx.w / 66, ctx.h / 60
    P = lambda *v: [round(v[i] * (sx if i % 2 == 0 else sy)) for i in range(len(v))]
    # ombres de modelé : bas des joues et mâchoire carrée, sous la frange
    d.rectangle(P(0, 54, 66, 60), fill=shade(SKIN, 0.92))
    d.rectangle(P(0, 50, 4, 60), fill=shade(SKIN, 0.88))
    d.rectangle(P(62, 50, 66, 60), fill=shade(SKIN, 0.88))
    d.rectangle(P(0, 12, 66, 16), fill=shade(SKIN, 0.9))
    # naissance des cheveux : bande sombre en haut, pointe en V au milieu, mèches sur les tempes
    d.rectangle(P(0, 0, 66, 13), fill=HAIR)
    d.polygon(P(24, 12, 42, 12, 33, 20), fill=HAIR)
    d.polygon(P(0, 12, 8, 12, 0, 30), fill=HAIR)
    d.polygon(P(66, 12, 58, 12, 66, 30), fill=HAIR)
    # sourcils (dessinés sous les sourcils en relief)
    d.polygon(P(11, 19, 27, 22, 27, 25, 11, 22), fill=LINE)
    d.polygon(P(55, 19, 39, 22, 39, 25, 55, 22), fill=LINE)
    # yeux
    for x0, inner_right in ((12, True), (42, False)):
        x1 = x0 + 12
        d.rectangle(P(x0, 26, x1, 41), fill=(250, 250, 250, 255))
        ix0 = x0 + (4 if inner_right else 0)
        d.rectangle(P(ix0, 28, ix0 + 8, 41), fill=EYE_IRIS)
        d.rectangle(P(ix0 + 2, 31, ix0 + 6, 39), fill=rgb('#1c0e08'))
        d.rectangle(P(ix0 + 1, 29, ix0 + 3, 31), fill=(255, 255, 255, 255))
        d.rectangle(P(ix0 + 6, 37, ix0 + 7, 38), fill=(255, 240, 225, 255))
        d.rectangle(P(x0 - 1, 25, x1 + 1, 27), fill=LINE)            # paupière supérieure épaisse
        if inner_right:                                               # petit trait de cil vers l'extérieur
            d.rectangle(P(x0 - 3, 26, x0 - 1, 28), fill=LINE)
        else:
            d.rectangle(P(x1 + 1, 26, x1 + 3, 28), fill=LINE)
        d.rectangle(P(x0 + 1, 41, x1 - 1, 41), fill=shade(SKIN, 0.7))  # paupière inférieure
    # nez et bouche
    d.rectangle(P(32, 44, 33, 46), fill=shade(SKIN, 0.8))
    d.rectangle(P(29, 51, 37, 51), fill=rgb('#7a4a36'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (0.5 if ctx.face != 'south' else 0.75))], fill=HAIR)


def build_face(m):
    hx, hz = 4.7, 4.3
    m.box('head', (0, 0), 16.8, (9.4, 8.6, 8.6), head_paint, density=FACE_D)
    m.box('head', (0, 0), 16.2, (3.0, 0.7, 3.0), material(shade(SKIN, 0.9), noise=2))          # cou
    for side in (-1, 1):                                                                       # oreilles
        m.box('head', (side * (hx + 0.35), 0.4), 19.6, (0.7, 1.9, 1.3), material(shade(SKIN, 0.95), noise=2),
              density=FACE_D)
    # sourcils en relief (expression concentrée, inclinés vers le centre)
    brow = material(LINE, grad=0.0, noise=0, outline=False)
    to_x = lambda c: -hx + c / FACE_D
    to_y = lambda r: 25.4 - r / FACE_D
    for c0, c1, rz in ((11, 27, -11), (39, 55, 11)):
        cx, cy = to_x((c0 + c1) / 2), to_y(22)
        m.cube('head', [cx - 1.15, cy - 0.27, -hz - 0.3], [2.3, 0.55, 0.32], brow,
               pivot=[cx, cy, -hz], rotation=[0, 0, rz])
    # petit nez cubique
    m.cube('head', [-0.35, to_y(46.5), -hz - 0.28], [0.7, 0.45, 0.3], material(shade(SKIN, 0.96), noise=1, outline=False))

# ---------------------------------------------------------------------------------------------------------------
# Étapes 3 à 5 — veste, ceinture, short, jambes, chaussures

def cloth(base, light=None, dark=None, ao_top=0.0, ao_bottom=0.12, folds=0, seed=0):
    """Tissu : dégradé, faces différenciées, occlusion en haut/bas (jonctions), quelques plis verticaux."""
    light = light or shade(base, 1.12)
    dark = dark or shade(base, 0.72)

    def paint(ctx):
        rnd = random.Random(seed * 31 + ctx.w * 7 + ctx.h)
        px = ctx.img.load()
        face_k = {'north': 1.0, 'west': 0.94, 'east': 0.86, 'south': 0.84, 'up': 1.08, 'down': 0.7}[ctx.face]
        fold_x = [rnd.randrange(2, max(3, ctx.w - 2)) for _ in range(folds)] if ctx.face not in ('up', 'down') else []
        for y in range(ctx.h):
            t = y / max(1, ctx.h - 1)
            for x in range(ctx.w):
                c = mix(light, base, min(1, t * 1.4)) if ctx.face not in ('up', 'down') else base
                c = shade(c, face_k)
                if ctx.face not in ('up', 'down'):
                    if t < ao_top:
                        c = shade(c, 0.78 + 0.22 * t / ao_top)
                    if t > 1 - ao_bottom:
                        c = shade(c, 0.82 + 0.18 * (1 - t) / ao_bottom)
                    if x in fold_x and 0.25 < t < 0.95:
                        c = mix(c, dark, 0.45)
                    if x == 0 or x == ctx.w - 1:
                        c = shade(c, 0.86)
                d = rnd.randint(-3, 3)
                px[x, y] = (max(0, min(255, c[0] + d)), max(0, min(255, c[1] + d)), max(0, min(255, c[2] + d)), 255)
    return paint


def flat(color, k=0.9):
    return material(color, grad=0.1, noise=2, edge=k)


def build_jacket(m):
    jacket = cloth(JACKET, JACKET_LIGHT, JACKET_DARK, ao_top=0.12, ao_bottom=0.18, folds=2, seed=1)
    m.cube('torso', [-3.7, 9.6, -2.1], [7.4, 6.8, 4.2], jacket)
    # col sombre et petite zone rouge à l'ouverture
    m.cube('torso', [-2.4, 16.2, -1.8], [4.8, 0.9, 3.6], flat(COLLAR, 0.8))
    for sx, rz in ((-1, -20), (1, 20)):
        m.cube('torso', [sx * 1.3 - 0.8, 15.0, -2.45], [1.6, 1.5, 0.4], flat(COLLAR, 0.8),
               pivot=[sx * 0.6, 16.4, -2.3], rotation=[0, 0, rz])
    m.cube('torso', [-0.55, 15.1, -2.42], [1.1, 1.2, 0.3], flat(RED, 0.85))
    # fermeture centrale rouge/orange, en relief
    m.cube('torso', [-0.42, 9.8, -2.45], [0.84, 5.4, 0.36], cloth(RED, shade(RED, 1.1), RED_DARK, seed=3))
    # motifs de poitrine : crochets anguleux rouge foncé, symétriques, en relief
    motif = flat(RED_DARK, 0.8)
    for sx in (-1, 1):
        o = lambda x: x if sx > 0 else -x - 0.0
        bars = [  # (x0, x1, y0, y1) côté droit de l'image (x > 0), recopié en miroir
            (0.9, 3.0, 12.8, 13.35),   # barre du haut
            (2.45, 3.0, 11.0, 13.35),  # montant extérieur
            (1.5, 3.0, 11.0, 11.55),   # barre du bas
            (1.5, 2.05, 11.0, 12.2),   # crochet intérieur
        ]
        for x0, x1, y0, y1 in bars:
            a, b = (x0, x1) if sx > 0 else (-x1, -x0)
            m.cube('torso', [a, y0, -2.32], [b - a, y1 - y0, 0.24], motif)


def build_arms(m):
    sleeve = cloth(JACKET, JACKET_LIGHT, JACKET_DARK, ao_top=0.05, ao_bottom=0.1, folds=1, seed=4)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 5.5
        m.box(f'{side}_arm', (cx, 0), 11.8, (3.4, 4.0, 3.4), sleeve)
        m.box(f'{side}_arm', (cx, 0), 14.7, (3.7, 1.3, 3.7), cloth(JACKET_LIGHT, seed=5), inflate=0.05)   # épaule
        m.box(f'{side}_forearm', (cx, 0), 10.2, (3.3, 1.65, 3.3), sleeve)
        m.box(f'{side}_forearm', (cx, 0), 9.2, (3.55, 1.0, 3.55), cloth(RED, shade(RED, 1.1), RED_DARK, seed=6))
        m.box(f'{side}_forearm', (cx, 0), 7.9, (3.55, 1.3, 3.55), cloth(CUFF, seed=7))
        m.box(f'{side}_hand', (cx, 0), 5.3, (3.0, 2.6, 3.0), material(SKIN, grad=0.12, noise=2, edge=0.86))
        m.box(f'{side}_hand', (cx - sx * 1.0, -1.0), 6.0, (0.9, 1.3, 1.0), material(shade(SKIN, 0.95), noise=2))


def build_waist_and_legs(m):
    shorts = cloth(SHORTS, shade(SHORTS, 1.12), shade(SHORTS, 0.7), ao_top=0.25, ao_bottom=0.08, folds=2, seed=8)
    # short : bassin, puis une jambe ample de chaque côté avec un ourlet
    m.cube('body', [-3.8, 7.3, -2.15], [7.6, 1.8, 4.3], shorts)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 2.05
        m.box(f'{side}_leg', (cx, 0), 3.9, (3.9, 3.6, 4.1), shorts)
        m.box(f'{side}_leg', (cx, 0), 3.55, (4.15, 0.75, 4.35), cloth(shade(SHORTS, 0.88), seed=9))
        # jambe nue (genou suggéré par un ton plus clair) et chaussure
        m.box(f'{side}_shin', (cx, 0), 1.7, (2.8, 2.3, 2.8),
              layered(material(SKIN, grad=0.18, noise=2, edge=0.86)))
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.4, 1.8, 4.4), cloth(SHOE, shade(SHOE, 1.25), seed=10))
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.55, 0.45, 4.6), flat(rgb('#080808'), 0.9))
        m.box(f'{side}_shin', (cx, 0), 1.6, (3.5, 1.0, 3.4), cloth(SHOE_TOP, shade(SHOE_TOP, 1.25), seed=11))
        m.cube(f'{side}_shin', [cx + sx * 1.62 - 0.1, 0.7, -1.4], [0.22, 0.5, 1.1], flat(SHOE_DETAIL, 0.95))
        m.cube(f'{side}_shin', [cx - 0.5, 0.9, -2.72], [1.0, 0.45, 0.2], flat(SHOE_DETAIL, 0.95))
    # ceinture noire et boucle métallique au centre
    m.cube('body', [-3.9, 8.9, -2.3], [7.8, 1.1, 4.6], cloth(BELT, shade(BELT, 1.8), seed=12))
    m.cube('body', [-0.9, 8.95, -2.55], [1.8, 1.0, 0.3], flat(BUCKLE, 0.75))
    m.cube('body', [-0.45, 9.2, -2.62], [0.9, 0.5, 0.12], flat(BUCKLE_LIGHT, 0.95))


# ---------------------------------------------------------------------------------------------------------------
# Construction

def build():
    m = Model('gon', density=4, tex_width=512)
    m.bone('body', [0, 7.0, 0])
    m.bone('torso', [0, 9.4, 0], 'body')
    m.bone('head', [0, 16.8, 0], 'torso')
    for side, sx in (('right', -1), ('left', 1)):
        m.bone(f'{side}_arm', [sx * 4.6, 15.6, 0], 'torso', rotation=[0, 0, -sx * 7])
        m.bone(f'{side}_forearm', [sx * 5.5, 11.8, 0], f'{side}_arm')
        m.bone(f'{side}_hand', [sx * 5.5, 7.9, 0], f'{side}_forearm')
        m.bone('item_main' if side == 'right' else 'item_off', [sx * 5.5, 6.2, -1.0], f'{side}_hand')
        m.bone(f'{side}_leg', [sx * 2.05, 7.4, 0], 'body', rotation=[0, 0, -sx * 3])
        m.bone(f'{side}_shin', [sx * 2.05, 3.8, 0], f'{side}_leg', rotation=[0, -sx * 8, 0])

    build_face(m)
    build_hair(m)
    build_jacket(m)
    build_arms(m)
    build_waist_and_legs(m)
    m.animations = humanoid_animations()
    return m


if __name__ == '__main__':
    build().save()
