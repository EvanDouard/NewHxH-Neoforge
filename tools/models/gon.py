"""
Gon Freecss — modèle d'après la référence d'Evan (style « Minecraft anime » : grosse tête cubique, cheveux en
grandes mèches prismatiques, veste verte à fermeture et motifs rouges, ceinture, short ample, chaussures sombres).
Construit par étapes : 1 cheveux, 2 visage, 3 veste, 4 ceinture et short, 5 chaussures et posture.
"""
from __future__ import annotations

import random

from chibi import strand
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
BOOT = rgb('#2f8a3c')               # bottes vertes montantes
BOOT_LIGHT = rgb('#47a853')
BOOT_DARK = rgb('#1c5725')
SOLE = rgb('#1a1a1a')
LACE = rgb('#e9e2c8')
EYE_IRIS = rgb('#6e3a12')
EYE_IRIS_LIGHT = rgb('#9a5a24')
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
                k = lit_face * (0.22 + 0.25 * t) * (0.55 + lit)
                c = mix(HAIR, HAIR_LIT, min(1, k))
                if ctx.face in ('north', 'west') and x == 1 and t > 0.3:
                    c = mix(c, rgb('#2e5a37'), 0.5 * lit_face)        # liseré de reflet
                if ctx.face not in ('up', 'down') and (x == 0 or x == ctx.w - 1):
                    c = shade(c, 0.7)
                d = RND.randint(-3, 3)
                px[x, y] = (max(0, c[0] + d), max(0, c[1] + d), max(0, c[2] + d), 255)
    return paint


def spike(m, name, base, length, width, rx, rz, ry=0, lit=0.0, depth=None, parent='hair', step=1.7):
    """Grande mèche en lame triangulaire « voxel » : tranches empilées qui rétrécissent jusqu'à la pointe (bords en
    escalier, comme sur la référence), avec une vraie épaisseur. L'os oriente la lame (rx > 0 : vers l'arrière ;
    rz > 0 : vers la gauche de l'image, rz < 0 : vers la droite)."""
    m.bone(name, base, parent, rotation=[rx, ry, rz])
    x, y, z = base
    depth = depth if depth is not None else max(1.6, width * 0.42)
    n = max(3, round(length / step))
    step = length / n
    for i in range(n):
        t = i / n
        w = max(0.9, width * (1 - t))
        d = max(0.8, depth * (1 - 0.5 * t))
        m.cube(name, [x - w / 2, y + i * step - 0.4, z - d / 2], [w, step + 0.4, d], hair_paint(lit + 0.08 * t))


_STRAND_CACHE = {}


def strand_paint(t, lit=0.0):
    """Peinture d'une tranche de mèche continue : couleur uniforme sur la face (aucun dégradé propre à la tranche,
    aucun liseré haut/bas), qui ne dépend que de l'orientation de la face et de la position t le long de la mèche.
    Les petits replis du dessus (faces « up ») prennent la couleur de la face avant : on ne voit plus d'étages."""
    key = (round(t, 2), round(lit, 2))
    if key in _STRAND_CACHE:
        return _STRAND_CACHE[key]
    rnd = random.Random(hash(key) & 0xffff)

    def paint(ctx):
        px = ctx.img.load()
        lit_face = {'north': 0.5, 'up': 0.3, 'west': 0.7, 'east': 0.14, 'south': 0.1, 'down': 0.0}[ctx.face]
        k = lit_face * (0.22 + 0.3 * t) * (0.55 + lit)
        c = mix(HAIR, HAIR_LIT, min(1, k))
        for yy in range(ctx.h):
            for xx in range(ctx.w):
                cc = c
                if ctx.face in ('north', 'south', 'east', 'west') and (xx == 0 or xx == ctx.w - 1) and ctx.w > 2:
                    cc = shade(c, 0.82)
                d = rnd.randint(-2, 2)
                px[xx, yy] = (max(0, cc[0] + d), max(0, cc[1] + d), max(0, cc[2] + d), 255)
    _STRAND_CACHE[key] = paint
    return paint


def build_hair(m):
    """Itération 3 : trois niveaux. 1) masse qui épouse le crâne ; 2) 7 grandes mèches continues (base large,
    pointe effilée, légère courbure, torsion variée, bases décalées en hauteur et en profondeur) qui portent la
    silhouette ; 3) quelques mèches secondaires plus petites qui ne dépassent jamais les grandes."""
    m.bone('hair', [0, 25.0, 0], 'head')
    cap = hair_paint(0.05)
    m.cube('hair', [-5.0, 23.3, -4.7], [10.0, 3.0, 9.4], cap)                # calotte
    m.cube('hair', [-4.3, 25.7, -3.6], [8.6, 1.5, 7.4], hair_paint(0.12))    # masse centrale d'ancrage
    m.cube('hair', [-5.0, 18.6, 1.6], [10.0, 5.0, 3.2], cap)                 # arrière du crâne
    m.cube('hair', [-4.4, 17.6, 2.6], [8.8, 1.4, 2.0], hair_paint(-0.1))     # nuque
    for sx in (-1, 1):                                                        # tempes, oreilles dégagées
        m.cube('hair', [sx * 5.0 - 0.55, 21.8, -3.4], [1.1, 1.8, 5.0], cap)
        m.cube('hair', [sx * 5.0 - 0.55, 19.0, 0.6], [1.1, 3.0, 1.2], cap)

    def mk(lit):
        return lambda t: strand_paint(t, lit)

    # (nom, x, y, z, longueur, largeur, rx, rz, ry, courbure (x, z), éclairage)
    dominant = [
        ('g_top', 0.8, 26.6, -0.2, 13.0, 6.6, 6, -7, 8, (-1.2, 1.0), 0.2),       # la plus haute
        ('g_top_r', -2.6, 26.1, 0.9, 10.6, 6.0, 18, 27, -14, (1.0, 1.4), 0.08),   # plus ouverte, vers l'arrière
        ('g_top_l', 3.2, 25.5, 0.1, 9.0, 5.6, 6, -38, 20, (0.8, 0.6), 0.04),      # courte, plus couchée
        ('g_front', -1.0, 25.9, -2.7, 7.6, 5.4, -26, 12, -6, (-0.6, -0.4), 0.26), # vers l'avant
        ('g_back', 0.2, 25.2, 2.8, 11.2, 6.4, 46, -6, 4, (0.6, 1.6), 0.0),        # le dessus continue vers l'arrière
        ('g_back_r', -2.9, 24.0, 3.4, 8.6, 5.4, 63, 34, 18, (0.8, 1.2), -0.04),
        ('g_back_l', 3.1, 24.6, 2.6, 7.4, 5.2, 54, -44, -16, (-0.6, 1.0), -0.06),
    ]
    for name, x, y, z, L, W, rx, rz, ry, bend, lit in dominant:
        strand(m, name, [x, y, z], L, W, rx, rz, mk(lit), ry=ry, depth=W * 0.52, bend=bend)
    secondary = [
        ('g_mid_1', 1.9, 25.6, -2.1, 5.2, 3.8, -14, -24, 12, (0.4, 0), 0.2),    # relief entre les grandes mèches
        ('g_mid_2', -3.3, 25.0, -1.4, 4.6, 3.6, -6, 46, -10, (-0.3, 0.4), 0.16),
        ('g_side_r', -4.6, 23.4, 0.4, 4.4, 4.0, 30, 56, 10, (0, 0.8), 0.06),     # côtés : courts, vers le haut-arrière
        ('g_side_l', 4.6, 23.8, 1.0, 3.8, 3.8, 38, -58, -8, (0, 0.8), 0.02),
        ('g_temple_r', -4.9, 22.8, -2.3, 2.8, 3.0, -4, 118, 0, (0, 0), 0.12),
        ('g_nape', 0.3, 19.8, 3.8, 3.4, 5.0, 116, 2, 0, (0, 0.4), -0.12),       # nuque
        ('g_nape_r', -3.0, 20.8, 3.6, 3.0, 3.8, 100, 36, 0, (0, 0.3), -0.12),
    ]
    for name, x, y, z, L, W, rx, rz, ry, bend, lit in secondary:
        strand(m, name, [x, y, z], L, W, rx, rz, mk(lit), ry=ry, depth=W * 0.46, bend=bend)

    # frange : 4 mèches épaisses et distinctes, longueurs variées, pointes anguleuses ; le front reste visible
    for name, x, L, W, rz, bx in (('b_1', -3.3, 2.7, 3.1, -22, 0.3), ('b_2', -0.9, 3.6, 3.3, -5, -0.3),
                                  ('b_3', 1.5, 2.2, 2.9, 11, 0.2), ('b_4', 3.6, 2.9, 2.7, 28, -0.2)):
        strand(m, name, [x, 25.6, -4.75], L, W, 170, rz, mk(0.18), depth=1.4, bend=(bx, 0), step=0.45, min_w=0.3)


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
    # naissance des cheveux : bande sombre plus fine (le front se voit), mèches sur les tempes
    d.rectangle(P(0, 0, 66, 9), fill=HAIR)
    d.rectangle(P(0, 9, 66, 11), fill=shade(SKIN, 0.88))
    d.polygon(P(0, 9, 7, 9, 0, 26), fill=HAIR)
    d.polygon(P(66, 9, 59, 9, 66, 26), fill=HAIR)
    # sourcils (sous les sourcils en relief) : épais, relevés à l'extérieur, abaissés vers le centre : déterminé
    d.polygon(P(9, 16, 27, 20, 27, 23, 9, 19), fill=LINE)
    d.polygon(P(57, 16, 39, 20, 39, 23, 57, 19), fill=LINE)
    # yeux : grands, plus hauts, iris brun chaud, pupille nette et contourée, deux reflets (regard ouvert)
    for x0, inner_right in ((11, True), (42, False)):
        x1 = x0 + 13
        d.rectangle(P(x0, 25, x1, 42), fill=(252, 252, 252, 255))
        ix0 = x0 + (4 if inner_right else 0)
        d.rectangle(P(ix0, 27, ix0 + 9, 42), fill=EYE_IRIS)
        d.rectangle(P(ix0, 36, ix0 + 9, 42), fill=EYE_IRIS_LIGHT)          # bas de l'iris plus clair
        d.rectangle(P(ix0, 27, ix0 + 9, 29), fill=shade(EYE_IRIS, 0.6))    # ombre de la paupière
        d.rectangle(P(ix0 + 2, 30, ix0 + 7, 39), fill=rgb('#1a0c05'))      # pupille
        d.rectangle(P(ix0 + 1, 28, ix0 + 4, 31), fill=(255, 255, 255, 255))
        d.rectangle(P(ix0 + 6, 37, ix0 + 8, 39), fill=(255, 236, 214, 255))
        d.rectangle(P(x0 - 1, 23, x1 + 1, 26), fill=LINE)                  # paupière supérieure épaisse
        if inner_right:
            d.rectangle(P(x0 - 3, 24, x0 - 1, 27), fill=LINE)
        else:
            d.rectangle(P(x1 + 1, 24, x1 + 3, 27), fill=LINE)
        d.rectangle(P(x0 + 1, 42, x1 - 1, 42), fill=shade(SKIN, 0.68))
    # nez ; bouche discrète légèrement souriante (coins relevés)
    d.rectangle(P(32, 45, 33, 47), fill=shade(SKIN, 0.8))
    d.rectangle(P(29, 52, 37, 52), fill=rgb('#7a4a36'))
    d.rectangle(P(27, 51, 28, 51), fill=rgb('#7a4a36'))
    d.rectangle(P(38, 51, 39, 51), fill=rgb('#7a4a36'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (0.36 if ctx.face != 'south' else 0.8))], fill=HAIR)


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
    for c0, c1, rz in ((9, 27, -14), (39, 57, 14)):
        cx, cy = to_x((c0 + c1) / 2), to_y(19.5)
        m.cube('head', [cx - 1.3, cy - 0.3, -hz - 0.3], [2.6, 0.6, 0.34], brow,
               pivot=[cx, cy, -hz], rotation=[0, 0, rz])
    # petit nez cubique
    m.cube('head', [-0.35, to_y(47.5), -hz - 0.28], [0.7, 0.45, 0.3], material(shade(SKIN, 0.96), noise=1, outline=False))

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
            (0.9, 3.0, 12.95, 13.35),  # barre du haut
            (2.6, 3.0, 11.1, 13.35),   # montant extérieur
            (1.5, 3.0, 11.1, 11.5),    # barre du bas
            (1.5, 1.9, 11.1, 12.3),    # crochet intérieur
            (1.0, 1.4, 12.5, 12.95),   # petit retour vers la fermeture
        ]
        for x0, x1, y0, y1 in bars:
            a, b = (x0, x1) if sx > 0 else (-x1, -x0)
            m.cube('torso', [a, y0, -2.32], [b - a, y1 - y0, 0.24], motif)
    # plis en relief : sous les bras et au-dessus de la ceinture
    fold = cloth(JACKET_DARK, JACKET, shade(JACKET_DARK, 0.8), seed=13)
    for x, y, w, h in ((-3.4, 9.9, 0.4, 2.6), (3.0, 10.1, 0.4, 2.2), (-2.1, 9.7, 1.4, 0.3), (1.0, 9.8, 1.2, 0.3)):
        m.cube('torso', [x, y, -2.24], [w, h, 0.18], fold)
    for sx in (-1, 1):
        m.cube('torso', [sx * 3.7 - 0.1, 10.0, -1.2], [0.2, 3.4, 0.4], fold)


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
    seam = cloth(shade(SHORTS, 0.68), seed=14)
    # short : bassin avec rabats de poches, puis deux jambes bien distinctes (ourlet retourné, couture latérale,
    # pli devant, entrejambe ombré)
    m.cube('body', [-3.8, 7.3, -2.15], [7.6, 1.8, 4.3], shorts)
    m.cube('body', [-0.35, 7.0, -2.2], [0.7, 0.6, 4.4], seam)                     # entrejambe
    for sx in (-1, 1):
        m.cube('body', [sx * 2.5 - 0.9, 8.1, -2.3], [1.8, 0.55, 0.2], seam)       # rabat de poche
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 2.05
        m.box(f'{side}_leg', (cx, 0), 4.3, (3.8, 3.2, 4.0), shorts)
        m.box(f'{side}_leg', (cx, 0), 3.95, (4.1, 0.8, 4.3), cloth(shade(SHORTS, 0.9), shade(SHORTS, 1.05), seed=9))
        m.cube(f'{side}_leg', [cx + sx * 1.9 - 0.1, 4.6, -0.3], [0.2, 2.8, 0.6], seam)   # couture latérale
        m.cube(f'{side}_leg', [cx - 0.15, 4.75, -2.1], [0.3, 2.2, 0.15], seam)          # pli devant
        m.cube(f'{side}_leg', [cx - sx * 1.9 - 0.08, 4.4, -1.6], [0.16, 3.0, 3.2], seam)  # intérieur ombré
        # genou nu entre le short et la botte
        m.box(f'{side}_shin', (cx, 0), 2.6, (2.7, 1.5, 2.7), layered(material(SKIN, grad=0.18, noise=2, edge=0.86)))
        # botte verte montante : pied, tige jusqu'au haut du mollet, revers, laçage, semelle sombre
        boot = cloth(BOOT, BOOT_LIGHT, BOOT_DARK, ao_bottom=0.15, folds=1, seed=10)
        m.box(f'{side}_shin', (cx, -0.4), 0.45, (3.3, 1.5, 4.4), boot)
        m.box(f'{side}_shin', (cx, -2.25), 0.45, (3.0, 1.15, 0.9), cloth(BOOT_LIGHT, seed=15))   # bout
        m.box(f'{side}_shin', (cx, 0.1), 1.6, (3.05, 1.6, 3.05), boot)                           # tige
        m.box(f'{side}_shin', (cx, 0.1), 2.85, (3.35, 0.6, 3.35), cloth(BOOT_LIGHT, BOOT, BOOT_DARK, seed=16))  # revers
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.5, 0.5, 4.7), flat(SOLE, 0.9))
        m.box(f'{side}_shin', (cx, 1.75), 0.45, (3.15, 0.9, 0.5), flat(BOOT_DARK, 0.85))        # talon
        # laçage : sur le dessus du pied puis sur le devant de la tige
        m.cube(f'{side}_shin', [cx - 0.6, 1.0, -2.68], [1.2, 0.95, 0.12], flat(BOOT_DARK, 0.85))
        m.cube(f'{side}_shin', [cx - 0.6, 1.95, -1.6], [1.2, 0.9, 0.14], flat(BOOT_DARK, 0.85))
        for k in range(2):
            m.cube(f'{side}_shin', [cx - 0.7, 1.12 + k * 0.42, -2.78], [1.4, 0.17, 0.12], flat(LACE, 0.95))
            m.cube(f'{side}_shin', [cx - 0.7, 2.05 + k * 0.42, -1.72], [1.4, 0.17, 0.12], flat(LACE, 0.95))
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
