#!/usr/bin/env python3
"""Génère data/hxh/world_places/hunter_exam_hotel.json : l'hôtel de la 5e phase (Kukan'yu), salle du Président."""
import json
fills, blocks, ents = [], [], []
def F(b, s, m=None):
    d = {"box": b, "block": s}
    if m: d["mode"] = m
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
GL = "minecraft:glass_pane"
W, D = 70, 66
F([0, -1, 0, W, -1, D], "minecraft:grass_block")
F([0, -1, 38, W, -1, D], "minecraft:smooth_stone")
# jardin arrière
for x, z in ((4, 6), (66, 6), (4, 30), (66, 30), (10, 34), (60, 34)):
    F([x, 0, z, x, 4, z], "minecraft:oak_log[axis=y]"); F([x - 2, 5, z - 2, x + 2, 7, z + 2], "minecraft:oak_leaves[persistent=true]")
# hôtel : 6 étages, façade blanche
HX0, HX1, HZ0, HZ1, H = 15, 55, 8, 36, 29
F([HX0, 0, HZ0, HX1, H, HZ1], "minecraft:white_concrete", "walls")
for f in range(1, 6):
    y = f * 5
    F([HX0 + 1, y, HZ0 + 1, HX1 - 1, y, HZ1 - 1], "minecraft:dark_oak_planks")
    F([HX0, y + 2, HZ0, HX1, y + 3, HZ0], GL); F([HX0, y + 2, HZ1, HX1, y + 3, HZ1], GL)
    F([HX0, y + 2, HZ0, HX0, y + 3, HZ1], GL); F([HX1, y + 2, HZ0, HX1, y + 3, HZ1], GL)
    # couloir central et chambres (cloisons)
    for x in range(HX0 + 6, HX1, 6): F([x, y + 1, HZ0 + 1, x, y + 4, HZ0 + 9], "minecraft:white_concrete"); F([x, y + 1, HZ1 - 9, x, y + 4, HZ1 - 1], "minecraft:white_concrete")
    for x in range(HX0 + 3, HX1 - 3, 6):
        Bk(x, y + 1, HZ0 + 2, "minecraft:red_bed[facing=north,part=foot]"); Bk(x, y + 1, HZ0 + 1, "minecraft:red_bed[facing=north,part=head]")
F([HX0 - 1, H + 1, HZ0 - 1, HX1 + 1, H + 1, HZ1 + 1], "minecraft:stone_brick_slab")
F([HX0, 0, HZ0, HX0, H, HZ0], "minecraft:quartz_pillar[axis=y]"); F([HX1, 0, HZ0, HX1, H, HZ0], "minecraft:quartz_pillar[axis=y]")
F([HX0, 0, HZ1, HX0, H, HZ1], "minecraft:quartz_pillar[axis=y]"); F([HX1, 0, HZ1, HX1, H, HZ1], "minecraft:quartz_pillar[axis=y]")
# hall d'accueil (rez-de-chaussée)
F([HX0 + 1, 0, HZ0 + 1, HX1 - 1, 4, HZ1 - 1], "minecraft:air")
F([HX0 + 1, -1, HZ0 + 1, HX1 - 1, -1, HZ1 - 1], "minecraft:polished_diorite")
F([32, 0, 36, 38, 4, 36], "minecraft:air")                      # porte d'entrée
F([31, 5, 36, 39, 5, 38], "minecraft:smooth_quartz")
for cx in (30, 40): F([cx, 0, 38, cx, 5, 38], "minecraft:quartz_pillar[axis=y]")
F([33, 0, 37, 37, 0, 50], "minecraft:red_carpet")
# réception
F([28, 0, 28, 42, 1, 28], "minecraft:dark_oak_planks"); F([28, 2, 28, 42, 2, 28], "minecraft:dark_oak_slab")
Bk(35, 3, 30, "minecraft:lectern[facing=south]") if False else Bk(35, 3, 29, "minecraft:lectern[facing=south]")
for x in range(18, 54, 6): Bk(x, 4, 12, "minecraft:lantern[hanging=true]"); Bk(x, 4, 30, "minecraft:lantern[hanging=true]")
# salons du hall : fauteuils
for (x, z) in ((19, 14), (19, 18), (51, 14), (51, 18)):
    Bk(x, 0, z, "minecraft:red_wool"); Bk(x + (1 if x < 30 else -1), 0, z, "minecraft:dark_oak_stairs[facing=east]" if x < 30 else "minecraft:dark_oak_stairs[facing=west]")
# SALLE DU PRÉSIDENT : pavillon japonais à l'arrière, tatamis, table basse, rideaux
PX0, PX1, PZ0, PZ1 = 24, 46, -14, 6
F([PX0, 0, PZ0, PX1, 6, PZ1], "minecraft:spruce_planks", "walls")
F([PX0 - 1, 7, PZ0 - 1, PX1 + 1, 7, PZ1 + 1], "minecraft:dark_oak_slab")
F([PX0 - 2, 8, PZ0 - 2, PX1 + 2, 8, PZ1 + 2], "minecraft:dark_oak_stairs[facing=south]") if False else None
F([PX0, 0, PZ0, PX1, 6, PZ1], "minecraft:air") if False else None
F([PX0 + 1, 0, PZ0 + 1, PX1 - 1, 6, PZ1 - 1], "minecraft:air")
F([PX0 + 1, -1, PZ0 + 1, PX1 - 1, -1, PZ1 - 1], "minecraft:green_carpet") if False else F([PX0 + 1, -1, PZ0 + 1, PX1 - 1, -1, PZ1 - 1], "minecraft:bamboo_planks")
F([PX0 + 1, 0, PZ0 + 1, PX1 - 1, 0, PZ1 - 1], "minecraft:green_carpet")
F([34, 0, 6, 36, 3, 8], "minecraft:air")                          # passage vers le hall (porte coulissante)
F([34, 0, 6, 36, 0, 8], "minecraft:green_carpet")
F([PX0 + 3, 1, PZ0 + 1, PX1 - 3, 3, PZ0 + 1], "minecraft:paper") if False else None
F([29, 0, -10, 41, 0, -4], "minecraft:green_carpet")
F([33, 0, -8, 37, 0, -6], "minecraft:dark_oak_slab[type=bottom]")   # table basse
for x in (31, 39):
    Bk(x, 0, -7, "minecraft:white_carpet")
Bk(35, 0, -9, "minecraft:white_carpet"); Bk(35, 0, -5, "minecraft:white_carpet")
F([PX0 + 2, 1, PZ1 - 1, PX0 + 2, 1, PZ1 - 1], "minecraft:air") if False else None
for x in range(PX0 + 2, PX1 - 1, 4): Bk(x, 5, PZ0 + 2, "minecraft:lantern[hanging=true]")
F([PX0 + 3, 3, PZ0, PX1 - 3, 4, PZ0], "minecraft:glass_pane")
# bonsaï / jardin zen devant le pavillon
F([PX0 - 6, -1, PZ0 - 2, PX0 - 2, -1, PZ0 + 6], "minecraft:sand") if False else None
F([8, -1, -10, 20, -1, 4], "minecraft:sand"); Bk(14, 0, -3, "minecraft:azalea") if False else Bk(14, 0, -3, "minecraft:potted_azalea_bush")
for (x, z) in ((10, -8), (18, 2), (12, 0)): Bk(x, 0, z, "minecraft:mossy_cobblestone")
# place du dirigeable à l'est de la façade : destination (900,1680) → rel (30, 30) pour ancre (870,1650)
Bk(30, 0, 44, "minecraft:air")
Bk(30, 0, 46, "minecraft:air")
F([27, -1, 42, 33, -1, 48], "minecraft:polished_andesite")
blocks.append({"pos": [30, 0, 30], "state": "hxh:airship_terminal[facing=south]"}) if False else None
# la borne est au point d'arrivée (30,30) → on l'ouvre devant la façade
blocks.append({"pos": [32, 0, 46], "state": "hxh:airship_terminal[facing=north]"})
# lampadaires de l'allée
for z in range(40, 64, 6):
    for x in (28, 42): Bk(x, 0, z, "minecraft:stone_brick_wall"); Bk(x, 1, z, "minecraft:stone_brick_wall"); Bk(x, 2, z, "minecraft:lantern[hanging=false]")
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": '{npc:"%s",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}' % i})
npc("netero", 35, 0, -9, 0); npc("beans", 35, 0, -5, 180)
npc("receptionniste_hotel", 35, 0, 31, 0)
json.dump({"x": 870, "z": 1636, "clear": [-3, 0, -17, W + 3, 40, D + 3], "fills": fills, "blocks": blocks, "entities": ents}, open("src/main/resources/data/hxh/world_places/hunter_exam_hotel.json", "w"), indent=1)
print(len(fills), len(blocks))
