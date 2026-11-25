"""
Hisoka Morow — même style que Gon, Killua, Kurapika et Leorio.
Traits iconiques : grand (≈ 1,95 bloc), mince et athlétique ; cheveux magenta plaqués vers le haut et l'arrière
en mèches pointues ; yeux dorés étroits en amande, sourire en coin, étoile sur la joue gauche (côté droit de
l'image) et larme sur la joue droite ; tenue de bouffon : haut clair près du corps à col large, symboles de cartes
(trèfle, carreau, cœur, pique) sur la poitrine et les épaules, écharpe-ceinture magenta nouée, pantalon clair,
poignets ornés, chaussures pointues ; cartes à jouer dans la main droite.
Expression « froid » : sourire carnassier (yeux plissés, grand sourire en croissant), en combat.
"""
from __future__ import annotations

import os

from chibi import FaceGrid, arched, cloth, flat, hair_paint, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f6dfd0')
HAIR = rgb('#a01f4e')
HAIR_LIT = rgb('#e03a74')
HAIR_RIM = rgb('#ff9cc0')
LINE = rgb('#2a1a20')
GOLD_EYE = rgb('#e6b22a')
PINK = rgb('#e2508a')
TEAL = rgb('#3b9fb8')
TOP = rgb('#efe9f1')
TOP_SHADE = rgb('#c9c0d2')
PANTS = rgb('#e8e2d4')
PANTS_SHADE = rgb('#c2bba8')
SASH = rgb('#c8306c')
SASH_DARK = rgb('#8e1c4a')
SHOE = rgb('#5a3c74')
CARD = rgb('#f7f5ef')
SUIT_RED = rgb('#d01f35')
SUIT_BLACK = rgb('#1f1a22')

HEAD = (8.0, 8.6, 7.8)
HEAD_Y = 22.6
HIP_Y = 12.4
KNEE_Y = 6.3
SHOULDER_Y = 21.0
ARM_X = 4.9
TORSO_W, TORSO_D = 7.2, 3.8


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
        m.bone(f'{side}_leg', [sx * 1.9, HIP_Y, 0], 'body', rotation=[0, 0, -sx * 1.5])
        m.bone(f'{side}_shin', [sx * 1.9, KNEE_Y, 0], f'{side}_leg', rotation=[0, -sx * 6, 0])


# ---------------------------------------------------------------------------------------------------------------
# Cheveux : plaqués vers le haut et l'arrière. Le front est dégagé ; de grandes mèches pointues partent de la
# naissance des cheveux, montent puis se couchent vers l'arrière (silhouette en flamme couchée) ; côtés plaqués.

def build_hair(m):
    top = HEAD_Y + HEAD[1]
    hx, hz = HEAD[0] / 2, HEAD[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    base = hair_paint(HAIR, HAIR_LIT, HAIR_RIM, lit=0.0, seed=5)
    m.cube('hair', [-hx - 0.15, top - 1.2, -hz + 0.9], [HEAD[0] + 0.3, 1.8, HEAD[2] - 0.6], base)
    m.cube('hair', [-hx - 0.15, top - 4.8, 0.6], [HEAD[0] + 0.3, 3.8, hz - 0.3], base)
    m.cube('hair', [-hx + 0.5, top - 5.8, hz - 0.3], [HEAD[0] - 1.0, 1.2, 0.5], base)
    for sx in (-1, 1):
        m.cube('hair', [sx * (hx + 0.05) - 0.25, top - 3.4, -1.8], [0.5, 2.6, 4.2], base)
    # (nom, x, y, z, longueur, largeur, rx, rz, ry, aplatissement, courbure, éclairage, options)
    spikes = [
        ('h_f1', 0.4, top - 0.2, -3.0, 8.6, 5.0, 50, -3, 6, 0.55, 34, 0.3, dict(tip=0.05, skew=-0.06)),
        ('h_f2', -2.4, top - 0.4, -2.6, 7.8, 4.4, 52, 14, -8, 0.55, 32, 0.24, dict(tip=0.06, skew=0.08)),
        ('h_f3', 2.9, top - 0.5, -2.4, 7.2, 4.2, 54, -16, 10, 0.55, 32, 0.2, dict(tip=0.06)),
        ('h_m1', -0.8, top + 0.3, 0.0, 8.0, 4.6, 66, 6, 0, 0.55, 22, 0.14, dict(tip=0.05)),
        ('h_m2', 1.8, top + 0.2, 0.6, 7.2, 4.2, 70, -10, 8, 0.55, 22, 0.1, dict(tip=0.06)),
        ('h_m3', -3.0, top - 0.2, 0.8, 6.6, 4.0, 72, 18, -6, 0.55, 20, 0.08, dict(tip=0.06)),
        ('h_b1', 0.2, top - 0.6, 2.8, 6.8, 4.6, 82, 2, 0, 0.5, 14, 0.02, dict(tip=0.06)),
        ('h_b2', -2.6, top - 1.2, 2.8, 5.6, 4.0, 90, 20, 0, 0.5, 12, -0.02, dict(tip=0.08)),
        ('h_b3', 2.8, top - 1.2, 2.6, 5.4, 4.0, 88, -22, 0, 0.5, 12, -0.04, dict(tip=0.08)),
        ('h_s1', -hx, top - 1.6, -0.2, 4.0, 3.2, 70, 46, 0, 0.5, 12, 0.06, dict(tip=0.1)),
        ('h_s2', hx, top - 1.6, 0.2, 3.8, 3.2, 72, -48, 0, 0.5, 12, 0.0, dict(tip=0.1)),
    ]
    arched(m, spikes, mk, segs=3)


# ---------------------------------------------------------------------------------------------------------------
# Visage

FACE_D = 7


def face_paint(ctx, mode='sourire'):
    psycho = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 52, 66, 60, shade(SKIN, 0.93))
    f.rect(0, 0, 66, 5, HAIR)
    f.poly((0, 5, 5, 5, 0, 24), HAIR)
    f.poly((66, 5, 61, 5, 66, 24), HAIR)
    # sourcils fins, très relevés vers l'extérieur
    f.poly((8, 15, 27, 21, 27, 23, 8, 17), LINE)
    f.poly((58, 15, 39, 21, 39, 23, 58, 17), LINE)
    for x0, outer_left in ((11, True), (41, False)):
        x1 = x0 + 14
        if psycho:                                               # yeux plissés en croissant
            if outer_left:
                f.poly((x0, 30, x1, 27, x1, 30, x0, 32), LINE)
            else:
                f.poly((x0, 27, x1, 30, x1, 32, x0, 30), LINE)
            f.rect(x0 + 4, 29, x0 + 9, 30, GOLD_EYE)
            continue
        if outer_left:                                           # amande étroite, coin extérieur relevé
            f.poly((x0, 27, x1, 29, x1, 34, x0 + 3, 33), (250, 248, 240))
            f.poly((x0 - 2, 25, x1 + 1, 27, x1 + 1, 29, x0, 28), LINE)
        else:
            f.poly((x0, 29, x1, 27, x1 - 3, 33, x0, 34), (250, 248, 240))
            f.poly((x0 - 1, 27, x1 + 2, 25, x1, 28, x0 - 1, 29), LINE)
        ix = x0 + (5 if outer_left else 3)
        f.rect(ix, 28, ix + 6, 34, GOLD_EYE)
        f.rect(ix + 2, 29, ix + 4, 34, rgb('#3a2408'))
        f.rect(ix + 1, 28, ix + 2, 29, (255, 255, 255))
    # étoile sur la joue gauche du personnage (droite de l'image), larme sur la droite (gauche de l'image)
    cx, cy = 50, 42
    f.poly((cx, cy - 5, cx + 1.5, cy - 1.5, cx + 5, cy - 1.5, cx + 2, cy + 1, cx + 3, cy + 5, cx, cy + 2.5,
            cx - 3, cy + 5, cx - 2, cy + 1, cx - 5, cy - 1.5, cx - 1.5, cy - 1.5), TEAL)
    f.poly((16, 37, 18.5, 43, 16, 46, 13.5, 43), PINK)
    f.rect(32, 40, 33, 45, shade(SKIN, 0.84))
    if psycho:                                                   # grand sourire carnassier
        f.poly((20, 47, 46, 47, 41, 53, 25, 53), rgb('#5a1424'))
        f.poly((22, 47, 44, 47, 42, 49, 24, 49), (246, 246, 246))
        f.rect(18, 45, 20, 47, LINE)
        f.rect(46, 45, 48, 47, LINE)
    else:                                                        # sourire en coin, fin
        f.rect(26, 50, 39, 50, LINE)
        f.rect(39, 49, 42, 49, LINE)
        f.rect(42, 48, 43, 48, LINE)
        f.rect(24, 49, 26, 49, LINE)


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * 0.26)], fill=HAIR)
        ctx.draw.rectangle([int(ctx.w * 0.62), 0, ctx.w, int(ctx.h * 0.6)], fill=HAIR)
    elif ctx.face == 'south':
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * 0.62)], fill=HAIR)


def build_face(m):
    hx, hz = HEAD[0] / 2, HEAD[2] / 2
    m.head_cube = m.box('head', (0, 0), HEAD_Y, HEAD, head_paint, density=FACE_D)
    m.box('head', (0, 0), HEAD_Y - 0.8, (2.8, 0.9, 2.8), skin_mat(shade(SKIN, 0.92)))
    for sx in (-1, 1):
        m.box('head', (sx * (hx + 0.3), 0.3), HEAD_Y + 2.8, (0.6, 2.0, 1.3), skin_mat(shade(SKIN, 0.95)),
              density=FACE_D)
    m.cube('head', [-0.4, HEAD_Y + 2.7, -hz - 0.32], [0.8, 1.3, 0.36], skin_mat(shade(SKIN, 0.97)))


# ---------------------------------------------------------------------------------------------------------------
# Tenue

def suit_symbol(f, kind, cx, cy, s):
    if kind == 'diamond':
        f.poly((cx, cy - s, cx + s * 0.7, cy, cx, cy + s, cx - s * 0.7, cy), SUIT_RED)
    elif kind == 'heart':
        f.poly((cx - s, cy - s * 0.3, cx - s * 0.5, cy - s, cx, cy - s * 0.5, cx + s * 0.5, cy - s,
                cx + s, cy - s * 0.3, cx, cy + s), SUIT_RED)
    elif kind == 'spade':
        f.poly((cx, cy - s, cx + s, cy + s * 0.2, cx + s * 0.4, cy + s * 0.6, cx, cy + s * 0.2,
                cx - s * 0.4, cy + s * 0.6, cx - s, cy + s * 0.2), SUIT_BLACK)
        f.poly((cx - s * 0.2, cy + s * 0.2, cx + s * 0.2, cy + s * 0.2, cx + s * 0.4, cy + s, cx - s * 0.4, cy + s),
               SUIT_BLACK)
    else:  # club
        for dx, dy in ((0, -0.5), (-0.5, 0.1), (0.5, 0.1)):
            f.rect(cx + dx * s - s * 0.35, cy + dy * s - s * 0.35, cx + dx * s + s * 0.35, cy + dy * s + s * 0.35,
                   SUIT_BLACK)
        f.rect(cx - s * 0.15, cy, cx + s * 0.15, cy + s, SUIT_BLACK)


def top_paint(ctx):
    cloth(TOP, shade(TOP, 1.02), TOP_SHADE, ao_top=0.04, ao_bottom=0.1, folds=2, seed=110)(ctx)
    f = FaceGrid(ctx)
    if ctx.face == 'north':
        suit_symbol(f, 'club', 18, 16, 7)                          # poitrine : trèfle à droite, carreau à gauche
        suit_symbol(f, 'diamond', 48, 16, 8)
        suit_symbol(f, 'heart', 33, 42, 5)
    elif ctx.face == 'south':
        suit_symbol(f, 'spade', 33, 22, 10)


def build_outfit(m):
    top = top_paint
    trim = cloth(PINK, shade(PINK, 1.12), SASH_DARK, seed=111)
    sash = cloth(SASH, shade(SASH, 1.12), SASH_DARK, folds=1, seed=112)
    pants = cloth(PANTS, shade(PANTS, 1.03), PANTS_SHADE, ao_top=0.1, folds=2, seed=113)
    y0, y1 = HIP_Y + 0.2, SHOULDER_Y + 0.6
    d2 = TORSO_D / 2
    # haut près du corps, taille fine, épaules un peu plus larges
    m.cube('torso', [-TORSO_W / 2 + 0.5, y0, -d2], [TORSO_W - 1.0, 4.2, TORSO_D], cloth(TOP, seed=114))
    m.cube('torso', [-TORSO_W / 2, y0 + 4.0, -d2 - 0.1], [TORSO_W, y1 - y0 - 4.0, TORSO_D + 0.2], top)
    # col large en V échancré, bordé de rose
    m.cube('torso', [-1.3, y1 - 0.4, -1.2], [2.6, 0.7, 2.4], skin_mat(shade(SKIN, 0.93)))
    for sx in (-1, 1):
        m.cube('torso', [sx * 1.1 - 0.35, y1 - 3.2, -d2 - 0.22], [0.7, 3.2, 0.2], trim,
               pivot=[sx * 1.1, y1, -d2], rotation=[0, 0, -sx * 22])
    m.cube('torso', [-0.9, y1 - 3.0, -d2 - 0.12], [1.8, 2.6, 0.1], skin_mat(SKIN))
    # écharpe-ceinture magenta nouée sur le côté, pans qui retombent
    m.cube('torso', [-TORSO_W / 2 + 0.3, y0 - 0.2, -d2 - 0.25], [TORSO_W - 0.6, 1.5, TORSO_D + 0.5], sash)
    m.cube('torso', [TORSO_W / 2 - 1.6, y0 - 0.5, -d2 - 0.5], [1.3, 1.9, 0.5], sash)
    for k, (dx, L, rz) in enumerate(((0.0, 4.6, 8), (0.6, 3.6, 18))):
        m.cube('body', [TORSO_W / 2 - 1.4 + dx, y0 - L, -d2 - 0.48], [0.6, L, 0.22], sash,
               pivot=[TORSO_W / 2 - 1.1 + dx, y0, -d2 - 0.4], rotation=[0, 0, rz])
    # bras : manches longues claires avec bande rose aux épaules et poignets ornés
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * ARM_X
        m.box(f'{side}_arm', (cx, 0), 17.0, (3.0, 4.4, 3.1), cloth(TOP, seed=115))
        m.box(f'{side}_arm', (cx, 0), 19.6, (3.3, 2.0, 3.3), top)
        m.box(f'{side}_arm', (cx, 0), 19.4, (3.35, 0.35, 3.35), trim)
        m.box(f'{side}_forearm', (cx, 0), 12.9, (2.9, 4.0, 3.0), cloth(TOP, seed=116))
        m.box(f'{side}_forearm', (cx, 0), 12.6, (3.2, 1.1, 3.2), trim)
        m.box(f'{side}_forearm', (cx, 0), 13.1, (3.25, 0.25, 3.25), flat(TEAL, 0.9))
        m.box(f'{side}_hand', (cx, 0), 10.1, (2.6, 2.5, 2.6), skin_mat(SKIN))
    # pantalon clair, légèrement bouffant au genou, resserré à la cheville ; chaussures pointues
    m.cube('body', [-TORSO_W / 2 + 0.6, HIP_Y - 1.2, -1.8], [TORSO_W - 1.2, 1.4, 3.6], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 1.9
        m.box(f'{side}_leg', (cx, 0), KNEE_Y - 0.2, (3.1, HIP_Y - KNEE_Y + 0.2, 3.2), pants)
        m.box(f'{side}_shin', (cx, 0), 2.6, (3.3, KNEE_Y - 2.6, 3.3), pants)
        m.box(f'{side}_shin', (cx, 0), 1.0, (2.6, 1.7, 2.7), pants)
        m.box(f'{side}_shin', (cx, 0), 2.4, (2.8, 0.4, 2.9), trim)
        shoe = cloth(SHOE, shade(SHOE, 1.25), shade(SHOE, 0.7), seed=117)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.8, 1.1, 4.0), shoe)
        m.cube(f'{side}_shin', [cx - 0.9, 0.0, -3.4], [1.8, 0.8, 1.6], shoe,
               pivot=[cx, 0.4, -2.4], rotation=[-18, 0, 0])                    # pointe relevée
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.9, 0.3, 4.1), flat(shade(SHOE, 0.6), 0.9))


def build_cards(m):
    """Trois cartes en éventail entre les doigts de la main droite (dos rouge, face blanche)."""
    cx, y = -ARM_X, 10.0
    m.bone('cards', [cx, y, -1.2], 'right_hand', rotation=[-20, 0, 0])
    face = material(CARD, grad=0.04, noise=1, edge=0.9)
    back = material(SUIT_RED, grad=0.08, noise=2, edge=0.8)
    for k, rz in enumerate((-22, 0, 22)):
        m.cube('cards', [cx - 0.9, y - 2.8, -1.6 - k * 0.05], [1.8, 2.6, 0.06], face,
               pivot=[cx, y - 1.2, -1.6], rotation=[0, 0, rz])
        m.cube('cards', [cx - 0.9, y - 2.8, -1.55 - k * 0.05], [1.8, 2.6, 0.03], back,
               pivot=[cx, y - 1.2, -1.6], rotation=[0, 0, rz])


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
    m = Model('hisoka', density=4, tex_width=512)
    skeleton(m)
    build_face(m)
    build_hair(m)
    build_outfit(m)
    build_cards(m)
    m.animations = humanoid_animations(arm_swing=18, leg_swing=24, breath=1.3)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, 'froid')
