"""
Zeno Zoldyck — même style. Vieillard menu (≈ 1,6 bloc), front dégarni ; cheveux blancs épais en pointes rejetées
vers le haut et l'arrière (air de famille avec Killua), longue tresse qui descend dans le dos jusqu'à la taille ;
longue moustache fine et tombante, sourcils broussailleux relevés ; yeux bleus perçants ; tunique bleu-gris
portant « 一日一殺 » en colonne sur le devant, ceinture, pantalon large sombre, chaussons.
Expression « froid » : regard d'assassin (yeux grands ouverts, pupilles fines).
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#efcfb4')
WHITE = rgb('#f3f3f6')
WHITE_SH = rgb('#a6aec4')
LINE = rgb('#3a2c24')
EYE = rgb('#4a8ad8')
ROBE = rgb('#8e9cb4')
ROBE_L = rgb('#aebad0')
INK = rgb('#1a1a20')
PANTS = rgb('#33333c')

BODY = Body(head=(8.0, 8.4, 7.8), head_y=16.8, hip_y=8.6, knee_y=4.4, shoulder_y=15.4, arm_x=4.6, leg_x=1.8,
            torso_w=6.8, torso_d=3.8)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(WHITE_SH, WHITE, t, lit=lit, rim=rgb('#ffffff'))


def build_hair(m):
    b = BODY
    top, hz = b.top, b.head[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(61)
    c, R = (0.0, top - 3.4, 0.5), 4.5
    scalp(m, c, R, lambda k: mk(k - 0.15)(0.5), skip_front=52, front_phi=40)
    rr = R + 0.25
    # pointes épaisses rejetées vers le haut et l'arrière (partent haut sur le crâne : front dégarni)
    for i, az in enumerate((-50, -30, -10, 10, 30, 50)):
        crown_lock(m, f'z_t{i}', c, rr, az + rnd.uniform(-4, 4), 112 + rnd.uniform(-4, 4),
                   7.2 + 1.2 * (1 - abs(i - 2.5) / 2.5) + rnd.uniform(-0.4, 0.4), 4.0 + rnd.uniform(0, 0.4),
                   mk(0.22 + 0.03 * (2.5 - abs(i - 2.5))), flat=0.55, turn=34, tip=0.08, segs=3,
                   twist=rnd.uniform(-10, 10), skew=rnd.uniform(-0.12, 0.12))
    # arrière et côtés : pointes en couronne, plus courtes, qui s'écartent
    for i, az in enumerate(range(-140, 141, 28)):
        if abs(az) < 20:
            continue
        crown_lock(m, f'z_b{i}', c, rr, az + rnd.uniform(-5, 5), 66 + rnd.uniform(-5, 5), 4.4 + rnd.uniform(-0.6, 0.8),
                   3.6, mk(0.08), flat=0.55, turn=62, tip=0.1, segs=3, lean=(1 if az > 0 else -1) * 6,
                   twist=rnd.uniform(-12, 12))
    # longue tresse : part de la nuque et descend dans le dos (maillons alternés)
    m.bone('braid', [0, b.head_y + 1.4, hz - 0.4], 'head', rotation=[7, 0, 0])
    y = b.head_y + 1.4
    m.cube('braid', [-1.4, y - 0.4, hz - 0.75], [2.8, 0.7, 1.9], flat(rgb('#5a4a3a'), 0.9))       # attache
    for k in range(14):                                     # tresse : maillons plus petits et serrés, croisés
        w = 2.3 - 0.06 * k
        off = 0.28 if k % 2 else -0.28
        zk = hz - 0.6 + 0.05 * k
        m.cube('braid', [-w / 2 + off, y - 1.1, zk], [w, 1.3, 1.5], mk(0.18 if k % 2 else 0.06)(0.5),
               pivot=[off, y - 0.45, zk + 0.75], rotation=[0, 0, 22 if k % 2 else -22])
        m.cube('braid', [-w / 2 + 0.25, y - 1.05, zk + 0.05], [w - 0.5, 1.2, 1.4], mk(0.0)(0.5))   # cœur sombre
        y -= 0.72
    m.cube('braid', [-0.7, y - 0.6, hz - 0.4], [1.4, 0.7, 1.2], flat(rgb('#5a6a8a'), 0.9))       # attache
    lock(m, 'z_bt', [0, y - 0.6, hz + 0.2], 2.2, 1.6, 180, 0, mk(0.1), flat=0.6, segs=2, root=1.0, tip=0.1,
         parent='braid')


def build_face_hair(m):
    b = BODY
    hz = b.head[2] / 2
    yf = lambda gy: b.head_y + b.head[1] * (1 - gy / 60)
    for sx in (-1, 1):
        # longue moustache fine et tombante
        lock(m, f'z_mu{sx}', [sx * 1.0, yf(43), -hz - 0.15], 6.0, 0.8, 180, sx * 18, mk(0.3), flat=0.7,
             curve=(-4, -sx * 14), segs=3, root=1.3, tip=0.25, taper_pow=0.5, parent='head')
        # sourcils broussailleux relevés vers l'extérieur
        lock(m, f'z_br{sx}', [sx * 1.6, yf(21), -hz - 0.15], 2.8, 1.3, 0, -sx * 98, mk(0.32), flat=0.45,
             curve=(0, sx * 12), segs=2, root=1.1, tip=0.2, taper_pow=0.6, parent='head')


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    for y in (7, 11, 15):                                       # rides du front
        f.rect(16, y, 50, y, shade(SKIN, 0.8))
    for x0, d in ((8, -1), (58, 1)):                            # pattes d'oie
        for k in range(3):
            f.rect(min(x0, x0 + d * 3), 26 + 3 * k, max(x0, x0 + d * 3), 26 + 3 * k, shade(SKIN, 0.78))
    f.poly((20, 40, 24, 46, 23, 46, 19, 41), shade(SKIN, 0.82))   # sillons nasogéniens
    f.poly((46, 40, 42, 46, 43, 46, 47, 41), shade(SKIN, 0.82))
    for x0 in (13, 40):
        f.rect(x0, 25, x0 + 13, 30, (246, 246, 250))
        f.rect(x0 + 4, 25, x0 + 9, 30, EYE)
        f.rect(x0 + 6 if cold else x0 + 5, 26, x0 + 7, 29, (10, 12, 20))
        f.rect(x0 - 1, 23, x0 + 14, 25, LINE)
        f.rect(x0 + 1, 32, x0 + 12, 32, shade(SKIN, 0.8))
    if cold:
        f.rect(8, 20, 58, 23, shade(SKIN, 0.84))
    f.rect(31, 32, 34, 41, shade(SKIN, 0.84))
    f.rect(26, 50, 40, 50, rgb('#8a5a50'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face == 'up' else 0.55))], fill=WHITE_SH)


def robe_paint(ctx):
    cloth(ROBE, ROBE_L, shade(ROBE, 0.7), ao_bottom=0.1, folds=3, seed=240)(ctx)
    if ctx.face != 'north':
        return
    f = FaceGrid(ctx)                         # « 一日一殺 » en colonne, stylisé en blocs
    f.rect(26, 4, 40, 7, INK)                                   # 一
    f.rect(27, 11, 39, 24, INK)                                 # 日
    f.rect(30, 14, 36, 16, ROBE)
    f.rect(30, 19, 36, 21, ROBE)
    f.rect(26, 28, 40, 31, INK)                                 # 一
    f.rect(24, 35, 32, 37, INK)                                 # 殺 (deux moitiés)
    f.rect(26, 38, 30, 52, INK)
    f.rect(22, 44, 33, 46, INK)
    f.rect(35, 35, 42, 38, INK)
    f.poly((34, 40, 42, 40, 40, 52, 36, 52), INK)


def build_body(m):
    b = BODY
    plain = cloth(ROBE, ROBE_L, shade(ROBE, 0.7), folds=2, seed=241)
    pants = cloth(PANTS, shade(PANTS, 1.25), shade(PANTS, 0.7), ao_top=0.1, folds=3, seed=242)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], robe_paint)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.6, 2.4], skin_mat(shade(SKIN, 0.92)))
    m.cube('torso', [-b.torso_w / 2 - 0.1, y0 - 0.3, -d2 - 0.15], [b.torso_w + 0.2, 1.0, b.torso_d + 0.3],
           cloth(rgb('#3c4a66'), seed=243))
    for name, sz in (('robe_f', -1), ('robe_b', 1)):
        m.bone(name, [0, y0, sz * d2], 'body', rotation=[-sz * 4, 0, 0])
        m.cube(name, [-b.torso_w / 2 - 0.05, b.hip_y - 2.2, sz * d2 - 0.3], [b.torso_w + 0.1, y0 - b.hip_y + 2.2, 0.6],
               plain)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (3.0, b.shoulder_y - b.elbow_y + 0.6, 3.1), plain)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.4, (3.2, b.elbow_y - b.wrist_y - 0.4, 3.3), plain)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.0, (2.5, 2.6, 2.5), skin_mat(SKIN))
    m.cube('body', [-b.torso_w / 2 + 0.3, b.hip_y - 1.2, -1.9], [b.torso_w - 0.6, 1.4, 3.8], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.3, (3.3, b.hip_y - b.knee_y + 0.3, 3.5), pants)
        m.box(f'{side}_shin', (cx, 0), 1.0, (3.4, b.knee_y - 1.0, 3.5), pants)
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.8, 1.0, 3.6), cloth(rgb('#1e1e24'), seed=244))


def build():
    m = Model('zeno', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3, feet_out=8)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_hair(m)
    build_face_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=20, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
