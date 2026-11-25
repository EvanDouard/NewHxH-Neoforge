"""
Branche un personnage modélisé dans le jeu (idempotent) : type d'entité CharacterEntity, œuf d'apparition, onglet,
rendu GeckoLib, mob d'exemple (stats / Nen / kit), traductions, GameTest, captures CI (6 angles + expression).
Usage : python3 integrate.py <id>  (les données sont dans CHARACTERS ci-dessous)
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
B = os.path.join(ROOT, 'src/main/java/com/evandouard/hxh/')
R = os.path.join(ROOT, 'src/main/resources/')

# id: (nom FR, nom EN, largeur, hauteur, couleur œuf 1, couleur œuf 2, stats, niveau Nen, maîtrise, kit)
CHARACTERS = {}


def add(cid, fr, en, w, h, c1, c2, stats, nen, mastery, kit=None):
    CHARACTERS[cid] = (fr, en, w, h, c1, c2, stats, nen, mastery, kit or cid)


add('netero', 'Isaac Netero', 'Isaac Netero', 0.6, 1.6, 0xF2F0EA, 0x2B2B2B, {"all": 85, "speed": 95}, 100, 95)
add('meruem', 'Meruem', 'Meruem', 0.65, 2.0, 0x4A7A3A, 0xE8D7A0, {"all": 95}, 100, 95)
add('neferpitou', 'Neferpitou', 'Neferpitou', 0.6, 1.8, 0xF2F2F4, 0x6E5A8E, {"all": 85, "agility": 95}, 95, 90)
add('shaiapouf', 'Shaiapouf', 'Shaiapouf', 0.6, 1.95, 0xE9D27A, 0x2A2A44, {"all": 82, "willpower": 90}, 95, 88)
add('youpi', 'Menthuthuyoupi', 'Menthuthuyoupi', 0.9, 2.4, 0x3B4A8A, 0x9E3A3A, {"all": 85, "strength": 98}, 92, 80)
add('chrollo', 'Chrollo Lucilfer', 'Chrollo Lucilfer', 0.62, 1.9, 0x1A1A1E, 0xEDEDED, {"all": 80}, 92, 90)
add('uvogin', 'Uvogin', 'Uvogin', 0.85, 2.3, 0x6B4A2E, 0x2A2A2A, {"all": 72, "strength": 95, "endurance": 95}, 80, 70)
add('illumi', 'Illumi Zoldyck', 'Illumi Zoldyck', 0.62, 1.95, 0x111114, 0x6A5A8A, {"all": 78, "speed": 85}, 88, 85)
add('zeno', 'Zeno Zoldyck', 'Zeno Zoldyck', 0.6, 1.6, 0xEDEDED, 0x7A6A4A, {"all": 85}, 95, 92)
add('biscuit', 'Biscuit Krueger', 'Biscuit Krueger', 0.55, 1.4, 0xF2C94C, 0xE87AA4, {"all": 80, "strength": 90}, 92, 92)
add('feitan', 'Feitan Portor', 'Feitan Portor', 0.55, 1.55, 0x1C2030, 0xECEBE4, {"all": 78, "speed": 92, "agility": 90}, 85, 82)
add('nobunaga', 'Nobunaga Hazama', 'Nobunaga Hazama', 0.6, 1.85, 0xE6DCB0, 0x3A5A8A, {"all": 78, "strength": 85}, 82, 80)
add('machi', 'Machi Komacine', 'Machi Komacine', 0.55, 1.75, 0xC23A72, 0x2C3266, {"all": 74, "agility": 85}, 82, 85)
add('silva', 'Silva Zoldyck', 'Silva Zoldyck', 0.75, 2.1, 0xEEF0F6, 0x1E2A44, {"all": 88, "strength": 92}, 94, 90)
add('kite', 'Kite', 'Kite', 0.6, 1.85, 0xF2F2F6, 0x283050, {"all": 76}, 82, 80)
add('kalluto', 'Kalluto Zoldyck', 'Kalluto Zoldyck', 0.5, 1.35, 0x5A3A7A, 0x121218, {"all": 66, "agility": 78}, 72, 70)
add('shalnark', 'Shalnark', 'Shalnark', 0.55, 1.75, 0xC99A3A, 0x7A7448, {"all": 70}, 80, 82)
add('pakunoda', 'Pakunoda', 'Pakunoda', 0.55, 1.85, 0xC8B47A, 0x1C1C22, {"all": 70}, 80, 80)
add('ging', 'Ging Freecss', 'Ging Freecss', 0.6, 1.8, 0x121214, 0xD8CCB0, {"all": 88}, 96, 94)
add('phinks', 'Phinks Magcub', 'Phinks Magcub', 0.6, 1.85, 0x22305A, 0xA8946A, {"all": 76, "strength": 88}, 82, 78)


def rep(p, a, b, once=True):
    s = open(p).read()
    if b.strip() in s:
        return
    assert a in s, (p, a[:60])
    open(p, 'w').write(s.replace(a, b, 1))


def run(cid):
    fr, en, w, h, c1, c2, stats, nen, mastery, kit = CHARACTERS[cid]
    up = cid.upper()
    ents = B + 'registry/ModEntities.java'
    s = open(ents).read()
    if f' {up} = character(' not in s:
        rep(ents, '    /** Kurapika sans le tabard',
            f'    public static final DeferredHolder<EntityType<?>, EntityType<com.evandouard.hxh.mob.CharacterEntity>> {up} = character("{cid}", {w}f, {h}f);\n    /** Kurapika sans le tabard')
        rep(ents, '        event.put(LEORIO.get(),',
            f'        event.put({up}.get(), com.evandouard.hxh.mob.CharacterEntity.createAttributes().build());\n        event.put(LEORIO.get(),')
    items = B + 'registry/ModItems.java'
    if f'{up}_SPAWN_EGG' not in open(items).read():
        rep(items, '    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> KILLUA_SPAWN_EGG =',
            f'''    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> {up}_SPAWN_EGG =
            ITEMS.register("{cid}_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                    ModEntities.{up}, 0x{c1:06X}, 0x{c2:06X}, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> KILLUA_SPAWN_EGG =''')
    tabs = B + 'registry/ModCreativeTabs.java'
    if f'{up}_SPAWN_EGG' not in open(tabs).read():
        rep(tabs, '                        output.accept(ModItems.HISOKA_SPAWN_EGG.get());',
            f'                        output.accept(ModItems.HISOKA_SPAWN_EGG.get());\n                        output.accept(ModItems.{up}_SPAWN_EGG.get());')
        # garder l'ordre d'ajout : la ligne Hisoka reste l'ancre, on insère après la dernière ajoutée
    nc = B + 'client/NenClient.java'
    if f'ModEntities.{up}.get()' not in open(nc).read():
        rep(nc, '                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.HISOKA.get(),',
            f'                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.{up}.get(), com.evandouard.hxh.client.geo.CharacterGeoRenderer::new);\n                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.HISOKA.get(),')
    preset = {"entity": f"hxh:{cid}", "stats": stats, "nen": {"level": nen, "kit": kit, "mastery": mastery}}
    open(R + f'data/hxh/mob_presets/{cid}.json', 'w').write(json.dumps(preset, indent=2, ensure_ascii=False) + '\n')
    open(R + f'assets/hxh/models/item/{cid}_spawn_egg.json', 'w').write('{ "parent": "minecraft:item/template_spawn_egg" }\n')
    langs = {'fr_fr': {f"entity.hxh.{cid}": fr, f"item.hxh.{cid}_spawn_egg": f"Œuf d'apparition : {fr}",
                       f"mob_preset.hxh.{cid}": fr},
             'en_us': {f"entity.hxh.{cid}": en, f"item.hxh.{cid}_spawn_egg": f"{en} Spawn Egg",
                       f"mob_preset.hxh.{cid}": en}}
    for f, kv in langs.items():
        p = R + f'assets/hxh/lang/{f}.json'
        lines = open(p).read().split('\n')
        d = json.loads('\n'.join(lines))
        i = next(i for i, l in enumerate(lines) if '"item.hxh.hisoka_spawn_egg"' in l)
        lines[i + 1:i + 1] = ['  %s: %s,' % (json.dumps(k), json.dumps(v, ensure_ascii=False))
                              for k, v in kv.items() if k not in d]
        open(p, 'w').write('\n'.join(lines))
        json.loads(open(p).read())
    test = B + 'gametest/CharacterMobTests.java'
    s = open(test).read()
    meth = f'{cid}IsInTheGameWithHisKit'
    if meth not in s:
        s = s.rstrip()[:-1] + f'''
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void {meth}(GameTestHelper helper) {{
        CharacterEntity c = helper.spawn(ModEntities.{up}.get(), 1, 1, 1);
        helper.runAfterDelay(5, () -> helper.succeedWhen(() -> {{
            helper.assertTrue("{kit}".equals(HunterStats.get(c).kit()), "Kit de {fr} (" + HunterStats.get(c).kit() + ")");
            helper.assertTrue(!(c instanceof Enemy) && c.getTarget() == null, "Neutre");
            helper.assertTrue(Math.abs(c.getBbHeight() - {h}f) < 0.01f, "Taille");
        }}));
    }}
}}
'''
        open(test, 'w').write(s)
    print('intégré :', cid)


LINEUP_START = '# Série « même style » : alignements de personnages (GeckoLib)'
LINEUP_END = '# Fin de la série « même style »'


def lineups():
    """Captures CI groupées : les personnages par rangées de 5, de face, de profil et de dos (au lieu de 7
    captures par personnage, qui faisaient dépasser le délai du client CI). Remplace les anciens blocs."""
    import re
    sc = os.path.join(ROOT, 'ci/scenario.txt')
    s = open(sc).read()
    for cid, (fr, *_rest) in CHARACTERS.items():
        tag = f'# {fr} (modèle GeckoLib) : 6 angles + expression\n'
        if tag in s:
            i = s.index(tag)
            end = 'cmd gamemode survival\ncmd tp @e[tag=geo_ci] ~ ~-40 ~\n'
            j = s.index(end, i) + len(end)
            s = s[:i] + s[j:]
    if LINEUP_START in s:
        i = s.index(LINEUP_START)
        j = s.index(LINEUP_END, i) + len(LINEUP_END) + 1
        s = s[:i] + s[j:]
    ids = list(CHARACTERS)
    blk = LINEUP_START + '\nhud off\n'
    for g in range(0, len(ids), 5):
        group = ids[g:g + 5]
        n = g // 5 + 1
        span = [max(1.9, 2.4 * CHARACTERS[c][2] + 0.6) for c in group]       # largeur selon la carrure
        xs, x = [], 0.0
        for k, w in enumerate(span):
            x += (w if k else 0)
            xs.append(x)
        mid = (xs[0] + xs[-1]) / 2
        tall = max(CHARACTERS[c][3] for c in group)
        dist = max(4.5, 0.62 * (xs[-1] - xs[0]) + 1.6)
        for suffix, name, rot in (('a', 'face', 180), ('b', 'profil', 90), ('c', 'dos', 0), ('d', 'expression', 180)):
            blk += f'look 0 {round(6 + 2 * (tall - 1.6))}\n'
            for c, x in zip(group, xs):
                extra = ',expression:"froid"' if name == 'expression' else ''
                blk += (f'cmd summon hxh:npc ~{mid - x:.1f} ~ ~{dist:.1f} {{npc:"{c}",NoAI:1b,Silent:1b,'
                        f'Rotation:[{rot}f,0f],Tags:["geo_ci"]{extra}}}\n')
            blk += f'wait 30\nclearchat\nshot 67{chr(96 + n)}{suffix}_serie_{"_".join(group)}_{name}\n'
            blk += 'cmd tp @e[tag=geo_ci] ~ ~-40 ~\n'
    blk += LINEUP_END + '\n'
    anchor = "# Gon et Killua en mobs (œufs d'apparition)"
    s = s.replace(anchor, blk + anchor, 1)
    open(sc, 'w').write(s)


if __name__ == '__main__':
    for cid in sys.argv[1:]:
        run(cid)
    lineups()
