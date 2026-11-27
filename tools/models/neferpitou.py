"""
Neferpitou (anime 2011) — refonte d'après les références : silhouette fine, taille marquée, hanches larges ;
cheveux blancs ondulés et ébouriffés, frange irrégulière ; grandes oreilles de chat couvertes de fourrure jaune,
plantées dans la chevelure ; grands yeux rubis aux pupilles de chat ; manteau bleu à six boutons jaunes et boutons
de manchette, short, chaussettes orange à fines rayures noires, chaussures bleues ; genoux articulés comme des pattes
d'insecte ; mains un peu plus grandes que la normale ; longue queue de chat blanche et fournie.
Expression « froid » : yeux de prédateur (pupilles fendues, sourire carnassier).
"""
from __future__ import annotations

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, _fin, arched, cloth, flat, hair_paint, lock, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f7e6dc')
HAIR = rgb('#e9e6f0')
HAIR_SH = rgb('#a8a3bb')
HAIR_RIM = rgb('#ffffff')
EAR_IN = rgb('#f2a9bf')
EYE = rgb('#d4243a')
LINE = rgb('#3a2630')
COAT = rgb('#3d5aa8')
COAT_L = rgb('#5876c4')
COAT_D = rgb('#26386e')
BUTTON = rgb('#f0c840')
SHORTS = rgb('#2c2f4a')
SOCK = rgb('#ef8a3a')
SOCK_LINE = rgb('#2a1a14')
SHOE = rgb('#3550a0')
EAR_FUR = rgb('#ecd27a')
EAR_FUR_D = rgb('#c8a64e')
JOINT = rgb('#d9c8bc')

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
    c, R = (0.0, top - 3.4, 0.4), 4.6
    scalp(m, c, R, lambda k: mk(k - 0.1)(0.5))
    rr = R + 0.3
    # mèches souples et ondulées : longueurs, largeurs et directions toutes différentes ; chacune s'enroule un peu
    # sur le côté (side) et rebique en pointe arrondie : volume ébouriffé, pas de coque
    azs = [0, 34, 62, 92, 118, 242, 268, 296, 326]
    for i, az in enumerate(azs):
        L = 9.6 + rnd.uniform(-1.4, 2.0)
        crown_lock(m, f'p_m{i}', c, rr, az + rnd.uniform(-8, 8), 70 + rnd.uniform(-10, 8), L, 4.0 + rnd.uniform(-0.6, 1.0),
                   mk(0.16 if 60 < az < 300 else 0.04), flat=0.42, turn=104 + rnd.uniform(-12, 14), tip=0.45,
                   segs=5, lean=rnd.uniform(-14, 14), twist=rnd.uniform(-22, 22), skew=rnd.uniform(-0.18, 0.18),
                   side=rnd.choice((-1, 1)) * rnd.uniform(14, 30))
    for i, az in enumerate((16, 48, 78, 105, 255, 282, 312, 342)):   # couche secondaire plus courte (boucles)
        crown_lock(m, f'p_s{i}', c, rr - 0.3, az + rnd.uniform(-6, 6), 64 + rnd.uniform(-6, 6), 7.2 + rnd.uniform(-0.8, 1.4),
                   2.8 + rnd.uniform(0, 0.8), mk(0.0), flat=0.5, turn=128 + rnd.uniform(-10, 14), tip=0.5, segs=4,
                   lean=rnd.uniform(-16, 16), twist=rnd.uniform(-26, 26), side=rnd.uniform(-45, 45))
    # frange : mèches ondulées qui tombent sur le front, la plus longue au centre
    for j, (az, L, W, lean, k, sd) in enumerate(((146, 5.6, 3.6, -12, 0.3, -24), (166, 6.4, 4.2, -4, 0.36, 18),
                                                 (186, 7.0, 3.8, 4, 0.4, -14), (206, 5.8, 3.6, 12, 0.32, 26))):
        crown_lock(m, f'p_f{j}', c, rr, az, 72, L, W, mk(k), flat=0.45, turn=96, tip=0.4, segs=4, lean=lean,
                   skew=rnd.uniform(-0.14, 0.14), side=sd)
    # grandes oreilles de chat à fourrure jaune, plantées dans la chevelure (base enfoncée dans les mèches),
    # pointes légèrement tournées vers l'extérieur, intérieur clair
    for sx in (-1, 1):
        ex, ey = sx * 2.9, top - 0.6
        m.bone(f'ear{sx}', [ex, ey, 0.4], 'hair', rotation=[-10, -sx * 12, -sx * 22])
        # oreille triangulaire à flancs droits (pas de marches) : fourrure jaune, intérieur clair en retrait
        fur = lambda t: material(shade(EAR_FUR, 1.04 - 0.1 * t), top=1.05, bottom=0.92, edge=0.9, noise=6)
        _fin(m, f'ear{sx}', ex, ey, 0.0, 6.4, 4.8, 1.4, fur, ((0.0, 0.5, 0.6, 1.0), (0.5, 1.0, 0.45, 0.8)))
        _fin(m, f'ear{sx}', ex, ey + 0.4, -0.72, 5.0, 3.0, 0.12, lambda t: flat(shade(EAR_FUR, 1.2), 0.95),
             ((0.0, 1.0, 0.3, 1.0),))
        n = 12
        m.cube(f'ear{sx}', [ex - 0.25, ey + n * 0.52 - 0.2, -0.75], [0.5, 0.9, 1.0], flat(EAR_FUR_D, 0.9))    # touffe
        for k, (daz, L) in enumerate(((-14, 5.6), (10, 6.2), (26, 5.0))):     # mèches ondulées autour de la base
            crown_lock(m, f'p_e{sx}{k}', c, rr + 0.35, sx * (68 + daz), 84, L, 3.2, mk(0.18), flat=0.45,
                       turn=118, tip=0.45, segs=4, twist=rnd.uniform(-20, 20), side=sx * rnd.uniform(15, 35))


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
        pw = 1 if hunt else 2
        f.rect(x0 + 8 - pw, 25, x0 + 8 + pw, 36, rgb('#2a0810'))          # pupille de chat
        f.rect(x0 + 4, 25, x0 + 6, 28, (255, 255, 255))
        f.rect(x0 + 1, 21, x0 + 15, 22, LINE)
        if x0 < 30:                                              # coin extérieur relevé (félin)
            f.poly((x0 - 2, 19, x0 + 3, 21, x0 + 1, 23), LINE)
        else:
            f.poly((x0 + 18, 19, x0 + 13, 21, x0 + 15, 23), LINE)
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


def sock_paint(ctx):
    material(SOCK, top=1.08, bottom=0.86, edge=0.85, noise=4)(ctx)
    for y in range(2, ctx.h, 5):                                   # fines rayures noires
        ctx.draw.line([(0, y), (ctx.w, y)], fill=SOCK_LINE, width=1)


def build_body(m):
    b = BODY
    coat = cloth(COAT, COAT_L, COAT_D, ao_bottom=0.12, folds=3, seed=170)
    shorts = cloth(SHORTS, shade(SHORTS, 1.25), shade(SHORTS, 0.7), ao_top=0.15, folds=2, seed=171)
    btn = flat(BUTTON, 0.85)
    btn_rim = flat(shade(BUTTON, 0.7), 0.9)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    # torse fin, taille marquée ; manteau bleu croisé avec deux rangées de trois boutons jaunes
    m.cube('torso', [-b.torso_w / 2 + 0.5, y0 + 1.4, -d2], [b.torso_w - 1.0, 2.4, b.torso_d], coat)        # taille
    m.cube('torso', [-b.torso_w / 2, y0 + 3.6, -d2], [b.torso_w, y1 - y0 - 3.6, b.torso_d], coat)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.7, 2.4], skin_mat(shade(SKIN, 0.93)))
    for sx in (-1, 1):                                                                              # revers
        m.cube('torso', [sx * 1.0 - 0.7, y1 - 2.6, -d2 - 0.2], [1.4, 2.4, 0.25], cloth(COAT_L, seed=173),
               pivot=[sx * 1.0, y1 - 1.4, -d2], rotation=[0, 0, sx * 18])
        for k in range(3):
            m.cube('torso', [sx * 1.15 - 0.5, y0 + 1.9 + k * 1.9, -d2 - 0.3], [1.0, 1.0, 0.25], btn_rim)
            m.cube('torso', [sx * 1.15 - 0.35, y0 + 2.05 + k * 1.9, -d2 - 0.45], [0.7, 0.7, 0.2], btn)
    m.cube('torso', [-0.25, y0 + 1.4, -d2 - 0.15], [0.5, y1 - y0 - 3.6, 0.15], cloth(COAT_D, seed=174))      # fente
    # basques du manteau : pans qui s'évasent sur les hanches (hanches larges), derrière et sur les côtés
    for half, hx in (('r', -1), ('l', 1)):
        bn = f'coat_tail_{half}'
        m.bone(bn, [hx * 1.6, y0 + 1.4, 0], 'torso', rotation=[0, 0, hx * 9])
        m.cube(bn, [hx * 1.6 - (2.4 if hx < 0 else 0.0) - (0.4 if hx < 0 else -0.4), y0 - 3.6, -d2 - 0.15],
               [2.4, 5.0, b.torso_d + 0.3], coat)
    m.cube('torso', [-b.torso_w / 2 + 0.2, y0 - 0.4, -d2 + 0.1], [b.torso_w - 0.4, 2.0, b.torso_d - 0.2], shorts)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y - 0.2, (2.9, b.shoulder_y - b.elbow_y + 0.8, 3.0), coat)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.5, (2.8, b.elbow_y - b.wrist_y - 0.7, 2.9), coat)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.3, (3.1, 0.9, 3.2), cloth(COAT_L, seed=175))        # poignet
        m.box(f'{side}_forearm', (cx + sx * 1.6, 0), b.wrist_y + 0.5, (0.3, 0.5, 0.5), btn)                  # bouton de manchette
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.4, (2.9, 2.8, 2.9), skin_mat(SKIN))                     # mains un peu grandes
        for k in range(4):
            m.cube(f'{side}_hand', [cx - 1.2 + k * 0.7, b.wrist_y - 3.1, -1.25], [0.5, 0.8, 0.6], skin_mat(shade(SKIN, 0.95)))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.hip_y - 2.2, (3.0, 2.4, 3.1), shorts)
        m.box(f'{side}_leg', (cx, 0), b.knee_y + 0.4, (2.4, b.hip_y - 2.2 - b.knee_y - 0.4, 2.5), skin_mat(SKIN))
        # genou articulé comme une patte d'insecte : rotule saillante et anneau
        m.box(f'{side}_leg', (cx, -0.7), b.knee_y - 0.6, (2.9, 1.6, 2.8), material(JOINT, top=1.1, bottom=0.8, edge=0.65))
        m.box(f'{side}_leg', (cx, -1.3), b.knee_y - 0.2, (1.4, 0.9, 0.6), flat(shade(JOINT, 0.8), 0.9))
        m.box(f'{side}_shin', (cx, 0), 1.0, (2.5, b.knee_y - 1.4, 2.6), sock_paint)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.8, 1.2, 3.8), cloth(SHOE, shade(SHOE, 1.3), shade(SHOE, 0.7), seed=176))
    # longue queue de chat blanche et fournie : épaisse à la base, en S, bout plus touffu
    lock(m, 'tail', [0, b.hip_y + 0.4, 1.9], 15.0, 2.4, 150, 0, mk(0.12), flat=1.0, curve=(-60, 14), segs=5,
         root=1.1, tip=0.8, taper_pow=0.2, belly=1.15, parent='body')


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
