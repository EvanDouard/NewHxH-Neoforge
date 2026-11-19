"""
Régénère la partie « dans le jeu » de docs/hatsu-et-techniques.md à partir du code et des fichiers de langue :
principes du Nen, coups de base, familles (clans) et coups secrets, personnages (passifs, techniques, serments),
serments communs, Hatsu des mobs, combattants nommés de la Tour Céleste.

    python3 tools/docs/hatsu_doc.py

Seul le texte entre les balises <!-- DEBUT-GENERE --> et <!-- FIN-GENERE --> est remplacé : le reste du document
(pistes non codées, décisions à prendre…) s'écrit à la main et n'est jamais touché.
"""
from __future__ import annotations

import glob
import json
import os
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
JAVA = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'evandouard', 'hxh')
RES = os.path.join(ROOT, 'src', 'main', 'resources')
DOC = os.path.join(ROOT, 'docs', 'hatsu-et-techniques.md')
START, END = '<!-- DEBUT-GENERE -->', '<!-- FIN-GENERE -->'

FR = json.load(open(os.path.join(RES, 'assets', 'hxh', 'lang', 'fr_fr.json'), encoding='utf-8'))
EN = json.load(open(os.path.join(RES, 'assets', 'hxh', 'lang', 'en_us.json'), encoding='utf-8'))
SRC = {os.path.basename(f)[:-5]: open(f, encoding='utf-8').read()
       for f in glob.glob(os.path.join(JAVA, '**', '*.java'), recursive=True)}

TYPES = {'ENHANCER': 'Renforcement', 'TRANSMUTER': 'Transformation', 'CONJURER': 'Matérialisation',
         'EMITTER': 'Émission', 'MANIPULATOR': 'Manipulation', 'SPECIALIST': 'Spécialisation'}
SHORT = {'EN': 'ENHANCER', 'TR': 'TRANSMUTER', 'CO': 'CONJURER', 'SP': 'SPECIALIST', 'MA': 'MANIPULATOR', 'EM': 'EMITTER'}
BARS = {'prayer': 'Prière', 'pain': 'Douleur', 'turns': 'Tours', 'debt': 'Dette'}
MOBS = {'SKELETON': 'squelette', 'STRAY': 'vagabond', 'BOGGED': 'embourbé', 'CREEPER': 'creeper', 'ZOMBIE': 'zombie',
        'HUSK': 'zombie momifié', 'DROWNED': 'noyé', 'ZOMBIE_VILLAGER': 'zombie villageois', 'SPIDER': 'araignée',
        'CAVE_SPIDER': 'araignée venimeuse', 'ENDERMAN': 'enderman', 'PIG': 'cochon', 'HOGLIN': 'hoglin', 'COW': 'vache',
        'MOOSHROOM': 'champimeuh', 'GOAT': 'chèvre', 'SHEEP': 'mouton', 'LLAMA': 'lama', 'CHICKEN': 'poule',
        'WOLF': 'loup', 'FOX': 'renard', 'CAT': 'chat', 'OCELOT': 'ocelot', 'SLIME': 'slime', 'MAGMA_CUBE': 'cube de magma',
        'FROG': 'grenouille', 'WITCH': 'sorcière', 'EVOKER': 'évocateur', 'IRON_GOLEM': 'golem de fer',
        'RAVAGER': 'ravageur', 'BLAZE': 'blaze', 'GHAST': 'ghast'}


def fr(key, default='—'):
    return FR.get(key, default)


def clean(text):
    return re.sub(r'§.', '', text).replace('\n', ' ').replace('|', '/')


# ---------------------------------------------------------------------------------------------------------------
# Lecture du code


def statements():
    """(Classe, CONSTANTE) -> texte de la déclaration « public static final … = … ; » (jusqu'à la suivante)."""
    out = {}
    for cls, s in SRC.items():
        heads = list(re.finditer(r'public static final [\w.<>]+ (\w+)\s*=', s))
        for i, m in enumerate(heads):
            end = heads[i + 1].start() if i + 1 < len(heads) else len(s)
            out[(cls, m.group(1))] = s[m.end():end]
    return out


STMT = statements()


def const_id(ref, default_cls):
    """« Techniques.GON_ROCK » ou « GON_ROCK » -> identifiant de la technique / du passif (premier texte entre guillemets)."""
    cls, name = ref.split('.') if '.' in ref else (default_cls, ref)
    body = STMT.get((cls, name))
    if body is None:
        return None
    m = re.search(r'"([a-z0-9_]+)"', body)
    return m.group(1) if m else None


def technique_stats():
    """id -> dict(type, coût, recharge en ticks, niveau mini, ressource)."""
    stats = {}
    ctor = re.compile(r'\(\s*"([a-z0-9_]+)",\s*(?:NenType\.)?([A-Z]+)\s*,\s*([^,]+?)\s*,\s*([^,)]+?)\s*[,)]')
    for (cls, name), body in STMT.items():
        if 'register(' not in body.split(';')[0] and 'register(' not in body[:200]:
            continue  # pas une technique (kits, listes…)
        m = ctor.search(body)
        if m and m.group(2) in TYPES:
            tid, typ, cost, cd = m.groups()
        else:
            b = re.search(r'new (?:BaseMove|Move)\(\s*"([a-z0-9_]+)",\s*(\d+)', body)
            if not b:
                continue
            tid, typ, cost, cd = b.group(1), 'PHYSICAL', '0', b.group(2)
        lvl = re.search(r'requiresNenLevel\((\d+)\)', body)
        res = re.search(r'costs\(KitBars\.(\w+)|costs\("(\w+)"', body)
        res_amt = re.search(r'costs\([^,]+,\s*([\w.]+)\)', body)
        stats.setdefault(tid, {
            'type': typ, 'cost': cost, 'cd': cd, 'level': int(lvl.group(1)) if lvl else 0,
            'res': (res.group(1) or res.group(2)).lower() if res else None,
            'res_amt': res_amt.group(1) if res_amt else None, 'cls': cls,
        })
    return stats


STATS = technique_stats()


def secondary_types():
    s = SRC['NenAffinity']
    two = {m.group(1): SHORT[m.group(2)] for m in re.finditer(r'two\("([a-z0-9_]+)",\s*(\w\w)\)', s)}
    phys = set(re.findall(r'PHYSICAL\.add\("([a-z0-9_]+)"\)', s))
    return two, phys


SECOND, PHYSICAL = secondary_types()


def num(expr):
    expr = expr.strip().rstrip('fF')
    try:
        return float(expr)
    except ValueError:
        consts = {'KitBars.PRAYER_MAX': 100, 'KitBars.PAIN_MAX': 100, 'PRAYER_MAX': 100, 'PAIN_MAX': 100}
        return consts.get(expr)


def seconds(ticks_expr):
    v = num(ticks_expr)
    if v is None:
        return ticks_expr
    s = v / 20
    return (f'{s:.1f}'.rstrip('0').rstrip('.') + ' s').replace('.', ',')


def type_label(tid, st):
    if st['type'] == 'PHYSICAL' or tid in PHYSICAL:
        return 'Physique'
    t = TYPES[st['type']]
    if tid in SECOND:
        t += ' + ' + TYPES[SECOND[tid]]
    return t


def technique_line(tid):
    st = STATS.get(tid)
    name = fr(f'technique.hxh.{tid}', tid)
    en = EN.get(f'technique.hxh.{tid}', '')
    desc = clean(fr(f'technique.hxh.{tid}.desc', ''))
    if st is None:
        return f'| **{name}**{" (" + en + ")" if en and en != name else ""} | ? | ? | ? | — | {desc} |'
    cost = st['cost']
    c = num(cost)
    cost = ('0' if c == 0 else f'{c:g}') if c is not None else cost
    if st['res']:
        amt = num(st['res_amt'] or '0')
        cost += f' + {amt:g} {BARS.get(st["res"], st["res"])}' if amt is not None else f' + {BARS.get(st["res"], st["res"])}'
    lvl = f'Nen {st["level"]}' if st['level'] else '—'
    ult = ' ★' if st['level'] >= 30 else ''
    label = f'**{name}**{ult}' + (f' <br>*{en}*' if en and en != name else '')
    return f'| {label} | {type_label(tid, st)} | {cost} | {seconds(st["cd"])} | {lvl} | {desc} |'


TABLE_HEAD = '| Technique | Type | Aura | Recharge | Niveau requis | Ce qu\'elle fait |\n|---|---|---|---|---|---|'


# ---------------------------------------------------------------------------------------------------------------
# Sections


def kits():
    s = SRC['HatsuKits']
    out = []
    for m in re.finditer(r'playable\("(\w+)",\s*(\w+),\s*List\.of\(([^)]*)\)((?:\s*,\s*[\w.]+)*)\)', s):
        kid, typ, passives, techs = m.groups()
        p_ids = [const_id(p.strip(), 'Passives') for p in passives.split(',') if p.strip()]
        t_ids = [const_id(t.strip(), 'Techniques') for t in techs.split(',') if t.strip()]
        out.append((kid, typ, p_ids, t_ids))
    return out


def wild_aura():
    m = re.search(r'WILD_AURA = java\.util\.Set\.of\(([^)]*)\)', SRC['HatsuKits'])
    return set(re.findall(r'"(\w+)"', m.group(1))) if m else set()


def aura_colors():
    return {k: v for k, v in re.findall(r'entry\("(\w+)",\s*0x([0-9A-Fa-f]+)\)', SRC['HatsuKits'])}


GROUPS = [
    ('Premier lot (un par type, puis Netero et Hisoka)', ['gon', 'killua', 'kurapika', 'neferpitou', 'illumi', 'leorio', 'netero', 'hisoka']),
    ('Association Hunter et examinateurs', ['pariston', 'ging', 'cheadle', 'botobai', 'biscuit', 'menchi', 'bushidora', 'tsezguerra']),
    ('Famille Zoldyck', ['silva', 'zeno', 'kalluto']),
    ('Brigade Fantôme', ['chrollo', 'feitan', 'nobunaga', 'phinks', 'uvogin', 'franklin', 'shalnark', 'machi', 'shizuku', 'pakunoda']),
    ('York Shin et Greed Island', ['neon', 'razor', 'genthru']),
    ('Hunters de l\'arc des Fourmis', ['kite', 'knov', 'knuckle', 'morel', 'palm']),
    ('Fourmis-Chimères', ['meruem', 'shaiapouf', 'youpi']),
    ('Autres', ['zushi', 'senritsu', 'wing']),
]


def section_characters(lines):
    all_kits = {k[0]: k for k in kits()}
    wild, colors = wild_aura(), aura_colors()
    vows = {m.group(2): m.group(1) for m in re.finditer(r'new Def\("(\w+)",\s*"(\w+)"', SRC['CharacterVows'])}
    placed = set()
    groups = [(g, [k for k in ids if k in all_kits]) for g, ids in GROUPS]
    rest = [k for k in all_kits if k not in {x for _, ids in GROUPS for x in ids}]
    if rest:
        groups.append(('Autres personnages', rest))
    lines.append(f'## 4. Personnages jouables ({len(all_kits)})\n')
    lines.append('Choisis une fois pour toutes au Hatsu, parmi les personnages de son type (pastilles de tri). '
                 'Toutes les techniques marchent aussi pour un mob qui incarne le personnage (déclenchées par son IA). '
                 '★ = ultime (Nen 30 et plus) : jamais pour une copie portée par un mob de Minecraft.\n')
    for g, ids in groups:
        lines.append(f'### {g}\n')
        for kid in ids:
            _, typ, p_ids, t_ids = all_kits[kid]
            placed.add(kid)
            name = fr(f'kit.hxh.{kid}', kid)
            extra = []
            if kid in colors:
                extra.append(f'aura #{colors[kid].upper()}')
            extra.append('aura sauvage (flammes même au repos)' if kid in wild else 'aura calme')
            lines.append(f'#### {name} — {TYPES[typ]}\n')
            lines.append(f'*{", ".join(extra)}*  ')
            kd = FR.get(f'kit.hxh.{kid}.desc')
            if kd:
                lines.append(clean(kd) + '\n')
            else:
                lines.append('')
            for pid in p_ids:
                lines.append(f'- **Passif — {fr("passive.hxh." + pid, pid)}** : {clean(fr("passive.hxh." + pid + ".desc", ""))}')
            if kid in vows:
                vid = vows[kid]
                lines.append(f'- **Serment — {fr("vow.hxh.character." + vid, vid)}** : {clean(fr("vow.hxh.character." + vid + ".desc", ""))}')
            lines.append('')
            lines.append(TABLE_HEAD)
            for tid in t_ids:
                lines.append(technique_line(tid))
            lines.append('')


def section_nen(lines):
    lines.append('## 1. Principes du Nen\n')
    lines.append('Wing enseigne les bases (Ten → In → Ren → Hatsu). Le reste se débloque **tout seul** quand les maîtrises '
                 'requises sont atteintes (onglet « Graphe du Nen » du carnet). In et Zetsu partagent la même maîtrise '
                 '(le Zetsu est un palier de l\'In), de même que Ten et Ken.\n')
    unlock = {'ten': 'enseigné par Wing', 'in': 'enseigné par Wing', 'ren': 'enseigné par Wing',
              'hatsu': 'enseigné par Wing (divination puis choix du personnage)', 'zetsu': 'In 5', 'shu': 'Ten 8',
              'ken': 'Ten 5 + Ren 3', 'en': 'Ten 10 + Ren 8', 'gyo': 'Ren 5',
              'ko': 'Ten 10 + In 10 + Ren 10 + Gyo 5 + Hatsu appris', 'ryu': 'Gyo 15 + Ko 10 + Ken 10 (lien Ken incertain)',
              'heal': 'quête de Netero'}
    lines.append('| Technique | Déblocage (réglages par défaut) | Ce qu\'elle fait |\n|---|---|---|')
    for a in ('ten', 'in', 'zetsu', 'ren', 'ken', 'gyo', 'shu', 'en', 'ko', 'ryu', 'hatsu', 'heal'):
        desc = FR.get(f'detail.hxh.desc.{a}') or FR.get(f'ability.hxh.{a}.desc', '')
        lines.append(f'| **{fr("ability.hxh." + a, a)}** | {unlock[a]} | {clean(desc)} |')
    lines.append('')


def section_base(lines):
    lines.append('## 2. Coups de base (sans Nen)\n')
    lines.append('Disponibles dès le début, gratuits, à la fin de la liste des techniques. Type Physique : 100 % pour '
                 'tout le monde. La recharge baisse avec l\'Endurance.\n')
    lines.append(TABLE_HEAD)
    for tid in re.findall(r'"(basic_\w+)"', re.search(r'List<String> BASE = List\.of\(([^)]*)\)', SRC['Techniques']).group(1)):
        lines.append(technique_line(tid))
    lines.append('')


def section_families(lines):
    lines.append('## 3. Familles (clans) et coups secrets\n')
    lines.append('La famille est tirée à la création du personnage (poids ci-dessous). Chaque famille a ses bonus et '
                 'un **coup secret** sans Nen, placé juste avant les coups de base : il se règle sur les stats, ne peut '
                 'être ni copié par Ging ni volé par Chrollo. Le niveau de clan monte avec l\'entraînement et les coups '
                 'secrets (+30 XP par coup) et renforce les bonus.\n')
    fdir = os.path.join(RES, 'data', 'hxh', 'families')
    fams = []
    for f in sorted(glob.glob(os.path.join(fdir, '*.json'))):
        d = json.load(open(f, encoding='utf-8'))
        fams.append((os.path.basename(f)[:-5], d))
    lines.append('| Famille | Tirage | Bonus | Coup secret |\n|---|---|---|---|')
    for fid, d in fams:
        name = d.get('name', {}).get('fr', fid)
        desc = clean(d.get('description', {}).get('fr', ''))
        mv = d.get('secret_move')
        mv_name = fr(f'technique.hxh.{mv}', mv) if mv else '—'
        lines.append(f'| **{name}** | poids {d.get("weight", "?")} | {desc} | {mv_name} |')
    lines.append('')
    lines.append(TABLE_HEAD)
    for fid, d in fams:
        if d.get('secret_move'):
            lines.append(technique_line(d['secret_move']))
    lines.append('')


def section_vows(lines):
    lines.append('## 5. Serments communs\n')
    lines.append('Accessibles à tous dans l\'écran des serments. Rompre un serment scelle le Hatsu. Les serments propres '
                 'à un personnage sont dans sa fiche (Kurapika, Feitan).\n')
    lines.append('| Serment | Effet |\n|---|---|')
    for k, v in FR.items():
        if k.startswith('vow.hxh.') and not k.endswith('.desc') and '.character.' not in k and k.count('.') == 2:
            lines.append(f'| **{v}** | {clean(FR.get(k + ".desc", ""))} |')
    lines.append('')


def section_mobs(lines):
    lines.append('## 6. Hatsu des mobs\n')
    lines.append('Un mob de Minecraft qui s\'éveille au Nen sans incarner de personnage reçoit le Hatsu de son espèce. '
                 'Jamais proposé au joueur. (Un mob qui reçoit une technique de personnage n\'en a qu\'une copie '
                 'affaiblie, sans ultime.)\n')
    lines.append('| Hatsu | Espèces | Type | Technique | Aura | Recharge | Ce qu\'elle fait |\n|---|---|---|---|---|---|---|')
    for m in re.finditer(r'kit\("(\w+)",\s*(\w+),\s*MobTechniques\.(\w+),\s*([^;]*)\);', SRC['MobKits']):
        kid, typ, tconst, types = m.groups()
        tid = const_id('MobTechniques.' + tconst, 'MobTechniques')
        st = STATS.get(tid, {})
        species = ', '.join(MOBS.get(t, t.lower()) for t in re.findall(r'EntityType\.(\w+)', types))
        lines.append(f'| **{fr("kit.hxh." + kid, kid)}** | {species} | {TYPES[typ]} | {fr("technique.hxh." + tid, tid)} | '
                     f'{st.get("cost", "?")} | {seconds(st.get("cd", "0"))} | {clean(fr("technique.hxh." + tid + ".desc", ""))} |')
    used = {const_id('MobTechniques.' + c, 'MobTechniques') for c in re.findall(r'MobTechniques\.(\w+)', SRC['MobKits'])}
    others = [t for t, st in STATS.items() if st['cls'] == 'MobTechniques' and t not in used]
    if others:
        lines.append('\nAutres techniques de mobs (portées par des mobs particuliers) :\n')
        lines.append(TABLE_HEAD)
        for t in others:
            lines.append(technique_line(t))
    lines.append('')


def section_arena(lines):
    lines.append('## 7. Combattants nommés de la Tour Céleste\n')
    lines.append('Au-delà du 200e étage, selon l\'étage du combat. Leurs Hatsu ne sont pas proposés au joueur.\n')
    s = SRC['ArenaChallengers']
    kits_ = {m.group(1): re.findall(r'(\w+)', m.group(3))
             for m in re.finditer(r'MobKits\.named\("(\w+)",\s*NenType\.(\w+),\s*([^)]*)\)', s)}
    ktype = {m.group(1): m.group(2) for m in re.finditer(r'MobKits\.named\("(\w+)",\s*NenType\.(\w+)', s)}
    for m in re.finditer(r'new Challenger\("(\w+)",\s*"(\w+)",\s*(\d+),\s*(\d+)', s):
        cid, kit, a, b = m.groups()
        name = fr(f'npc.hxh.{cid}', fr(f'kit.hxh.{kit}', cid.capitalize()))
        if kit not in kits_:
            lines.append(f'- **{name}** (étages {a}–{b}) : son vrai kit de personnage (voir sa fiche).')
            continue
        lines.append(f'\n**{name}** — {TYPES[ktype[kit]]}, étages {a}–{b}\n')
        lines.append(TABLE_HEAD)
        for c in kits_[kit]:
            tid = const_id(c, 'ArenaChallengers')
            if tid:
                lines.append(technique_line(tid))
    lines.append('')


def generate():
    lines = []
    total_t = sum(1 for t in STATS)
    lines.append(f'*Généré depuis le code par `tools/docs/hatsu_doc.py` — {total_t} techniques enregistrées. '
                 'Aura = coût d\'aura au lancement ; recharge en secondes ; une technique ratée coûte quand même.*\n')
    section_nen(lines)
    section_base(lines)
    section_families(lines)
    section_characters(lines)
    section_vows(lines)
    section_mobs(lines)
    section_arena(lines)
    # contrôle : toute technique du code apparaît quelque part
    body = '\n'.join(lines)
    missing = [t for t in STATS if fr(f'technique.hxh.{t}', t) not in body]
    if missing:
        lines.append('## Techniques présentes dans le code mais rangées nulle part\n')
        lines.append(TABLE_HEAD)
        for t in missing:
            lines.append(technique_line(t))
        lines.append('')
    return '\n'.join(lines).rstrip() + '\n'


def main():
    text = open(DOC, encoding='utf-8').read() if os.path.exists(DOC) else f'{START}\n{END}\n'
    a, b = text.index(START) + len(START), text.index(END)
    text = text[:a] + '\n' + generate() + text[b:]
    open(DOC, 'w', encoding='utf-8').write(text)
    print('écrit', os.path.relpath(DOC, ROOT))


if __name__ == '__main__':
    main()
