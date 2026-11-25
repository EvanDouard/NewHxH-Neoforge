"""
Uvogin — même style. Colosse (≈ 2,3 blocs), torse nu massif ; énorme crinière brun-châtain sauvage : mèches
épaisses en pointes qui partent du sommet, rejetées en arrière depuis le front, retombant jusqu'au milieu du dos et
s'écartant sur les côtés (silhouette de lion) ; gros sourcils, sourire carnassier ; peau de bête sur une épaule
(bandoulière de fourrure), ceinture de fourrure, pantalon de peau, poignets de cuir, pieds nus.
Expression « froid » : rugissement (bouche grande ouverte, crocs).
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#d9a77e')
HAIR = rgb('#3e2616')
HAIR_L = rgb('#7a5030')
LINE = rgb('#2a1a12')
FUR = rgb('#9c7446')
FUR_D = rgb('#5a3e22')
PANTS = rgb('#6a4a2e')
LEATHER = rgb('#3e2a1c')

BODY = Body(head=(8.6, 8.6, 8.2), head_y=27.8, hip_y=14.6, knee_y=7.2, shoulder_y=26.2, arm_x=7.3, leg_x=2.6,
            torso_w=11.6, torso_d=6.2)
FACE_D = 7


def skin(k=1.0):
    return material(shade(SKIN, k), grad=0.12, noise=2, edge=0.86)


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#a8784a'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(41)
    c, R = (0.0, top - 3.6, 0.5), 4.9
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=40, front_phi=20)
    rr = R + 0.25
    # crinière : grosses mèches du sommet vers l'arrière, très longues dans le dos, plus courtes et plus écartées
    # sur les côtés (silhouette de lion)
    for i, az in enumerate(range(-120, 121, 20)):
        side = abs(az) / 120
        L = 21.0 - 9.0 * side + rnd.uniform(-1.2, 1.2)
        crown_lock(m, f'u_m{i}', c, rr, az + rnd.uniform(-5, 5), 62 + rnd.uniform(-5, 5), L,
                   5.2 + rnd.uniform(-0.4, 0.6), mk(0.06 + 0.06 * (i % 2)), flat=0.5, turn=88 - 6 * side,
                   tip=0.1, segs=4, lean=(1 if az > 0 else -1) * (3 + 9 * side), twist=rnd.uniform(-10, 10),
                   skew=rnd.uniform(-0.12, 0.12))
    # pointes sauvages qui s'écartent de la masse (mi-hauteur)
    for i, az in enumerate((-110, -78, -46, -14, 18, 50, 82, 112)):
        sgn = 1 if az > 0 else -1
        crown_lock(m, f'u_w{i}', c, rr + 0.2, az + rnd.uniform(-6, 6), 46 + rnd.uniform(-6, 6),
                   8.5 + rnd.uniform(-1, 1.5), 4.0, mk(-0.04), flat=0.5, turn=42, tip=0.08, segs=3,
                   lean=sgn * (14 + rnd.uniform(0, 10)), twist=rnd.uniform(-12, 12), skew=rnd.uniform(-0.15, 0.15))
    # couche haute : mèches rejetées depuis le front, qui gonflent le dessus et retombent sur la crinière
    for i, az in enumerate((-44, -26, -9, 9, 26, 44)):
        crown_lock(m, f'u_t{i}', c, rr + 0.3, az + rnd.uniform(-3, 3), 128 + rnd.uniform(-4, 4),
                   12.5 + rnd.uniform(-1, 1), 4.6 + rnd.uniform(0, 0.5), mk(0.2 + 0.03 * (2.5 - abs(i - 2.5))),
                   flat=0.45, turn=112, tip=0.12, segs=5, twist=rnd.uniform(-8, 8), skew=rnd.uniform(-0.1, 0.1))
    # pointes du front qui se relèvent (épis) et favoris épais
    for i, az in enumerate((-30, -10, 12, 32)):
        crown_lock(m, f'u_f{i}', c, rr + 0.1, 180 + az, 58, 4.6 + rnd.uniform(-0.4, 0.6), 3.4, mk(0.26), flat=0.5,
                   turn=-70, tip=0.1, segs=3, twist=rnd.uniform(-10, 10))
    for sx in (-1, 1):
        crown_lock(m, f'u_sb{sx}', c, rr - 0.1, sx * 128, 42, 7.8, 3.6, mk(0.04), flat=0.5, turn=20, tip=0.2,
                   segs=3, lean=sx * 6)


def face_paint(ctx, mode='calme'):
    roar = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 5, HAIR)
    f.poly((8, 19, 28, 16, 29, 21, 9, 23), HAIR)                # gros sourcils
    f.poly((58, 19, 38, 16, 37, 21, 57, 23), HAIR)
    for x0 in (12, 40):
        f.rect(x0, 25, x0 + 14, 31, (246, 242, 236))
        f.rect(x0 + 5, 25, x0 + 10, 31, rgb('#3a2414'))
        f.rect(x0 + 6, 26, x0 + 8, 30, (10, 6, 4))
        f.rect(x0 - 1, 23, x0 + 15, 25, LINE)
    f.rect(30, 32, 36, 40, shade(SKIN, 0.82))
    if roar:
        f.rect(16, 44, 50, 58, rgb('#4a1410'))
        for x in range(18, 48, 6):
            f.poly((x, 44, x + 4, 44, x + 2, 48), (244, 240, 230))
        f.poly((18, 58, 22, 58, 20, 52), (244, 240, 230))
        f.poly((44, 58, 48, 58, 46, 52), (244, 240, 230))
    else:
        f.rect(16, 46, 50, 51, (244, 240, 230))                 # large sourire carnassier
        f.rect(16, 45, 50, 46, LINE)
        f.rect(16, 51, 50, 52, LINE)
        f.poly((18, 46, 21, 46, 19.5, 50), (255, 255, 255))
        f.poly((45, 46, 48, 46, 46.5, 50), (255, 255, 255))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (0.95 if ctx.face == 'south' else 0.5))], fill=HAIR)


def build_body(m):
    b = BODY
    fur = cloth(FUR, shade(FUR, 1.12), FUR_D, folds=5, seed=220)
    pants = cloth(PANTS, shade(PANTS, 1.2), shade(PANTS, 0.7), ao_top=0.1, folds=3, seed=221)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2 + 1.2, y0, -d2 + 0.4], [b.torso_w - 2.4, 5.0, b.torso_d - 0.8], skin(0.96))
    m.cube('torso', [-b.torso_w / 2, y0 + 4.8, -d2], [b.torso_w, y1 - y0 - 4.8, b.torso_d], skin())
    for sx in (-1, 1):
        m.cube('torso', [sx * 2.6 - 2.4, y1 - 5.4, -d2 - 0.5], [4.8, 3.6, 0.6], skin(1.05))      # pectoraux
        for k in range(3):
            m.cube('torso', [sx * 1.2 - 1.0, y0 + 0.8 + k * 1.7, -d2 + 0.1], [2.0, 1.4, 0.4], skin(1.02))
        m.cube('torso', [sx * 3.4 - 2.2, y1 - 0.8, -2.4], [4.4, 1.8, 4.8], skin(0.95),
               pivot=[sx * 3.4, y1, 0], rotation=[0, 0, -sx * 22])                                 # trapèzes
    m.cube('torso', [-2.0, y1 - 0.2, -2.0], [4.0, 1.0, 4.0], skin(0.9))
    # peau de bête en bandoulière (épaule gauche → hanche droite) et pelisse sur l'épaule
    m.cube('torso', [-1.8, y0 - 1.0, -d2 - 0.7], [3.6, y1 - y0 + 4.0, b.torso_d + 1.4], fur,
           pivot=[0, (y0 + y1) / 2, 0], rotation=[0, 0, -38])
    m.cube('torso', [2.0, y1 - 2.0, -d2 - 0.6], [5.6, 3.0, b.torso_d + 1.2], fur, pivot=[4.8, y1, 0],
           rotation=[0, 0, -16])
    m.cube('torso', [-b.torso_w / 2 - 0.2, y0 - 1.2, -d2 - 0.4], [b.torso_w + 0.4, 2.0, b.torso_d + 0.8], fur)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.shoulder_y - 3.6, (5.2, 4.6, 5.2), skin(0.98))
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (4.6, b.shoulder_y - b.elbow_y - 3.2, 4.6), skin())
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (4.4, b.elbow_y - b.wrist_y - 0.2, 4.4), skin(0.97))
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y, (4.8, 2.0, 4.8), cloth(LEATHER, seed=222))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 3.6, (4.2, 3.8, 4.2), skin(0.95))
    m.cube('body', [-b.torso_w / 2 + 1.0, b.hip_y - 1.6, -2.8], [b.torso_w - 2.0, 1.8, 5.6], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.3, (5.0, b.hip_y - b.knee_y + 0.3, 5.2), pants)
        m.box(f'{side}_shin', (cx, 0), 1.4, (4.6, b.knee_y - 1.4, 4.8), pants)
        m.box(f'{side}_shin', (cx, 0), 1.0, (5.0, 1.6, 5.2), fur)                                  # jambières
        m.box(f'{side}_shin', (cx, -0.6), 0.0, (4.2, 1.2, 6.0), skin(0.92))


def build():
    m = Model('uvogin', density=4, tex_width=512)
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
