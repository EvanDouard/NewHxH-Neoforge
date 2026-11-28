#!/usr/bin/env python3
"""Reconstruit le terrain Minecraft du monde HxH (hauteur, biomes) pour le comparer à la carte dessinée dans un viewer web.
Reprend HxHWorldMap.height + HxHBiomeSource : grille, hauteurs de base, déformation des côtes, relief, zones plates, biomes de
région et de lieux. LES BRUITS sont recréés (même fréquences/amplitudes que Minecraft, graines différentes) : l'ensemble
(côtes, massifs, biomes) est fidèle, pas le détail bloc à bloc. Usage : export_world_view.py <dossier de sortie>"""
import glob, json, os, sys
import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
RES = os.path.join(ROOT, 'src/main/resources')
OUT = sys.argv[1] if len(sys.argv) > 1 else '/tmp/viewer'
os.makedirs(OUT, exist_ok=True)
SCALE, CELL, SEA = 20, 640, 63
STEP = 40  # blocs par pixel du rendu de relief et de biomes
HOVER = 160  # blocs par pixel des données de survol

rows = [l.strip() for l in open(f'{RES}/data/hxh/world/cells.txt') if l.strip() and not l.startswith('#')]
H, W = len(rows), max(len(r) for r in rows)
cells = np.array([[r[x] if x < len(r) else '~' for x in range(W)] for r in rows])
land = cells != '~'

def dist(sources):
    from collections import deque
    d = np.full((H, W), 10**9)
    q = deque()
    for z, x in zip(*np.nonzero(sources)):
        d[z, x] = 0; q.append((z, x))
    while q:
        z, x = q.popleft()
        for dz in (-1, 0, 1):
            for dx in (-1, 0, 1):
                a, b = z + dz, x + dx
                if 0 <= a < H and 0 <= b < W and d[a, b] > d[z, x] + 1:
                    d[a, b] = d[z, x] + 1; q.append((a, b))
    return d
# distance à la mer : le bord de la carte compte comme mer (distance 1)
dl = np.full((H, W), 10**9); border = np.zeros((H, W), bool); border[0] = border[-1] = True; border[:, 0] = border[:, -1] = True
from collections import deque
q = deque(); d_l = np.where(~land, 0, 10**9)
for z, x in zip(*np.nonzero(~land)): q.append((z, x))
for z, x in zip(*np.nonzero(land & border)):
    d_l[z, x] = 1; q.append((z, x))
while q:
    z, x = q.popleft()
    for dz in (-1, 0, 1):
        for dx in (-1, 0, 1):
            a, b = z + dz, x + dx
            if 0 <= a < H and 0 <= b < W and d_l[a, b] > d_l[z, x] + 1:
                d_l[a, b] = d_l[z, x] + 1; q.append((a, b))
to_land = dist(land)
target = np.where(land, 64 + np.minimum(d_l, 12) * 2.0, 62 - np.minimum(to_land, 8) * 4.5)

class Perlin:
    """Bruit de Perlin amélioré (y = 0), octaves à la Minecraft : firstOctave, amplitudes."""
    def __init__(self, seed, first, amps):
        rs = np.random.RandomState(seed)
        self.oct = []
        n = len(amps)
        lowest = 2.0 ** (n - 1) / (2.0 ** n - 1)
        for i, a in enumerate(amps):
            p = rs.permutation(256); p = np.concatenate([p, p])
            off = rs.uniform(0, 256, 3)
            self.oct.append((p, off, 2.0 ** (first + i), a * lowest / 2.0 ** i))
    @staticmethod
    def _noise(p, x, z):
        xi = np.floor(x).astype(int); zi = np.floor(z).astype(int)
        xf = x - xi; zf = z - zi; xi &= 255; zi &= 255
        u = xf * xf * xf * (xf * (xf * 6 - 15) + 10); w = zf * zf * zf * (zf * (zf * 6 - 15) + 10)
        def g(h, x_, z_):
            h &= 3
            return np.where(h == 0, x_ + z_, np.where(h == 1, -x_ + z_, np.where(h == 2, x_ - z_, -x_ - z_)))
        aa = p[p[xi] + zi]; ba = p[p[xi + 1] + zi]; ab = p[p[xi] + zi + 1]; bb = p[p[xi + 1] + zi + 1]
        l1 = g(aa, xf, zf) + u * (g(ba, xf - 1, zf) - g(aa, xf, zf))
        l2 = g(ab, xf, zf - 1) + u * (g(bb, xf - 1, zf - 1) - g(ab, xf, zf - 1))
        return (l1 + w * (l2 - l1)) * 0.9
    def get(self, x, z):
        s = 0
        for p, off, f, a in self.oct:
            s = s + a * self._noise(p, x * f + off[0], z * f + off[2]) 
        return s * 1.0

hills = Perlin(1337, -5, [1, 1, .5, .5]); broad = Perlin(4242, -9, [1, 1, .5, .5])
warpx = Perlin(77, -8, [1, 1]); warpz = Perlin(78, -8, [1, 1]); WARP = 110
flats = [tuple(map(float, l.split())) for l in open(f'{RES}/data/hxh/world/flats.txt') if l.strip() and not l.startswith('#')]

def tgt(ix, iz):
    out = np.full(ix.shape, 20.0)
    ok = (ix >= 0) & (iz >= 0) & (ix < W) & (iz < H)
    out[ok] = target[iz[ok], ix[ok]]
    return out
def smooth(t): return t * t * (3 - 2 * t)

def height(X, Z):
    ox, oz = X, Z
    x = ox + warpx.get(ox, oz) * WARP; z = oz + warpz.get(ox, oz) * WARP
    fx = x / CELL - .5; fz = z / CELL - .5
    ix = np.floor(fx).astype(int); iz = np.floor(fz).astype(int)
    tx = smooth(fx - ix); tz = smooth(fz - iz)
    base = (1 - tz) * ((1 - tx) * tgt(ix, iz) + tx * tgt(ix + 1, iz)) + tz * ((1 - tx) * tgt(ix, iz + 1) + tx * tgt(ix + 1, iz + 1))
    n = hills.get(x, z)
    landm = base > SEA
    inland = np.clip((base - SEA - 1.5) / 4.0, 0, 1); mass = np.clip((base - SEA) / 24.0, 0, 1)
    hl = base + n * inland * (2.0 + (base - SEA) * 0.9) + broad.get(x, z) * 18.0 * np.sqrt(mass) + mass * 6.0
    base = np.where(landm, hl, base + n * 2.5)
    for fx_, fz_, r, y in flats:
        d = np.hypot(ox - fx_, oz - fz_)
        w = np.clip((r - d) / (r * .25), 0, 1)
        base = np.where((base >= SEA - 1) & (d < r), base + smooth(w) * (y - base), base)
    return np.maximum(base, -20)

# --- biomes
BIOMES = {  # id: (nom, couleur)
    0: ('deep_ocean', (24, 48, 112)), 1: ('ocean', (44, 84, 170)), 2: ('beach', (234, 220, 160)), 3: ('plains', (141, 179, 96)),
    4: ('taiga', (11, 102, 89)), 5: ('swamp', (7, 249, 178)), 6: ('forest', (5, 102, 33)), 7: ('dark_forest', (64, 81, 26)),
    8: ('jungle', (83, 123, 9)), 9: ('savanna', (189, 178, 95)),
}
REGION = {'m': 4, 'k': 5, 's': 6, 'K': 7, 'o': 8, 'b': 9}
EXTRA = {}  # biomes de lieux (world.json, champ places)
dim = json.load(open(f'{RES}/data/hxh/dimension/world.json'))
places_biome = dim['generator']['biome_source'].get('places', [])
for pb in places_biome:
    name = pb['biome'].split(':')[1]
    if name not in [b[0] for b in BIOMES.values()]:
        BIOMES[10 + len(EXTRA)] = (name, {'windswept_forest': (80, 112, 80), 'badlands': (217, 69, 21), 'old_growth_spruce_taiga': (129, 142, 121),
                                          'bamboo_jungle': (118, 142, 20), 'cherry_grove': (255, 183, 220), 'mangrove_swamp': (103, 115, 57),
                                          'stony_shore': (162, 162, 132)}.get(name, (200, 100, 200)))
        EXTRA[name] = 10 + len(EXTRA) - 0
bid = {v[0]: k for k, v in BIOMES.items()}

def biomes(X, Z, h):
    cx = np.floor_divide(X.astype(int), CELL); cz = np.floor_divide(Z.astype(int), CELL)
    inside = (cx >= 0) & (cz >= 0) & (cx < W) & (cz < H)
    letter = np.full(X.shape, '~'); letter[inside] = cells[cz[inside], cx[inside]]
    out = np.where(letter == '~', np.where(h < 40, 0, 1), 3)
    for ch, b in REGION.items(): out = np.where(letter == ch, b, out)
    best = np.full(X.shape, 1e18); pb = np.full(X.shape, -1)
    for p in places_biome:
        d = (X - p['x']) ** 2 + (Z - p['z']) ** 2
        m = (d <= p['radius'] ** 2) & (d < best)
        best = np.where(m, d, best); pb = np.where(m, bid[p['biome'].split(':')[1]], pb)
    out = np.where((letter != '~') & (pb >= 0), pb, out)
    out = np.where((letter != '~') & (h < SEA + 3), 2, out)
    return out, letter

def render(step, tag):
    w, h_ = W * CELL // step, H * CELL // step
    hm = np.zeros((h_, w), np.float32); bm = np.zeros((h_, w), np.uint8)
    for y0 in range(0, h_, 256):
        ys = np.arange(y0, min(y0 + 256, h_)); X, Z = np.meshgrid((np.arange(w) + .5) * step, (ys + .5) * step)
        hh = height(X, Z); hm[ys[0]:ys[-1] + 1] = hh
        b, _ = biomes(X, Z, hh); bm[ys[0]:ys[-1] + 1] = b
    return hm, bm

hm, bm = render(STEP, 'main')
print('rendu', hm.shape, hm.min(), hm.max())
# biomes PNG
pal = np.zeros((256, 3), np.uint8)
for k, (n, c) in BIOMES.items(): pal[k] = c
Image.fromarray(pal[bm]).save(f'{OUT}/biomes.png', optimize=True)
# relief : teinte par altitude + ombrage
def tint(h):
    stops = [(-20, (10, 25, 80)), (40, (24, 55, 130)), (62, (60, 120, 200)), (64, (70, 140, 210)), (66, (225, 210, 150)), (72, (90, 150, 70)),
             (90, (60, 120, 50)), (110, (140, 120, 80)), (130, (150, 130, 110)), (150, (235, 235, 240))]
    xs = [s[0] for s in stops]; out = np.zeros(h.shape + (3,), np.float32)
    for ch in range(3): out[..., ch] = np.interp(h, xs, [s[1][ch] for s in stops])
    return out
col = tint(hm)
gz, gx = np.gradient(hm)
shade = np.clip(0.5 + (-gx - gz) * 6.0 / np.sqrt(2) * 0.5, 0.25, 1.2)[..., None]
Image.fromarray(np.clip(col * shade, 0, 255).astype(np.uint8)).save(f'{OUT}/relief.jpg', quality=88)
# données de survol (HOVER blocs / pixel)
f = HOVER // STEP
hh = hm[::f, ::f]; bb = bm[::f, ::f]
open(f'{OUT}/heights.bin', 'wb').write(np.round(hh).astype('<i2').tobytes())
open(f'{OUT}/biomes.bin', 'wb').write(bb.astype(np.uint8).tobytes())
# carte dessinée
Image.open(f'{RES}/assets/hxh/textures/gui/world_map.png').convert('RGB').save(f'{OUT}/drawn.png', optimize=True)
# écarts entre la grille de jeu (cells.txt) et la carte dessinée (regions.txt) : un pixel de la carte = 1/6 de case
reg = [l.strip() for l in open(f'{RES}/assets/hxh/world/regions.txt') if l.strip() and not l.startswith('#')]
dif = np.zeros((H * 6, W * 6, 4), np.uint8); n_game_sea = n_game_land = 0
for z in range(H):
    for x in range(W):
        g = cells[z, x] != '~'; q = x < len(reg[z]) and reg[z][x] != '~'
        if g != q:
            dif[z * 6:(z + 1) * 6, x * 6:(x + 1) * 6] = (255, 40, 200, 200) if q else (255, 170, 0, 200)
            if q: n_game_sea += 1
            else: n_game_land += 1
Image.fromarray(dif, 'RGBA').save(f'{OUT}/ecarts.png', optimize=True)
print('écarts : terre dessinée mais mer en jeu', n_game_sea, '; terre en jeu mais mer dessinée', n_game_land)
# lieux
places = []
for fpath in sorted(glob.glob(f'{RES}/data/hxh/world_places/*.json')):
    d = json.load(open(fpath)); c = d['clear']
    places.append({'id': os.path.basename(fpath)[:-5], 'x': d['x'], 'z': d['z'], 'w': c[3] - c[0] + 1, 'd': c[5] - c[2] + 1,
                   'stub': len(d.get('fills', [])) <= 6 and len(d.get('blocks', [])) <= 2})
json.dump({'cell': CELL, 'w': W, 'h': H, 'step': STEP, 'hover': HOVER, 'hw': hh.shape[1], 'hh': hh.shape[0], 'sea': SEA,
           'cells': [''.join(r) for r in cells], 'biomes': {k: {'name': v[0], 'color': '#%02x%02x%02x' % v[1]} for k, v in BIOMES.items()},
           'places': places, 'placeBiomes': places_biome}, open(f'{OUT}/data.json', 'w'))
print('ok', OUT)
