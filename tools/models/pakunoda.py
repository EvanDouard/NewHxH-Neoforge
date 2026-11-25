"""
Pakunoda — même style. Grande (≈ 1,85 bloc) ; cheveux blond platine mi-longs, raie sur le côté : mèches qui
partent du sommet, longue mèche balayée qui couvre un côté du front, pointes qui rebiquent vers l'extérieur au
niveau des épaules ; yeux bleu-gris en amande, rouge à lèvres ; tailleur noir (veste cintrée, pantalon), chemise
claire, escarpins noirs.
Expression « froid » : regard grave (sourcils froncés).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f2d8c4')
HAIR = rgb('#c8b47a')
HAIR_L = rgb('#f4ecc8')
LINE = rgb('#2a2024')
EYE = rgb('#6a84a8')
SUIT = rgb('#1c1c22')
SUIT_L = rgb('#30303a')
SHIRT = rgb('#e8e6e2')

BODY = Body(head=(7.6, 8.2, 7.4), head_y=21.6, hip_y=11.8, knee_y=6.0, shoulder_y=20.1, arm_x=4.4, leg_x=1.7,
            torso_w=6.2, torso_d=3.4)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#fffbe8'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(139)
    c, R = (0.0, top - 3.6, 0.3), 4.6
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=46, front_phi=34)
    rr = R + 0.25
    # mèches du sommet qui suivent le crâne
    for i, az in enumerate(range(-150, 151, 22)):
        crown_lock(m, f'p_m{i}', c, rr, az + rnd.uniform(-4, 4), 90 + rnd.uniform(-4, 4), 8.4 + rnd.uniform(-0.5, 0.5),
                   4.2, mk(0.16 + 0.06 * math.cos(math.radians(az))), flat=0.42, turn=104, tip=0.3, segs=4,
                   twist=rnd.uniform(-6, 6), skew=rnd.uniform(-0.1, 0.1))
    # mi-longs : rideau jusqu'aux épaules, pointes qui rebiquent vers l'extérieur
    for i, az in enumerate(range(-132, 133, 16)):
        a = math.radians(az)
        r = rr - 0.7
        lock(m, f'p_c{i}', [c[0] + r * math.sin(a), c[1] + 1.4, c[2] + r * math.cos(a)], 9.4, 3.6, 180, 0,
             mk(0.06 + 0.05 * (i % 2)), flat=0.36, curve=(-30 * math.cos(a), 30 * math.sin(a)), segs=4, root=1.05,
             tip=0.15, taper_pow=0.4, belly=1.05, skew=rnd.uniform(-0.1, 0.1))
    # raie sur le côté : longue mèche balayée sur le front (côté droit du perso), mèches courtes de l'autre côté
    for j, (az, L, W, lean, k) in enumerate(((150, 9.6, 4.4, -26, 0.34), (166, 8.6, 4.2, -30, 0.3),
                                             (184, 7.4, 3.8, -34, 0.26), (206, 6.0, 3.4, 10, 0.3))):
        crown_lock(m, f'p_f{j}', c, rr + 0.1, az, 84, L, W, mk(k), flat=0.42, turn=96, tip=0.15, segs=4, lean=lean,
                   skew=0.1)


def face_paint(ctx, mode='calme'):
    grave = mode == 'froid'
    material(SKIN, grad=0.04, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 10, HAIR)
    for x0 in (11, 39):
        f.poly((x0, 28, x0 + 16, 26, x0 + 16, 33, x0 + 2, 33), (248, 248, 250))
        f.rect(x0 + 5, 26, x0 + 11, 33, EYE)
        f.rect(x0 + 7, 28, x0 + 9, 32, (14, 18, 28))
        f.poly((x0 - 1, 27, x0 + 17, 24, x0 + 17, 26, x0 - 1, 29), LINE)
        if grave:
            f.poly((x0, 20, x0 + 16, 23, x0 + 16, 24, x0, 22), LINE)
    f.rect(32, 36, 33, 41, shade(SKIN, 0.86))
    f.rect(28, 47, 38, 50, rgb('#b02a3a'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR)


def suit_paint(ctx):
    cloth(SUIT, SUIT_L, shade(SUIT, 0.7), ao_bottom=0.1, folds=2, seed=330)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.poly((22, 0, 44, 0, 36, 34, 30, 34), SHIRT)
        f.poly((22, 0, 28, 0, 33, 30), SUIT_L)
        f.poly((44, 0, 38, 0, 33, 30), SUIT_L)


def build_body(m):
    b = BODY
    suit = cloth(SUIT, SUIT_L, shade(SUIT, 0.7), folds=2, seed=331)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2 + 0.3, y0, -d2], [b.torso_w - 0.6, 4.0, b.torso_d], suit)          # taille fine
    m.cube('torso', [-b.torso_w / 2, y0 + 3.6, -d2], [b.torso_w, y1 - y0 - 3.6, b.torso_d], suit_paint)
    m.cube('torso', [-b.torso_w / 2 - 0.1, y0 - 1.6, -d2 - 0.1], [b.torso_w + 0.2, 2.0, b.torso_d + 0.2], suit)  # basque
    m.cube('torso', [-1.1, y1 - 0.2, -1.1], [2.2, 0.6, 2.2], skin_mat(shade(SKIN, 0.92)))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.7, b.shoulder_y - b.elbow_y + 0.6, 2.8), suit)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (2.6, b.elbow_y - b.wrist_y - 0.2, 2.7), suit)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.1, (2.7, 0.5, 2.8), cloth(SHIRT, seed=332))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.2, (2.1, 2.2, 2.1), skin_mat(SKIN))
    pants = cloth(SUIT, SUIT_L, shade(SUIT, 0.7), folds=2, seed=333)
    m.cube('body', [-b.torso_w / 2 + 0.4, b.hip_y - 1.2, -1.6], [b.torso_w - 0.8, 1.4, 3.2], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.7, b.hip_y - b.knee_y + 0.2, 2.8), pants)
        m.box(f'{side}_shin', (cx, 0), 0.8, (2.6, b.knee_y - 0.8, 2.7), pants)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.5, 0.9, 3.6), flat(rgb('#101014'), 0.9))


def build():
    m = Model('pakunoda', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
