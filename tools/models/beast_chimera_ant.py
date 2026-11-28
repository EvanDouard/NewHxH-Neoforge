"""
Fourmi-Chimère soldat — modèle GeckoLib `beast_chimera_ant`.

Une fourmi géante en armure : carapace brun-rouge sombre et luisante (reflets clairs sur le dessus), grosse tête
aux yeux composés noirs, longues antennes coudées, puissantes mandibules en tenailles, thorax étroit portant six
pattes articulées qui s'écartent, taille de guêpe, gros abdomen ovale strié de bandes. Basse et large
(boîte 2,0 × 1,3).
"""
from __future__ import annotations

from chibi import flat
from geolib import Model, material, rgb, shade

SHELL = rgb('#6a2a1e')
SHELL_D = rgb('#3a1610')
SHELL_L = rgb('#b0583a')
JOINT = rgb('#c8a070')
EYE = rgb('#141012')


def shell(k=1.0, seed=0, bands=0):
    base = material(shade(SHELL, k), top=1.3, bottom=0.7, edge=0.65, noise=4, grad=0.25, seed=seed)

    def paint(ctx):
        base(ctx)
        d = ctx.draw
        if bands and ctx.face != 'down':
            horiz = ctx.face in ('east', 'west', 'up')
            n = bands
            for i in range(1, n):
                if ctx.face == 'up':
                    y = round(ctx.h * i / n)
                    d.line([0, y, ctx.w - 1, y], fill=SHELL_D, width=1)
                elif horiz:
                    x = round(ctx.w * i / n)
                    d.line([x, 0, x, ctx.h - 1], fill=SHELL_D, width=1)
        if ctx.face == 'up':          # reflet luisant
            d.rectangle([ctx.w // 4, ctx.h // 5, ctx.w // 4 + max(1, ctx.w // 6), ctx.h // 5 + max(1, ctx.h // 3)],
                        fill=SHELL_L)
    return paint


def leg(m, name, x, z, sx, spread, length=(9, 12), parent='body'):
    """Patte articulée : fémur qui monte vers l'extérieur, tibia qui redescend jusqu'au sol."""
    y0 = 11
    m.bone(name, [x, y0, z], parent, rotation=[0, spread, 0])
    L1, L2 = length
    # fémur : part du thorax, monte en s'écartant
    m.cube(name, [x if sx > 0 else x - L1, y0 - 1, z - 1], [L1, 2, 2], shell(0.9, 3), pivot=[x, y0, z],
           rotation=[0, 0, sx * 28])
    kx = x + sx * L1 * 0.88
    ky = y0 + L1 * 0.47
    m.cube(name, [kx - 1.2, ky - 1.2, z - 1.2], [2.4, 2.4, 2.4], flat(JOINT, 0.85))
    # tibia : redescend en s'écartant jusqu'au sol
    import math
    lt = ky / math.cos(math.radians(22))
    m.cube(name, [kx - 0.8, ky - lt, z - 0.8], [1.6, lt, 1.6], shell(0.85, 4), pivot=[kx, ky, z],
           rotation=[0, 0, sx * 22])
    tipx = kx + sx * lt * math.sin(math.radians(22))
    m.cube(name, [tipx - 0.7, 0, z - 0.7], [1.4, 1.0, 1.4], flat(SHELL_D, 0.8))


def build():
    m = Model('beast_chimera_ant', density=4, tex_width=512)
    m.bone('root', [0, 0, 0])
    m.bone('body', [0, 11, 0], 'root')
    # thorax étroit
    m.cube('body', [-3.5, 8, -9], [7, 6.5, 11], shell(1.0, 1, bands=3))
    m.cube('body', [-2.5, 14, -8], [5, 2, 7], shell(1.1, 2))
    # taille de guêpe (pétiole)
    m.cube('body', [-1.6, 9, 1.5], [3.2, 3.2, 3.5], shell(0.85, 5))
    m.cube('body', [-1.0, 11.5, 2.5], [2.0, 2.2, 1.6], shell(0.9, 6))
    # gros abdomen ovale
    m.bone('gaster', [0, 12, 4.5], 'body', rotation=[8, 0, 0])
    m.cube('gaster', [-6, 7, 4.5], [12, 11, 15], shell(1.0, 7, bands=4))
    m.cube('gaster', [-5, 8, 19], [10, 9, 3], shell(0.95, 8))
    m.cube('gaster', [-4.5, 17.5, 6], [9, 1.5, 12], shell(1.1, 9))
    m.cube('gaster', [-3, 9.5, 21.8], [6, 5, 1.6], shell(0.9, 10))

    # tête
    m.bone('head', [0, 12, -9], 'body')
    m.cube('head', [-5.5, 7.5, -17.5], [11, 9.5, 9], shell(1.05, 11))
    m.cube('head', [-4.5, 16.5, -16.5], [9, 1.2, 7], shell(1.15, 12))
    for sx in (-1, 1):
        # yeux composés
        m.cube('head', [sx * 5.4 - 1.6, 11, -15.5], [3.2, 4, 4.6], material(EYE, top=1.6, edge=0.9, noise=12, grad=0.3))
        # antennes coudées
        m.bone(f'ant{sx}', [sx * 2.2, 16, -17], 'head', rotation=[-30, sx * 18, 0])
        m.cube(f'ant{sx}', [sx * 2.2 - 0.5, 16, -17.5], [1, 9, 1], shell(0.8, 13))
        m.cube(f'ant{sx}', [sx * 2.2 - 0.45, 24.4, -17.4], [0.9, 0.9, 9], shell(0.8, 14), pivot=[sx * 2.2, 24.8, -17],
               rotation=[-30, 0, 0])
    # mandibles en tenailles
    m.bone('jaw', [0, 9, -17], 'head')
    for sx in (-1, 1):
        m.cube('jaw', [sx * 2.6 - 1.2, 8, -24], [2.4, 2.2, 7], shell(0.75, 15), pivot=[sx * 2.6, 9, -17.5],
               rotation=[0, sx * 18, 0])
        m.cube('jaw', [sx * 0.6 - 1.0, 8.2, -25.2], [2.0, 1.8, 2.4], flat(SHELL_D, 0.8), pivot=[sx * 2.6, 9, -17.5],
               rotation=[0, sx * 18, 0])

    # six pattes (avant tournées vers l'avant, arrière vers l'arrière)
    for sx, s in ((1, 'l'), (-1, 'r')):
        leg(m, f'leg_f{s}', sx * 3.4, -8.0, sx, -sx * 42)
        leg(m, f'leg_m{s}', sx * 3.4, -4.0, sx, 0)
        leg(m, f'leg_b{s}', sx * 3.4, -0.5, sx, sx * 44, length=(10, 12))

    # marche en trépied : (avant G, milieu D, arrière G) puis l'autre trépied
    T = 0.6

    def swing(phase):
        a, b = (14, -14) if phase == 0 else (-14, 14)
        return {'rotation': {'0.0': [0, a, 0], f'{T / 2:.2f}': [0, b, 0], f'{T:.2f}': [0, a, 0]},
                'position': {'0.0': [0, 0, 0], f'{T / 4:.2f}': [0, 1.2 if phase == 0 else 0, 0], f'{T / 2:.2f}': [0, 0, 0],
                             f'{3 * T / 4:.2f}': [0, 0 if phase == 0 else 1.2, 0], f'{T:.2f}': [0, 0, 0]}}
    walk = {'leg_fl': swing(0), 'leg_mr': swing(0), 'leg_bl': swing(0),
            'leg_fr': swing(1), 'leg_ml': swing(1), 'leg_br': swing(1),
            'gaster': {'rotation': {'0.0': [0, -3, 0], f'{T / 2:.2f}': [0, 3, 0], f'{T:.2f}': [0, -3, 0]}},
            'ant1': {'rotation': {'0.0': [0, 0, 0], f'{T / 2:.2f}': [8, 0, 0], f'{T:.2f}': [0, 0, 0]}},
            'ant-1': {'rotation': {'0.0': [8, 0, 0], f'{T / 2:.2f}': [0, 0, 0], f'{T:.2f}': [8, 0, 0]}}}
    m.animations = {
        'idle': {'loop': True, 'animation_length': 2.4, 'bones': {
            'ant1': {'rotation': {'0.0': [0, 0, 0], '0.6': [10, 6, 0], '1.2': [0, 0, 0], '2.4': [0, 0, 0]}},
            'ant-1': {'rotation': {'0.0': [0, 0, 0], '1.2': [0, 0, 0], '1.8': [10, -6, 0], '2.4': [0, 0, 0]}},
            'jaw': {'scale': {'0.0': [1, 1, 1], '1.0': [1, 1, 1], '1.15': [0.85, 1, 1], '1.3': [1, 1, 1], '2.4': [1, 1, 1]}},
            'gaster': {'rotation': {'0.0': [0, 0, 0], '1.2': [3, 0, 0], '2.4': [0, 0, 0]}}}},
        'walk': {'loop': True, 'animation_length': T, 'bones': walk},
        'attack': {'loop': False, 'animation_length': 0.45, 'bones': {
            'head': {'rotation': {'0.0': [0, 0, 0], '0.12': [-14, 0, 0], '0.25': [12, 0, 0], '0.45': [0, 0, 0]}},
            'jaw': {'scale': {'0.0': [1, 1, 1], '0.12': [1.5, 1, 1], '0.25': [0.6, 1, 1], '0.45': [1, 1, 1]}},
            'body': {'position': {'0.0': [0, 0, 0], '0.25': [0, 0, -2.5], '0.45': [0, 0, 0]}}}},
    }
    return m


if __name__ == '__main__':
    build().save()
