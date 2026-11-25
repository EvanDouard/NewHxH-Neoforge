"""
Zushi — même style. Enfant (≈ 1,3 bloc), tête plus grosse ; cheveux noirs très courts en brosse (pointes courtes
et drues sur tout le crâne, un peu plus longues au sommet), nuque dégagée ; gros sourcils, grands yeux ronds
sérieux ; kimono d'entraînement (dogi) blanc croisé, ceinture noire, pantalon blanc, pieds nus.
Expression « froid » : concentration (yeux plissés, sourcils froncés).
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f0cfb0')
HAIR = rgb('#141416')
HAIR_L = rgb('#383c48')
LINE = rgb('#22181a')
GI = rgb('#f2f0ea')
GI_SH = rgb('#c4c0b4')

BODY = Body(head=(8.2, 8.2, 7.8), head_y=12.6, hip_y=6.3, knee_y=3.1, shoulder_y=11.5, arm_x=3.7, leg_x=1.3,
            torso_w=5.0, torso_d=3.0)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#5a6070'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(167)
    c, R = (0.0, top - 3.6, 0.3), 4.6
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=50, front_phi=50,
          rings=((8, 14, -0.12), (30, 12, -0.08), (52, 9, -0.04), (72, 5, 0.0)))
    rr = R + 0.15
    import math
    for ring, (phi, n, L) in enumerate(((36, 12, 2.0), (58, 10, 2.4), (78, 7, 2.8), (90, 1, 3.0))):   # brosse drue
        for i in range(n):
            az = i * 360 / n + ring * 15 + rnd.uniform(-6, 6)
            if phi < 50 and abs(((az + 180) % 360) - 180) > 130:
                continue
            a, p = math.radians(az), math.radians(phi)
            base = [c[0] + rr * math.cos(p) * math.sin(a), c[1] + rr * math.sin(p) - 0.3,
                    c[2] + rr * math.cos(p) * math.cos(a)]
            tilt = (90 - phi) * 0.9 + rnd.uniform(-6, 6)
            lock(m, f'z_b{ring}{i}', base, L + rnd.uniform(-0.3, 0.4), 2.8, tilt * math.cos(a), -tilt * math.sin(a),
                 mk(0.1 + 0.06 * ring), flat=0.7, curve=(8 * math.cos(a), -8 * math.sin(a)), segs=2, root=1.15,
                 tip=0.1, taper_pow=0.6, ry=rnd.uniform(-25, 25))


def face_paint(ctx, mode='calme'):
    focus = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 9, HAIR)
    f.poly((9, 18 if focus else 19, 28, 21, 28, 25, 9, 23), LINE)
    f.poly((57, 18 if focus else 19, 38, 21, 38, 25, 57, 23), LINE)
    for x0 in (11, 39):
        top = 30 if focus else 27
        f.rect(x0, top, x0 + 16, 38, (248, 246, 242))
        f.rect(x0 + 4, top, x0 + 12, 38, rgb('#2a1e16'))
        f.rect(x0 + 6, top + 1, x0 + 10, 37, (8, 6, 4))
        f.rect(x0 + 5, top + 1, x0 + 7, top + 3, (255, 255, 255))
    f.rect(30, 48, 36, 48, rgb('#8a4a44'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face == 'up' else 0.45))], fill=HAIR)


def gi_paint(ctx):
    cloth(GI, shade(GI, 1.02), GI_SH, ao_bottom=0.06, folds=3, seed=380)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.poly((18, 0, 26, 0, 40, 40, 34, 46), GI_SH)
        f.poly((26, 0, 40, 0, 33, 20), SKIN)


def build_body(m):
    b = BODY
    gi = cloth(GI, shade(GI, 1.02), GI_SH, folds=3, seed=381)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.6, -d2], [b.torso_w, y1 - y0 + 0.6, b.torso_d], gi_paint)
    m.cube('torso', [-b.torso_w / 2 - 0.15, y0 - 0.2, -d2 - 0.15], [b.torso_w + 0.3, 1.0, b.torso_d + 0.3],
           cloth(rgb('#16161a'), seed=382))
    for sx in (-1, 1):                                             # pans de la ceinture noire
        m.cube('torso', [sx * 0.9 - 0.4, y0 - 2.0, -d2 - 0.3], [0.8, 2.0, 0.3], flat(rgb('#16161a'), 0.9),
               pivot=[sx * 0.9, y0, -d2], rotation=[0, 0, -sx * 12])
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y - 0.2, (2.6, b.shoulder_y - b.elbow_y + 0.8, 2.6), gi)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.8, (2.6, b.elbow_y - b.wrist_y - 0.6, 2.7), gi)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y, (2.0, 1.0, 2.0), skin_mat(SKIN))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 1.9, (2.1, 2.0, 2.1), skin_mat(SKIN))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.4, b.hip_y - b.knee_y + 0.2, 2.5), gi)
        m.box(f'{side}_shin', (cx, 0), 0.6, (2.4, b.knee_y - 0.6, 2.5), gi)
        m.box(f'{side}_shin', (cx, -0.2), 0.0, (2.0, 0.7, 2.8), skin_mat(shade(SKIN, 0.95)))


def build():
    m = Model('zushi', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=4, leg_out=2, feet_out=6)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=16, leg_swing=22, breath=0.8)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
