"""
Razor — même style. Colosse (≈ 2,3 blocs), teint pâle ; longue chevelure noire ondulée rejetée en arrière depuis
un front dégagé, qui tombe en mèches épaisses jusqu'au milieu du dos, quelques mèches rebelles sur les tempes ;
sourire carnassier, yeux sombres ; torse nu musclé sous un gilet noir sans manches ouvert, large pantalon blanc,
ceinture rouge, bottes.
Expression « froid » : sourire sadique (dents, sourcils froncés).
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade
from hairkit import curtain

SKIN = rgb('#ead6c8')
HAIR = rgb('#141418')
HAIR_L = rgb('#383a48')
LINE = rgb('#1e181a')
VEST = rgb('#16161a')
PANTS = rgb('#e8e4dc')

BODY = Body(head=(8.6, 8.8, 8.2), head_y=27.6, hip_y=14.6, knee_y=7.2, shoulder_y=26.0, arm_x=7.0, leg_x=2.6,
            torso_w=11.0, torso_d=6.0)
FACE_D = 7


def skin(k=1.0):
    return material(shade(SKIN, k), grad=0.12, noise=2, edge=0.86)


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#5c5e72'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(211)
    c, R = (0.0, top - 3.7, 0.4), 5.0
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=48, front_phi=42)
    rr = R + 0.25
    for i, x_az in enumerate((-36, -22, -8, 8, 22, 36)):
        crown_lock(m, f'r_t{i}', c, rr + 0.1, x_az + rnd.uniform(-3, 3), 132, 11.5 + rnd.uniform(-0.5, 0.8), 4.8,
                   mk(0.18 + 0.03 * (2.5 - abs(i - 2.5))), flat=0.42, turn=94, tip=0.15, segs=5,
                   twist=rnd.uniform(-8, 8), skew=rnd.uniform(-0.12, 0.12))
    for sx in (-1, 1):
        for j, (az, phi, L) in enumerate(((sx * 62, 120, 10.0), (sx * 86, 104, 8.8), (sx * 110, 90, 7.6))):
            crown_lock(m, f'r_s{sx}{j}', c, rr, az, phi, L, 4.2, mk(0.1), flat=0.42, turn=90, tip=0.15, segs=4,
                       twist=rnd.uniform(-10, 10))
        crown_lock(m, f'r_w{sx}', c, rr + 0.3, sx * 140, 96, 5.4, 2.8, mk(0.22), flat=0.5, turn=60, tip=0.08,
                   segs=3, lean=sx * 10, twist=sx * 14)                # mèche rebelle sur la tempe
    curtain(m, 'r_c', c, rr - 0.5, range(-100, 101, 14), 1.8, lambda az: 17.0 - 4.0 * abs(az) / 100, 5.0,
            lambda i: mk(0.05 + 0.05 * (i % 2)), inward=22, flare=0.3, wave=16, rnd=rnd, tip=0.1, belly=1.25)


def face_paint(ctx, mode='calme'):
    sad = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 4, HAIR)
    f.poly((8, 18 if sad else 20, 28, 22, 28, 25, 8, 23), LINE)
    f.poly((58, 18 if sad else 20, 38, 22, 38, 25, 58, 23), LINE)
    for x0 in (12, 40):
        f.rect(x0, 27, x0 + 14, 32, (244, 240, 236))
        f.rect(x0 + 5, 27, x0 + 10, 32, rgb('#2a2024'))
        f.rect(x0 + 6, 28, x0 + 8, 31, (8, 6, 6))
    f.rect(31, 34, 34, 42, shade(SKIN, 0.84))
    f.rect(16, 45, 50, 51, (246, 244, 240))                       # sourire carnassier
    f.rect(16, 44, 50, 45, LINE)
    f.rect(16, 51, 50, 52, LINE)
    if sad:
        for x in range(18, 50, 5):
            f.rect(x, 45, x + 1, 51, LINE)


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.6))], fill=HAIR)


def build_body(m):
    b = BODY
    vest = cloth(VEST, shade(VEST, 1.5), shade(VEST, 0.7), folds=2, seed=460)
    pants = cloth(PANTS, shade(PANTS, 1.03), shade(PANTS, 0.8), ao_top=0.1, folds=4, seed=461)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2 + 1.0, y0, -d2 + 0.4], [b.torso_w - 2.0, 5.0, b.torso_d - 0.8], skin(0.96))
    m.cube('torso', [-b.torso_w / 2, y0 + 4.8, -d2], [b.torso_w, y1 - y0 - 4.8, b.torso_d], skin())
    for sx in (-1, 1):
        m.cube('torso', [sx * 2.5 - 2.3, y1 - 5.2, -d2 - 0.5], [4.6, 3.4, 0.6], skin(1.05))
        for k in range(3):
            m.cube('torso', [sx * 1.2 - 1.0, y0 + 0.8 + k * 1.7, -d2 + 0.1], [2.0, 1.4, 0.4], skin(1.02))
        x0 = sx * b.torso_w / 2 - (2.8 if sx > 0 else 0)          # gilet ouvert
        m.cube('torso', [x0, y0 - 0.2, -d2 - 0.4], [2.8, y1 - y0 + 0.2, b.torso_d + 0.8], vest)
    m.cube('torso', [-b.torso_w / 2, y0 - 0.2, d2 - 0.2], [b.torso_w, y1 - y0 + 0.2, 0.6], vest)
    m.cube('torso', [-2.0, y1 - 0.2, -2.0], [4.0, 1.0, 4.0], skin(0.92))
    m.cube('torso', [-b.torso_w / 2 - 0.2, y0 - 1.2, -d2 - 0.4], [b.torso_w + 0.4, 1.8, b.torso_d + 0.8],
           cloth(rgb('#b02a30'), seed=462))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.shoulder_y - 3.6, (5.0, 4.6, 5.0), skin(0.98))
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (4.4, b.shoulder_y - b.elbow_y - 3.2, 4.4), skin())
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y, (4.2, b.elbow_y - b.wrist_y, 4.2), skin(0.97))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 3.6, (4.0, 3.8, 4.0), skin(0.95))
    m.cube('body', [-b.torso_w / 2 + 1.0, b.hip_y - 1.6, -2.8], [b.torso_w - 2.0, 1.8, 5.6], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.3, (5.2, b.hip_y - b.knee_y + 0.3, 5.4), pants)
        m.box(f'{side}_shin', (cx, 0), 1.8, (5.0, b.knee_y - 1.8, 5.2), pants)
        m.box(f'{side}_shin', (cx, -0.5), 0.0, (4.6, 2.4, 6.0), cloth(rgb('#1a1616'), seed=463))


def build():
    m = Model('razor', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=7, leg_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=20, breath=1.6)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
