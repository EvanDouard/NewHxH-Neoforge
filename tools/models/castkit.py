"""Corps simples partagés par les personnages secondaires (costume, robe, tenue de ninja)."""
from __future__ import annotations

from chibi import cloth, flat, skin_mat
from geolib import rgb, shade


def suit_body(m, b, skin, suit, suit_l, shirt, *, tie=None, tails=False, shoes=rgb('#0e0e12'), torso_paint=None):
    from chibi import FaceGrid
    sm = cloth(suit, suit_l, shade(suit, 0.72), folds=2, seed=700)

    def front(ctx):
        cloth(suit, suit_l, shade(suit, 0.72), ao_bottom=0.1, folds=2, seed=701)(ctx)
        if ctx.face == 'north':
            f = FaceGrid(ctx)
            f.poly((22, 0, 44, 0, 36, 34, 30, 34), shirt)
            if tie is not None:
                f.poly((32, 2, 34, 2, 35, 30, 33, 33, 31, 30), tie)
            f.poly((22, 0, 28, 0, 33, 34), suit_l)
            f.poly((44, 0, 38, 0, 33, 34), suit_l)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 1.0, -d2], [b.torso_w, y1 - y0 + 1.0, b.torso_d], torso_paint or front)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.6, 2.4], skin_mat(shade(skin, 0.92)))
    if tails:                                                      # queue-de-pie
        m.bone('tails', [0, y0, d2], 'body', rotation=[-5, 0, 0])
        m.cube('tails', [-b.torso_w / 2 + 0.4, b.knee_y + 0.6, d2 - 0.3], [b.torso_w - 0.8, y0 - b.knee_y - 0.6, 0.6], sm)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.9, b.shoulder_y - b.elbow_y + 0.6, 3.0), sm)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (2.8, b.elbow_y - b.wrist_y - 0.2, 2.9), sm)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.1, (2.9, 0.5, 3.0), cloth(shirt, seed=702))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.3, (2.3, 2.3, 2.3), skin_mat(skin))
    m.cube('body', [-b.torso_w / 2 + 0.3, b.hip_y - 1.2, -1.75], [b.torso_w - 0.6, 1.5, 3.5], sm)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.9, b.hip_y - b.knee_y + 0.2, 3.0), sm)
        m.box(f'{side}_shin', (cx, 0), 1.0, (2.8, b.knee_y - 1.0, 2.9), sm)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.9, 1.1, 3.9), flat(shoes, 0.95))
