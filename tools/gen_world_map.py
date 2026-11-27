#!/usr/bin/env python3
"""Génère la carte du dirigeable : texture propre (assets/hxh/textures/gui/world_map.png) et grille des régions nettoyée
(assets/hxh/world/regions.txt, lue par le client pour le survol). Source : data/hxh/world/cells.txt (non modifiée).
Usage : python3 tools/gen_world_map.py"""
import numpy as np
from collections import deque
from PIL import Image, ImageFilter

CELLS = 'src/main/resources/data/hxh/world/cells.txt'
OUT_TXT = 'src/main/resources/assets/hxh/world/regions.txt'
OUT_PNG = 'src/main/resources/assets/hxh/textures/gui/world_map.png'
PX = 6          # pixels par case dans la texture finale
SS = 3          # sur-échantillonnage pour des côtes lisses

LETTERS = 'mksKobprhwen'  # six régions + République de Padokea (p) + États de l'Union de Mitene (n NGL, r Rokario, h Hass, w Gorteau-Ouest, e Gorteau-Est)
rows = [r for r in open(CELLS).read().split('\n') if r]
H, W = len(rows), max(len(r) for r in rows)
g = [list(r.ljust(W, '~')) for r in rows]

def comps(pred):
    seen = np.zeros((H, W), bool); out = []
    for z in range(H):
        for x in range(W):
            if seen[z, x] or not pred(g[z][x]): continue
            q = deque([(x, z)]); seen[z, x] = True; cs = []
            while q:
                cx, cz = q.popleft(); cs.append((cx, cz))
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
                    nx, nz = cx + dx, cz + dz
                    if 0 <= nx < W and 0 <= nz < H and not seen[nz, nx] and pred(g[nz][nx]):
                        seen[nz, nx] = True; q.append((nx, nz))
            out.append(cs)
    return out

# 0. Un trou de 1 à 2 cases de terre libre ou de mer entre deux cases de la même région est comblé
for _ in range(2):
    for z in range(H):
        for x in range(W):
            if g[z][x] in LETTERS: continue
            for gap in (1, 2):
                for dx, dz in ((1, 0), (0, 1)):
                    ax, az, bx, bz = x - dx, z - dz, x + dx * gap, z + dz * gap
                    if 0 <= ax < W and 0 <= bx < W and 0 <= az < H and 0 <= bz < H:
                        a, b = g[az][ax], g[bz][bx]
                        if a == b and a in LETTERS and all(g[z + dz * i][x + dx * i] in '.~' for i in range(gap)):
                            for i in range(gap): g[z + dz * i][x + dx * i] = a
# 1. Les petits morceaux isolés d'une lettre redeviennent de la terre libre
for letter in 'mksKobp':
    for small in comps(lambda c, L=letter: c == L):
        if len(small) < (25 if letter == 'o' else 100):  # Ochima est un archipel : ses îles restent
            for x, z in small: g[z][x] = '.'

# 2. Les poches de terre libre enclavées dans une région (ou petites) sont absorbées par la région voisine majoritaire
for cs in comps(lambda c: c == '.'):
    border = {}
    sea = False
    for x, z in cs:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                nx, nz = x + dx, z + dz
                if 0 <= nx < W and 0 <= nz < H:
                    c = g[nz][nx]
                    if c == '~': sea = True
                    elif c != '.': border[c] = border.get(c, 0) + 1
    if border and (len(cs) <= 60 or not sea):
        best = max(border, key=border.get)
        for x, z in cs: g[z][x] = best

# 3. Les îlots de moins de 3 cases disparaissent
for cs in comps(lambda c: c != '~'):
    if len(cs) < 3:
        for x, z in cs: g[z][x] = '~'

# 4. Lissage : une case prend la lettre majoritaire de ses 8 voisins si elle est en minorité écrasante
for _ in range(2):
    new = [r[:] for r in g]
    for z in range(H):
        for x in range(W):
            cnt = {}
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    if dx == 0 and dz == 0: continue
                    nx, nz = x + dx, z + dz
                    c = g[nz][nx] if 0 <= nx < W and 0 <= nz < H else '~'
                    cnt[c] = cnt.get(c, 0) + 1
            best = max(cnt, key=cnt.get)
            if cnt[best] >= 6 and best != g[z][x]: new[z][x] = best
    g = new

open(OUT_TXT, 'w').write('\n'.join(''.join(r) for r in g) + '\n')

# 5. Texture
SEA = (38, 78, 140); SEA_DEEP = (28, 62, 118)
LAND = (196, 184, 140)
TINT = {'m': (132, 178, 206), 'k': (160, 130, 184), 's': (196, 104, 92), 'K': (222, 190, 96), 'o': (110, 170, 108), 'b': (222, 148, 84),
        'p': (88, 176, 168), 'n': (124, 196, 122), 'r': (224, 138, 200), 'h': (200, 216, 96), 'w': (154, 106, 208), 'e': (232, 154, 80)}
big = np.zeros((H * PX * SS, W * PX * SS, 3), np.uint8)
mask = np.zeros((H, W), np.uint8)
for z in range(H):
    for x in range(W): mask[z, x] = 255 if g[z][x] != '~' else 0
m = Image.fromarray(mask).resize((W * PX * SS, H * PX * SS), Image.NEAREST).filter(ImageFilter.GaussianBlur(PX * SS * 0.55))
land = np.array(m) > 127
# dégradé de mer : plus sombre loin des côtes
dist = np.array(Image.fromarray((land * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(PX * SS * 6))).astype(float) / 255
sea_img = np.zeros_like(big, float)
for i in range(3): sea_img[..., i] = SEA[i] + (SEA_DEEP[i] - SEA[i]) * (1 - np.clip(dist * 3, 0, 1))
big[:] = sea_img.astype(np.uint8)
big[land] = LAND
for letter, col in TINT.items():
    rm = np.zeros((H, W), np.uint8)
    for z in range(H):
        for x in range(W):
            if g[z][x] == letter: rm[z, x] = 255
    rimg = Image.fromarray(rm).resize((W * PX * SS, H * PX * SS), Image.NEAREST).filter(ImageFilter.GaussianBlur(PX * SS * 0.55))
    sel = (np.array(rimg) > 127) & land
    big[sel] = col
# liseré côtier
edge = land & ~(np.array(Image.fromarray((land * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(2 * SS + 1))) > 127)
big[edge] = (120, 108, 78)
img = Image.fromarray(big).resize((W * PX, H * PX), Image.LANCZOS)
img.save(OUT_PNG, optimize=True)
print('ok', img.size)

# 6. Seconde carte (« Continent Noir connu ») : le monde connu au centre d'un lac (Mobius), le Continent Noir au sud et à l'est.
#    Purement visuel : les limites du Continent Noir ne sont pas connues. Même style, sur une toile plus grande ; le monde y est
#    posé en (DX, DY). Les mêmes constantes sont dans AirshipMapScreen.
OUT_DARK = 'src/main/resources/assets/hxh/textures/gui/world_map_dark.png'
CW, CH, DX, DY = 1944, 1446, 90, 60
rng = np.random.default_rng(27)
def fractal(w, h):
    acc = np.zeros((h, w)); amp = 1.0; tot = 0
    for cells_ in (6, 12, 24, 48, 96):
        n = rng.random((cells_ * h // w + 2, cells_ + 2))
        acc += amp * np.array(Image.fromarray((n * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)).astype(float) / 255
        tot += amp; amp *= 0.5
    return acc / tot
yy, xx = np.mgrid[0:CH, 0:CW].astype(float)
def ell(cx, cy, rx, ry): return 1 - (((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2)
shape = np.maximum.reduce([ell(1900, 880, 280, 700), ell(1050, 1440, 760, 190), ell(1720, 1330, 330, 260)])
dark_land = (shape + (fractal(CW, CH) - 0.5) * 0.9) > 0.18
# garde une mer entre le monde connu et le Continent Noir
dark_land[max(0, DY - 40):DY + H * PX + 70, max(0, DX - 40):DX + W * PX + 60] &= False
dl = Image.fromarray((dark_land * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(5))
dark_land = np.array(dl) > 127
halo = np.array(Image.fromarray((dark_land * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(70))).astype(float) / 255
canvas = np.zeros((CH, CW, 3), float)
for i in range(3): canvas[..., i] = SEA_DEEP[i] * (1 - 0.45 * np.clip(halo * 2.2, 0, 1))
inland = np.array(Image.fromarray((dark_land * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(60))).astype(float) / 255
for i, (a, b) in enumerate(zip((78, 58, 88), (34, 24, 46))):
    canvas[..., i][dark_land] = (a + (b - a) * np.clip(inland * 1.6, 0, 1))[dark_land]
stripes = ((xx + yy) % 22 < 3) & dark_land            # hachures de brume : « terre inconnue »
canvas[stripes] = canvas[stripes] * 0.82
edge2 = dark_land & ~(np.array(Image.fromarray((dark_land * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(5))) > 127)
canvas[edge2] = (150, 70, 110)
canvas = canvas.astype(np.uint8)
# on pose le monde connu avec un bord fondu
world = Image.fromarray(canvas).convert('RGB')
fade = Image.new('L', img.size, 255)
fp = fade.load()
for yv in range(img.size[1]):
    for xv in range(img.size[0]):
        d = min(xv, yv, img.size[0] - 1 - xv, img.size[1] - 1 - yv)
        fp[xv, yv] = min(255, int(255 * d / 40))
world.paste(img, (DX, DY), fade)
world.save(OUT_DARK, optimize=True)
print('ok dark', world.size)
