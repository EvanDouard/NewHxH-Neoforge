"""
Pot-Clean, le petit génie de Knuckle (Hakoware) — invocation GeckoLib (Geo-Fx).

Petit esprit bleu pâle qui flotte au-dessus de sa cible : grosse tête ronde au visage d'enfant (grands yeux,
sourire, joues roses), petit chapeau melon noir, corps en goutte qui finit en queue de fantôme, deux petits bras
qui tiennent la pancarte du compteur (les chiffres de la dette sont dessinés par l'effet par-dessus).
Hauteur de référence : 1 bloc. Animations : `appear`, `idle` (il flotte et se balance), `collect`
(il secoue la pancarte : la dette grimpe).
"""
from __future__ import annotations

from chibi import FaceGrid, flat
from geolib import Model, material, rgb, shade

BODY = rgb('#bfe6ff')
BODY_D = rgb('#7ab8e6')
INK = rgb('#1a1a22')
HAT = rgb('#2c2c38')


def blue(k=1.0):
    return material(shade(BODY, k), top=1.08, bottom=0.86, edge=0.8, noise=2, grad=0.14)


def face_paint(ctx):
    blue(1.04)(ctx)
    if ctx.face != 'north':
        return
    f = FaceGrid(ctx)
    for x0 in (12, 38):
        f.rect(x0, 22, x0 + 16, 38, (250, 250, 252))
        f.rect(x0 + 4, 25, x0 + 12, 37, INK)
        f.rect(x0 + 5, 26, x0 + 8, 29, (255, 255, 255))
        f.rect(x0 - 1, 20, x0 + 17, 22, INK)
    f.rect(6, 40, 14, 44, rgb('#f6a0b4'))
    f.rect(52, 40, 60, 44, rgb('#f6a0b4'))
    f.poly((20, 44, 46, 44, 42, 52, 24, 52), INK)                  # grand sourire
    f.rect(23, 44, 43, 46, (255, 255, 255))


def build():
    m = Model('summon_pot_clean', density=8, tex_width=512)
    m.bone('body', [0, 0, 0])
    m.bone('head', [0, 7, 0], 'body')
    # corps en goutte : trois tranches qui s'affinent, puis la pointe
    for k, (y, w, dz) in enumerate(((5.2, 5.0, 0), (3.6, 3.8, 0.3), (2.2, 2.8, 0.7), (1.0, 1.8, 1.2))):
        m.cube('body', [-w / 2, y, -w / 2 + dz], [w, 2.0 if k == 0 else 1.6, w], blue(1.0 - 0.05 * k))
    m.cube('body', [-0.6, -0.4, 1.4], [1.2, 1.6, 1.2], blue(0.86), pivot=[0, 0.4, 2], rotation=[30, 0, 0])
    # tête ronde : cube central + joues arrondies
    m.cube('head', [-4.0, 7.6, -4.4], [8, 6.8, 8.6], face_paint)
    m.cube('head', [-3.0, 6.6, -3.6], [6, 8.8, 7.4], blue(0.98))
    m.cube('head', [-5.0, 8.6, -3.4], [10, 4.8, 7.0], blue(0.96))
    m.cube('head', [-4.6, 7.8, -3.8], [9.2, 6.4, 7.6], blue(0.97))
    m.cube('head', [-3.4, 7.2, -4.0], [6.8, 7.6, 8.4], blue(1.0))
    m.cube('head', [-3.0, 8.0, -3.4], [6, 6, 8.2], blue(0.95))
    # chapeau melon
    m.cube('head', [-3.8, 15, -3.6], [7.6, 0.8, 7.2], flat(HAT, 0.9))
    m.cube('head', [-2.6, 15.6, -2.4], [5.2, 2.6, 4.8], flat(HAT, 0.9))
    m.cube('head', [-2.7, 15.8, -2.5], [5.4, 0.6, 5.0], flat(rgb('#8a2a3a'), 0.9))
    # petits bras et pancarte du compteur
    for sx, side in ((-1, 'r'), (1, 'l')):
        m.bone(f'arm_{side}', [sx * 2.4, 6.4, -0.5], 'body', rotation=[-50, 0, -sx * 20])
        m.cube(f'arm_{side}', [sx * 2.4 - 0.6, 3.6, -1.1], [1.2, 3.0, 1.2], blue(0.95))
    m.bone('sign', [0, 4.2, -3.6], 'body')
    m.cube('sign', [-3.6, 2.6, -4.0], [7.2, 3.6, 0.5], flat(rgb('#fbf8ee'), 0.85))
    m.cube('sign', [-3.9, 2.3, -3.9], [7.8, 4.2, 0.3], flat(INK, 0.9))
    m.animations = {
        'appear': {'loop': False, 'animation_length': 0.4, 'bones': {
            'body': {'scale': {'0.0': [0.1, 0.1, 0.1], '0.25': [1.15, 1.15, 1.15], '0.4': [1, 1, 1]}}}},
        'idle': {'loop': True, 'animation_length': 2.0, 'bones': {
            'body': {'position': {'0.0': [0, 0, 0], '1.0': [0, 0.8, 0], '2.0': [0, 0, 0]},
                     'rotation': {'0.0': [0, 0, -4], '1.0': [0, 0, 4], '2.0': [0, 0, -4]}},
            'head': {'rotation': {'0.0': [0, 0, 3], '1.0': [0, 0, -3], '2.0': [0, 0, 3]}}}},
        'collect': {'loop': False, 'animation_length': 0.6, 'bones': {
            'sign': {'rotation': {'0.0': [0, 0, 0], '0.15': [0, 0, 12], '0.3': [0, 0, -12], '0.45': [0, 0, 8],
                                  '0.6': [0, 0, 0]}},
            'arm_r': {'rotation': {'0.0': [0, 0, 0], '0.15': [-20, 0, 0], '0.3': [10, 0, 0], '0.6': [0, 0, 0]}},
            'arm_l': {'rotation': {'0.0': [0, 0, 0], '0.15': [10, 0, 0], '0.3': [-20, 0, 0], '0.6': [0, 0, 0]}}}},
    }
    return m


if __name__ == '__main__':
    build().save()
