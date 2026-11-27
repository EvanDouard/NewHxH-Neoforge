"""
Isaac Netero — même style (« Minecraft anime », mèches organiques).
Traits : vieillard menu mais droit (≈ 1,6 bloc) ; crâne chauve sur le dessus ; cheveux blancs sur les côtés et
l'arrière, rassemblés en une queue relevée en chignon à l'arrière du crâne ; longs sourcils blancs tombants ;
longue barbe blanche en pointe et moustache tombante ; longs lobes d'oreille ; chemise blanche ample au kanji
« 心 » sur la poitrine, pantalon large sombre serré à la taille par une ceinture, sandales.
Expression « froid » : regard sérieux de combat (yeux grands ouverts sous les sourcils).
"""
from __future__ import annotations

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, arched, cloth, flat, hair_paint, lock, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f0cfb2')
WHITE = rgb('#f4f3ef')
WHITE_SH = rgb('#b9bcc8')
LINE = rgb('#3a2a20')
SHIRT = rgb('#f2f0ea')
SHIRT_SH = rgb('#c9c6bc')
PANTS = rgb('#2f2f36')
BELT = rgb('#8a6a3a')
SANDAL = rgb('#6a4a2a')

BODY = Body(head=(8.0, 8.4, 7.8), head_y=16.8, hip_y=8.6, knee_y=4.4, shoulder_y=15.4, arm_x=4.6, leg_x=1.8,
            torso_w=6.8, torso_d=3.8)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(WHITE_SH, WHITE, t, lit=lit, rim=rgb('#ffffff'))


def build_hair(m):
    b = BODY
    top, hx, hz = b.top, b.head[0] / 2, b.head[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    base = mk(0.1)(0.5)
    # couronne de cheveux autour du crâne chauve (côtés et arrière), d'où partent des mèches plaquées qui
    # remontent toutes vers le chignon à l'arrière du crâne
    m.cube('hair', [-hx - 0.2, b.head_y + 2.6, -1.2], [0.5, 3.4, 5.2], base)
    m.cube('hair', [hx - 0.3, b.head_y + 2.6, -1.2], [0.5, 3.4, 5.2], base)
    m.cube('hair', [-hx + 0.3, b.head_y + 2.4, hz - 0.3], [b.head[0] - 0.6, 3.4, 0.5], base)
    for sx in (-1, 1):                     # côtés : rubans plaqués peignés vers l'arrière (épaisseur faible)
        for j, (y, z, L, k) in enumerate(((b.head_y + 5.0, -1.8, 5.8, 0.3), (b.head_y + 3.2, -1.4, 5.4, 0.18))):
            lock(m, f'n_s{sx}{j}', [sx * (hx + 0.2), y, z], L, 0.7, 92, 0, mk(k if sx < 0 else k - 0.1), flat=3.0,
                 curve=(8, 0), segs=2, root=1.0, tip=0.3, taper_pow=0.5)
    for j, (x, L, k) in enumerate(((-2.4, 4.6, 0.06), (0.0, 5.0, 0.12), (2.4, 4.4, 0.02))):   # arrière : remonte
        lock(m, f'n_b{j}', [x, b.head_y + 2.2, hz + 0.25], L, 3.0, -4, -x * 5, mk(k), flat=0.35, curve=(-14, 0),
             segs=2, root=1.05, tip=0.3, taper_pow=0.5)
    # queue relevée : part de l'arrière du crâne, remonte et retombe en chignon en pointe
    m.bone('topknot', [0, top - 1.2, hz + 0.2], 'hair', rotation=[-28, 0, 0])
    m.cube('topknot', [-0.9, top - 1.1, hz - 0.1], [1.8, 0.7, 1.4], flat(rgb('#4a3a2a'), 0.9))   # lien du chignon
    lock(m, 'n_knot', [0, top - 0.6, hz + 0.6], 4.6, 3.0, 10, 0, mk(0.3), flat=0.8, curve=(60, 0), segs=4,
         root=1.1, tip=0.08, taper_pow=0.6, parent='topknot')
    lock(m, 'n_knot2', [0.5, top - 0.6, hz + 0.4], 3.8, 2.2, 4, -14, mk(0.18), flat=0.8, curve=(50, 8), segs=3,
         root=1.0, tip=0.1, taper_pow=0.6, parent='topknot')


def build_beard(m):
    """Moustache tombante, longue barbe en pointe (deux couches), longs sourcils en relief."""
    b = BODY
    hz = b.head[2] / 2
    yf = lambda gy: b.head_y + b.head[1] * (1 - gy / 60)
    m.bone('beard', [0, yf(46), -hz], 'head')
    for sx in (-1, 1):                                           # moustache
        lock(m, f'n_mu{sx}', [sx * 0.8, yf(43), -hz - 0.25], 3.6, 1.6, 180, sx * 40, mk(0.3), flat=0.7,
             curve=(0, -sx * 30), segs=2, root=1.1, tip=0.15, taper_pow=0.6, parent='beard')
    # longue barbe en mèches groupées : une pointe centrale longue, des mèches latérales plus courtes qui
    # convergent vers elle, une couche arrière plus sombre qui donne l'épaisseur
    for j, (x, L, W, rz, k) in enumerate(((-2.5, 6.4, 3.0, 18, 0.2), (-1.3, 8.8, 3.2, 8, 0.26), (0.0, 11.4, 3.4, 0, 0.32),
                                          (1.3, 9.0, 3.2, -8, 0.24), (2.5, 6.2, 3.0, -18, 0.18))):
        lock(m, f'n_beard{j}', [x, yf(50), -hz - 0.3 - 0.08 * (2 - abs(j - 2))], L, W, 184, rz, mk(k), flat=0.55,
             curve=(-22, -rz * 0.8), segs=4, root=1.05, tip=0.06, taper_pow=0.65, parent='beard',
             skew=0.08 * (j - 2) / 2)
    lock(m, 'n_beard_b', [0, yf(52), -hz - 0.02], 7.0, 6.2, 182, 0, mk(0.06), flat=0.4, curve=(-10, 0), segs=3,
         root=1.0, tip=0.12, taper_pow=0.7, parent='beard')
    for sx in (-1, 1):                                           # favoris qui rejoignent la barbe
        lock(m, f'n_fav{sx}', [sx * 3.4, yf(36), -hz - 0.1], 5.2, 2.4, 182, sx * 12, mk(0.16), flat=0.5,
             curve=(-8, -sx * 18), segs=2, root=1.0, tip=0.2, taper_pow=0.6, parent='beard')
        # sourcils longs, tombants vers l'extérieur
        lock(m, f'n_br{sx}', [sx * 1.4, yf(19), -hz - 0.3], 3.6, 1.4, 0, -sx * 104, mk(0.32), flat=0.7,
             curve=(0, sx * 40), segs=3, root=1.1, tip=0.12, taper_pow=0.6, parent='head')


def face_paint(ctx, mode='calme'):
    fight = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 4, shade(SKIN, 1.04))                   # crâne chauve qui brille un peu
    for y in (9, 13):                                         # rides du front
        f.rect(18, y, 48, y, shade(SKIN, 0.8))
    for x0 in (13, 40):
        if fight:
            f.rect(x0, 24, x0 + 13, 30, (246, 244, 238))
            f.rect(x0 + 4, 24, x0 + 9, 30, rgb('#2c1f17'))
            f.rect(x0 + 5, 25, x0 + 6, 26, (255, 255, 255))
        else:                                                 # yeux plissés, bienveillants
            f.poly((x0, 27, x0 + 13, 27, x0 + 11, 29, x0 + 2, 29), LINE)
        f.rect(x0 + 1, 32, x0 + 12, 32, shade(SKIN, 0.78))    # cernes / rides
    f.rect(31, 33, 34, 41, shade(SKIN, 0.84))                 # nez fort
    f.rect(29, 40, 36, 42, shade(SKIN, 0.8))
    f.rect(8, 36, 12, 44, shade(SKIN, 0.86))                  # pommettes
    f.rect(54, 36, 58, 44, shade(SKIN, 0.86))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face == 'up':
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=shade(SKIN, 1.05))


def build_face(m):
    b = BODY
    hx, hz = b.head[0] / 2, b.head[2] / 2
    head_box(m, b, head_paint, FACE_D, skin=SKIN, ears=False)
    for sx in (-1, 1):                                        # oreilles à longs lobes
        m.box('head', (sx * (hx + 0.3), 0.3), b.head_y + 1.2, (0.6, 3.6, 1.4), skin_mat(shade(SKIN, 0.95)),
              density=FACE_D)
        m.box('head', (sx * (hx + 0.35), 0.4), b.head_y + 0.2, (0.6, 1.2, 1.2), skin_mat(shade(SKIN, 0.92)))
    m.cube('head', [-0.5, b.head_y + 2.6, -hz - 0.4], [1.0, 1.7, 0.45], skin_mat(shade(SKIN, 0.97)))


def shirt_paint(ctx):
    cloth(SHIRT, shade(SHIRT, 1.02), SHIRT_SH, ao_top=0.04, ao_bottom=0.12, folds=3, seed=120)(ctx)
    if ctx.face != 'north':
        return
    f = FaceGrid(ctx)
    k = rgb('#1c1c1c')                                         # kanji 心 (stylisé : trois points et un crochet)
    f.poly((14, 30, 20, 30, 24, 46, 44, 46, 50, 38, 54, 38, 48, 52, 20, 52), k)
    f.rect(20, 18, 25, 26, k)
    f.rect(32, 14, 37, 24, k)
    f.rect(45, 20, 50, 28, k)


def build_outfit(m):
    b = BODY
    shirt = shirt_paint
    plain = cloth(SHIRT, shade(SHIRT, 1.02), SHIRT_SH, folds=2, seed=121)
    pants = cloth(PANTS, shade(PANTS, 1.25), shade(PANTS, 0.7), ao_top=0.1, folds=3, seed=122)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], shirt)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.6, 2.4], skin_mat(shade(SKIN, 0.92)))
    for sx in (-1, 1):                                          # col croisé
        m.cube('torso', [sx * 0.9 - 0.4, y1 - 2.6, -d2 - 0.15], [0.8, 2.8, 0.2], cloth(SHIRT_SH, seed=123),
               pivot=[sx * 0.9, y1, -d2], rotation=[0, 0, -sx * 26])
    m.cube('torso', [-b.torso_w / 2 - 0.1, y0 - 0.3, -d2 - 0.15], [b.torso_w + 0.2, 1.1, b.torso_d + 0.3],
           cloth(BELT, seed=124))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (3.0, b.shoulder_y - b.elbow_y + 0.6, 3.1), plain)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.6, (3.2, b.elbow_y - b.wrist_y - 0.4, 3.3), plain)  # manche ample
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.0, (2.5, 2.6, 2.5), skin_mat(SKIN))
    m.cube('body', [-b.torso_w / 2 + 0.3, b.hip_y - 1.2, -1.9], [b.torso_w - 0.6, 1.4, 3.8], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.3, (3.3, b.hip_y - b.knee_y + 0.3, 3.5), pants)
        m.box(f'{side}_shin', (cx, 0), 1.0, (3.4, b.knee_y - 1.0, 3.5), pants)              # pantalon large
        m.box(f'{side}_shin', (cx, -0.2), 0.0, (2.5, 1.0, 3.0), skin_mat(shade(SKIN, 0.95)))  # pied
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.8, 0.35, 3.6), flat(SANDAL, 0.9))           # sandale
        m.cube(f'{side}_shin', [cx - 0.12, 0.3, -1.6], [0.24, 0.5, 0.9], flat(SANDAL, 0.9))


def build():
    m = Model('netero', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3, feet_out=8)
    build_face(m)
    build_hair(m)
    build_beard(m)
    build_outfit(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=20, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
