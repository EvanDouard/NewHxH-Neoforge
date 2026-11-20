"""
Générateur de modèles GeckoLib pour les personnages du mod (voir docs/Analyse_model.md).

Un modèle = des os (pivot, parent, rotation) portant des cubes ; chaque face de cube est peinte par une fonction à
une densité donnée (pixels de texture par pixel de modèle), puis rangée automatiquement dans la texture. Le script
écrit :
    assets/hxh/geo/entity/npc/<id>.geo.json
    assets/hxh/animations/entity/npc/<id>.animation.json
    assets/hxh/textures/entity/npc_geo/<id>.png
    build/model-previews/<id>.png   (aperçu 3D : face, trois-quarts, profil, dos + texture)

Repère (géométrie Bedrock, comme dans Blockbench) : pixels, Y vers le haut, pieds à y = 0, l'avant regarde vers -Z
(face « north »), le côté gauche du personnage est en +X (comme dans les modèles de référence).
"""
from __future__ import annotations

import json
import math
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'hxh')
PREVIEWS = os.path.join(ROOT, 'build', 'model-previews')

FACES = ('north', 'south', 'east', 'west', 'up', 'down')


# ---------------------------------------------------------------------------------------------------------------
# Couleurs

def rgb(c):
    if c is None:
        return None
    if isinstance(c, tuple):
        return c if len(c) == 4 else (*c, 255)
    c = c.lstrip('#')
    if len(c) == 6:
        c += 'ff'
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4, 6))


def shade(c, k):
    r, g, b, a = rgb(c)
    if k >= 1:
        t = k - 1
        return (min(255, int(r + (255 - r) * t)), min(255, int(g + (255 - g) * t)), min(255, int(b + (255 - b) * t)), a)
    return (int(r * k), int(g * k), int(b * k), a)


def mix(a, b, t):
    a, b = rgb(a), rgb(b)
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))


# ---------------------------------------------------------------------------------------------------------------
# Peintures de face

class FaceCtx:
    """Contexte d'une face : taille en texels (w, h), dessin PIL dans une image dédiée."""

    def __init__(self, face, w, h, density):
        self.face, self.w, self.h, self.density = face, w, h, density
        self.img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        self.draw = ImageDraw.Draw(self.img)


def material(base, top=1.12, bottom=0.82, edge=0.78, noise=5, grad=0.16, outline=True, seed=0, side_dark=0.93):
    """Peinture « manga » : dégradé vertical, dessus clair, dessous sombre, bords assombris, grain léger."""
    base = rgb(base)

    def paint(ctx: FaceCtx):
        rnd = random.Random(hash((ctx.face, ctx.w, ctx.h, seed)) & 0xffffffff)
        px = ctx.img.load()
        for y in range(ctx.h):
            for x in range(ctx.w):
                if ctx.face == 'up':
                    c = shade(base, top)
                elif ctx.face == 'down':
                    c = shade(base, bottom)
                else:
                    t = y / max(1, ctx.h - 1)
                    c = shade(base, 1 + grad * (0.5 - t))
                    if ctx.face in ('east', 'west'):
                        c = shade(c, side_dark)
                    if ctx.face == 'south':
                        c = shade(c, 0.95)
                if outline and (x == 0 or x == ctx.w - 1 or y == ctx.h - 1 or (ctx.face in ('up', 'down') and y == 0)):
                    c = shade(c, edge)
                d = rnd.randint(-noise, noise) if noise else 0
                px[x, y] = (max(0, min(255, c[0] + d)), max(0, min(255, c[1] + d)), max(0, min(255, c[2] + d)), c[3])
    return paint


def layered(*painters):
    def paint(ctx):
        for p in painters:
            if p is not None:
                p(ctx)
    return paint


def only(faces, painter):
    def paint(ctx):
        if ctx.face in faces:
            painter(ctx)
    return paint


def clear():
    def paint(ctx):
        ctx.img.paste((0, 0, 0, 0), (0, 0, ctx.w, ctx.h))
    return paint


# ---------------------------------------------------------------------------------------------------------------
# Modèle

class Cube:
    def __init__(self, origin, size, paint, density, inflate=0.0, pivot=None, rotation=None, name=None):
        self.origin, self.size, self.paint, self.density = list(origin), list(size), paint, density
        self.inflate, self.pivot, self.rotation, self.name = inflate, pivot, rotation, name
        self.uv = {}


class Bone:
    def __init__(self, name, pivot, parent=None, rotation=None):
        self.name, self.pivot, self.parent, self.rotation = name, list(pivot), parent, rotation
        self.cubes = []


class Model:
    def __init__(self, ident, density=4, tex_width=512):
        self.id = ident
        self.density = density
        self.tex_width = tex_width
        self.bones = {}
        self.order = []
        self.animations = {}

    def bone(self, name, pivot, parent=None, rotation=None):
        b = Bone(name, pivot, parent, rotation)
        self.bones[name] = b
        self.order.append(name)
        return b

    def cube(self, bone, origin, size, paint, density=None, inflate=0.0, pivot=None, rotation=None, name=None):
        c = Cube(origin, size, paint, density or self.density, inflate, pivot, rotation, name)
        self.bones[bone].cubes.append(c)
        return c

    def box(self, bone, center_xz, y0, size, paint, **kw):
        """Cube décrit par son centre en X/Z et sa base en Y (plus lisible pour un corps)."""
        cx, cz = center_xz
        w, h, d = size
        return self.cube(bone, [cx - w / 2, y0, cz - d / 2], size, paint, **kw)

    # ---- texture : chaque face peinte puis rangée (étagères)
    def _face_dims(self, c, face):
        w, h, d = c.size
        dims = {'north': (w, h), 'south': (w, h), 'east': (d, h), 'west': (d, h), 'up': (w, d), 'down': (w, d)}
        fw, fh = dims[face]
        return max(1, int(round(fw * c.density))), max(1, int(round(fh * c.density)))

    def bake(self):
        items = []
        for bn in self.order:
            for c in self.bones[bn].cubes:
                for f in FACES:
                    w, h = self._face_dims(c, f)
                    items.append((c, f, w, h))
        items.sort(key=lambda it: -it[3])
        W = self.tex_width
        x = y = shelf = 0
        placed = []
        for c, f, w, h in items:
            if x + w > W:
                x, y, shelf = 0, y + shelf, 0
            placed.append((c, f, x, y, w, h))
            x += w
            shelf = max(shelf, h)
        H = y + shelf
        H = 1 << max(4, (H - 1).bit_length())
        tex = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        for c, f, px, py, w, h in placed:
            ctx = FaceCtx(f, w, h, c.density)
            c.paint(ctx)
            tex.paste(ctx.img, (px, py))
            c.uv[f] = (px, py, w, h)
        self.texture = tex
        return tex

    def geo_json(self):
        bones = []
        for bn in self.order:
            b = self.bones[bn]
            jb = {'name': b.name, 'pivot': [round(v, 4) for v in b.pivot]}
            if b.parent:
                jb['parent'] = b.parent
            if b.rotation:
                jb['rotation'] = list(b.rotation)
            cubes = []
            for c in b.cubes:
                jc = {'origin': [round(v, 4) for v in c.origin], 'size': [round(v, 4) for v in c.size]}
                if c.inflate:
                    jc['inflate'] = c.inflate
                if c.rotation:
                    jc['pivot'] = [round(v, 4) for v in (c.pivot or [c.origin[i] + c.size[i] / 2 for i in range(3)])]
                    jc['rotation'] = list(c.rotation)
                jc['uv'] = {f: {'uv': [c.uv[f][0], c.uv[f][1]], 'uv_size': [c.uv[f][2], c.uv[f][3]]} for f in FACES}
                cubes.append(jc)
            if cubes:
                jb['cubes'] = cubes
            bones.append(jb)
        ys = [c.origin[1] + c.size[1] for b in self.bones.values() for c in b.cubes]
        height = max(ys) / 16
        return {
            'format_version': '1.12.0',
            'minecraft:geometry': [{
                'description': {
                    'identifier': 'geometry.hxh.' + self.id,
                    'texture_width': self.texture.width, 'texture_height': self.texture.height,
                    'visible_bounds_width': round(max(2, height), 1), 'visible_bounds_height': round(height + 1, 1),
                    'visible_bounds_offset': [0, round(height / 2 + 0.25, 2), 0],
                },
                'bones': bones,
            }],
        }

    def save(self):
        self.bake()
        geo = os.path.join(ASSETS, 'geo', 'entity', 'npc', self.id + '.geo.json')
        anim = os.path.join(ASSETS, 'animations', 'entity', 'npc', self.id + '.animation.json')
        tex = os.path.join(ASSETS, 'textures', 'entity', 'npc_geo', self.id + '.png')
        for p in (geo, anim, tex):
            os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(geo, 'w') as f:
            json.dump(self.geo_json(), f, separators=(',', ':'))
        with open(anim, 'w') as f:
            json.dump({'format_version': '1.8.0', 'animations': self.animations}, f, indent=1)
        self.texture.save(tex)
        preview(self, os.path.join(PREVIEWS, self.id + '.png'))
        n = sum(len(b.cubes) for b in self.bones.values())
        print(f'{self.id}: {len(self.bones)} os, {n} cubes, texture {self.texture.size}')


# ---------------------------------------------------------------------------------------------------------------
# Aperçu (rendu logiciel orthographique)

def _rot(rx, ry, rz):
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    m = lambda a, b: [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return m(Rz, m(Ry, Rx))


def _apply(R, p):
    return tuple(sum(R[i][k] * p[k] for k in range(3)) for i in range(3))


def _rotate_about(p, pivot, rot):
    if not rot or not any(rot):
        return p
    R = _rot(*[math.radians(a) for a in rot])
    q = _apply(R, (p[0] - pivot[0], p[1] - pivot[1], p[2] - pivot[2]))
    return (q[0] + pivot[0], q[1] + pivot[1], q[2] + pivot[2])


def _face_quad(c, f):
    i = c.inflate
    x0, y0, z0 = c.origin[0] - i, c.origin[1] - i, c.origin[2] - i
    x1, y1, z1 = c.origin[0] + c.size[0] + i, c.origin[1] + c.size[1] + i, c.origin[2] + c.size[2] + i
    # coins dans l'ordre (haut-gauche, haut-droit, bas-droit, bas-gauche) vus de l'extérieur de la face
    return {
        'north': [(x0, y1, z0), (x1, y1, z0), (x1, y0, z0), (x0, y0, z0)],
        'south': [(x1, y1, z1), (x0, y1, z1), (x0, y0, z1), (x1, y0, z1)],
        'east': [(x1, y1, z0), (x1, y1, z1), (x1, y0, z1), (x1, y0, z0)],
        'west': [(x0, y1, z1), (x0, y1, z0), (x0, y0, z0), (x0, y0, z1)],
        'up': [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)],
        'down': [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
    }[f]


def render(model, yaw=0, pitch=0, px=11, canvas=(300, 520), pose=None):
    pose = pose or {}
    tex = model.texture
    quads = []
    view = _rot(math.radians(pitch), math.radians(yaw), 0)

    def chain(bn):
        out = []
        while bn:
            out.append(model.bones[bn])
            bn = model.bones[bn].parent
        return out

    for bn in model.order:
        bones = chain(bn)
        for c in model.bones[bn].cubes:
            for f in FACES:
                u, v, w, h = c.uv[f]
                corners = _face_quad(c, f)
                def tf(p):
                    if c.rotation:
                        p = _rotate_about(p, c.pivot or [c.origin[i] + c.size[i] / 2 for i in range(3)], c.rotation)
                    for b in bones:
                        rot = list(b.rotation or [0, 0, 0])
                        if b.name in pose:
                            rot = [rot[i] + pose[b.name][i] for i in range(3)]
                        p = _rotate_about(p, b.pivot, rot)
                    return _apply(view, p)
                P = [tf(p) for p in corners]
                # normale (vers le spectateur si z < 0)
                ax = [P[1][i] - P[0][i] for i in range(3)]
                ay = [P[3][i] - P[0][i] for i in range(3)]
                n = (ax[1] * ay[2] - ax[2] * ay[1], ax[2] * ay[0] - ax[0] * ay[2], ax[0] * ay[1] - ax[1] * ay[0])
                ln = math.sqrt(sum(a * a for a in n)) or 1
                n = tuple(a / ln for a in n)
                for ty in range(h):
                    for tx in range(w):
                        col = tex.getpixel((u + tx, v + ty))
                        if col[3] < 20:
                            continue
                        a0, a1, b0, b1 = tx / w, (tx + 1) / w, ty / h, (ty + 1) / h
                        def at(a, b):
                            return tuple(P[0][i] + (P[1][i] - P[0][i]) * a + (P[3][i] - P[0][i]) * b for i in range(3))
                        quads.append(([at(a0, b0), at(a1, b0), at(a1, b1), at(a0, b1)], col, n))
    W, H = canvas
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    light = (0.3, 0.75, -0.6)
    ll = math.sqrt(sum(a * a for a in light))
    light = tuple(a / ll for a in light)
    quads.sort(key=lambda q: -sum(p[2] for p in q[0]))
    for pts, col, n in quads:
        if n[2] > 0.05:
            continue  # face tournée vers l'arrière
        k = 0.62 + 0.38 * max(0.0, n[0] * light[0] + n[1] * light[1] + n[2] * light[2])
        c = (int(col[0] * k), int(col[1] * k), int(col[2] * k), 255)
        # X de l'écran : +X du modèle (côté gauche du personnage) à droite de l'image, vu de face
        poly = [(W / 2 + p[0] * px, H - 18 - p[1] * px) for p in pts]
        dr.polygon(poly, fill=c, outline=c)
    return img


def preview(model, path):
    views = [render(model, a, p) for a, p in ((0, 0), (-35, 10), (-90, 0), (180, 0))]
    W, H = views[0].size
    tw = min(512, model.texture.width)
    th = int(model.texture.height * tw / model.texture.width)
    sheet = Image.new('RGBA', (W * 4 + tw + 16, max(H, th + 16)), (58, 62, 70, 255))
    for i, v in enumerate(views):
        sheet.alpha_composite(v, (i * W, 0))
    sheet.alpha_composite(model.texture.resize((tw, th), Image.NEAREST), (W * 4 + 8, 8))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sheet.save(path)


# ---------------------------------------------------------------------------------------------------------------
# Animations communes (noms attendus par NpcEntity : idle, walk)

def kf(**frames):
    return {('%.2f' % float(k[1:].replace('_', '.'))): v for k, v in frames.items()}


def humanoid_animations(arm_swing=28, leg_swing=30, breath=1.6):
    return {
        'idle': {
            'loop': True, 'animation_length': 3.0,
            'bones': {
                'torso': {'rotation': {'0.0': [0, 0, 0], '1.5': [-breath, 0, 0], '3.0': [0, 0, 0]}},
                'right_arm': {'rotation': {'0.0': [0, 0, 2], '1.5': [0, 0, 4], '3.0': [0, 0, 2]}},
                'left_arm': {'rotation': {'0.0': [0, 0, -2], '1.5': [0, 0, -4], '3.0': [0, 0, -2]}},
            },
        },
        'walk': {
            'loop': True, 'animation_length': 0.8,
            'bones': {
                'right_leg': {'rotation': {'0.0': [leg_swing, 0, 0], '0.4': [-leg_swing, 0, 0], '0.8': [leg_swing, 0, 0]}},
                'left_leg': {'rotation': {'0.0': [-leg_swing, 0, 0], '0.4': [leg_swing, 0, 0], '0.8': [-leg_swing, 0, 0]}},
                'right_shin': {'rotation': {'0.0': [0, 0, 0], '0.2': [-25, 0, 0], '0.4': [0, 0, 0], '0.8': [0, 0, 0]}},
                'left_shin': {'rotation': {'0.0': [0, 0, 0], '0.4': [0, 0, 0], '0.6': [-25, 0, 0], '0.8': [0, 0, 0]}},
                'right_arm': {'rotation': {'0.0': [-arm_swing, 0, 0], '0.4': [arm_swing, 0, 0], '0.8': [-arm_swing, 0, 0]}},
                'left_arm': {'rotation': {'0.0': [arm_swing, 0, 0], '0.4': [-arm_swing, 0, 0], '0.8': [arm_swing, 0, 0]}},
                'right_forearm': {'rotation': {'0.0': [-15, 0, 0], '0.4': [-5, 0, 0], '0.8': [-15, 0, 0]}},
                'left_forearm': {'rotation': {'0.0': [-5, 0, 0], '0.4': [-15, 0, 0], '0.8': [-5, 0, 0]}},
                'torso': {'rotation': {'0.0': [3, 0, 0], '0.8': [3, 0, 0]}},
            },
        },
    }
