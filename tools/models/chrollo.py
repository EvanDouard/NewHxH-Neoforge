"""
Chrollo Lucilfer (tenue de chef de la Brigade) — même style. Grand et mince (≈ 1,9 bloc) ; cheveux noirs plaqués
vers l'arrière depuis le front jusqu'à la nuque (mèches qui suivent le crâne), quelques mèches qui retombent
derrière les oreilles ; croix tatouée sur le front, boucles d'oreilles bleues, yeux gris sombre calmes ; long
manteau noir à grand col de fourrure blanche, croix inversée dans le dos, chemise et pantalon noirs.
Expression « froid » : regard de chef (yeux plissés, ombre).
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f1dcc8')
HAIR = rgb('#141418')
HAIR_L = rgb('#353a4c')
LINE = rgb('#1e1a1c')
COAT = rgb('#18181c')
COAT_L = rgb('#2c2c34')
FUR = rgb('#f2f0ec')
FUR_SH = rgb('#bfbcb6')
CROSS = rgb('#e8e6e0')
EARRING = rgb('#3a6ad8')

BODY = Body(head=(7.8, 8.4, 7.6), head_y=22.4, hip_y=12.2, knee_y=6.2, shoulder_y=20.8, arm_x=4.8, leg_x=1.85,
            torso_w=7.0, torso_d=3.8)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#58607a'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(29)
    c, R = (0.0, top - 3.6, 0.4), 4.7
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=40, front_phi=20)
    rr = R + 0.22
    # couche arrière (posée d'abord) : tombe de l'arrière du crâne jusqu'à la nuque, pointes légèrement écartées
    for i, az in enumerate(range(-60, 61, 20)):
        crown_lock(m, f'c_k{i}', c, rr - 0.1, az + rnd.uniform(-4, 4), 72 + rnd.uniform(-4, 4), 9.0 + rnd.uniform(-0.6, 0.8),
                   3.8 + rnd.uniform(0, 0.4), mk(0.04 + 0.02 * (i % 2)), flat=0.4, turn=84, tip=0.28, segs=4,
                   twist=rnd.uniform(-6, 6), skew=rnd.uniform(-0.06, 0.06))
    # dessus : plaqué depuis la naissance des cheveux jusqu'à l'arrière du crâne
    for i, x_az in enumerate((-30, -18, -6, 6, 18, 30)):
        crown_lock(m, f'c_t{i}', c, rr, x_az + rnd.uniform(-2, 2), 140 + rnd.uniform(-3, 3), 9.6 + rnd.uniform(-0.5, 0.5),
                   3.6 + rnd.uniform(0, 0.4), mk(0.18 + 0.03 * (2.5 - abs(i - 2.5))), flat=0.32, turn=100, tip=0.35,
                   segs=5, twist=rnd.uniform(-4, 4), skew=rnd.uniform(-0.05, 0.05))
    # côtés : peignés vers l'arrière au-dessus des oreilles
    for sx in (-1, 1):
        for j, (az, phi, L) in enumerate(((sx * 56, 130, 8.6), (sx * 78, 116, 7.4), (sx * 96, 108, 4.6))):
            crown_lock(m, f'c_s{sx}{j}', c, rr - 0.05 * j, az, phi, L, 3.4, mk(0.08 if sx > 0 else 0.0), flat=0.36,
                       turn=118, tip=0.3, segs=4, twist=rnd.uniform(-6, 6), lean=sx * 4)


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 6, HAIR)
    f.rect(31, 9, 35, 19, rgb('#2a2a2a'))                       # croix tatouée
    f.rect(28, 12, 38, 15, rgb('#2a2a2a'))
    f.poly((11, 22, 27, 21, 27, 23, 11, 24), LINE)
    f.poly((55, 22, 39, 21, 39, 23, 55, 24), LINE)
    for x0 in (12, 40):
        top = 29 if cold else 27
        f.rect(x0, top, x0 + 14, 33, (246, 246, 248))
        f.rect(x0 + 4, top, x0 + 10, 33, rgb('#3a3a46'))
        f.rect(x0 + 6, top + 1, x0 + 8, 33, rgb('#0a0a0e'))
        f.rect(x0 - 1, top - 2, x0 + 15, top, LINE)
    if cold:
        f.rect(8, 24, 58, 27, shade(SKIN, 0.84))
    f.rect(32, 37, 33, 43, shade(SKIN, 0.84))
    f.rect(29, 49, 37, 49, rgb('#8a5a50'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (0.85 if ctx.face == 'south' else 0.45))], fill=HAIR)


def coat_paint(back=False):
    def paint(ctx):
        cloth(COAT, COAT_L, shade(COAT, 0.7), ao_bottom=0.1, folds=2, seed=210)(ctx)
        if back and ctx.face == 'south':
            f = FaceGrid(ctx)                                    # croix inversée
            f.rect(30, 8, 36, 56, CROSS)
            f.rect(20, 40, 46, 46, CROSS)
    return paint


def build_body(m):
    b = BODY
    coat = coat_paint()
    fur = cloth(FUR, shade(FUR, 1.02), FUR_SH, folds=4, seed=211)
    inner = cloth(rgb('#101014'), seed=212)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2 + 0.6, y0, -d2 + 0.1], [b.torso_w - 1.2, y1 - y0, b.torso_d - 0.2], inner)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.7, 2.4], skin_mat(shade(SKIN, 0.93)))
    m.cube('torso', [-b.torso_w / 2, y0 - 0.2, -d2 + 0.6], [b.torso_w, y1 - y0 + 0.2, b.torso_d - 0.2], coat)
    m.cube('torso', [-b.torso_w / 2 + 0.05, y0 - 0.2, d2 + 0.1], [b.torso_w - 0.1, y1 - y0 - 1.0, 0.4],
           coat_paint(back=True))
    for sx in (-1, 1):                                                       # pans avant ouverts
        x0 = sx * b.torso_w / 2 - (2.4 if sx > 0 else 0)
        m.cube('torso', [x0, y0 - 0.2, -d2 - 0.3], [2.4, y1 - y0 - 1.0, 0.9], coat)
    # grand col de fourrure blanche
    m.cube('torso', [-b.torso_w / 2 - 0.4, y1 - 1.6, -d2 - 0.6], [b.torso_w + 0.8, 2.2, b.torso_d + 1.2], fur)
    m.cube('torso', [-b.torso_w / 2 + 0.2, y1 + 0.4, -d2 - 0.2], [b.torso_w - 0.4, 1.4, b.torso_d + 0.6], fur)
    for k in range(6):                                                       # touffes
        x = -b.torso_w / 2 + 0.4 + k * 1.25
        m.cube('torso', [x, y1 - 2.2, -d2 - 0.75], [1.0, 1.0, 0.5], fur, pivot=[x + 0.5, y1 - 1.6, -d2 - 0.5],
               rotation=[0, 0, 45])
    # pans du manteau (genoux), qui suivent le bassin
    for name, sz in (('coat_f', -1), ('coat_b', 1)):
        m.bone(name, [0, y0, sz * d2], 'body', rotation=[-sz * 5, 0, 0])
        for sx in ((-1, 1) if sz < 0 else (0,)):
            w = 2.6 if sz < 0 else b.torso_w + 0.2
            x0 = (sx * (b.torso_w / 2) - (w if sx > 0 else 0)) if sz < 0 else -w / 2
            m.cube(name, [x0, b.knee_y - 0.6, sz * d2 - 0.35], [w, y0 - b.knee_y + 0.6, 0.7], coat)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (3.0, b.shoulder_y - b.elbow_y + 0.6, 3.1), coat)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (2.9, b.elbow_y - b.wrist_y - 0.2, 3.0), coat)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y - 0.1, (3.2, 0.8, 3.2), fur)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.4, (2.4, 2.4, 2.4), skin_mat(SKIN))
    pants = cloth(rgb('#16161a'), rgb('#26262c'), rgb('#0c0c10'), folds=1, seed=213)
    m.cube('body', [-b.torso_w / 2 + 0.6, b.hip_y - 1.2, -1.8], [b.torso_w - 1.2, 1.4, 3.6], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (3.0, b.hip_y - b.knee_y + 0.2, 3.1), pants)
        m.box(f'{side}_shin', (cx, 0), 1.0, (2.8, b.knee_y - 1.0, 2.9), pants)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.9, 1.2, 4.2), cloth(rgb('#0e0e12'), seed=214))


def build():
    m = Model('chrollo', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    for sx in (-1, 1):
        m.cube('head', [sx * (BODY.head[0] / 2 + 0.45) - 0.25, BODY.head_y + 0.8, -0.1], [0.5, 0.9, 0.5],
               flat(EARRING, 0.9))
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.2)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
