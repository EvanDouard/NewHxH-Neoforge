"""
Outils pour dessiner les apparences des personnages du mod (skins 64x64 + cubes en plus).

Un personnage = un skin au format joueur (64x64, deux couches) + une « apparence »
(assets/hxh/looks/<id>.json) : échelle, bras fins, et des cubes en plus accrochés aux membres
(cheveux en pointes, barbe, ventre…) avec leur propre texture <id>_extra.png.

    python3 tools/skins/characters.py            # régénère tout + aperçus dans build/skin-previews
    python3 tools/skins/characters.py netero     # un seul personnage

Conventions (identiques à Minecraft) : coordonnées en pixels de modèle, Y vers le bas, pieds à y = 24,
l'avant du personnage regarde vers -Z, sa droite est en -X.
"""
from __future__ import annotations

import json
import math
import os
import random

from PIL import Image, ImageDraw

# ---------------------------------------------------------------------------------------------------------------
# Couleurs


def rgb(c) -> tuple:
    if c is None:
        return None
    if isinstance(c, tuple):
        return c if len(c) == 4 else (*c, 255)
    c = c.lstrip('#')
    if len(c) == 6:
        c += 'ff'
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4, 6))


def shade(c, k: float):
    """k > 1 éclaircit (vers le blanc), k < 1 assombrit."""
    r, g, b, a = rgb(c)
    if k >= 1:
        t = k - 1
        return (int(r + (255 - r) * t), int(g + (255 - g) * t), int(b + (255 - b) * t), a)
    return (int(r * k), int(g * k), int(b * k), a)


def mix(c1, c2, t: float):
    a, b = rgb(c1), rgb(c2)
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))


def jitter(c, amount: int, rnd: random.Random):
    if amount <= 0:
        return rgb(c)
    r, g, b, a = rgb(c)
    d = rnd.randint(-amount, amount)
    return (max(0, min(255, r + d)), max(0, min(255, g + d)), max(0, min(255, b + d)), a)


# ---------------------------------------------------------------------------------------------------------------
# Format de skin


def face_rects(u, v, w, h, d):
    """Rectangles (x, y, largeur, hauteur) des 6 faces d'une boîte dans la texture (dépliage Minecraft)."""
    return {
        'top': (u + d, v, w, d),
        'bottom': (u + d + w, v, w, d),
        'right': (u, v + d, d, h),
        'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h),
        'back': (u + 2 * d + w, v + d, w, h),
    }


FACES = ('top', 'bottom', 'right', 'front', 'left', 'back')
SIDES = ('right', 'front', 'left', 'back')


def part_specs(slim=False):
    """nom -> (taille, uv couche 1, uv couche 2, pivot, coin de la boîte, gonflement couche 2)."""
    aw = 3 if slim else 4
    ay = 2.5 if slim else 2
    return {
        'head': ((8, 8, 8), (0, 0), (32, 0), (0, 0, 0), (-4, -8, -4), 0.5),
        'body': ((8, 12, 4), (16, 16), (16, 32), (0, 0, 0), (-4, 0, -2), 0.25),
        'right_arm': ((aw, 12, 4), (40, 16), (40, 32), (-5, ay, 0), (-aw + 1, -2, -2), 0.25),
        'left_arm': ((aw, 12, 4), (32, 48), (48, 48), (5, ay, 0), (-1, -2, -2), 0.25),
        'right_leg': ((4, 12, 4), (0, 16), (0, 32), (-1.9, 12, 0), (-2, 0, -2), 0.25),
        'left_leg': ((4, 12, 4), (16, 48), (0, 48), (1.9, 12, 0), (-2, 0, -2), 0.25),
    }


# hauteur (en pixels depuis le haut de la tête) du haut de chaque membre, pour peindre par bandes
PART_TOP = {'head': 0, 'body': 8, 'right_arm': 8, 'left_arm': 8, 'right_leg': 20, 'left_leg': 20}


class Skin:
    def __init__(self, slim=False, seed=1):
        self.slim = slim
        self.img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        self.specs = part_specs(slim)
        self.rnd = random.Random(seed)

    def rect(self, part, face, layer='base'):
        size, uv1, uv2, *_ = self.specs[part]
        u, v = uv1 if layer == 'base' else uv2
        return face_rects(u, v, *size)[face]

    def put(self, part, face, x, y, c, layer='base'):
        fx, fy, fw, fh = self.rect(part, face, layer)
        if 0 <= x < fw and 0 <= y < fh and c is not None:
            self.img.putpixel((fx + x, fy + y), rgb(c))

    def paint(self, part, fn, layer='base', faces=FACES, noise=0):
        """fn(face, x, y, w, h, gy) -> couleur ou None ; gy = hauteur globale (0 = haut de la tête)."""
        for face in faces:
            fx, fy, fw, fh = self.rect(part, face, layer)
            for y in range(fh):
                for x in range(fw):
                    gy = PART_TOP[part] + y if face not in ('top', 'bottom') else None
                    c = fn(face, x, y, fw, fh, gy)
                    if c is not None:
                        self.img.putpixel((fx + x, fy + y), jitter(c, noise, self.rnd))

    def fill(self, part, color, layer='base', faces=FACES, noise=0):
        self.paint(part, lambda *a: color, layer, faces, noise)

    def grid(self, part, face, rows, pal, layer='base', noise=0, x0=0, y0=0):
        """Dessin en caractères : chaque lettre est une couleur de la palette, '.' ou ' ' = on ne touche pas,
        '_' = transparent (efface)."""
        fx, fy, fw, fh = self.rect(part, face, layer)
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in '. ':
                    continue
                X, Y = x0 + x, y0 + y
                if not (0 <= X < fw and 0 <= Y < fh):
                    continue
                if ch == '_':
                    self.img.putpixel((fx + X, fy + Y), (0, 0, 0, 0))
                else:
                    self.img.putpixel((fx + X, fy + Y), jitter(pal[ch], noise, self.rnd))

    def bands(self, part, bands, layer='base', faces=SIDES, noise=0):
        """bands : liste (gy_début, gy_fin_exclue, couleur) appliquée aux faces latérales selon la hauteur."""
        def fn(face, x, y, w, h, gy):
            for a, b, c in bands:
                if a <= gy < b:
                    return c(face, x, y, w, h, gy) if callable(c) else c
            return None
        self.paint(part, fn, layer, faces, noise)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path)


# ---------------------------------------------------------------------------------------------------------------
# Cubes en plus


def style(color, outline=0.62, top=1.18, bottom=0.78, grad=0.18, noise=4, faces=None):
    """Peinture « manga » d'un cube : dégradé vertical, dessus clair, contour sombre sur les arêtes."""
    base = rgb(color)

    def fn(face, x, y, w, h):
        if faces and face in faces:
            f = faces[face]
            c = f(face, x, y, w, h) if callable(f) else f
            if c is not None:
                return c
        c = base
        if face == 'top':
            c = shade(c, top)
        elif face == 'bottom':
            c = shade(c, bottom)
        else:
            t = y / max(1, h - 1)
            c = shade(c, 1 + grad * (0.5 - t)) if grad else c
        if outline and face not in ('top',) and (x == 0 or x == w - 1 or (face != 'bottom' and y == h - 1)):
            c = shade(c, outline + (1 - outline) * 0.35) if (w > 2 and h > 2) else c
        return c
    fn.noise = noise
    return fn


class Extras:
    """Cubes accrochés aux membres, avec dépliage UV automatique dans une texture dédiée."""

    def __init__(self, tw=64, th=64, seed=7):
        self.tw, self.th = tw, th
        self.img = Image.new('RGBA', (tw, th), (0, 0, 0, 0))
        self.cubes = []
        self.rnd = random.Random(seed)
        self._shelf_x = 0
        self._shelf_y = 0
        self._shelf_h = 0

    def _alloc(self, rw, rh):
        if self._shelf_x + rw > self.tw:
            self._shelf_x = 0
            self._shelf_y += self._shelf_h
            self._shelf_h = 0
        if self._shelf_y + rh > self.th:
            # agrandit la texture vers le bas
            new_h = self.th * 2
            img = Image.new('RGBA', (self.tw, new_h), (0, 0, 0, 0))
            img.paste(self.img, (0, 0))
            self.img, self.th = img, new_h
        u, v = self._shelf_x, self._shelf_y
        self._shelf_x += rw
        self._shelf_h = max(self._shelf_h, rh)
        return u, v

    def cube(self, part, frm, size, paint, pivot=None, rot=(0, 0, 0), inflate=0.0):
        """frm/size en pixels de modèle, dans le repère du membre (comme dans Blockbench).
        pivot : point de rotation (repère du membre) ; rot : degrés (x, y, z)."""
        w, h, d = (max(1, int(round(s))) for s in size)
        u, v = self._alloc(2 * (d + w), d + h)
        rects = face_rects(u, v, w, h, d)
        noise = getattr(paint, 'noise', 0)
        for face, (fx, fy, fw, fh) in rects.items():
            for y in range(fh):
                for x in range(fw):
                    c = paint(face, x, y, fw, fh)
                    if c is not None:
                        self.img.putpixel((fx + x, fy + y), jitter(c, noise, self.rnd))
        pivot = pivot if pivot is not None else [frm[i] + size[i] / 2 for i in range(3)]
        self.cubes.append({
            'part': part, 'uv': [u, v], 'uv_size': [w, h, d],
            'from': [round(f, 3) for f in frm], 'size': [round(s, 3) for s in size],
            'pivot': [round(p, 3) for p in pivot], 'rotation': list(rot), 'inflate': inflate,
        })
        return self

    def json(self, texture):
        return {'texture': texture, 'texture_size': [self.tw, self.th], 'cubes': self.cubes}


def dump_look(look) -> str:
    """JSON lisible : un cube par ligne."""
    cubes = look.get('extras', {}).get('cubes')
    if not cubes:
        return json.dumps(look, ensure_ascii=False, indent=2) + '\n'
    head = dict(look)
    ex = dict(look['extras'])
    ex['cubes'] = '@CUBES@'
    head['extras'] = ex
    text = json.dumps(head, ensure_ascii=False, indent=2)
    lines = ',\n'.join('      ' + json.dumps(c, ensure_ascii=False) for c in cubes)
    return text.replace('"@CUBES@"', '[\n' + lines + '\n    ]') + '\n'


# ---------------------------------------------------------------------------------------------------------------
# Aperçu 3D (rendu logiciel orthographique, pour vérifier sans lancer le jeu)


def _rot(rx, ry, rz):
    """Matrice Rz * Ry * Rx (même ordre que ModelPart)."""
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return _mm(Rz, _mm(Ry, Rx))


def _mm(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def _mv(m, v):
    return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))


def _box_faces(x0, y0, z0, x1, y1, z1):
    """face -> (origine, vecteur pleine largeur le long de x texture, vecteur pleine hauteur le long de y texture)."""
    return {
        'front': ((x0, y0, z0), (x1 - x0, 0, 0), (0, y1 - y0, 0)),
        'back': ((x1, y0, z1), (x0 - x1, 0, 0), (0, y1 - y0, 0)),
        'right': ((x0, y0, z1), (0, 0, z0 - z1), (0, y1 - y0, 0)),
        'left': ((x1, y0, z0), (0, 0, z1 - z0), (0, y1 - y0, 0)),
        'top': ((x0, y0, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)),
        'bottom': ((x0, y1, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)),
    }


NORMALS = {'front': (0, 0, -1), 'back': (0, 0, 1), 'right': (-1, 0, 0), 'left': (1, 0, 0),
           'top': (0, -1, 0), 'bottom': (0, 1, 0)}


def _quads_for_box(tex, uv, size, frm, inflate, transform, out):
    w, h, d = (int(round(s)) for s in size)
    x0, y0, z0 = (frm[i] - inflate for i in range(3))
    x1, y1, z1 = frm[0] + size[0] + inflate, frm[1] + size[1] + inflate, frm[2] + size[2] + inflate
    rects = face_rects(uv[0], uv[1], w, h, d)
    for face, (o, ux, uy) in _box_faces(x0, y0, z0, x1, y1, z1).items():
        fx, fy, fw, fh = rects[face]
        n = transform(NORMALS[face], normal=True)
        for ty in range(fh):
            for tx in range(fw):
                c = tex.getpixel((fx + tx, fy + ty))
                if c[3] < 16:
                    continue
                pts = []
                for ax, ay in ((tx, ty), (tx + 1, ty), (tx + 1, ty + 1), (tx, ty + 1)):
                    p = tuple(o[i] + ux[i] * ax / fw + uy[i] * ay / fh for i in range(3))
                    pts.append(transform(p))
                out.append((pts, c, n))


def render_preview(skin_img, look, extras_img=None, yaw=0.0, pitch=0.0, px=9, canvas=(240, 440)):
    slim = look.get('slim', False)
    specs = part_specs(slim)
    sc = look.get('scale', [1, 1, 1])
    hidden = set(look.get('hide', []))
    quads = []
    view = _rot(math.radians(pitch), math.radians(yaw), 0)

    def make_transform(pivot, prot=(0, 0, 0), sub=None):
        R = _rot(*[math.radians(a) for a in prot])

        def tf(p, normal=False):
            if sub is not None:
                p = sub(p, normal)
            q = _mv(R, p)
            if normal:
                v = (q[0] / sc[0], q[1] / sc[1], q[2] / sc[2])
                return _mv(view, v)
            q = (q[0] + pivot[0], q[1] + pivot[1], q[2] + pivot[2])
            q = (q[0] * sc[0], (q[1] - 24) * sc[1] + 24, q[2] * sc[2])
            return _mv(view, (q[0], q[1] - 24, q[2]))
        return tf

    for name, (size, uv1, uv2, pivot, frm, infl) in specs.items():
        tf = make_transform(pivot)
        if name not in hidden:
            _quads_for_box(skin_img, uv1, size, frm, 0, tf, quads)
        if name + '_overlay' not in hidden and 'overlays' not in hidden:
            _quads_for_box(skin_img, uv2, size, frm, infl, tf, quads)
    if extras_img is not None and look.get('extras'):
        for cb in look['extras']['cubes']:
            ppivot = specs[cb['part']][3]
            cp = cb['pivot']
            Rc = _rot(*[math.radians(a) for a in cb['rotation']])

            def sub(p, normal, cp=cp, Rc=Rc):
                if normal:
                    return _mv(Rc, p)
                q = _mv(Rc, (p[0] - cp[0], p[1] - cp[1], p[2] - cp[2]))
                return (q[0] + cp[0], q[1] + cp[1], q[2] + cp[2])
            tf = make_transform(ppivot, sub=sub)
            _quads_for_box(extras_img, cb['uv'], cb['size'], cb['from'], cb.get('inflate', 0), tf, quads)

    W, H = canvas
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    light = (-0.35, -0.8, -0.5)
    ln = math.sqrt(sum(a * a for a in light))
    light = tuple(a / ln for a in light)
    quads.sort(key=lambda q: -sum(p[2] for p in q[0]))
    for pts, c, n in quads:
        if n[2] > 0.05:  # face tournée vers l'arrière
            continue
        k = 0.55 + 0.45 * max(0.0, sum(n[i] * light[i] for i in range(3)))
        col = (int(c[0] * k), int(c[1] * k), int(c[2] * k), 255)
        poly = [(W / 2 + p[0] * px, H - 16 + p[1] * px) for p in pts]
        dr.polygon(poly, fill=col, outline=col)
    return img


def preview_sheet(skin_img, look, extras_img, path, title=''):
    views = [render_preview(skin_img, look, extras_img, yaw=a, pitch=p)
             for a, p in ((0, 0), (-35, -12), (90, 0), (180, 0))]
    W, H = views[0].size
    sheet = Image.new('RGBA', (W * 4 + 64 * 4 + 16, H), (58, 62, 70, 255))
    for i, v in enumerate(views):
        sheet.alpha_composite(v, (i * W, 0))
    sheet.alpha_composite(skin_img.resize((256, 256), Image.NEAREST), (W * 4 + 8, 8))
    if extras_img is not None:
        ew, eh = extras_img.size
        sheet.alpha_composite(extras_img.resize((256, int(256 * eh / ew)), Image.NEAREST), (W * 4 + 8, 272))
    ImageDraw.Draw(sheet).text((6, 4), title, fill='white')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sheet.save(path)
