"""
Palm Siberia — même style. Mince (≈ 1,7 bloc) ; immense chevelure noire sauvage qui tombe jusqu'aux cuisses :
mèches épaisses et désordonnées qui s'écartent du corps, longues mèches devant les épaules, frange irrégulière qui
mange le haut du visage ; énormes yeux ronds noirs aux pupilles minuscules (inquiétante), cernes ; longue robe
noire, tablier sombre, bottines.
Expression « froid » : sourire dément (bouche fendue, yeux exorbités).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade
from hairkit import curtain

SKIN = rgb('#f2ddd2')
HAIR = rgb('#141418')
HAIR_L = rgb('#2c2e38')
LINE = rgb('#141216')
DRESS = rgb('#1e1c22')
DRESS_L = rgb('#323038')

BODY = Body(head=(7.6, 8.2, 7.4), head_y=19.8, hip_y=10.8, knee_y=5.5, shoulder_y=18.4, arm_x=4.1, leg_x=1.55,
            torso_w=5.6, torso_d=3.1)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#565c74'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(181)
    c, R = (0.0, top - 3.6, 0.3), 4.7
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=46, front_phi=34)
    rr = R + 0.25
    for i, az in enumerate(range(0, 360, 22)):                    # dessus en désordre
        if 140 < az < 220:
            continue
        crown_lock(m, f'p_m{i}', c, rr, az + rnd.uniform(-6, 6), 90 + rnd.uniform(-6, 6), 9.0 + rnd.uniform(-0.8, 1.0),
                   4.4, mk(0.12 + 0.05 * (i % 2)), flat=0.45, turn=94 + rnd.uniform(-12, 10), tip=0.1, segs=4,
                   lean=rnd.uniform(-12, 12), twist=rnd.uniform(-14, 14), skew=rnd.uniform(-0.15, 0.15))
    # masse jusqu'aux cuisses, mèches qui s'écartent du corps
    curtain(m, 'p_c', c, rr - 0.1, range(-90, 91, 12), 1.2, lambda az: 20.0 - 4.0 * abs(az) / 90, 4.4,
            lambda i: mk(0.04 + 0.05 * (i % 2)), inward=8, flare=0.5, wave=18, rnd=rnd, vary=0.22, layer=0.55, tip=0.08, belly=1.2)
    for sx in (-1, 1):                                            # mèches devant les épaules
        curtain(m, f'p_s{sx}', c, rr, [sx * 108, sx * 120], 1.0, 14.0, 3.4, lambda i: mk(0.1),
                inward=-6, flare=0.3, wave=14, rnd=rnd, tip=0.08, belly=1.15)
    for j, (az, L, lean) in enumerate(((148, 4.6, -16), (162, 5.4, -6), (176, 4.8, 4), (190, 5.6, 8), (206, 4.4, 14))):
        crown_lock(m, f'p_f{j}', c, rr, az, 70, L, 3.6, mk(0.2), flat=0.45, turn=78, tip=0.08, segs=3, lean=lean,
                   twist=rnd.uniform(-10, 10), skew=rnd.uniform(-0.16, 0.16))


def face_paint(ctx, mode='calme'):
    mad = mode == 'froid'
    material(SKIN, grad=0.04, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 12, HAIR)
    for x0 in (9, 37):
        r = 22 if mad else 23
        f.rect(x0, r, x0 + 20, 40, (250, 250, 250))                # énormes yeux ronds
        f.rect(x0 + 8, 29, x0 + 12, 33, (10, 10, 12))
        f.rect(x0 - 1, r - 2, x0 + 21, r, LINE)
        f.rect(x0 + 2, 41, x0 + 18, 43, shade(SKIN, 0.78))         # cernes
    if mad:
        f.poly((16, 48, 50, 48, 46, 52, 20, 52), rgb('#5a1a20'))
        f.rect(18, 48, 48, 49, (240, 240, 240))
    else:
        f.rect(29, 50, 37, 50, rgb('#8a5a5a'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR)


def build_body(m):
    b = BODY
    dress = cloth(DRESS, DRESS_L, shade(DRESS, 0.7), ao_bottom=0.1, folds=4, seed=410)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], dress)
    m.cube('torso', [-1.1, y1 - 0.2, -1.1], [2.2, 0.6, 2.2], skin_mat(shade(SKIN, 0.92)))
    m.bone('skirt', [0, b.hip_y + 0.4, 0], 'body')
    m.cube('skirt', [-b.torso_w / 2 - 0.6, 1.0, -d2 - 0.6], [b.torso_w + 1.2, b.hip_y - 0.6, b.torso_d + 1.2], dress)
    m.cube('skirt', [-2.0, 2.0, -d2 - 0.75], [4.0, b.hip_y - 1.6, 0.2], cloth(rgb('#3a3640'), seed=411))   # tablier
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.4, b.shoulder_y - b.elbow_y + 0.6, 2.5), dress)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y, (2.3, b.elbow_y - b.wrist_y, 2.4), dress)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.1, (2.0, 2.1, 2.0), skin_mat(SKIN))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.2, b.hip_y - b.knee_y + 0.2, 2.3), skin_mat(SKIN))
        m.box(f'{side}_shin', (cx, 0), 0.0, (2.3, b.knee_y, 2.4), cloth(rgb('#141216'), seed=412))
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.4, 1.0, 3.2), cloth(rgb('#141216'), seed=413))


def build():
    m = Model('palm', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=10, leg_swing=18, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
