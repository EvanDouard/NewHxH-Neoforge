#!/usr/bin/env python3
"""Génère data/hxh/world_places/heavens_arena.json : la Tour Céleste et sa place commerçante."""
import json
fills, blocks, ents = [], [], []
def F(b, s, m=None):
    d = {"box": b, "block": s}
    if m: d["mode"] = m
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
W, D = 90, 92
STONE, TRIM, GL = "minecraft:stone_bricks", "minecraft:polished_andesite", "minecraft:glass_pane"
F([0, -1, 0, W, -1, D], "minecraft:stone_bricks")
F([0, -1, 41, W, -1, D], "minecraft:smooth_stone")
for x in range(0, W + 1, 6):
    F([x, -1, 41, x, -1, D], "minecraft:stone_brick_slab") if False else None
# tour : 3 étages en retrait
tiers = [(21, 2, 59, 40, 0, 30), (26, 7, 54, 35, 31, 58), (31, 12, 49, 30, 59, 84)]
for x0, z0, x1, z1, y0, y1 in tiers:
    F([x0, y0, z0, x1, y1, z1], STONE, "walls")
    F([x0, y1 + 1, z0, x1, y1 + 1, z1], TRIM)
    for y in range(y0 + 4, y1, 5):
        for (a, b, c, d) in ((x0, z0, x1, z0), (x0, z1, x1, z1), (x0, z0, x0, z1), (x1, z0, x1, z1)):
            F([a, y, b, c, y + 1, d], GL)
    for y in range(y0, y1 + 1, 10):
        F([x0 - 1, y, z0 - 1, x1 + 1, y, z1 + 1], TRIM) if y > 0 else None
    for (a, b) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        F([a, y0, b, a, y1 + 3, b], "minecraft:chiseled_stone_bricks")
F([34, 85, 22, 46, 85, 30], TRIM)
F([40, 86, 26, 40, 98, 26], "minecraft:iron_bars"); Bk(40, 99, 26, "minecraft:lightning_rod[facing=up]")
# planchers intérieurs
for y in (10, 20, 30):
    F([22, y, 3, 58, y, 39], "minecraft:dark_oak_planks")
F([22, 31, 3, 58, 31, 39], "minecraft:air") if False else None
# hall d'accueil au rez-de-chaussée
F([22, 0, 3, 58, 9, 39], "minecraft:air")
F([22, -1, 3, 58, -1, 39], "minecraft:polished_diorite")
# entrée sud
F([37, 0, 40, 43, 6, 40], "minecraft:air")
F([36, 7, 40, 44, 7, 41], TRIM)
for cx in (35, 45): F([cx, 0, 41, cx, 7, 41], "minecraft:quartz_pillar[axis=y]")
F([38, -1, 41, 42, -1, 52], "minecraft:red_carpet") if False else None
F([39, 0, 41, 41, 0, 55], "minecraft:red_carpet")
F([37, 0, 42, 43, 0, 42], "minecraft:stone_brick_stairs[facing=south]") if False else None
# ring central (Arène de combat) : sable + bordure
F([33, -1, 14, 47, -1, 28], "minecraft:sand")
F([32, 0, 13, 48, 0, 13], "minecraft:quartz_slab"); F([32, 0, 29, 48, 0, 29], "minecraft:quartz_slab")
F([32, 0, 13, 32, 0, 29], "minecraft:quartz_slab"); F([48, 0, 13, 48, 0, 29], "minecraft:quartz_slab")
for r in range(3):
    F([30 - r, r, 11 - r, 50 + r, r, 11 - r], "minecraft:stone_brick_stairs[facing=south]")
    F([30 - r, r, 31 + r, 50 + r, r, 31 + r], "minecraft:stone_brick_stairs[facing=north]")
    F([30 - r, r, 11 - r, 30 - r, r, 31 + r], "minecraft:stone_brick_stairs[facing=east]")
    F([50 + r, r, 11 - r, 50 + r, r, 31 + r], "minecraft:stone_brick_stairs[facing=west]")
# guichet d'inscription
F([30, 0, 34, 50, 1, 34], "minecraft:dark_oak_planks"); F([30, 2, 34, 50, 2, 34], "minecraft:dark_oak_slab")
Bk(40, 3, 35, "minecraft:lectern[facing=south]")
for x in range(24, 57, 8): Bk(x, 8, 6, "minecraft:lantern[hanging=true]"); Bk(x, 8, 36, "minecraft:lantern[hanging=true]")
# escalier d'accès aux étages (symbolique) : colonne d'escalier dans le coin
for i in range(9): F([23 + i, i, 4, 23 + i, i, 5], "minecraft:stone_brick_stairs[facing=east]")
# place pavée, fontaine, bornes, boutiques
F([36, -1, 52, 44, -1, 60], "minecraft:polished_andesite")
F([39, 0, 55, 41, 0, 57], "minecraft:stone_bricks"); F([40, 1, 56, 40, 3, 56], "minecraft:stone_brick_wall"); Bk(40, 4, 56, "minecraft:water")
F([40, 0, 56, 40, 0, 56], "minecraft:water") if False else None
Bk(40, 0, 45, "minecraft:air")
blocks.append({"pos": [40, 0, 45], "state": "hxh:airship_terminal[facing=north]"})
shop_mat = ["minecraft:orange_terracotta", "minecraft:white_terracotta", "minecraft:red_terracotta", "minecraft:yellow_terracotta"]
for i, x0 in enumerate((2, 14, 56, 68)):
    for j, z0 in enumerate((52, 72)):
        m = shop_mat[(i + j) % 4]
        F([x0, 0, z0, x0 + 9, 5, z0 + 11], m, "walls")
        F([x0, 6, z0, x0 + 9, 6, z0 + 11], "minecraft:dark_oak_slab")
        F([x0 + 1, 0, z0 + 1, x0 + 8, 5, z0 + 10], "minecraft:air")
        face_z = z0 if x0 < 30 or True else z0
        F([x0 + 4, 0, z0, x0 + 5, 2, z0], "minecraft:air")
        F([x0 + 1, 3, z0, x0 + 2, 4, z0], GL); F([x0 + 7, 3, z0, x0 + 8, 4, z0], GL)
        Bk(x0 + 6, 0, z0 + 6, "minecraft:barrel[facing=up]")
        Bk(x0 + 2, 0, z0 + 9, "minecraft:crafting_table")
        F([x0 + 3, 0, z0 + 2, x0 + 6, 0, z0 + 2], "minecraft:oak_slab")
        Bk(x0 + 2, 5, z0 + 5, "minecraft:lantern[hanging=true]")
for x in range(4, W, 10):
    for z in (47, 66):
        Bk(x, 0, z, "minecraft:stone_brick_wall"); Bk(x, 1, z, "minecraft:stone_brick_wall"); Bk(x, 2, z, "minecraft:lantern[hanging=false]")
for x in range(3, 90, 8):
    Bk(x, 0, 43, "minecraft:potted_oak_sapling") if False else None
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": '{npc:"%s",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}' % i})
npc("receptionniste_arene", 40, 2, 33, 0)
npc("marchand_arene", 6, 0, 58, 180); npc("marchand_arene", 62, 0, 58, 180)
json.dump({"x": 2730, "z": 1400, "clear": [-3, 0, -3, W + 3, 110, D + 3], "fills": fills, "blocks": blocks, "entities": ents}, open("src/main/resources/data/hxh/world_places/heavens_arena.json", "w"), indent=1)
print(len(fills), len(blocks))
