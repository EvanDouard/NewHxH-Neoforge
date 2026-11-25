"""
Base commune aux personnages « adultes » du style Minecraft anime (Leorio, Hisoka et suivants) : squelette aux
proportions réglables (mêmes noms d'os que Gon : animations partagées), texture d'expression, aperçus.
"""
from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass
class Body:
    head: tuple = (8.0, 8.6, 7.8)      # largeur, hauteur, profondeur de la tête
    head_y: float = 22.6               # base de la tête
    hip_y: float = 12.4
    knee_y: float = 6.3
    shoulder_y: float = 21.0
    arm_x: float = 4.9                 # axe des bras
    leg_x: float = 1.9
    torso_w: float = 7.2
    torso_d: float = 3.8

    @property
    def top(self):
        return self.head_y + self.head[1]

    @property
    def elbow_y(self):
        return self.hip_y + (self.shoulder_y - self.hip_y) * 0.48

    @property
    def wrist_y(self):
        return self.hip_y - 0.2


def skeleton(m, b: Body, arm_out=4, leg_out=1.5, feet_out=6):
    m.bone('body', [0, b.hip_y, 0])
    m.bone('torso', [0, b.hip_y + 1.6, 0], 'body')
    m.bone('head', [0, b.head_y, 0], 'torso')
    for side, sx in (('right', -1), ('left', 1)):
        m.bone(f'{side}_arm', [sx * (b.arm_x - 0.4), b.shoulder_y, 0], 'torso', rotation=[0, 0, -sx * arm_out])
        m.bone(f'{side}_forearm', [sx * b.arm_x, b.elbow_y, 0], f'{side}_arm')
        m.bone(f'{side}_hand', [sx * b.arm_x, b.wrist_y, 0], f'{side}_forearm')
        m.bone('item_main' if side == 'right' else 'item_off', [sx * b.arm_x, b.wrist_y - 2.2, -1.0], f'{side}_hand')
        m.bone(f'{side}_leg', [sx * b.leg_x, b.hip_y, 0], 'body', rotation=[0, 0, -sx * leg_out])
        m.bone(f'{side}_shin', [sx * b.leg_x, b.knee_y, 0], f'{side}_leg', rotation=[0, -sx * feet_out, 0])


def write_expression(model, face_paint, face_d, mode='froid'):
    """Texture <id>_<mode> où seul le visage change (mêmes UV) + aperçu côte à côte."""
    from geolib import ASSETS, PREVIEWS, FaceCtx
    from PIL import Image
    x, y, w, h = model.head_cube.uv['north']
    ctx = FaceCtx('north', w, h, face_d)
    face_paint(ctx, mode)
    tex = model.texture.copy()
    tex.paste(ctx.img, (x, y))
    tex.save(os.path.join(ASSETS, 'textures', 'entity', 'npc_geo', f'{model.id}_{mode}.png'))
    out = Image.new('RGBA', (w * 8 + 8, h * 4), (40, 40, 48, 255))
    out.paste(model.texture.crop((x, y, x + w, y + h)).resize((w * 4, h * 4), 0), (0, 0))
    out.paste(ctx.img.resize((w * 4, h * 4), 0), (w * 4 + 8, 0))
    out.save(os.path.join(PREVIEWS, f'{model.id}_expressions.png'))


def head_box(m, b: Body, head_paint, face_d=7, skin=None, ears=True, neck=True):
    from chibi import skin_mat
    from geolib import shade
    hx = b.head[0] / 2
    m.head_cube = m.box('head', (0, 0), b.head_y, b.head, head_paint, density=face_d)
    if neck:
        m.box('head', (0, 0), b.head_y - 0.8, (2.8, 0.9, 2.8), skin_mat(shade(skin, 0.92)))
    if ears:
        for sx in (-1, 1):
            m.box('head', (sx * (hx + 0.3), 0.3), b.head_y + 2.8, (0.6, 2.0, 1.3), skin_mat(shade(skin, 0.95)),
                  density=face_d)
