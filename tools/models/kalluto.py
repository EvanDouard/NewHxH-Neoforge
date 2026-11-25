"""
Kalluto Zoldyck — même style. Enfant menu (≈ 1,35 bloc), tête plus grosse ; carré noir lisse coupé net à hauteur
du menton (mèches qui suivent le crâne puis tombent droit), frange droite épaisse au ras des sourcils ; grands
yeux sombres en amande, visage de poupée ; kimono violet à motifs de fleurs claires, obi lavande, tabi blancs et
sandales ; éventail en papier dans la main droite.
Expression « froid » : regard vide d'assassin (yeux mi-clos, ombre).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f6e4d8')
HAIR = rgb('#121218')
HAIR_L = rgb('#323648')
LINE = rgb('#1a1418')
KIMONO = rgb('#5a3a7a')
KIMONO_L = rgb('#7a58a0')
FLOWER = rgb('#e8d4f0')
OBI = rgb('#c8b4e0')

BODY = Body(head=(8.2, 8.2, 7.8), head_y=13.2, hip_y=6.6, knee_y=3.3, shoulder_y=12.0, arm_x=3.7, leg_x=1.3,
            torso_w=5.0, torso_d=3.0)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#4e5470'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(131)
    c, R = (0.0, top - 3.6, 0.2), 4.75
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=46, front_phi=36)
    rr = R + 0.22
    # carré lisse : mèches du sommet qui suivent le crâne et tombent droit jusqu'au menton, coupe nette
    for i, az in enumerate(range(0, 360, 20)):
        if 136 < az < 224:
            continue
        crown_lock(m, f'k_m{i}', c, rr, az, 96, 10.2, 3.6, mk(0.14 + 0.06 * math.cos(math.radians(az - 140))),
                   flat=0.3, turn=96, tip=0.9, segs=4, taper_pow=0.15)
    # bas du carré : rideau droit de l'équateur du crâne jusqu'au menton, coupe nette
    for i, az in enumerate(range(-126, 127, 14)):
        a = math.radians(az)
        r = rr - 0.5
        lock(m, f'k_c{i}', [c[0] + r * math.sin(a), c[1] + 0.8, c[2] + r * math.cos(a)], 5.6, 3.2, 180, 0,
             mk(0.08 + 0.04 * (i % 2)), flat=0.3, curve=(-8 * math.cos(a), 0), segs=3, root=1.05, tip=0.92,
             taper_pow=0.15, belly=1.02)
    # frange droite épaisse
    for j, az in enumerate((144, 158, 172, 188, 202, 216)):
        crown_lock(m, f'k_f{j}', c, rr + 0.05, az, 92, 7.0, 3.4, mk(0.24), flat=0.28, turn=74, tip=0.95, segs=3,
                   taper_pow=0.1)


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.04, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 18, HAIR)
    for x0 in (11, 39):
        top = 28 if cold else 25
        f.poly((x0, top + 2, x0 + 16, top, x0 + 16, 36, x0 + 2, 36), (246, 244, 248))
        f.rect(x0 + 4, top, x0 + 12, 36, rgb('#2a2236'))
        f.rect(x0 + 6, top + 2, x0 + 10, 35, (8, 6, 12))
        if not cold:
            f.rect(x0 + 5, top + 1, x0 + 7, top + 3, (255, 255, 255))
        f.poly((x0 - 1, top + 1, x0 + 17, top - 2, x0 + 17, top, x0 - 1, top + 3), LINE)
    if cold:
        f.rect(8, 20, 58, 26, shade(SKIN, 0.84))
    f.rect(30, 48, 36, 48, rgb('#b06070'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR)


def kimono_paint(ctx):
    cloth(KIMONO, KIMONO_L, shade(KIMONO, 0.7), ao_bottom=0.08, folds=3, seed=310)(ctx)
    f = FaceGrid(ctx)
    rnd = random.Random(ctx.w * 7 + ctx.h)
    for _ in range(4):                                             # petites fleurs
        x, y = rnd.uniform(6, 56), rnd.uniform(6, 50)
        f.rect(x, y + 2, x + 6, y + 4, FLOWER)
        f.rect(x + 2, y, x + 4, y + 6, FLOWER)
    if ctx.face == 'north':
        f.poly((20, 0, 27, 0, 40, 40, 34, 46), shade(KIMONO_L, 1.1))
        f.poly((27, 0, 39, 0, 33, 20), SKIN)


def build_body(m):
    b = BODY
    sleeve = kimono_paint
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], kimono_paint)
    m.cube('torso', [-b.torso_w / 2 - 0.15, y0 - 0.2, -d2 - 0.15], [b.torso_w + 0.3, 1.8, b.torso_d + 0.3],
           cloth(OBI, seed=311))
    m.cube('torso', [-1.2, y0 - 0.6, d2 + 0.1], [2.4, 2.6, 1.0], cloth(shade(OBI, 0.9), seed=312))
    # bas du kimono (jusqu'aux chevilles), en deux pans qui suivent le bassin
    m.bone('kimono_b', [0, y0, 0], 'body')
    m.cube('kimono_b', [-b.torso_w / 2 - 0.2, 0.9, -d2 - 0.2], [b.torso_w + 0.4, y0 - 0.9, b.torso_d + 0.4], kimono_paint)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y - 0.2, (2.6, b.shoulder_y - b.elbow_y + 0.8, 2.6), sleeve)
        m.box(f'{side}_forearm', (cx, 0.3), b.wrist_y + 0.6, (3.0, b.elbow_y - b.wrist_y - 0.4, 3.8), sleeve)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 1.8, (2.0, 2.2, 2.0), skin_mat(SKIN))
    # éventail fermé dans la main droite
    cx = -b.arm_x
    m.cube('right_hand', [cx - 0.35, b.wrist_y - 4.8, -1.4], [0.7, 5.0, 1.2], cloth(rgb('#f2ece0'), seed=313),
           pivot=[cx, b.wrist_y - 1.2, -0.8], rotation=[-30, 0, 0])
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.0, b.hip_y - b.knee_y + 0.2, 2.0), cloth(KIMONO, seed=314))
        m.box(f'{side}_shin', (cx, 0), 0.6, (2.0, b.knee_y - 0.6, 2.0), cloth(rgb('#f4f2ee'), seed=315))   # tabi
        m.box(f'{side}_shin', (cx, -0.2), 0.0, (2.3, 0.6, 3.0), flat(rgb('#8a6a3a'), 0.9))


def build():
    m = Model('kalluto', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=4, leg_out=1, feet_out=6)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=12, leg_swing=18, breath=0.8)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
