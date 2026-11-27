"""
Botobai Gigante, le Dragon des Zodiaques — même style. Vieillard massif (≈ 2,1 blocs) ; peau du visage écailleuse
(motif d'écailles), crâne chauve couronné de deux crêtes en cornes de dragon vers l'arrière, longue barbe blanche
et moustache tombante ; yeux sévères ; long manteau d'officier vert sombre à épaulettes, pantalon, bottes.
Expression « froid » : procureur (yeux plissés, sourcils froncés).
"""
from __future__ import annotations

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, flat, lock, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#c8a888')
SCALE = rgb('#a88a6a')
WHITE = rgb('#eeeeea')
WHITE_SH = rgb('#a8aab0')
LINE = rgb('#2a1e18')
COAT = rgb('#2e4434')
COAT_L = rgb('#425e48')

BODY = Body(head=(8.4, 8.8, 8.0), head_y=25.2, hip_y=13.6, knee_y=6.8, shoulder_y=23.6, arm_x=6.4, leg_x=2.4,
            torso_w=10.0, torso_d=5.2)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(WHITE_SH, WHITE, t, lit=lit, rim=rgb('#ffffff'))


def scales(ctx, base):
    material(base, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    for y in range(0, 60, 6):
        for x in range(-3 if (y // 6) % 2 else 0, 66, 6):
            f.poly((x, y + 4, x + 3, y + 1, x + 6, y + 4), shade(base, 0.85))


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    scales(ctx, SKIN)
    f = FaceGrid(ctx)
    f.poly((8, 18 if cold else 20, 28, 22, 28, 25, 8, 23), WHITE)
    f.poly((58, 18 if cold else 20, 38, 22, 38, 25, 58, 23), WHITE)
    for x0 in (12, 40):
        top = 28 if cold else 27
        f.rect(x0, top, x0 + 14, 32, (240, 236, 226))
        f.rect(x0 + 5, top, x0 + 10, 32, rgb('#3a2a1a'))
        f.rect(x0 + 6, top + 1, x0 + 8, 31, (8, 6, 4))
    f.rect(30, 33, 35, 41, shade(SKIN, 0.82))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    scales(ctx, shade(SKIN, 0.96))


def build_body(m):
    b = BODY
    coat = cloth(COAT, COAT_L, shade(COAT, 0.7), ao_bottom=0.1, folds=3, seed=770)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.2, -d2], [b.torso_w, y1 - y0 + 0.2, b.torso_d], coat)
    m.cube('torso', [-2.4, y1 - 0.4, -2.2], [4.8, 1.8, 4.4], coat)
    for sx in (-1, 1):                                                         # épaulettes dorées
        m.cube('torso', [sx * (b.torso_w / 2 - 1.2) - 1.8, y1 - 0.4, -2.0], [3.6, 0.8, 4.0], flat(rgb('#c8a848'), 0.9))
    m.cube('torso', [-b.torso_w / 2 - 0.15, y0 + 0.4, -d2 - 0.15], [b.torso_w + 0.3, 1.4, b.torso_d + 0.3], cloth(rgb('#1a1a1e'), seed=771))
    for name, sz in (('coat_f', -1), ('coat_b', 1)):
        m.bone(name, [0, y0, sz * d2], 'body', rotation=[-sz * 5, 0, 0])
        m.cube(name, [-b.torso_w / 2 - 0.1, b.knee_y - 1.0, sz * d2 - 0.4], [b.torso_w + 0.2, y0 - b.knee_y + 1.0, 0.8], coat)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (4.2, b.shoulder_y - b.elbow_y + 0.6, 4.3), coat)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (4.0, b.elbow_y - b.wrist_y - 0.2, 4.1), coat)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 3.0, (3.4, 3.2, 3.4), skin_mat(SKIN))
    pants = cloth(rgb('#22262a'), rgb('#343a40'), rgb('#141618'), folds=2, seed=772)
    m.cube('body', [-b.torso_w / 2 + 0.8, b.hip_y - 1.4, -2.4], [b.torso_w - 1.6, 1.6, 4.8], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (4.2, b.hip_y - b.knee_y + 0.2, 4.4), pants)
        m.box(f'{side}_shin', (cx, 0), 0.0, (4.2, b.knee_y, 4.4), cloth(rgb('#141414'), seed=773))
        m.box(f'{side}_shin', (cx, -0.5), 0.0, (4.2, 1.2, 5.4), cloth(rgb('#141414'), seed=774))


def build():
    m = Model('botobai', density=4, tex_width=512)
    b = BODY
    skeleton(m, b, arm_out=5, leg_out=2)
    head_box(m, b, head_paint, FACE_D, skin=SKIN)
    hz, top = b.head[2] / 2, b.top
    yf = lambda gy: b.head_y + b.head[1] * (1 - gy / 60)
    for sx in (-1, 1):
        # crêtes en cornes de dragon rejetées en arrière
        lock(m, f'b_horn{sx}', [sx * 2.6, top - 0.6, -1.0], 6.6, 2.6, 20, -sx * 12, lambda t: material(SCALE, grad=0.2, edge=0.8),
             flat=0.7, curve=(-70, 0), segs=4, root=1.1, tip=0.15, parent='head')
        # moustache tombante
        lock(m, f'b_mu{sx}', [sx * 1.2, yf(43), -hz - 0.2], 7.0, 1.2, 180, sx * 16, mk(0.3), flat=0.7,
             curve=(-4, -sx * 12), segs=3, root=1.3, tip=0.2, taper_pow=0.5, parent='head')
    # longue barbe blanche
    for k, (x, L, W) in enumerate(((0, 11.0, 4.4), (-2.2, 9.0, 3.2), (2.2, 9.0, 3.2))):
        lock(m, f'b_beard{k}', [x, yf(50), -hz - 0.3], L, W, 176, -x * 2, mk(0.22 - 0.04 * k), flat=0.5,
             curve=(-8, 0), segs=4, root=1.1, tip=0.2, taper_pow=0.5, parent='head')
    build_body(m)
    m.animations = humanoid_animations(arm_swing=12, leg_swing=18, breath=1.4)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
