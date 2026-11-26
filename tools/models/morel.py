"""
Morel Mackernasey — même style. Grand et massif (≈ 2,1 blocs) ; longs cheveux noirs ondulés rejetés en arrière
depuis un front haut, qui tombent jusqu'aux omoplates en mèches épaisses ; moustache et bouc, sourcils épais ;
grosse pipe en bois au coin de la bouche ; long manteau brun ouvert sur un débardeur noir, pantalon sombre, bottes.
Expression « froid » : regard de combat (sourcils froncés).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade
from hairkit import curtain

SKIN = rgb('#d9ae8a')
HAIR = rgb('#18161a')
HAIR_L = rgb('#3c3a46')
LINE = rgb('#22181a')
COAT = rgb('#5a4430')
COAT_L = rgb('#78603e')
PIPE = rgb('#6a3e22')

BODY = Body(head=(8.4, 8.6, 8.0), head_y=25.2, hip_y=13.6, knee_y=6.8, shoulder_y=23.6, arm_x=6.4, leg_x=2.4,
            torso_w=10.0, torso_d=5.2)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#5c5a6e'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(191)
    c, R = (0.0, top - 3.6, 0.4), 4.9
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=48, front_phi=42)
    rr = R + 0.25
    for i, x_az in enumerate((-32, -19, -6, 7, 20, 33)):           # rejeté en arrière depuis le front
        crown_lock(m, f'm_t{i}', c, rr + 0.1, x_az + rnd.uniform(-2, 2), 132, 11.0 + rnd.uniform(-0.5, 0.8),
                   4.4, mk(0.2 + 0.03 * (2.5 - abs(i - 2.5))), flat=0.4, turn=96, tip=0.2, segs=5,
                   twist=rnd.uniform(-6, 6), skew=rnd.uniform(-0.1, 0.1))
    for sx in (-1, 1):
        for j, (az, phi, L) in enumerate(((sx * 60, 120, 9.6), (sx * 84, 104, 8.4), (sx * 108, 90, 7.4))):
            crown_lock(m, f'm_s{sx}{j}', c, rr, az, phi, L, 4.0, mk(0.1), flat=0.4, turn=92, tip=0.2, segs=4,
                       twist=rnd.uniform(-8, 8))
    curtain(m, 'm_c', c, rr - 0.5, range(-100, 101, 14), 1.8, lambda az: 13.0 - 3.0 * abs(az) / 96, 4.6,
            lambda i: mk(0.05 + 0.05 * (i % 2)), inward=22, flare=0.2, wave=12, rnd=rnd, tip=0.12, belly=1.2)


def face_paint(ctx, mode='calme'):
    fight = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 4, HAIR)
    f.poly((8, 19 if fight else 21, 28, 22, 28, 25, 8, 24), LINE)
    f.poly((58, 19 if fight else 21, 38, 22, 38, 25, 58, 24), LINE)
    for x0 in (12, 40):
        f.rect(x0, 27, x0 + 14, 32, (240, 236, 228))
        f.rect(x0 + 5, 27, x0 + 10, 32, rgb('#2a2018'))
        f.rect(x0 + 6, 28, x0 + 8, 31, (8, 6, 4))
    f.rect(31, 34, 34, 42, shade(SKIN, 0.82))
    f.rect(20, 43, 46, 46, HAIR)                                  # moustache
    f.rect(27, 50, 39, 58, HAIR)                                  # bouc
    f.rect(26, 47, 40, 48, rgb('#6a3e34'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.6))], fill=HAIR)


def build_body(m):
    b = BODY
    coat = cloth(COAT, COAT_L, shade(COAT, 0.7), ao_bottom=0.1, folds=3, seed=420)
    tank = cloth(rgb('#16161a'), rgb('#26262c'), rgb('#0c0c0e'), folds=2, seed=421)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2 + 0.8, y0, -d2 + 0.2], [b.torso_w - 1.6, y1 - y0, b.torso_d - 0.4], tank)
    m.cube('torso', [-1.8, y1 - 0.2, -1.8], [3.6, 0.8, 3.6], skin_mat(shade(SKIN, 0.92)))
    for sx in (-1, 1):                                            # pans du manteau ouvert (devant), dos plein
        x0 = sx * b.torso_w / 2 - (3.0 if sx > 0 else 0)
        m.cube('torso', [x0, y0 - 0.2, -d2 - 0.3], [3.0, y1 - y0 + 0.2, b.torso_d + 0.6], coat)
    m.cube('torso', [-b.torso_w / 2, y0 - 0.2, d2 - 0.2], [b.torso_w, y1 - y0 + 0.2, 0.6], coat)
    m.cube('torso', [-b.torso_w / 2 - 0.3, y1 - 1.2, -d2 - 0.5], [b.torso_w + 0.6, 1.8, b.torso_d + 1.0], coat)  # col
    for name, sz in (('coat_f', -1), ('coat_b', 1)):
        m.bone(name, [0, y0, sz * d2], 'body', rotation=[-sz * 5, 0, 0])
        if sz < 0:
            for sx in (-1, 1):
                m.cube(name, [sx * b.torso_w / 2 - (3.0 if sx > 0 else 0), b.knee_y - 1.0, sz * d2 - 0.4],
                       [3.0, y0 - b.knee_y + 1.0, 0.8], coat)
        else:
            m.cube(name, [-b.torso_w / 2 - 0.1, b.knee_y - 1.0, sz * d2 - 0.4], [b.torso_w + 0.2, y0 - b.knee_y + 1.0, 0.8], coat)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (4.2, b.shoulder_y - b.elbow_y + 0.6, 4.3), coat)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (4.0, b.elbow_y - b.wrist_y - 0.2, 4.1), coat)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 3.0, (3.4, 3.2, 3.4), skin_mat(SKIN))
    pants = cloth(rgb('#2a2622'), rgb('#3c3630'), rgb('#18150f'), folds=2, seed=422)
    m.cube('body', [-b.torso_w / 2 + 0.8, b.hip_y - 1.4, -2.4], [b.torso_w - 1.6, 1.6, 4.8], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (4.2, b.hip_y - b.knee_y + 0.2, 4.4), pants)
        m.box(f'{side}_shin', (cx, 0), 0.0, (4.2, b.knee_y, 4.4), cloth(rgb('#1e1a16'), seed=423))
        m.box(f'{side}_shin', (cx, -0.5), 0.0, (4.2, 1.2, 5.4), cloth(rgb('#1e1a16'), seed=424))


def build():
    m = Model('morel', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=5, leg_out=2)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    b = BODY                                                      # grosse pipe au coin de la bouche
    hz = b.head[2] / 2
    ym = b.head_y + b.head[1] * (1 - 47 / 60)
    m.cube('head', [1.6, ym - 0.4, -hz - 2.6], [0.6, 0.6, 2.8], flat(PIPE, 0.9))
    m.cube('head', [1.3, ym - 0.4, -hz - 3.6], [1.4, 1.8, 1.4], flat(shade(PIPE, 1.15), 0.9))
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=20, breath=1.4)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
