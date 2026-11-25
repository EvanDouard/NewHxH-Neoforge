"""
Neferpitou — même style. Silhouette fine (≈ 1,8 bloc) ; cheveux blancs légèrement lavande, courts, ondulés et
ébouriffés, frange qui tombe sur le front, mèches qui s'enroulent vers l'intérieur ; oreilles de chat blanches à
intérieur rose ; grands yeux ronds écarlates aux pupilles de chat, sourire en « ω » ; chemisier blanc ample aux
manches bouffantes, short sombre à bretelles, ceinture, chaussettes, chaussures ; longue queue de chat blanche.
Expression « froid » : yeux de prédateur (pupilles fendues, sourire carnassier).
"""
from __future__ import annotations

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, arched, cloth, flat, hair_paint, lock, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f7e6dc')
HAIR = rgb('#e9e6f0')
HAIR_SH = rgb('#a8a3bb')
HAIR_RIM = rgb('#ffffff')
EAR_IN = rgb('#f2a9bf')
EYE = rgb('#d4243a')
LINE = rgb('#3a2630')
BLOUSE = rgb('#f2f1f4')
BLOUSE_SH = rgb('#c6c2d0')
SHORTS = rgb('#3a3352')
STRAP = rgb('#2a2440')
SOCK = rgb('#f0eef2')
SHOE = rgb('#4a2a3a')

BODY = Body(head=(8.4, 8.4, 8.0), head_y=19.8, hip_y=10.6, knee_y=5.6, shoulder_y=18.4, arm_x=4.5, leg_x=1.7,
            torso_w=6.4, torso_d=3.6)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR_SH, HAIR, t, lit=lit, rim=HAIR_RIM)


def build_hair(m):
    b = BODY
    top, hx, hz = b.top, b.head[0] / 2, b.head[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    import random
    from chibi import crown_lock, scalp
    rnd = random.Random(7)
    c, R = (0.0, top - 3.3, 0.4), 4.95
    scalp(m, c, R, lambda k: mk(k - 0.1)(0.5))
    rr = R + 0.3
    # mèches courtes et ondulées qui partent du sommet et retombent en s'écartant un peu (volume ébouriffé),
    # pointes arrondies qui rebiquent
    for i, az in enumerate(range(0, 360, 30)):
        if 130 < az < 230:
            continue
        L = 9.0 + rnd.uniform(-1.0, 1.4)
        crown_lock(m, f'p_m{i}', c, rr, az + rnd.uniform(-6, 6), 74 + rnd.uniform(-6, 6), L, 4.4 + rnd.uniform(-0.4, 0.6),
                   mk(0.16 if 60 < az < 300 else 0.04), flat=0.5, turn=118 + rnd.uniform(-8, 8), tip=0.32,
                   segs=4, lean=rnd.uniform(-10, 10), twist=rnd.uniform(-14, 14), skew=rnd.uniform(-0.12, 0.12))
    for i, az in enumerate(range(15, 360, 30)):          # couche secondaire plus courte (épaisseur, boucles)
        if 130 < az < 230:
            continue
        crown_lock(m, f'p_s{i}', c, rr - 0.3, az, 68, 6.6 + rnd.uniform(-0.6, 1.0), 3.4, mk(0.0), flat=0.55,
                   turn=130, tip=0.4, segs=3, lean=rnd.uniform(-12, 12), twist=rnd.uniform(-20, 20))
    # frange : mèches ondulées qui tombent sur le front, la plus longue au centre
    for j, (az, L, W, lean, k) in enumerate(((146, 6.0, 3.8, -10, 0.3), (164, 6.6, 4.0, -4, 0.36),
                                             (182, 7.2, 4.0, 3, 0.4), (200, 6.4, 3.8, 9, 0.34),
                                             (216, 5.6, 3.4, 14, 0.28))):
        crown_lock(m, f'p_f{j}', c, rr, az, 72, L, W, mk(k), flat=0.5, turn=92, tip=0.3, segs=4, lean=lean,
                   skew=rnd.uniform(-0.1, 0.1))
    # oreilles de chat : triangles épais, intérieur rose
    for sx in (-1, 1):
        m.bone(f'ear{sx}', [sx * 2.6, top + 0.8, 0.2], 'hair', rotation=[-6, 0, -sx * 18])
        for i in range(9):
            w = 3.4 * (1 - i / 9)
            m.cube(f'ear{sx}', [sx * 2.6 - w / 2, top + 0.8 + i * 0.47, -0.6], [w, 0.55, 1.4],
                   hair_paint(HAIR_SH, HAIR, HAIR_RIM, lit=0.3, seed=20 + i))
            if w > 0.9:
                m.cube(f'ear{sx}', [sx * 2.6 - w / 2 + 0.35, top + 0.8 + i * 0.47, -0.66], [w - 0.7, 0.55, 0.1],
                       flat(EAR_IN, 0.95))


def face_paint(ctx, mode='joueur'):
    hunt = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 9, HAIR_SH)
    f.poly((0, 9, 7, 9, 0, 34), HAIR_SH)
    f.poly((66, 9, 59, 9, 66, 34), HAIR_SH)
    for x0 in (10, 40):                                         # grands yeux ronds
        f.rect(x0 + 2, 22, x0 + 14, 38, (250, 248, 250))
        f.rect(x0, 24, x0 + 16, 36, (250, 248, 250))
        f.rect(x0 + 3, 23, x0 + 13, 37, EYE)
        f.rect(x0 + 3, 23, x0 + 13, 26, shade(EYE, 0.6))
        pw = 1 if hunt else 3
        f.rect(x0 + 8 - pw, 25, x0 + 8 + pw, 36, rgb('#2a0810'))          # pupille de chat
        f.rect(x0 + 4, 25, x0 + 6, 28, (255, 255, 255))
        f.rect(x0 + 1, 21, x0 + 15, 22, LINE)
    f.rect(32, 42, 33, 43, shade(SKIN, 0.82))
    if hunt:
        f.poly((24, 48, 42, 48, 38, 53, 28, 53), rgb('#5a1424'))
        f.rect(26, 48, 27, 50, (250, 250, 250))
        f.rect(39, 48, 40, 50, (250, 250, 250))
    else:                                                       # bouche en ω
        f.rect(27, 48, 29, 49, LINE)
        f.rect(29, 49, 32, 50, LINE)
        f.rect(32, 48, 34, 49, LINE)
        f.rect(34, 49, 37, 50, LINE)
        f.rect(37, 48, 39, 49, LINE)


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * 0.7)], fill=HAIR_SH)


def build_body(m):
    b = BODY
    blouse = cloth(BLOUSE, shade(BLOUSE, 1.02), BLOUSE_SH, ao_bottom=0.12, folds=3, seed=170)
    shorts = cloth(SHORTS, shade(SHORTS, 1.2), shade(SHORTS, 0.7), ao_top=0.15, folds=2, seed=171)
    strap = cloth(STRAP, seed=172)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 + 1.6, -d2], [b.torso_w, y1 - y0 - 1.6, b.torso_d], blouse)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.7, 2.4], skin_mat(shade(SKIN, 0.93)))
    m.cube('torso', [-1.6, y1 - 1.2, -d2 - 0.25], [3.2, 1.0, 0.3], cloth(BLOUSE_SH, seed=173))      # col
    m.cube('torso', [-0.5, y1 - 2.6, -d2 - 0.3], [1.0, 1.4, 0.3], flat(rgb('#c23a5a'), 0.9))        # nœud
    m.cube('torso', [-b.torso_w / 2 + 0.1, y0, -d2 - 0.1], [b.torso_w - 0.2, 2.0, b.torso_d + 0.2], shorts)
    for sx in (-1, 1):                                                                              # bretelles
        m.cube('torso', [sx * 1.7 - 0.35, y0 + 1.8, -d2 - 0.12], [0.7, y1 - y0 - 2.2, 0.15], strap)
        m.cube('torso', [sx * 1.7 - 0.35, y0 + 1.8, d2 - 0.03], [0.7, y1 - y0 - 2.2, 0.15], strap)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y - 0.4, (3.5, b.shoulder_y - b.elbow_y + 1.0, 3.6), blouse)  # bouffant
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.6, (2.7, b.elbow_y - b.wrist_y - 0.8, 2.8), blouse)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.4, (2.9, 0.6, 3.0), cloth(BLOUSE_SH, seed=174))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.0, (2.4, 2.4, 2.4), skin_mat(SKIN))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.hip_y - 3.0, (3.1, 3.2, 3.2), shorts)
        m.box(f'{side}_leg', (cx, 0), b.knee_y, (2.5, b.hip_y - 3.0 - b.knee_y, 2.6), skin_mat(SKIN))
        m.box(f'{side}_shin', (cx, 0), 1.0, (2.6, b.knee_y - 1.4, 2.7), cloth(SOCK, seed=175))
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.8, 1.2, 3.8), cloth(SHOE, shade(SHOE, 1.3), shade(SHOE, 0.7), seed=176))
    # queue de chat : longue, souple, en S
    lock(m, 'tail', [0, b.hip_y + 0.4, 1.9], 14.0, 1.8, 150, 0, mk(0.1), flat=1.0, curve=(-50, 0), segs=5,
         root=1.0, tip=0.45, taper_pow=0.3, parent='body')


def build():
    m = Model('neferpitou', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=5)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=18, leg_swing=26, breath=1.2)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
