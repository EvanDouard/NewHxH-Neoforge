"""
Menthuthuyoupi — même style. Colosse (≈ 2,4 blocs), épaules et bras énormes ; peau bleu-gris sombre ; crâne
surmonté d'une crête de courtes cornes recourbées vers l'arrière (sa « coiffure »), grandes oreilles pointues,
yeux rouges sous d'épais arcs sourciliers, large bouche ; gilet rouge sombre ouvert sans manches, ceinture-écharpe,
pantalon large sombre resserré aux chevilles, pieds nus.
Expression « froid » : rage (bouche ouverte, crocs).
"""
from __future__ import annotations

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, flat, lock, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#5c6a86')
SKIN_L = rgb('#7a89a6')
SKIN_D = rgb('#3a4560')
HORN = rgb('#d9cdb4')
HORN_D = rgb('#8a7d64')
LINE = rgb('#1a1c26')
EYE = rgb('#e2283a')
VEST = rgb('#8a2a2e')
VEST_L = rgb('#b03a3e')
PANTS = rgb('#2c2a34')
SASH = rgb('#c9a24a')

BODY = Body(head=(8.6, 8.6, 8.4), head_y=29.4, hip_y=15.4, knee_y=7.6, shoulder_y=27.6, arm_x=7.6, leg_x=2.7,
            torso_w=12.0, torso_d=6.4)
FACE_D = 7


def skin(k=1.0):
    return material(shade(SKIN, k), grad=0.12, noise=2, edge=0.86)


def mkh(lit):
    return lambda t: strand_paint(HORN_D, HORN, t, lit=lit, rim=rgb('#fff6e0'))


def build_crest(m):
    b = BODY
    top, hz = b.top, b.head[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    # crête centrale de cornes courtes, de plus en plus grandes vers l'arrière, recourbées
    for i, (z, L, W) in enumerate(((-2.4, 2.6, 2.0), (-0.6, 3.4, 2.4), (1.2, 4.2, 2.6), (3.0, 4.6, 2.8))):
        lock(m, f'y_h{i}', [0, top - 0.2, z], L + 1.0, W, 10 + 8 * i, 0, mkh(0.2 - 0.04 * i), flat=0.9, curve=(34, 0),
             segs=3, root=1.15, tip=0.12, taper_pow=0.6)
    for sx in (-1, 1):                                          # cornes latérales
        for i, (z, L) in enumerate(((-1.0, 2.8), (1.6, 3.4))):
            lock(m, f'y_s{sx}{i}', [sx * 3.0, top - 0.6, z], L, 1.8, 20, -sx * 30, mkh(0.1), flat=0.9,
                 curve=(26, -sx * 6), segs=2, root=1.1, tip=0.12, taper_pow=0.6)
        # grandes oreilles pointues
        lock(m, f'y_ear{sx}', [sx * (b.head[0] / 2 + 0.2), b.head_y + 4.0, 0.2], 4.6, 2.4, 20, -sx * 70,
             lambda t: strand_paint(SKIN_D, SKIN_L, t, lit=0.1), flat=0.35, curve=(10, -sx * 16), segs=2, root=1.1,
             tip=0.1, taper_pow=0.6)


def face_paint(ctx, mode='calme'):
    rage = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 4, SKIN_D)
    f.rect(6, 16, 60, 22, SKIN_D)                               # arcade sourcilière lourde
    for x0 in (12, 41):
        f.rect(x0, 23, x0 + 13, 29, (40, 10, 14))
        f.rect(x0 + 3, 24, x0 + 10, 29, EYE)
        f.rect(x0 + 6, 25, x0 + 7, 28, (255, 220, 200))
    f.rect(30, 30, 36, 40, SKIN_D)                              # nez épaté
    f.rect(27, 38, 39, 41, SKIN_D)
    if rage:
        f.rect(16, 45, 50, 56, rgb('#3a0a10'))
        for x in range(18, 48, 5):
            f.poly((x, 45, x + 3, 45, x + 1.5, 49), (240, 236, 220))
            f.poly((x, 56, x + 3, 56, x + 1.5, 52), (240, 236, 220))
    else:
        f.rect(14, 49, 52, 50, LINE)
        f.rect(14, 47, 16, 49, LINE)
        f.rect(50, 47, 52, 49, LINE)


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)


def build_body(m):
    b = BODY
    vest = cloth(VEST, VEST_L, shade(VEST, 0.65), ao_bottom=0.1, folds=2, seed=200)
    pants = cloth(PANTS, shade(PANTS, 1.3), shade(PANTS, 0.7), ao_top=0.1, folds=3, seed=201)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    # torse musclé (taille plus fine), pectoraux, abdominaux en relief
    m.cube('torso', [-b.torso_w / 2 + 1.4, y0, -d2 + 0.4], [b.torso_w - 2.8, 5.0, b.torso_d - 0.8], skin(0.96))
    m.cube('torso', [-b.torso_w / 2, y0 + 4.8, -d2], [b.torso_w, y1 - y0 - 4.8, b.torso_d], skin())
    for sx in (-1, 1):
        m.cube('torso', [sx * 2.6 - 2.4, y1 - 5.2, -d2 - 0.5], [4.8, 3.4, 0.6], skin(1.05))     # pectoraux
        for k in range(3):
            m.cube('torso', [sx * 1.2 - 1.0, y0 + 0.8 + k * 1.7, -d2 + 0.1], [2.0, 1.4, 0.4], skin(1.02))
    m.cube('torso', [-2.0, y1 - 0.2, -2.0], [4.0, 1.0, 4.0], skin(0.9))                           # cou épais
    for sx in (-1, 1):
        m.cube('torso', [sx * 3.4 - 2.2, y1 - 0.8, -2.4], [4.4, 1.8, 4.8], skin(0.95),
               pivot=[sx * 3.4, y1, 0], rotation=[0, 0, -sx * 22])                                 # trapèzes
    # gilet ouvert : deux pans devant, dos, épaules
    for sx in (-1, 1):
        x0 = sx * b.torso_w / 2 - (3.2 if sx > 0 else -0.0)
        m.cube('torso', [min(x0, x0 + 3.2 * sx) if sx < 0 else x0, y0 - 0.4, -d2 - 0.35], [3.2, y1 - y0, 0.8], vest)
    m.cube('torso', [-b.torso_w / 2 - 0.1, y0 - 0.4, d2 - 0.3], [b.torso_w + 0.2, y1 - y0, 0.8], vest)
    m.cube('torso', [-b.torso_w / 2 - 0.1, y0 - 1.0, -d2 - 0.4], [b.torso_w + 0.2, 1.4, b.torso_d + 0.8],
           cloth(SASH, seed=202))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.shoulder_y - 3.6, (5.4, 4.6, 5.4), skin(0.98))           # deltoïde
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (4.8, b.shoulder_y - b.elbow_y - 3.2, 4.8), skin())
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (4.6, b.elbow_y - b.wrist_y - 0.2, 4.6), skin(0.97))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 3.6, (4.4, 3.8, 4.4), skin(0.95))
    m.cube('body', [-b.torso_w / 2 + 1.2, b.hip_y - 1.6, -2.8], [b.torso_w - 2.4, 1.8, 5.6], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.3, (5.2, b.hip_y - b.knee_y + 0.3, 5.4), pants)
        m.box(f'{side}_shin', (cx, 0), 1.6, (5.0, b.knee_y - 1.6, 5.2), pants)                   # bouffant
        m.box(f'{side}_shin', (cx, 0), 1.2, (4.0, 0.8, 4.2), cloth(SASH, seed=203))
        m.box(f'{side}_shin', (cx, -0.6), 0.0, (4.2, 1.4, 6.0), skin(0.92))                      # pieds nus


def build():
    m = Model('youpi', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=8, leg_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_crest(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=20, breath=1.6)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
