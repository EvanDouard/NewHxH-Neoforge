"""
Hanzo, ninja de Jappon — même style. Taille moyenne (≈ 1,7 bloc) ; crâne entièrement rasé et luisant (pas de
cheveux : seule la forme du crâne compte), grands sourcils, yeux ronds vifs, sourire fier ; tenue de ninja
bleu-gris moulante, brassards et jambières bandés, ceinture, tabi ; lame cachée au poignet.
Expression « froid » : sérieux du ninja (yeux plissés, bouche serrée).
"""
from __future__ import annotations

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, flat, skin_mat
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#e8c4a2')
LINE = rgb('#22181a')
NINJA = rgb('#4a5a6e')
NINJA_L = rgb('#64768c')
WRAP = rgb('#d8d2c4')

BODY = Body(head=(7.8, 8.2, 7.6), head_y=19.8, hip_y=10.8, knee_y=5.5, shoulder_y=18.4, arm_x=4.4, leg_x=1.7,
            torso_w=6.2, torso_d=3.4)
FACE_D = 7


def face_paint(ctx, mode='calme'):
    ser = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(16, 2, 50, 6, shade(SKIN, 1.12))                         # reflet du crâne
    f.poly((9, 20, 28, 18, 28, 22, 9, 23), LINE)
    f.poly((57, 20, 38, 18, 38, 22, 57, 23), LINE)
    for x0 in (12, 40):
        top = 28 if ser else 25
        f.rect(x0, top, x0 + 14, 34, (246, 244, 240))
        f.rect(x0 + 5, top + 1, x0 + 10, 33, (14, 10, 8))
        f.rect(x0 + 6, top + 1, x0 + 7, top + 2, (255, 255, 255))
    f.rect(31, 35, 34, 42, shade(SKIN, 0.84))
    if ser:
        f.rect(27, 49, 39, 49, rgb('#7a4a40'))
    else:
        f.poly((24, 46, 42, 46, 39, 50, 27, 50), rgb('#6a2a28'))
        f.rect(25, 46, 41, 47, (250, 250, 250))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.1, noise=2, edge=0.9)(ctx)
    if ctx.face == 'up':
        f = FaceGrid(ctx)
        f.rect(18, 18, 40, 30, shade(SKIN, 1.14))


def build():
    m = Model('hanzo', density=4, tex_width=512)
    b = BODY
    skeleton(m, b, arm_out=3)
    head_box(m, b, head_paint, FACE_D, skin=SKIN)
    hx, hz, top = b.head[0] / 2, b.head[2] / 2, b.top
    # crâne arrondi (bords du cube adoucis)
    m.cube('head', [-hx + 0.6, top - 0.2, -hz + 0.6], [b.head[0] - 1.2, 0.7, b.head[2] - 1.2], skin_mat(shade(SKIN, 1.05)))
    m.cube('head', [-hx - 0.3, b.head_y + 2.0, -hz + 0.8], [b.head[0] + 0.6, b.head[1] - 3.2, b.head[2] - 1.6], skin_mat(shade(SKIN, 0.98)))
    m.cube('head', [-hx + 0.8, b.head_y + 2.0, hz - 0.3], [b.head[0] - 1.6, b.head[1] - 3.2, 0.6], skin_mat(shade(SKIN, 0.96)))
    ninja = cloth(NINJA, NINJA_L, shade(NINJA, 0.7), folds=2, seed=730)
    wrap = cloth(WRAP, shade(WRAP, 1.05), shade(WRAP, 0.75), folds=6, seed=731)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], ninja)
    m.cube('torso', [-1.6, y1 - 0.6, -1.6], [3.2, 1.2, 3.2], ninja)
    m.cube('torso', [-b.torso_w / 2 - 0.15, y0 - 0.2, -d2 - 0.15], [b.torso_w + 0.3, 1.4, b.torso_d + 0.3], cloth(rgb('#2a3240'), seed=732))
    m.cube('torso', [-b.torso_w / 2 - 0.1, y1 - 4.0, -d2 - 0.1], [b.torso_w + 0.2, 0.6, b.torso_d + 0.2], cloth(rgb('#2a3240'), seed=733),
           pivot=[0, y1 - 3.7, 0], rotation=[0, 0, 28])
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.7, b.shoulder_y - b.elbow_y + 0.6, 2.8), ninja)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y, (2.8, b.elbow_y - b.wrist_y, 2.9), wrap)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.2, (2.3, 2.2, 2.3), skin_mat(SKIN))
    m.cube('right_forearm', [-b.arm_x - 0.15, b.wrist_y - 3.6, -1.8], [0.3, 4.2, 0.9], flat(rgb('#d8dce4'), 1.0))   # lame
    m.cube('body', [-b.torso_w / 2 + 0.3, b.hip_y - 1.2, -1.6], [b.torso_w - 0.6, 1.4, 3.2], ninja)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.8, b.hip_y - b.knee_y + 0.2, 2.9), ninja)
        m.box(f'{side}_shin', (cx, 0), 0.8, (2.7, b.knee_y - 0.8, 2.8), wrap)
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.7, 0.9, 3.5), cloth(rgb('#2a3240'), seed=734))
    m.animations = humanoid_animations(arm_swing=16, leg_swing=24, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
