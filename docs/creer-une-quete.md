# Créer une quête

Une quête = **un fichier JSON** dans `src/main/resources/data/hxh/quests/<nom>.json`.
Aucune ligne de Java à écrire : en jeu, `/reload` la charge, `/hunter quest check` liste les erreurs.

Exemple complet à copier : `data/hxh/quests/exemple_pecheur.json`.

## Squelette

```json
{
  "title": { "fr": "Le vieux pêcheur", "en": "The old fisherman" },
  "start_npc": "pecheur",
  "requires": ["hxh:hunter_exam"],
  "intro": { "fr": "Tu pourrais m'aider ?", "en": "Could you help me?" },
  "npcs": { "pecheur": { "fr": "Vieux pêcheur", "en": "Old fisherman" } },
  "steps": [
    { "id": "fish", "type": "bring", "npc": "pecheur", "item": "minecraft:cod", "count": 3,
      "objective": { "fr": "Rapporter 3 morues", "en": "Bring 3 cod" },
      "dialog": { "fr": "Merci !", "en": "Thanks!" } }
  ]
}
```

| Champ | Rôle |
|---|---|
| `title` | Nom de la quête (bulle, carnet, notifications). |
| `start_npc` | *(optionnel)* PNJ qui donne la quête quand on lui parle. Sans lui : `/hunter quest start hxh:<nom>`, ou du code. |
| `requires` | *(optionnel)* quêtes à avoir terminées avant. |
| `intro` | *(optionnel)* ce que dit le PNJ en donnant la quête. |
| `npcs` | *(optionnel)* nouveaux PNJ : identifiant → nom. Ils apparaissent ensuite dans le cycle du **repère de PNJ** (clic droit). Skin : `assets/hxh/textures/entity/npc/<id>.png` (sinon apparence par défaut). |
| `steps` | Les étapes, dans l'ordre. |

**Textes** : une simple chaîne (`"objective": "Rapporter 3 morues"`) ou `{"fr": …, "en": …}`.
Si une clé existe dans les fichiers de langue (`quest.hxh.<nom>`, `quest.hxh.<nom>.<étape>`,
`dialog.hxh.<nom>.<étape>`, `dialog.hxh.<nom>.intro`, `npc.hxh.<id>`), elle l'emporte sur le texte du JSON.

## Types d'étapes

| `type` | Champs | Se valide quand… |
|---|---|---|
| `talk` | `npc` | on parle au PNJ (il dit `dialog`). |
| `bring` | `npc`, `item`, `count` | on parle au PNJ en ayant les objets (ils sont pris). |
| `kill` | `entity`, `count` | on a tué `count` mobs de ce type (`minecraft:zombie`, `hxh:npc`…). |
| `reach` | `pos` `[x, y, z]`, `radius` (4 par défaut) | on arrive à moins de `radius` blocs du point. |
| `follow` | `npc`, `seconds`, `max_distance` | on reste près du PNJ (marathon de Satotz). |
| `custom` | `note` | **condition pas encore codée** : `note` la décrit, `/hunter quest validate` la valide en attendant. |
| `arena_win`, `arena_floor`, `arena_register_200` | `count` | Tour Céleste (victoires, étage atteint, inscription au 200e). |

**Récompenses** (sur n'importe quelle étape, données quand elle se valide) :
`"reward_jenny": 500`, `"reward_item": "minecraft:diamond"`, `"reward_hunter": true` (licence).

## Les conditions « bizarres » : l'étape `custom`

Pour tout ce qui n'existe pas encore (« pêcher sous l'orage de nuit », « vaincre Hisoka sans Nen »…) :

```json
{ "id": "storm", "type": "custom",
  "note": "Pêcher un poisson pendant un orage, la nuit",
  "objective": { "fr": "Pêcher sous l'orage, de nuit" } }
```

- Le joueur voit `objective` ; le carnet affiche aussi « (à coder : … ) » avec la `note`.
- En test : `/hunter quest validate` valide l'étape en cours de la quête suivie.
- Pour la coder plus tard, il suffit d'appeler `QuestManager.completeCustom(joueur, id_de_la_quête)` au bon moment.

## Commandes utiles

- `/reload` — recharge les quêtes après modification.
- `/hunter quest check` — erreurs (type inconnu, PNJ manquant, objet inexistant…).
- `/hunter quest start hxh:<nom>` · `/hunter quest skip` · `/hunter quest validate` · `/hunter quest reset`.
