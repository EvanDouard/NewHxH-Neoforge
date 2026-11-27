"""
Franklin Bordeau — même style. Géant (≈ 2,4 blocs), visage allongé couturé de cicatrices (points de suture en
travers du front, des joues et du menton), lobes d'oreilles étirés et percés ; cheveux noirs très courts plaqués,
front haut ; petits yeux enfoncés, air calme ; long manteau bleu nuit boutonné jusqu'au col, pantalon sombre,
grosses bottes ; doigts en canons (bouts noirs).
Expression « froid » : prêt à tirer (yeux plissés, sourcils froncés).
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#d8b496')
HAIR = rgb('#16141a')
HAIR_L = rgb('#383644')
LINE = rgb('#22181a')
SCAR = rgb('#8a5a50')
COAT = rgb('#22283c')
COAT_L = rgb('#343c58')

BODY = Body(head=(8.4, 10.0, 8.0), head_y=28.6, hip_y=15.0, knee_y=7.4, shoulder_y=27.0, arm_x=7.9, leg_x=2.8,
            torso_w=12.6, torso_d=6.4)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#5a5870'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    c, R = (0.0, top - 3.8, 0.5), 5.6
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=56, front_phi=50,
          rings=((8, 14, -0.12), (30, 12, -0.08), (52, 9, -0.04), (72, 5, 0.0)))
    rr = R + 0.15
    for i, x_az in enumerate((-30, -10, 10, 30)):
        crown_lock(m, f'f_t{i}', c, rr, x_az, 124, 7.0, 3.6, mk(0.2), flat=0.25, turn=96, tip=0.4, segs=4)
    for sx in (-1, 1):
        crown_lock(m, f'f_s{sx}', c, rr, sx * 80, 100, 5.2, 3.6, mk(0.1), flat=0.25, turn=80, tip=0.4, segs=3)


def face_paint(ctx, mode='calme'):
    aim = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 4, HAIR)
    for x0 in (14, 40):
        f.rect(x0 - 2, 22, x0 + 14, 30, shade(SKIN, 0.86))         # orbites enfoncées
        f.rect(x0 + 2, 25 if not aim else 26, x0 + 10, 28, (238, 234, 226))
        f.rect(x0 + 4, 25 if not aim else 26, x0 + 8, 28, (14, 10, 8))
        if aim:
            f.poly((x0 - 2, 19, x0 + 14, 23, x0 + 14, 24, x0 - 2, 21), LINE)
    f.rect(31, 31, 35, 42, shade(SKIN, 0.84))
    f.rect(25, 49, 41, 50, rgb('#6a3e34'))
    for (x0, y0, x1) in ((10, 10, 56), (6, 36, 24), (42, 36, 60), (22, 55, 44)):   # cicatrices cousues
        f.rect(x0, y0, x1, y0 + 1, SCAR)
        for x in range(x0 + 1, x1, 3):
            f.rect(x, y0 - 2, x + 1, y0 + 3, SCAR)


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face == 'up' else 0.8 if ctx.face == 'south' else 0.4))], fill=HAIR)


def coat_paint(ctx):
    cloth(COAT, COAT_L, shade(COAT, 0.7), ao_bottom=0.1, folds=3, seed=760)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.rect(32, 0, 34, 60, shade(COAT, 0.7))
        for y in range(6, 60, 10):
            f.rect(36, y, 39, y + 3, rgb('#c8b878'))


def build_body(m):
    b = BODY
    coat = cloth(COAT, COAT_L, shade(COAT, 0.7), folds=3, seed=761)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.2, -d2], [b.torso_w, y1 - y0 + 0.2, b.torso_d], coat_paint)
    m.cube('torso', [-2.6, y1 - 0.6, -2.4], [5.2, 2.0, 4.8], coat)                             # col boutonné
    for name, sz in (('coat_f', -1), ('coat_b', 1)):
        m.bone(name, [0, y0, sz * d2], 'body', rotation=[-sz * 5, 0, 0])
        m.cube(name, [-b.torso_w / 2 - 0.1, b.knee_y - 1.0, sz * d2 - 0.4], [b.torso_w + 0.2, y0 - b.knee_y + 1.0, 0.8],
               coat_paint if sz < 0 else coat)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (5.2, b.shoulder_y - b.elbow_y + 0.6, 5.3), coat)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (5.0, b.elbow_y - b.wrist_y - 0.2, 5.1), coat)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.8, (3.8, 3.0, 3.8), skin_mat(SKIN))
        for k in range(4):                                                                  # doigts-canons
            m.cube(f'{side}_hand', [cx - 1.7 + k * 0.95, b.wrist_y - 4.2, -1.2], [0.7, 1.6, 0.7], skin_mat(shade(SKIN, 0.95)))
            m.cube(f'{side}_hand', [cx - 1.7 + k * 0.95, b.wrist_y - 4.4, -1.2], [0.7, 0.3, 0.7], flat(rgb('#141414'), 0.9))
    pants = cloth(rgb('#1e1e26'), rgb('#2e2e38'), rgb('#101016'), folds=2, seed=762)
    m.cube('body', [-b.torso_w / 2 + 1.0, b.hip_y - 1.6, -2.8], [b.torso_w - 2.0, 1.8, 5.6], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.3, (4.6, b.hip_y - b.knee_y + 0.3, 4.8), pants)
        m.box(f'{side}_shin', (cx, 0), 0.0, (4.6, b.knee_y, 4.8), cloth(rgb('#16141a'), seed=763))
        m.box(f'{side}_shin', (cx, -0.6), 0.0, (4.6, 1.4, 6.0), cloth(rgb('#16141a'), seed=764))


def build():
    m = Model('franklin', density=4, tex_width=512)
    b = BODY
    skeleton(m, b, arm_out=6, leg_out=3)
    head_box(m, b, head_paint, FACE_D, skin=SKIN, ears=False)
    for sx in (-1, 1):                                                         # lobes étirés et percés
        m.cube('head', [sx * (b.head[0] / 2 + 0.4) - 0.5, b.head_y + 1.0, -0.8], [1.0, 4.4, 1.8], skin_mat(SKIN))
        m.cube('head', [sx * (b.head[0] / 2 + 0.4) - 0.55, b.head_y + 1.6, -0.3], [1.1, 1.0, 0.8], flat(rgb('#3a2a24'), 0.9))
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=12, leg_swing=18, breath=1.6)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
