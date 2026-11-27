"""
Silva Zoldyck — même style. Grand et massif (≈ 2,1 blocs) ; immense crinière blanc argenté : mèches épaisses du
sommet rejetées vers l'arrière, longue masse ondulée qui tombe jusqu'au bas du dos en s'évasant, pointes en
dents de scie, mèches de côté devant les épaules ; yeux bleus perçants (air de famille avec Killua) ; tunique
bleu nuit à col montant et manches amples, ceinture grise, pantalon sombre, bottes.
Expression « froid » : regard du chef des Zoldyck (pupilles fines, ombre).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#ecd0ba')
WHITE = rgb('#eef0f6')
WHITE_SH = rgb('#9aa4bc')
LINE = rgb('#2a2228')
EYE = rgb('#3a86d6')
TUNIC = rgb('#1e2a44')
TUNIC_L = rgb('#30406a')
BELT = rgb('#7a7a86')

BODY = Body(head=(8.2, 8.6, 8.0), head_y=25.2, hip_y=13.6, knee_y=6.8, shoulder_y=23.6, arm_x=6.7, leg_x=2.4,
            torso_w=10.4, torso_d=5.3)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(WHITE_SH, WHITE, t, lit=lit, rim=rgb('#ffffff'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(113)
    c, R = (0.0, top - 3.7, 0.4), 4.9
    scalp(m, c, R, lambda k: mk(k - 0.12)(0.5), skip_front=46, front_phi=34)
    rr = R + 0.25
    # longue masse ondulée : mèches épaisses qui pendent de l'arrière du crâne jusqu'au bas du dos, en s'évasant
    for i, az in enumerate(range(-80, 81, 13)):
        a = math.radians(az)
        r = rr + 0.2 + (0.45 if i % 2 else 0.0)
        x, z = c[0] + r * math.sin(a), c[2] + r * math.cos(a)
        side = abs(az) / 80
        L = 19.0 - 4.0 * side + rnd.uniform(-2.0, 1.6)
        lock(m, f's_c{i}', [x, c[1] + 0.6, z], L, 4.4, 178, az * 0.1, mk(0.04 + 0.05 * (i % 2)),
             ry=rnd.uniform(-8, 8), flat=0.45, curve=(-20 * math.cos(a), az * 0.08), segs=5, root=1.1, tip=0.1,
             taper_pow=0.5, belly=1.18, skew=rnd.uniform(-0.15, 0.15))
    # dessus : mèches épaisses rejetées depuis le front, qui gonflent et retombent sur la masse
    for i, az in enumerate(range(-150, 151, 25)):
        crown_lock(m, f's_t{i}', c, rr + 0.25, az + rnd.uniform(-4, 4), 96 + rnd.uniform(-4, 4),
                   10.5 + rnd.uniform(-0.8, 0.8), 4.8, mk(0.2 + 0.05 * math.cos(math.radians(az))), flat=0.42,
                   turn=88, tip=0.12, segs=4, twist=rnd.uniform(-8, 8), skew=rnd.uniform(-0.12, 0.12),
                   side=rnd.uniform(-20, 20))
    for i, az in enumerate((-24, -8, 8, 24)):                     # front : rejeté vers l'arrière
        crown_lock(m, f's_fr{i}', c, rr + 0.3, az, 138, 9.0, 4.2, mk(0.28), flat=0.4, turn=96, tip=0.15, segs=4,
                   twist=rnd.uniform(-6, 6))
    # mèches de côté qui passent devant les épaules
    for sx in (-1, 1):
        for j, az in enumerate((118, 132)):
            a = math.radians(sx * az)
            r = rr + 0.2
            lock(m, f's_s{sx}{j}', [c[0] + r * math.sin(a), c[1] + 1.0, c[2] + r * math.cos(a)], 13.5 - 1.5 * j, 3.6,
                 180, -sx * 6, mk(0.12), flat=0.45, curve=(-10, sx * 10), segs=4, root=1.1, tip=0.1,
                 taper_pow=0.5, belly=1.15)


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 7, WHITE)
    f.poly((9, 21, 28, 19, 28, 22, 9, 24), WHITE_SH)
    f.poly((57, 21, 38, 19, 38, 22, 57, 24), WHITE_SH)
    for x0 in (12, 40):
        f.rect(x0, 26, x0 + 14, 32, (246, 246, 250))
        f.rect(x0 + 4, 26, x0 + 10, 32, EYE)
        f.rect(x0 + 6 if cold else x0 + 5, 27, x0 + 7 if cold else x0 + 8, 31, (8, 12, 24))
        f.rect(x0 - 1, 24, x0 + 15, 26, LINE)
    if cold:
        f.rect(8, 22, 58, 25, shade(SKIN, 0.84))
    f.rect(32, 34, 33, 42, shade(SKIN, 0.82))
    f.rect(27, 50, 39, 50, rgb('#8a5a50'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=WHITE_SH)


def build_body(m):
    b = BODY
    tunic = cloth(TUNIC, TUNIC_L, shade(TUNIC, 0.65), ao_bottom=0.1, folds=3, seed=290)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.2, -d2], [b.torso_w, y1 - y0 + 0.2, b.torso_d], tunic)
    m.cube('torso', [-2.4, y1 - 0.4, -2.2], [4.8, 2.0, 4.4], tunic)                              # col montant
    m.cube('torso', [-b.torso_w / 2 - 0.15, y0 + 0.4, -d2 - 0.15], [b.torso_w + 0.3, 1.8, b.torso_d + 0.3],
           cloth(BELT, seed=291))
    for name, sz in (('tunic_f', -1), ('tunic_b', 1)):
        m.bone(name, [0, y0, sz * d2], 'body', rotation=[-sz * 4, 0, 0])
        m.cube(name, [-b.torso_w / 2 - 0.1, b.hip_y - 3.6, sz * d2 - 0.35], [b.torso_w + 0.2, y0 - b.hip_y + 3.6, 0.7],
               tunic)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y - 0.4, (4.0, b.shoulder_y - b.elbow_y + 1.0, 4.0), tunic)
        m.box(f'{side}_forearm', (cx, 0.2), b.wrist_y + 0.6, (4.4, b.elbow_y - b.wrist_y - 0.2, 4.6), tunic)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.8, (3.2, 3.4, 3.2), skin_mat(SKIN))
    pants = cloth(rgb('#1a1c24'), rgb('#2a2c36'), rgb('#0e0f14'), folds=3, seed=292)
    m.cube('body', [-b.torso_w / 2 + 0.6, b.hip_y - 1.4, -2.3], [b.torso_w - 1.2, 1.6, 4.6], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (4.0, b.hip_y - b.knee_y + 0.2, 4.2), pants)
        m.box(f'{side}_shin', (cx, 0), 0.0, (4.0, b.knee_y, 4.2), cloth(rgb('#16161c'), seed=293))
        m.box(f'{side}_shin', (cx, -0.5), 0.0, (4.0, 1.2, 5.2), cloth(rgb('#16161c'), seed=294))


def build():
    m = Model('silva', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=5, leg_out=2)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=20, breath=1.4)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
