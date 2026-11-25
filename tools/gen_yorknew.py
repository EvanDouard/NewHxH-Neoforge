#!/usr/bin/env python3
"""Génère data/hxh/world_places/yorknew_city.json (ville côtière : avenues, tours, maison de ventes, entrepôt de la Brigade)."""
import json, random
random.seed(11)
fills, blocks, ents = [], [], []
def F(box, block, mode=None):
    d = {"box": box, "block": block}
    if mode: d["mode"] = mode
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
W, D = 87, 79
F([0, -1, 0, W, -1, D], "minecraft:smooth_stone")
# avenues
H_ROADS = [(18, 21), (50, 53)]
V_ROADS = [(24, 27), (60, 63)]
for a, b in H_ROADS: F([0, -1, a, W, -1, b], "minecraft:black_concrete")
for a, b in V_ROADS: F([a, -1, 0, b, -1, D], "minecraft:black_concrete")
for a, b in H_ROADS:
    for x in range(0, W + 1, 4): Bk(x, -1, (a + b) // 2, "minecraft:white_concrete")
for a, b in V_ROADS:
    for z in range(0, D + 1, 4): Bk((a + b) // 2, -1, z, "minecraft:white_concrete")
# îlots
xs = [(2, 22), (29, 58), (65, 85)]
zs = [(2, 16), (23, 48), (55, 77)]
MAT = ["minecraft:gray_concrete", "minecraft:light_gray_concrete", "minecraft:white_concrete", "minecraft:stone_bricks", "minecraft:blue_terracotta", "minecraft:cyan_terracotta"]
GLASS = "minecraft:light_blue_stained_glass"
def tower(x0, z0, x1, z1, h, mat, glass=GLASS):
    F([x0, 0, z0, x1, h, z1], mat)
    for y in range(2, h, 4):
        F([x0, y, z0, x1, y + 1, z0], glass); F([x0, y, z1, x1, y + 1, z1], glass)
        F([x0, y, z0, x0, y + 1, z1], glass); F([x1, y, z0, x1, y + 1, z1], glass)
    F([x0 - 1, h + 1, z0 - 1, x1 + 1, h + 1, z1 + 1], "minecraft:stone_brick_slab")
    cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
    F([cx, h + 2, cz, cx, h + 6, cz], "minecraft:iron_bars"); Bk(cx, h + 7, cz, "minecraft:lightning_rod[facing=up]")
SPECIAL = {(1, 1): "auction", (2, 2): "troupe"}
for i, (x0, x1) in enumerate(xs):
    for j, (z0, z1) in enumerate(zs):
        if (i, j) in SPECIAL: continue
        # 1 ou 2 tours par îlot
        if x1 - x0 > 22 and random.random() < 0.7:
            mid = (x0 + x1) // 2
            tower(x0, z0, mid - 2, z1, random.randint(14, 38), random.choice(MAT))
            tower(mid + 2, z0, x1, z1, random.randint(10, 30), random.choice(MAT))
        else:
            tower(x0, z0, x1, z1, random.randint(12, 40), random.choice(MAT))
# portes des tours (rez-de-chaussée, côté avenue)
for i, (x0, x1) in enumerate(xs):
    for j, (z0, z1) in enumerate(zs):
        if (i, j) in SPECIAL: continue
        cx = (x0 + x1) // 2
        dz = z1 if j < 2 else z0
        F([cx - 1, 0, dz, cx + 1, 2, dz], "minecraft:air")
        F([cx - 1, 3, dz, cx + 1, 3, dz], "minecraft:smooth_quartz")
# maison de ventes (centre)
ax0, ax1, az0, az1 = 29, 58, 23, 48
F([ax0, 0, az0, ax1, 17, az1], "minecraft:quartz_bricks")
F([ax0 - 1, 18, az0 - 1, ax1 + 1, 18, az1 + 1], "minecraft:smooth_quartz_slab")
F([ax0 + 4, 19, az0 + 4, ax1 - 4, 22, az1 - 4], "minecraft:quartz_bricks")
F([ax0 + 3, 23, az0 + 3, ax1 - 3, 23, az1 - 3], "minecraft:smooth_quartz_slab")
for y in (4, 10, 14):
    F([ax0, y, az0, ax1, y + 1, az0], GLASS); F([ax0, y, az1, ax1, y + 1, az1], GLASS)
    F([ax0, y, az0, ax0, y + 1, az1], GLASS); F([ax1, y, az0, ax1, y + 1, az1], GLASS)
# entrée monumentale côté avenue sud (z = az1) : colonnes, escalier, tapis rouge, porte
ex = (ax0 + ax1) // 2
F([ex - 4, 0, az1, ex + 4, 7, az1], "minecraft:air")
for cx in (ex - 6, ex - 3, ex + 3, ex + 6):
    F([cx, 0, az1 + 1, cx, 8, az1 + 1], "minecraft:quartz_pillar[axis=y]")
F([ex - 7, 9, az1, ex + 7, 9, az1 + 2], "minecraft:smooth_quartz")
F([ex - 4, 0, az1 + 2, ex + 4, 0, az1 + 2], "minecraft:quartz_stairs[facing=north]")
F([ex - 1, 0, az1 + 2, ex + 1, 0, az1 + 2], "minecraft:red_carpet")
F([ex - 1, 0, az1 - 8, ex + 1, 0, az1], "minecraft:red_carpet")
# salle des ventes : sol, estrade, sièges
F([ax0 + 1, 0, az0 + 1, ax1 - 1, 15, az1 - 1], "minecraft:air")
F([ax0 + 1, -1, az0 + 1, ax1 - 1, -1, az1 - 1], "minecraft:polished_diorite")
F([ex - 4, 0, az0 + 2, ex + 4, 1, az0 + 5], "minecraft:dark_oak_planks")
Bk(ex, 2, az0 + 3, "minecraft:lectern[facing=south]")
for r in range(10):
    for c in range(-10, 11):
        if abs(c) < 2: continue
        Bk(ex + c, 0, az0 + 9 + r * 1 if False else az0 + 9 + r, "minecraft:red_wool") if False else None
for r in range(8):
    for c in list(range(-11, -1)) + list(range(2, 12)):
        Bk(ex + c, 0, az0 + 8 + r * 2, "minecraft:dark_oak_stairs[facing=north]")
for lx in (ax0 + 8, ex, ax1 - 8):
    for lz in (az0 + 8, az0 + 20):
        Bk(lx, 14, lz, "minecraft:lantern[hanging=true]")
# entrepôt abandonné : repaire de la Brigade Fantôme (coin sud-est)
tx0, tx1, tz0, tz1 = 65, 85, 55, 77
F([tx0, 0, tz0, tx1, 9, tz1], "minecraft:cracked_stone_bricks", "walls")
F([tx0, 10, tz0, tx1, 10, tz1], "minecraft:stone_brick_slab")
F([tx0 + 1, -1, tz0 + 1, tx1 - 1, -1, tz1 - 1], "minecraft:cobblestone")
F([tx0 + 8, 0, tz0, tx0 + 12, 6, tz0], "minecraft:air")           # grande porte éventrée
F([tx0, 3, tz0 + 6, tx0, 6, tz0 + 9], "minecraft:air")            # brèche
F([tx1, 5, tz0 + 12, tx1, 8, tz0 + 16], "minecraft:air")
F([tx0 + 3, 11, tz0 + 3, tx1 - 3, 11, tz1 - 3], "minecraft:air")
cxm, czm = (tx0 + tx1) // 2, (tz0 + tz1) // 2
import math
for k in range(13):                                               # 13 sièges en cercle : la Brigade Fantôme
    a = 2 * math.pi * k / 13
    sx, sz = round(cxm + 5 * math.cos(a)), round(czm + 5 * math.sin(a))
    face = ["north", "east", "south", "west"][int(((math.degrees(a) + 180 + 45) % 360) // 90)]
    Bk(sx, 0, sz, f"minecraft:dark_oak_stairs[facing={face}]")
Bk(cxm, 0, czm, "minecraft:campfire[lit=true]")
for (x, z) in ((tx0 + 3, tz0 + 3), (tx1 - 3, tz1 - 3), (tx0 + 4, tz1 - 4)):
    Bk(x, 0, z, "minecraft:cobweb"); Bk(x, 1, z, "minecraft:cobweb")
for (x, z) in ((tx0 + 2, tz0 + 8), (tx1 - 2, tz1 - 10), (cxm + 8, czm - 6)): Bk(x, 0, z, "minecraft:mossy_cobblestone")
# place du dirigeable (la borne d'arrivée : x/z de la destination, 695/3915 → relatif 45, 31 pour l'ancre 650/3884)
F([43, -1, 29, 47, -1, 33], "minecraft:stone_bricks")
F([43, 0, 29, 47, 3, 33], "minecraft:air")
Bk(47, 0, 31, "minecraft:air")
blocks.append({"pos": [47, 0, 31], "state": "hxh:airship_terminal[facing=west]"})
# lampadaires le long des avenues
for a, b in H_ROADS:
    for x in range(2, W, 8):
        for z in (a - 1, b + 1):
            if not (43 <= x <= 47 and 29 <= z <= 33):
                Bk(x, 0, z, "minecraft:stone_brick_wall"); Bk(x, 1, z, "minecraft:stone_brick_wall"); Bk(x, 2, z, "minecraft:lantern[hanging=false]")
for a, b in V_ROADS:
    for z in range(2, D, 8):
        for x in (a - 1, b + 1):
            Bk(x, 0, z, "minecraft:stone_brick_wall"); Bk(x, 1, z, "minecraft:stone_brick_wall"); Bk(x, 2, z, "minecraft:lantern[hanging=false]")
# PNJ
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": "{npc:\"%s\",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}" % i})
npc("commissaire_priseur", ex, 2, az0 + 4, 0)
npc("passant_yorknew", 40, 0, 20, 90); npc("passant_yorknew", 56, 0, 52, 270)
place = {"x": 650, "z": 3884, "clear": [-3, 0, -3, W + 3, 46, D + 3], "fills": fills, "blocks": blocks, "entities": ents}
json.dump(place, open("src/main/resources/data/hxh/world_places/yorknew_city.json", "w"), indent=1)
print(len(fills), len(blocks), len(ents))
