"""
Bombe de Genthru (Compte à rebours / Petite fleur) — invocation GeckoLib (Geo-Fx).

Bombe d'aura collée au-dessus de la cible (≈ 0,8 bloc) : sphère sombre cerclée de métal, cadran blanc sur le
devant où s'affichent les secondes restantes (chiffres dessinés par le rendu), quatre pointes de mine, petite
mèche qui grésille au sommet. Animations : `appear`, `idle` (elle bat comme un cœur, plus fort à chaque seconde).
"""
from __future__ import annotations

from chibi import FaceGrid, flat
from geolib import Model, material, rgb, shade

SHELL = rgb('#2a2a34')
METAL = rgb('#a8aeb8')
WHITE = rgb('#f4f2ea')
FIRE = rgb('#ff9a3a')


def shell(k=1.0):
    return material(shade(SHELL, k), top=1.25, bottom=0.8, edge=0.8, noise=3, grad=0.2)


def build():
    m = Model('summon_bomb', density=8, tex_width=512)
    m.bone('body', [0, 6, 0])
    m.cube('body', [-5, 1.5, -4], [10, 9, 8], shell(1.0))
    m.cube('body', [-4, 0.6, -4.8], [8, 10.8, 9.6], shell(0.96))
    m.cube('body', [-4.4, 1.0, -4.4], [8.8, 10, 8.8], shell(0.98))
    m.cube('body', [-5.2, 9.4, -5.2], [10.4, 1.0, 10.4], flat(METAL, 0.9))         # cerclage
    m.cube('body', [-2.8, 3.2, -5.2], [5.6, 5.6, 0.6], flat(WHITE, 0.9))            # cadran
    m.cube('body', [-3.1, 2.9, -5.0], [6.2, 6.2, 0.4], flat(METAL, 0.9))
    for k, (x, z) in enumerate(((-5.6, 0), (5.6, 0), (0, 5.6), (0, -5.6))):          # pointes de mine
        if z == -5.6:
            continue
        m.cube('body', [x - 0.6, 9.3, z - 0.6], [1.2, 1.2, 1.2], flat(METAL, 0.9), pivot=[x, 9.9, z], rotation=[0, 45, 45])
    m.cube('body', [-1, 11.2, -1], [2, 1.2, 2], flat(METAL, 0.9))
    m.cube('body', [-0.3, 12.2, -0.3], [0.6, 2.2, 0.6], flat(rgb('#6a5a3a'), 0.9), pivot=[0, 12.2, 0], rotation=[0, 0, 18])
    m.cube('body', [0.2, 14.0, -0.6], [1.2, 1.2, 1.2], flat(FIRE, 1.0))
    m.animations = {
        'appear': {'loop': False, 'animation_length': 0.3, 'bones': {
            'body': {'scale': {'0.0': [0.1, 0.1, 0.1], '0.2': [1.2, 1.2, 1.2], '0.3': [1, 1, 1]}}}},
        'idle': {'loop': True, 'animation_length': 1.0, 'bones': {
            'body': {'scale': {'0.0': [1.14, 1.14, 1.14], '0.25': [1, 1, 1], '1.0': [1, 1, 1]}}}},
    }
    return m


if __name__ == '__main__':
    build().save()
