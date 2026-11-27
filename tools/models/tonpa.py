"""
Tonpa, le « briseur de rookies » — même style. Petit et trapu (≈ 1,5 bloc), grosse tête carrée ; cheveux bruns
très courts en brosse, petit épi sur le front, grandes oreilles ; gros nez rond, petits yeux plissés, sourire
faussement amical ; chemise bleue à manches courtes, pantalon marron, ceinture, sac en bandoulière avec des
canettes de jus.
Expression « froid » : sourire fourbe (yeux en coin, rictus).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#e6bc98')
HAIR = rgb('#4a3020')
HAIR_L = rgb('#7a5638')
LINE = rgb('#2a1a12')
SHIRT = rgb('#4a7ac0')
PANTS = rgb('#7a5a3a')

BODY = Body(head=(8.8, 8.4, 8.2), head_y=15.4, hip_y=7.4, knee_y=3.8, shoulder_y=14.0, arm_x=4.8, leg_x=1.8,
            torso_w=7.2, torso_d=4.2)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#a07a58'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(331)
    c, R = (0.0, top - 3.0, 0.3), 5.0
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=52, front_phi=50,
          rings=((8, 14, -0.12), (30, 12, -0.08), (52, 9, -0.04), (72, 5, 0.0)))
    rr = R + 0.15
    for ring, (phi, n, L) in enumerate(((40, 12, 1.8), (62, 10, 2.0), (82, 6, 2.2))):        # brosse courte
        for i in range(n):
            az = i * 360 / n + ring * 15
            if phi < 50 and abs(((az + 180) % 360) - 180) > 130:
                continue
            a, p = math.radians(az), math.radians(phi)
            base = [c[0] + rr * math.cos(p) * math.sin(a), c[1] + rr * math.sin(p) - 0.3, c[2] + rr * math.cos(p) * math.cos(a)]
            tilt = (90 - phi) * 0.9
            lock(m, f't_b{ring}{i}', base, L, 2.8, tilt * math.cos(a), -tilt * math.sin(a), mk(0.1 + 0.06 * ring),
                 flat=0.7, segs=2, root=1.15, tip=0.1, taper_pow=0.6, ry=rnd.uniform(-25, 25))
    lock(m, 't_epi', [0, top - 0.3, -3.4], 3.0, 2.4, 160, 0, mk(0.3), flat=0.6, segs=2, root=1.1, tip=0.1)
    for sx in (-1, 1):                                                     # grandes oreilles
        m.cube('head', [sx * (b.head[0] / 2 + 0.5) - 0.5, b.head_y + 2.6, -0.8], [1.0, 3.0, 2.0], skin_mat(SKIN))


def face_paint(ctx, mode='calme'):
    sly = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 7, HAIR)
    for x0 in (14, 40):
        f.poly((x0, 27, x0 + 6, 25, x0 + 12, 27, x0 + 12, 29, x0 + 6, 27, x0, 29), LINE)      # yeux plissés
        if sly:
            f.rect(x0 + 8 if x0 < 30 else x0 + 1, 27, x0 + 10 if x0 < 30 else x0 + 3, 29, (10, 8, 6))
    f.poly((27, 32, 39, 32, 41, 40, 25, 40), shade(SKIN, 0.92))           # gros nez rond
    f.rect(27, 40, 39, 41, shade(SKIN, 0.78))
    if sly:
        f.poly((22, 47, 46, 44, 46, 47, 22, 49), rgb('#6a2a28'))
    else:
        f.poly((20, 45, 46, 45, 42, 50, 24, 50), rgb('#6a2a28'))
        f.rect(22, 45, 44, 46, (250, 250, 250))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face == 'up' else 0.4))], fill=HAIR)


def build_body(m):
    b = BODY
    shirt = cloth(SHIRT, shade(SHIRT, 1.15), shade(SHIRT, 0.7), folds=3, seed=740)
    pants = cloth(PANTS, shade(PANTS, 1.2), shade(PANTS, 0.7), ao_top=0.1, folds=2, seed=741)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], shirt)
    m.cube('torso', [-1.6, y1 - 0.2, -1.6], [3.2, 0.6, 3.2], skin_mat(shade(SKIN, 0.92)))
    m.cube('torso', [-b.torso_w / 2 - 0.1, y0 - 0.2, -d2 - 0.1], [b.torso_w + 0.2, 1.0, b.torso_d + 0.2], cloth(rgb('#3a2a1e'), seed=742))
    # sac en bandoulière avec des canettes
    m.cube('torso', [-0.6, y0, -d2 - 0.4], [1.2, y1 - y0 + 1, 0.4], cloth(rgb('#5a4a32'), seed=743), pivot=[0, (y0 + y1) / 2, -d2],
           rotation=[0, 0, -38])
    m.cube('torso', [b.torso_w / 2 - 0.2, y0 - 2.2, -1.8], [1.8, 3.4, 3.6], cloth(rgb('#7a6444'), seed=744))
    for k in range(2):
        m.cube('torso', [b.torso_w / 2 + 0.2, y0 + 0.8, -1.4 + k * 1.6], [1.0, 1.6, 1.0], flat(rgb('#e85a3a') if k else rgb('#5ac03a'), 0.95))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.shoulder_y - 2.6, (3.2, 3.2, 3.3), shirt)
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.8, b.shoulder_y - b.elbow_y - 2.0, 2.9), skin_mat(SKIN))
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y, (2.7, b.elbow_y - b.wrist_y, 2.8), skin_mat(SKIN))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.3, (2.5, 2.3, 2.5), skin_mat(SKIN))
    m.cube('body', [-b.torso_w / 2 + 0.3, b.hip_y - 1.2, -2.0], [b.torso_w - 0.6, 1.4, 4.0], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (3.1, b.hip_y - b.knee_y + 0.2, 3.2), pants)
        m.box(f'{side}_shin', (cx, 0), 0.9, (3.0, b.knee_y - 0.9, 3.1), pants)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.1, 1.0, 4.0), cloth(rgb('#3a2a1e'), seed=745))


def build():
    m = Model('tonpa', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=4)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.2)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
