#!/usr/bin/env python3
"""Génère les succès (data/hxh/advancement/*.json) et leurs textes fr/en.
Chaque succès utilise le critère « impossible » : il est accordé par le code (achievement/Achievements.java)."""
import json, os, re
R = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
# id, parent, icône, frame, fr(titre, desc), en(titre, desc)
A = [
 ('root', None, 'minecraft:book', 'task', ('Hunter x Hunter', 'Ton aventure de Hunter commence ici.'), ('Hunter x Hunter', 'Your Hunter adventure starts here.')),
 ('hunter_license', 'root', 'minecraft:paper', 'goal', ('Licence de Hunter', "Obtenir la licence de Hunter."), ('Hunter License', 'Obtain the Hunter License.')),
 ('first_kill', 'root', 'minecraft:iron_sword', 'task', ('Premier sang', 'Vaincre un monstre ou un adversaire.'), ('First Blood', 'Defeat a monster or an opponent.')),
 ('kills_50', 'first_kill', 'minecraft:diamond_sword', 'task', ('Chasseur confirmé', 'Vaincre 50 créatures.'), ('Seasoned Hunter', 'Defeat 50 creatures.')),
 ('kills_500', 'kills_50', 'minecraft:netherite_sword', 'goal', ('Fléau du monde', 'Vaincre 500 créatures.'), ('Scourge of the World', 'Defeat 500 creatures.')),
 ('nen_awakened', 'root', 'minecraft:blaze_powder', 'goal', ('Éveil du Nen', "Éveiller son Nen."), ('Nen Awakening', 'Awaken your Nen.')),
 ('nen_level_10', 'nen_awakened', 'minecraft:glowstone_dust', 'task', ('Nen : niveau 10', 'Atteindre le niveau 10 de Nen.'), ('Nen: Level 10', 'Reach Nen level 10.')),
 ('nen_level_25', 'nen_level_10', 'minecraft:amethyst_shard', 'task', ('Nen : niveau 25', 'Atteindre le niveau 25 de Nen.'), ('Nen: Level 25', 'Reach Nen level 25.')),
 ('nen_level_50', 'nen_level_25', 'minecraft:nether_star', 'goal', ('Nen : niveau 50', 'Atteindre le niveau 50 de Nen.'), ('Nen: Level 50', 'Reach Nen level 50.')),
 ('tower_floor_50', 'root', 'minecraft:stone_bricks', 'task', ('Tour Céleste : étage 50', "Atteindre le 50e étage de l'arène."), ('Heavens Arena: Floor 50', 'Reach floor 50 of the arena.')),
 ('tower_floor_100', 'tower_floor_50', 'minecraft:gold_block', 'task', ('Tour Céleste : étage 100', "Atteindre le 100e étage."), ('Heavens Arena: Floor 100', 'Reach floor 100.')),
 ('tower_floor_200', 'tower_floor_100', 'minecraft:beacon', 'goal', ('Tour Céleste : étage 200', "Atteindre le 200e étage, là où le Nen est de rigueur."), ('Heavens Arena: Floor 200', 'Reach floor 200, where Nen is the rule.')),
 ('tower_top', 'tower_floor_200', 'minecraft:dragon_head', 'challenge', ('Sommet de la Tour', "Atteindre le dernier étage de la Tour Céleste."), ('Top of the Tower', 'Reach the top floor of Heavens Arena.')),
 ('first_craft', 'root', 'minecraft:crafting_table', 'task', ('Artisan', 'Fabriquer un objet du mod.'), ('Craftsman', 'Craft an item from the mod.')),
 ('first_purchase', 'root', 'minecraft:emerald', 'task', ('Premier achat', 'Acheter un objet chez un marchand.'), ('First Purchase', 'Buy an item from a merchant.')),
 ('explorer', 'root', 'minecraft:compass', 'goal', ('Explorateur', 'Découvrir les six régions du monde.'), ('Explorer', 'Discover all six regions of the world.')),
]
REG = [('m', 'Minbo', 'Minbo'), ('k', "Kukan'yu", "Kukan'yu"), ('s', 'Saherta', 'Saherta'), ('K', 'Kakin', 'Kakin'), ('o', 'Ochima', 'Ochima'), ('b', 'Begerosse', 'Begerosse')]
for c, fr, en in REG:
    A.append(('region_' + c, 'explorer', 'minecraft:map', 'task', ('Région : ' + fr, 'Découvrir la région de %s.' % fr), ('Region: ' + en, 'Discover the %s region.' % en)))

out = os.path.join(R, 'data', 'hxh', 'advancement')
os.makedirs(out, exist_ok=True)
for f in os.listdir(out):
    if f.endswith('.json'): os.remove(os.path.join(out, f))
lang = {'fr_fr': {}, 'en_us': {}}
for id_, parent, icon, frame, fr, en in A:
    d = {'display': {'icon': {'id': icon}, 'title': {'translate': 'advancement.hxh.%s.title' % id_},
                     'description': {'translate': 'advancement.hxh.%s.desc' % id_}, 'frame': frame,
                     'show_toast': id_ != 'root', 'announce_to_chat': id_ != 'root', 'hidden': False},
         'criteria': {'done': {'trigger': 'minecraft:impossible'}}}
    if parent: d['parent'] = 'hxh:' + parent
    else: d['display']['background'] = 'minecraft:textures/gui/advancements/backgrounds/stone.png'
    json.dump(d, open(os.path.join(out, id_ + '.json'), 'w'), indent=2, ensure_ascii=False)
    lang['fr_fr']['advancement.hxh.%s.title' % id_] = fr[0]; lang['fr_fr']['advancement.hxh.%s.desc' % id_] = fr[1]
    lang['en_us']['advancement.hxh.%s.title' % id_] = en[0]; lang['en_us']['advancement.hxh.%s.desc' % id_] = en[1]
for f, kv in lang.items():
    p = os.path.join(R, 'assets', 'hxh', 'lang', f + '.json')
    s = open(p, encoding='utf-8').read()
    # retire d'anciennes clés générées, puis ajoute avant l'accolade finale
    s = re.sub(r',\n  "advancement\.hxh\.[^"]*": "(?:[^"\\]|\\.)*"', '', s)
    add = ''.join(',\n  %s: %s' % (json.dumps(k), json.dumps(v, ensure_ascii=False)) for k, v in kv.items())
    i = s.rstrip().rfind('\n}')
    open(p, 'w', encoding='utf-8').write(s[:i] + add + s[i:])
print(len(A), 'succès')
