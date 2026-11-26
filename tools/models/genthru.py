"""
Genthru, le « Bomber » — même style. Grand et dégingandé (≈ 1,9 bloc), menton pointu ; cheveux blonds en pointes
hérissées vers le haut et l'arrière, quelques mèches qui tombent sur le front ; lunettes anguleuses, yeux orange
plissés, sourire en coin ; long manteau vert d'eau orné de croix, écharpe bleu clair, pantalon sombre, bottes.
Expression « froid » : rictus du tueur (yeux grands ouverts derrière les lunettes, dents).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade
from hairkit import glasses

SKIN = rgb('#ecd0b6')
HAIR = rgb('#d8b048')
HAIR_L = rgb('#f8e08a')
LINE = rgb('#2a2018')
EYE = rgb('#e8862a')
COAT = rgb('#7ec2ae')
COAT_L = rgb('#a2dccb')
SCARF = rgb('#9ccbea')
CROSS = rgb('#2e5a50')

BODY = Body(head=(7.6, 8.6, 7.4), head_y=22.4, hip_y=12.2, knee_y=6.2, shoulder_y=20.8, arm_x=4.6, leg_x=1.8,
            torso_w=6.6, torso_d=3.6)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#fff4c0'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(223)
    c, R = (0.0, top - 3.2, 0.3), 4.8
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=46, front_phi=34)
    rr = R + 0.25
    # pointes hérissées vers le haut et l'arrière (partent du dessus, se redressent)
    for i, az in enumerate(range(-150, 151, 25)):
        back = 1 - abs(az) / 150
        crown_lock(m, f'g_u{i}', c, rr, az + rnd.uniform(-5, 5), 66 + rnd.uniform(-6, 6),
                   6.0 + 1.4 * back + rnd.uniform(-0.6, 0.8), 4.4, mk(0.12 + 0.08 * (i % 2)), flat=0.55,
                   turn=6 - 10 * back, tip=0.06, segs=3, lean=(1 if az > 0 else -1) * 6 * (1 - back),
                   twist=rnd.uniform(-14, 14), skew=rnd.uniform(-0.14, 0.14))
    for i, az in enumerate((-30, 0, 30, 160, 200)):
        crown_lock(m, f'g_t{i}', c, rr + 0.3, az, 112, 5.6, 3.8, mk(0.26), flat=0.55, turn=-10, tip=0.06, segs=3,
                   twist=rnd.uniform(-12, 12))
    for i, az in enumerate(range(-120, 121, 30)):                 # nuque et côtés : mèches courtes qui couvrent le crâne
        crown_lock(m, f'g_n{i}', c, rr - 0.05, az, 40, 5.0, 4.2, mk(0.04), flat=0.5, turn=40, tip=0.15, segs=3,
                   twist=rnd.uniform(-10, 10))
    for j, (az, L, lean) in enumerate(((158, 4.6, -10), (176, 5.4, 2), (196, 4.4, 10))):     # mèches sur le front
        crown_lock(m, f'g_f{j}', c, rr, az, 66, L, 3.0, mk(0.3), flat=0.5, turn=74, tip=0.08, segs=3, lean=lean)


def face_paint(ctx, mode='calme'):
    killer = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 7, HAIR)
    for x0 in (12, 40):
        top = 26 if killer else 28
        f.rect(x0 + 1, top, x0 + 13, 32, (246, 244, 238))
        f.rect(x0 + 5, top, x0 + 10, 32, EYE)
        f.rect(x0 + 7, top + 1, x0 + 8, 31, (10, 6, 4))
        f.poly((x0, top - 1, x0 + 14, top - 3, x0 + 14, top - 1, x0, top + 1), LINE)
    f.rect(32, 35, 33, 42, shade(SKIN, 0.84))
    if killer:
        f.poly((22, 46, 44, 46, 41, 51, 25, 51), rgb('#5a2020'))
        f.rect(23, 46, 43, 47, (250, 250, 250))
    else:
        f.poly((26, 49, 40, 47, 40, 49, 26, 51), rgb('#7a4a40'))                 # sourire en coin
    f.poly((24, 56, 42, 56, 33, 60), shade(SKIN, 0.9))                           # menton pointu


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.5))], fill=HAIR)


def coat_paint(ctx):
    cloth(COAT, COAT_L, shade(COAT, 0.7), ao_bottom=0.1, folds=3, seed=470)(ctx)
    if ctx.face in ('north', 'south'):
        f = FaceGrid(ctx)
        for (x, y) in ((14, 18), (50, 18), (14, 44), (50, 44)):                  # croix décoratives
            f.rect(x - 1, y - 6, x + 1, y + 6, CROSS)
            f.rect(x - 5, y - 2, x + 5, y, CROSS)


def build_body(m):
    b = BODY
    coat = cloth(COAT, COAT_L, shade(COAT, 0.7), ao_bottom=0.1, folds=3, seed=471)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.2, -d2], [b.torso_w, y1 - y0 + 0.2, b.torso_d], coat_paint)
    # écharpe bleu clair autour du cou, un pan qui tombe devant
    m.cube('torso', [-2.6, y1 - 1.4, -2.4], [5.2, 2.2, 4.8], cloth(SCARF, shade(SCARF, 1.08), shade(SCARF, 0.75), folds=3, seed=472))
    m.cube('torso', [0.6, y1 - 6.0, -d2 - 0.5], [1.8, 5.0, 0.5], cloth(SCARF, seed=473))
    for name, sz in (('coat_f', -1), ('coat_b', 1)):
        m.bone(name, [0, y0, sz * d2], 'body', rotation=[-sz * 5, 0, 0])
        if sz < 0:
            for sx in (-1, 1):
                m.cube(name, [sx * b.torso_w / 2 - (2.6 if sx > 0 else 0), b.knee_y - 1.6, sz * d2 - 0.35],
                       [2.6, y0 - b.knee_y + 1.6, 0.7], coat)
        else:
            m.cube(name, [-b.torso_w / 2 - 0.1, b.knee_y - 1.6, sz * d2 - 0.35], [b.torso_w + 0.2, y0 - b.knee_y + 1.6, 0.7], coat_paint)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.9, b.shoulder_y - b.elbow_y + 0.6, 3.0), coat)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.1, (2.9, b.elbow_y - b.wrist_y - 0.1, 3.0), coat)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.4, (2.4, 2.4, 2.4), skin_mat(SKIN))
    pants = cloth(rgb('#2a2c34'), rgb('#3c3e48'), rgb('#18191e'), folds=2, seed=474)
    m.cube('body', [-b.torso_w / 2 + 0.5, b.hip_y - 1.2, -1.8], [b.torso_w - 1.0, 1.4, 3.6], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (3.0, b.hip_y - b.knee_y + 0.2, 3.1), pants)
        m.box(f'{side}_shin', (cx, 0), 2.0, (2.9, b.knee_y - 2.0, 3.0), pants)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.1, 2.6, 4.0), cloth(rgb('#3a2e24'), seed=475))


def build():
    m = Model('genthru', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    glasses(m, BODY, rgb('#2a2a30'), rgb('#e8f0f4'), gy=29.5, w=2.6, h=1.4)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
