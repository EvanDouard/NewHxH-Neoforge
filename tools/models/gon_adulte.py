"""
Gon adulte (Gon-san, après le Serment contre Neferpitou) — même style que Gon enfant, mais proportions d'adulte
géant : épaules très larges, torse nu musclé, short vert déchiré, bottes vertes, et surtout la chevelure noire
qui monte très haut (presque une fois et demie la hauteur de la tête… en plusieurs fois) et retombe dans le dos.
Utilisé par le mob « Gon ultime » une fois transformé (voir mob/GonUltimeEntity).
"""
from __future__ import annotations

from chibi import FaceGrid, arched, lock
from geolib import Model, humanoid_animations, material, mix, rgb, shade
from gon import (BELT, BOOT, BOOT_DARK, BOOT_LIGHT, BUCKLE, BUCKLE_LIGHT, HAIR, LACE, LINE, SHORTS, SKIN, SOLE, cloth,
                 flat, hair_paint, strand_paint)

SKIN_SHADE = shade(SKIN, 0.82)
SKIN_DEEP = shade(SKIN, 0.7)
SHORTS_DARK = shade(SHORTS, 0.66)
IRIS = rgb('#3a1d0a')

# Proportions (px, pieds à 0)
HEAD_Y, HEAD = 31.6, (7.8, 8.2, 7.8)
TORSO = (13.0, 10.6, 6.6)       # épaules très larges
WAIST_Y = 20.6


def skin(k=1.0, grad=0.14):
    return material(shade(SKIN, k), grad=grad, noise=2, edge=0.86)


# ---------------------------------------------------------------------------------------------------------------
# Muscles peints (pectoraux, abdominaux, dos)

def torso_paint(ctx):
    skin()(ctx)
    f = FaceGrid(ctx)          # grille 66 × 60 sur la face
    if ctx.face == 'north':
        # pectoraux : ombre sous chaque pectoral, sillon central
        f.poly((4, 22, 31, 25, 31, 28, 4, 25), SKIN_SHADE)
        f.poly((62, 22, 35, 25, 35, 28, 62, 25), SKIN_SHADE)
        f.rect(32, 6, 34, 27, SKIN_SHADE)
        # abdominaux : 3 rangées de 2, ligne blanche centrale
        for row in range(3):
            y = 32 + row * 9
            f.rect(20, y, 31, y + 1, SKIN_SHADE)
            f.rect(35, y, 46, y + 1, SKIN_SHADE)
        f.rect(32, 28, 34, 58, SKIN_SHADE)
        # obliques
        f.poly((10, 30, 13, 30, 19, 60, 15, 60), SKIN_SHADE)
        f.poly((56, 30, 53, 30, 47, 60, 51, 60), SKIN_SHADE)
        f.rect(32, 54, 33, 55, SKIN_DEEP)     # nombril
    elif ctx.face == 'south':
        f.rect(32, 4, 34, 60, SKIN_SHADE)       # colonne
        f.poly((6, 18, 28, 24, 26, 26, 6, 21), SKIN_SHADE)   # omoplates
        f.poly((60, 18, 38, 24, 40, 26, 60, 21), SKIN_SHADE)
        f.poly((8, 34, 30, 50, 28, 52, 8, 38), SKIN_SHADE)   # dorsaux
        f.poly((58, 34, 36, 50, 38, 52, 58, 38), SKIN_SHADE)
    elif ctx.face in ('east', 'west'):
        f.poly((0, 30, 66, 22, 66, 26, 0, 34), SKIN_SHADE)


# ---------------------------------------------------------------------------------------------------------------
# Visage : adulte, regard sombre et déterminé (iris très foncé, paupières lourdes), sourcils épais froncés

def face_paint(ctx):
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 54, 66, 60, shade(SKIN, 0.9))
    f.rect(0, 0, 66, 8, HAIR)
    f.rect(0, 8, 66, 10, shade(SKIN, 0.86))
    f.poly((0, 8, 6, 8, 0, 30), HAIR)
    f.poly((66, 8, 60, 8, 66, 30), HAIR)
    f.poly((8, 17, 28, 23, 28, 26, 8, 21), LINE)       # sourcils froncés
    f.poly((58, 17, 38, 23, 38, 26, 58, 21), LINE)
    for x0 in (11, 41):
        x1 = x0 + 14
        f.rect(x0, 27, x1, 36, (246, 244, 240))
        ix = x0 + (5 if x0 < 30 else 2)
        f.rect(ix, 28, ix + 7, 36, IRIS)
        f.rect(ix + 2, 30, ix + 5, 35, (8, 4, 2))
        f.rect(ix + 1, 29, ix + 2, 30, (230, 220, 210))
        f.rect(x0 - 1, 25, x1 + 1, 28, LINE)             # paupière lourde
        f.rect(x0 + 1, 37, x1 - 1, 37, SKIN_DEEP)
    f.rect(32, 40, 33, 46, SKIN_SHADE)                   # nez plus long
    f.rect(31, 46, 35, 47, SKIN_SHADE)
    f.rect(27, 51, 39, 51, rgb('#6a3a2a'))               # bouche serrée
    f.rect(18, 44, 21, 52, shade(SKIN, 0.9))             # pommettes marquées
    f.rect(45, 44, 48, 52, shade(SKIN, 0.9))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    skin(grad=0.08)(ctx)
    if ctx.face in ('east', 'west'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * 0.34)], fill=HAIR)
        ctx.draw.rectangle([int(ctx.w * 0.6), 0, ctx.w, ctx.h], fill=HAIR)
    elif ctx.face == 'south':
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR)


def build_head(m):
    m.box('head', (0, 0), HEAD_Y, HEAD, head_paint, density=7)
    m.box('head', (0, 0), HEAD_Y - 1.4, (3.8, 1.6, 3.6), skin(0.95))           # cou épais
    for sx in (-1, 1):
        m.box('head', (sx * 4.2, 0.4), HEAD_Y + 2.6, (0.7, 2.0, 1.3), skin(0.95), density=7)
        # sourcils en relief
        m.cube('head', [sx * 1.9 - 1.4, HEAD_Y + 5.0, -HEAD[2] / 2 - 0.3], [2.8, 0.6, 0.34], flat(LINE, 0.9),
               pivot=[sx * 1.9, HEAD_Y + 5.3, -HEAD[2] / 2], rotation=[0, 0, -sx * 14])
    m.cube('head', [-0.4, HEAD_Y + 2.6, -HEAD[2] / 2 - 0.3], [0.8, 1.2, 0.34], skin(0.97))   # nez


# ---------------------------------------------------------------------------------------------------------------
# Cheveux : masse qui épouse le crâne, très grandes mèches qui montent très haut et vers l'arrière, longues
# mèches qui retombent dans le dos

def mk(lit):
    return lambda t: strand_paint(t, lit)


def build_hair(m):
    top = HEAD_Y + HEAD[1]
    m.bone('hair', [0, top, 0], 'head')
    cap = hair_paint(0.05)
    m.cube('hair', [-4.2, top - 2.0, -4.2], [8.4, 2.6, 8.4], cap)
    m.cube('hair', [-3.6, top + 0.4, -3.4], [7.2, 1.4, 6.8], hair_paint(0.12))
    m.cube('hair', [-4.2, HEAD_Y + 1.0, 1.2], [8.4, 5.4, 3.2], cap)
    for sx in (-1, 1):
        m.cube('hair', [sx * 4.1 - 0.5, top - 3.4, -3.0], [1.0, 1.8, 4.4], cap)
    # Colonne de Gon adulte : une immense masse verticale, étroite (largeur presque constante), faite de longues
    # mèches verticales serrées qui se chevauchent ; sommet irrégulier (hauteurs variées, quelques pointes qui
    # dépassent), pas de pics radiaux. Chaque mèche : un long fût de section constante (sa propre largeur et
    # profondeur) puis une pointe effilée (mèche organique courte). Un noyau sombre bouche les interstices.
    import random
    rnd = random.Random(11)
    m.cube('hair', [-3.5, top - 1.2, -2.6], [7.0, 33.0, 5.4], hair_paint(-0.1))          # noyau
    # (x, z, largeur, profondeur, hauteur totale, base y, inclinaison rx, rz, éclairage)
    strands = [
        # rangée avant (au-dessus du front)
        (-3.5, -3.1, 2.9, 2.8, 39, top - 1.0, -2, 2, 0.26),
        (-1.2, -3.3, 3.1, 2.9, 43, top - 0.8, -3, 0, 0.3),
        (1.2, -3.2, 3.0, 2.8, 41, top - 0.8, -2, -1, 0.24),
        (3.5, -3.0, 2.8, 2.7, 37, top - 1.0, -1, -2, 0.2),
        # côtés : partent des tempes, montent à la verticale (parois de la colonne)
        (-4.5, -1.2, 2.6, 3.2, 40, top - 3.0, 0, 2, 0.22),
        (-4.6, 1.4, 2.6, 3.2, 36, top - 3.2, 1, 3, 0.12),
        (4.5, -1.0, 2.6, 3.2, 38, top - 3.0, 0, -2, 0.08),
        (4.6, 1.6, 2.6, 3.2, 41, top - 3.2, 1, -3, 0.04),
        # milieu (au-dessus du noyau) : les plus hautes, pour une masse centrale dense
        (-1.4, 0.2, 3.4, 3.4, 47, top - 0.4, 1, 1, 0.18),
        (1.6, -0.4, 3.2, 3.2, 45, top - 0.4, 0, -1, 0.14),
        # rangée arrière
        (-3.3, 3.2, 3.0, 2.8, 38, top - 2.0, 3, 2, 0.02),
        (-1.0, 3.5, 3.2, 2.8, 42, top - 1.6, 4, 0, -0.04),
        (1.4, 3.4, 3.0, 2.8, 40, top - 1.6, 3, -1, 0.1),
        (3.6, 3.1, 2.8, 2.8, 36, top - 2.0, 2, -2, -0.06),
    ]
    for i, (x, z, w, d, H, y0, rx, rz, lit) in enumerate(strands):
        bone = f'col_{i}'
        m.bone(bone, [x, y0, z], 'hair', rotation=[rx, rnd.uniform(-6, 6), rz])
        body = H * rnd.uniform(0.74, 0.8)
        m.cube(bone, [x - w / 2, y0, z - d / 2], [w, body, d], mk(lit)(0.45))
        # pointe : courte mèche effilée, légèrement déjetée, qui prolonge le fût
        lock(m, f'col_{i}_tip', [x, y0 + body - 0.4, z], H - body + 0.4, w * 1.04, rnd.uniform(-5, 5),
             rnd.uniform(-7, 7), mk(lit + 0.04), flat=d / w, curve=(rnd.uniform(-6, 6), rnd.uniform(-6, 6)),
             segs=2, root=1.0, tip=rnd.choice((0.06, 0.1, 0.16)), skew=rnd.uniform(-0.12, 0.12), parent=bone)
    # quelques mèches fines qui dépassent du sommet (irrégularités)
    for name, x, z, y_off, L, W, rx, rz, lit in (('col_x1', -0.4, -0.6, 40.0, 9.0, 2.6, -3, 5, 0.2),
                                                  ('col_x2', 2.4, 1.0, 37.5, 7.0, 2.4, 4, -8, 0.1),
                                                  ('col_x3', -2.8, 1.6, 34.0, 7.5, 2.4, 2, 9, 0.12)):
        lock(m, name, [x, top + y_off - 6, z], L + 6, W, rx, rz, mk(lit), flat=0.9, curve=(2, 3), segs=2,
             root=1.0, tip=0.08)
    # transition à la base : mèches courtes qui épousent le crâne puis se redressent vers la colonne
    base_locks = [
        ('b_l', -4.3, top - 3.4, -2.6, 5.0, 3.4, -4, 12, 0, 0.6, 10, 0.16, dict(tip=0.3, root=1.2)),
        ('b_r', 4.3, top - 3.4, -2.4, 4.6, 3.2, -4, -12, 0, 0.6, 10, 0.06, dict(tip=0.3, root=1.2)),
        ('b_bl', -3.2, top - 4.4, 3.6, 5.4, 3.6, 10, 10, 0, 0.6, 10, 0.0, dict(tip=0.3, root=1.2)),
        ('b_br', 3.2, top - 4.4, 3.6, 5.0, 3.6, 10, -10, 0, 0.6, 10, -0.04, dict(tip=0.3, root=1.2)),
        ('b_n', 0.0, top - 5.6, 4.0, 4.4, 4.4, 14, 0, 0, 0.6, 8, -0.06, dict(tip=0.3, root=1.2)),
    ]
    arched(m, base_locks, mk, segs=2)
    for name, x, z, L, W, rz, arc, sk, rxb, lt in (('ab_1', -2.6, -4.2, 2.8, 3.2, -22, 10, 0.12, 166, 0.14),
                                                    ('ab_2', -0.4, -4.45, 3.6, 3.4, -6, 12, -0.06, 170, 0.24),
                                                    ('ab_3', 1.8, -4.25, 2.2, 3.0, 14, 8, 0.1, 176, 0.1),
                                                    ('ab_4', 3.4, -4.4, 2.8, 2.6, 28, 10, -0.12, 166, 0.18)):
        lock(m, name, [x, top + 0.2, z], L, W, rxb, rz, mk(lt), flat=0.5, curve=(-arc, arc * (1 if rz > 0 else -1) * 0.5),
             segs=2, root=1.15, tip=0.1, skew=sk)


# ---------------------------------------------------------------------------------------------------------------
# Corps : torse nu musclé, épaules très larges, bras épais, short vert déchiré, ceinture, bottes vertes

def build_body(m):
    tw, th, td = TORSO
    m.box('torso', (0, 0), WAIST_Y, (tw - 2.2, th, td - 0.6), torso_paint)             # torse (taille plus fine)
    m.box('torso', (0, 0), WAIST_Y + 4.4, (tw, th - 4.4, td), torso_paint)              # cage thoracique / épaules
    m.box('torso', (0, -0.15), WAIST_Y + 6.4, (tw - 3.0, 3.2, td + 0.5), torso_paint)     # pectoraux en relief
    for sx in (-1, 1):                                                                 # trapèzes
        m.cube('torso', [sx * 3.2 - 1.6, WAIST_Y + th - 0.6, -1.8], [3.2, 1.6, 3.6], skin(0.96),
               pivot=[sx * 3.2, WAIST_Y + th, 0], rotation=[0, 0, -sx * 18])
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 8.5
        m.box(f'{side}_arm', (cx, 0), 25.4, (4.8, 4.8, 4.8), skin(0.98))                 # deltoïde
        m.box(f'{side}_arm', (cx, 0), 21.4, (4.3, 4.6, 4.3), skin())                     # biceps
        m.box(f'{side}_arm', (cx, -0.4), 22.4, (3.4, 2.8, 0.6), skin(1.03))              # relief du biceps
        m.box(f'{side}_forearm', (cx, 0), 15.0, (4.0, 6.6, 4.0), skin(0.97))
        m.box(f'{side}_forearm', (cx, 0), 15.0, (4.2, 1.4, 4.2), cloth(shade(BOOT, 0.9), seed=40))   # bande au poignet
        m.box(f'{side}_hand', (cx, 0), 11.6, (3.8, 3.4, 3.8), skin(0.98))
    # ceinture + boucle
    m.box('body', (0, 0), 19.4, (tw - 1.8, 1.4, td - 0.2), flat(BELT, 0.85))
    m.box('body', (0, -(td - 0.2) / 2 - 0.1), 19.3, (2.2, 1.6, 0.3), flat(BUCKLE, 0.9))
    m.box('body', (0, -(td - 0.2) / 2 - 0.25), 19.7, (1.0, 0.8, 0.2), flat(BUCKLE_LIGHT, 0.95))
    shorts = cloth(SHORTS, shade(SHORTS, 1.1), SHORTS_DARK, ao_top=0.2, folds=3, seed=41)
    m.box('body', (0, 0), 16.4, (tw - 2.4, 3.2, td - 0.4), shorts)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 3.0
        m.box(f'{side}_leg', (cx, 0), 12.6, (5.6, 5.6, 5.8), shorts)
        # bas déchiré : dents de tissu de longueurs différentes
        for k, (dx, dz, h) in enumerate(((-2.2, -2.6, 1.6), (-0.6, -2.7, 0.9), (1.2, -2.6, 1.9), (2.3, -1.0, 1.2),
                                         (2.3, 1.4, 1.7), (0.4, 2.7, 1.0), (-1.6, 2.6, 1.8), (-2.4, 0.6, 1.1))):
            m.cube(f'{side}_leg', [cx + dx - 0.55, 12.6 - h, dz - 0.3], [1.1, h, 0.6],
                   cloth(shade(SHORTS, 0.92 - 0.04 * (k % 3)), seed=42 + k))
        m.box(f'{side}_leg', (cx, 0), 11.6, (4.6, 1.2, 4.6), skin(0.92))                 # genou
        m.box(f'{side}_shin', (cx, 0), 7.6, (4.2, 4.2, 4.2), skin(0.96))                 # mollet
        boot = cloth(BOOT, BOOT_LIGHT, BOOT_DARK, ao_bottom=0.15, folds=1, seed=43)
        m.box(f'{side}_shin', (cx, 0.1), 2.0, (4.4, 5.8, 4.4), boot)
        m.box(f'{side}_shin', (cx, 0.1), 7.2, (4.8, 0.9, 4.8), cloth(BOOT_LIGHT, BOOT, BOOT_DARK, seed=44))   # revers
        m.box(f'{side}_shin', (cx, -0.7), 0.6, (4.4, 2.0, 6.2), boot)                    # pied
        m.box(f'{side}_shin', (cx, -3.2), 0.6, (4.0, 1.6, 1.2), cloth(BOOT_LIGHT, seed=45))
        m.box(f'{side}_shin', (cx, -0.7), 0.0, (4.7, 0.7, 6.6), flat(SOLE, 0.9))
        for k in range(3):
            m.cube(f'{side}_shin', [cx - 0.9, 3.0 + k * 1.2, -2.32], [1.8, 0.24, 0.14], flat(LACE, 0.95))


def build():
    m = Model('gon_adulte', density=4, tex_width=1024)
    m.bone('body', [0, 18.0, 0])
    m.bone('torso', [0, WAIST_Y, 0], 'body')
    m.bone('head', [0, HEAD_Y, 0], 'torso')
    for side, sx in (('right', -1), ('left', 1)):
        m.bone(f'{side}_arm', [sx * 8.5, 29.4, 0], 'torso', rotation=[0, 0, -sx * 6])
        m.bone(f'{side}_forearm', [sx * 8.5, 21.6, 0], f'{side}_arm')
        m.bone(f'{side}_hand', [sx * 8.5, 15.0, 0], f'{side}_forearm')
        m.bone('item_main' if side == 'right' else 'item_off', [sx * 8.5, 12.4, -1.2], f'{side}_hand')
        m.bone(f'{side}_leg', [sx * 3.0, 18.0, 0], 'body', rotation=[0, 0, -sx * 3])
        m.bone(f'{side}_shin', [sx * 3.0, 11.6, 0], f'{side}_leg', rotation=[0, -sx * 8, 0])
    build_head(m)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=24, leg_swing=26, breath=2.0)
    return m


if __name__ == '__main__':
    build().save()
