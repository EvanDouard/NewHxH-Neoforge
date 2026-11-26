"""
Terpsichora, la marionnette de Neferpitou — invocation GeckoLib (Geo-Fx).

Grand pantin d'aura (≈ 3,4 blocs) qui se tient derrière Pitou et la manipule : corps de danseuse articulé (rotules
rondes aux épaules, coudes, poignets, hanches, genoux), masque de poupée lisse et inexpressif (yeux en amande
peints, bouche fine), collerette à plis, coiffe en pointes de bouffon ; ses longs bras passent au-dessus de la
tête de Pitou, doigts tendus vers le bas d'où partent les fils (dessinés par l'effet) ; les jambes pendent, pointes
tendues comme une ballerine suspendue. Rendu translucide, teinté par la couleur de l'aura.
Animations : `appear`, `idle` (les doigts tirent les fils en alternance, le corps tangue), `pull` (grand geste des
deux bras : Pitou bondit).
"""
from __future__ import annotations

from chibi import FaceGrid, flat
from geolib import Model, material, rgb, shade

BODY = rgb('#e8e2f2')
JOINT = rgb('#b8aed0')
INK = rgb('#2a2236')
ACC = rgb('#7a5aa8')


def doll(k=1.0):
    return material(shade(BODY, k), top=1.06, bottom=0.86, edge=0.82, noise=2, grad=0.12)


def mask_paint(ctx):
    doll(1.04)(ctx)
    if ctx.face != 'north':
        return
    f = FaceGrid(ctx)
    for x0 in (12, 38):
        f.poly((x0, 30, x0 + 8, 25, x0 + 16, 30, x0 + 8, 33), INK)          # yeux en amande
        f.rect(x0 + 6, 28, x0 + 10, 31, rgb('#c03050'))
    f.rect(28, 46, 38, 47, INK)
    f.rect(31, 12, 35, 22, ACC)                                      # losange peint au front
    f.rect(8, 38, 12, 46, ACC)
    f.rect(54, 38, 58, 46, ACC)


def joint(m, bone, c, r=1.6):
    m.cube(bone, [c[0] - r, c[1] - r, c[2] - r], [2 * r, 2 * r, 2 * r], flat(JOINT, 0.88))


def build():
    m = Model('summon_terpsichora', density=4, tex_width=512)
    m.bone('body', [0, 0, 0])
    m.bone('torso', [0, 30, 0], 'body')
    m.bone('head', [0, 46, 0], 'torso')
    # buste de danseuse : taille fine, bassin, poitrine
    m.cube('torso', [-3.5, 30, -2.5], [7, 6, 5], doll(0.95))
    m.cube('torso', [-2.6, 35.5, -2.0], [5.2, 3, 4], doll(0.92))
    m.cube('torso', [-4.2, 38, -2.6], [8.4, 7, 5.2], doll(1.0))
    m.cube('torso', [-1.2, 44.5, -1.2], [2.4, 2, 2.4], doll(0.95))
    # collerette à plis
    for i in range(10):
        a = i * 36
        m.cube('torso', [-1.2, 44.2, -5.0], [2.4, 1.6, 3.4], doll(1.08 if i % 2 else 0.96), pivot=[0, 45, 0],
               rotation=[0, a, 0])
    # masque et coiffe de bouffon en pointes
    m.cube('head', [-3.6, 46.5, -3.6], [7.2, 8, 7.2], mask_paint)
    m.cube('head', [-3.0, 46, -3.0], [6.0, 9, 6.6], doll(0.97))
    m.cube('head', [-4.1, 47.5, -3.0], [8.2, 6, 6.6], doll(0.95))
    for k, (rz, rx) in enumerate(((-40, 0), (0, -10), (40, 0))):
        m.cube('head', [-1.5, 55, -1.5], [3, 6, 3], flat(ACC, 0.88), pivot=[0, 55, 0], rotation=[rx, 0, rz])
        m.cube('head', [-1.0, 60, -1.0], [2, 2, 2], flat(JOINT, 0.9), pivot=[0, 55, 0], rotation=[rx, 0, rz])
    # bras levés au-dessus de Pitou : épaule, bras, coude, avant-bras, main ouverte, doigts tendus vers le bas
    for sx, side in ((-1, 'r'), (1, 'l')):
        sh = [sx * 5.4, 43, 0]
        joint(m, 'torso', sh, 1.8)
        m.bone(f'arm_{side}', sh, 'torso', rotation=[-62, 0, -sx * 14])
        m.cube(f'arm_{side}', [sh[0] - 1.1, 43, -1.1], [2.2, 11, 2.2], doll(0.98))
        joint(m, f'arm_{side}', [sh[0], 55, 0], 1.5)
        m.bone(f'forearm_{side}', [sh[0], 55, 0], f'arm_{side}', rotation=[-36, 0, 0])
        m.cube(f'forearm_{side}', [sh[0] - 1.0, 55, -1.0], [2.0, 11, 2.0], doll(1.0))
        joint(m, f'forearm_{side}', [sh[0], 66.5, 0], 1.2)
        m.bone(f'hand_{side}', [sh[0], 67, 0], f'forearm_{side}', rotation=[-70, 0, 0])
        m.cube(f'hand_{side}', [sh[0] - 1.6, 67, -0.6], [3.2, 3, 1.2], doll(1.04))
        for fgr in range(4):
            m.cube(f'hand_{side}', [sh[0] - 1.5 + fgr * 0.85, 70, -0.4], [0.6, 3.2 - 0.3 * abs(fgr - 1.5), 0.8],
                   doll(1.06))
    # jambes pendantes de ballerine suspendue, pointes tendues
    for sx, side in ((-1, 'r'), (1, 'l')):
        hip = [sx * 2.2, 30, 0]
        joint(m, 'body', hip, 1.6)
        m.bone(f'leg_{side}', hip, 'body', rotation=[8 if sx < 0 else -6, 0, sx * 4])
        m.cube(f'leg_{side}', [hip[0] - 1.3, 16, -1.3], [2.6, 14, 2.6], doll(0.97))
        joint(m, f'leg_{side}', [hip[0], 16, 0], 1.4)
        m.cube(f'leg_{side}', [hip[0] - 1.1, 2.5, -1.1], [2.2, 13.5, 2.2], doll(1.0))
        m.cube(f'leg_{side}', [hip[0] - 0.8, -1, -0.8], [1.6, 3.5, 1.6], flat(ACC, 0.88))      # pointe

    m.animations = {
        'appear': {'loop': False, 'animation_length': 0.6, 'bones': {
            'body': {'position': {'0.0': [0, 20, 0], '0.45': [0, -1, 0], '0.6': [0, 0, 0]},
                     'scale': {'0.0': [0.3, 0.3, 0.3], '0.45': [1.05, 1.05, 1.05], '0.6': [1, 1, 1]}}}},
        'idle': {'loop': True, 'animation_length': 2.4, 'bones': {
            'body': {'rotation': {'0.0': [0, 0, -3], '1.2': [0, 0, 3], '2.4': [0, 0, -3]},
                     'position': {'0.0': [0, 0, 0], '1.2': [0, 1.0, 0], '2.4': [0, 0, 0]}},
            'hand_r': {'rotation': {'0.0': [0, 0, 0], '0.6': [-18, 0, 0], '1.2': [0, 0, 0], '2.4': [0, 0, 0]}},
            'hand_l': {'rotation': {'0.0': [0, 0, 0], '1.2': [0, 0, 0], '1.8': [-18, 0, 0], '2.4': [0, 0, 0]}},
            'leg_r': {'rotation': {'0.0': [0, 0, 0], '1.2': [6, 0, 0], '2.4': [0, 0, 0]}},
            'leg_l': {'rotation': {'0.0': [0, 0, 0], '1.2': [-6, 0, 0], '2.4': [0, 0, 0]}}}},
        'pull': {'loop': False, 'animation_length': 0.5, 'bones': {
            'arm_r': {'rotation': {'0.0': [0, 0, 0], '0.15': [-30, 0, 0], '0.5': [0, 0, 0]}},
            'arm_l': {'rotation': {'0.0': [0, 0, 0], '0.15': [-30, 0, 0], '0.5': [0, 0, 0]}}}},
    }
    return m


if __name__ == '__main__':
    build().save()
