# Structures du mod — fiche de revue

Même principe que `modeles-a-refaire.md` : tu passes les structures **une par une** pour les valider ou les modifier.
Pour chacune : `[ ]` à valider · `[~]` à modifier (note à côté) · `[x]` validée. Rien n'est figé : tout est remplaçable en
déposant ton fichier (voir §0). Dernière mise à jour : 28 nov 2026 (monde ×20 : les ancres sont en blocs du monde HxH).

**État** : **Complet** = contenu travaillé · **Premier jet** = bâti en blocs de base, à retravailler · **Minimal** = juste
de quoi exister (volume + porte + borne), prévu pour être remplacé · **Provisoire** = généré par le code en attendant
ton fichier · **Absent** = rien du tout.

## 0. Comment remplacer une structure

| Type | Où est le fichier | Pour le remplacer |
|---|---|---|
| **Lieu du monde HxH** (JSON) | `data/hxh/world_places/<lieu>.json` | Construis en créatif, puis `/hunter place export <nom> <coin1> <coin2>` → `hxh_exports/<nom>.json` dans le dossier du serveur. Copie-le **à la place** du fichier (même nom) ; les clés `x` et `z` donnent l'ancre (coin nord-ouest) : garde celles de l'ancien fichier, ou change-les pour déplacer le lieu. Le lieu est posé une seule fois par monde : nouveau monde (ou zone neuve) pour le revoir. |
| **Structure NBT** (Tour Céleste, Examen) | `data/hxh/structure/<chemin>.nbt` | `/hunter structure save hxh:<chemin> X1 Y1 Z1 X2 Y2 Z2` → `saves/<monde>/generated/hxh/structures/<chemin>.nbt`. Dépose ce fichier dans `data/hxh/structure/` sous le même nom : il passe avant le lieu provisoire. Repères (PNJ, parcours, arrivée…) : voir `examen-constructions.md` et `tour-celeste-constructions.md`. |
| **Destination du dirigeable** | `data/hxh/airship_destinations/<lieu>.json` | Si tu déplaces un lieu, déplace aussi son arrivée (x, z) ; l'écart au lieu est de quelques blocs. |

## 1. Lieux du monde HxH (`world_places`)
x, z = ancre en blocs du monde HxH (monde ×20). Taille = zone dégagée (largeur × hauteur × profondeur).

| # | Fichier / lieu | Ancre (x, z) | Région | Taille | État | Contenu actuel | Validation |
|---|---|---|---|---|---|---|---|
| 1 | `whale_island_village` — Village de l'île de la Baleine | 87 000, 82 680 | île | 71 × 23 × 40 | Premier jet | 3 maisons dont celle de Mito, puits, Navigateur, épicerie, école + cloche, phare 19 blocs, cabane de pêcheur, barque, campement forestier | [ ] |
| 2 | `kukuroo_domaine_zoldyck` — Domaine Zoldyck | 31 200, 16 200 | Padokea / Mimbo (mt Kukuroo) | 93 × 15 × 161 | Premier jet, le plus abouti | rempart + tours, grande porte, porte visiteurs, maison de Zebro, enclos de Mike, manoir 3 étages, majordomes (détails : `kukuroo.md`) | [ ] |
| 3 | `padokea_town` — Ville autour de la Tour Céleste | 52 200, 28 000 | Padokea (terre libre) | 91 × 26 × 96 | Premier jet | rues, bâtiments ; PNJ : chef de gare, infirmière, policier, habitant | [ ] |
| 4 | `heavens_arena` — Place d'arrivée de la Tour | 55 500, 28 900 | Padokea (terre libre) | 17 × 7 × 17 | Minimal | place d'arrivée (borne + dalle) ; la tour est `tour.nbt` (§2) | [ ] |
| 5 | `yorknew_city` — Yorknew | 13 000, 77 680 | Saherta/Yorknew | 94 × 47 × 86 | Premier jet | tours, maison de ventes, entrepôt de la Brigade, décors ; PNJ : commissaire-priseur, 2 passants | [ ] |
| 6 | `mafia_hq` — QG de la mafia | 13 600, 75 800 | Yorknew | 71 × 41 × 86 | Premier jet | bâtiment, PNJ : parrain, 4 gardes | [ ] |
| 7 | `meteor_city` — Meteor City | 32 900, 76 000 | Meteor City | 111 × 57 × 111 | Premier jet | montagnes de déchets, bidonvilles ; PNJ : ferrailleur, 2 habitants | [ ] |
| 8 | `hunter_exam_hotel` — Hôtel de l'Association | 17 400, 32 720 | terre libre | 77 × 41 × 87 | Premier jet | hôtel, PNJ : Netero, Beans, réceptionniste | [ ] |
| 9 | `kakin_palace` — Palais royal de Kakin | 119 000, 48 200 | Kakin | 101 × 35 × 101 | Premier jet | palais, PNJ : chambellan, 4 gardes royaux | [ ] |
| 10 | `black_whale_port` — Port de la Baleine Noire | 107 700, 42 600 | bord de mer (près de Kakin) | 83 × 31 × 63 | Premier jet | quais, navire, PNJ : capitaine, docker | [ ] |
| 11 | `kite_forest` — Forêt de Kite | 128 000, 44 800 | terre libre (bord de Kakin) | 41 × 13 × 41 | Premier jet | clairière, cabane, guide de la forêt | [ ] |
| 12 | `greed_island` — Greed Island (lieu d'arrivée) | 66 900, 81 500 | Greed Island | 51 × 26 × 71 | Premier jet | arrivée, guide de Greed Island | [ ] |
| 13 | `masadora` — Ville de Masadora | 68 800, 81 500 | Greed Island | 71 × 35 × 81 | Premier jet | ville, PNJ : marchand de cartes, habitant | [ ] |
| 14 | `ngl_village` — Village NGL | 39 800, 104 640 | NGL | 81 × 21 × 81 | Premier jet | cabanes, halle, champ de blé, tour de guet, Ancien et fermier | [ ] |
| 15 | `chimera_ant_queen_arm` — Palais souterrain de la Reine | 15 440, 91 140 | terre libre | 57 × 31 × 57 | Complet (export) | 1 849 blocs, survivant de l'atterrissage | [ ] |
| 16 | `east_gorteau_palace` — Palais de Gorteau Est | 52 140, 100 140 | East Gorteau | 41 × 25 × 41 | **Minimal (28 nov)** | plateforme, bloc de pierre 25×25×11 avec porte, 4 tours, chemin, borne du dirigeable ; destination « Palais de Gorteau Est » | [ ] |
| 17 | `greed_antokiba`, `greed_soufrabi`, `greed_aiai`, `greed_limeiro`, `greed_jispa`, `greed_pukku` — villes de Greed Island | 67 500–69 100, 80 800 / 82 200 | Greed Island | 15 × 8 × 15 chacune | **Minimal (28 nov)** | bâtiment creux + porte | [ ] |
| 18 | `yorknew_residential` — Quartier résidentiel de Yorknew | 13 120, 77 690 | Yorknew | 31 × 20 × 21 | **Minimal** | bâtiment creux + porte | [ ] |
| 19 | `padokea_gare`, `padokea_hopital`, `padokea_commissariat`, `padokea_ecole_assassins`, `padokea_prison` | 52 320, 28 000 → 28 125 | Padokea | 17–25 × 8–12 × 13–17 | **Minimal** | bâtiments creux alignés à l'est de la ville | [ ] |
| 20 | `gorteau_caserne`, `gorteau_centre_selection` | 52 200, 100 140 / 100 175 | East Gorteau | 21–25 × 8–10 × 15 | **Minimal** | bâtiments creux + porte | [ ] |

## 2. Tour Céleste (structures NBT, `data/hxh/structure/heavens_arena/`)
| Fichier | Rôle | État | Validation |
|---|---|---|---|
| `tour.nbt` (1,9 Mo) | Tour extérieure, posée une fois par monde près de l'origine (`/place structure hxh:heavens_arena_tower` pour un monde existant) | Complet (ton fichier) | [ ] |
| `arene.nbt` (1,6 Mo) | Ring des 15 tranches d'étages (21 × 21, rayon 10) | Complet (ton fichier) | [ ] |
| `exemple.nbt` | Exemple de format | Outil | — |
| `lobby.nbt` (absent) | Hall d'entrée de la dimension d'arène | **Provisoire** (généré par le code) | [ ] |
| `room.nbt` (absent) | Chambre d'un joueur | **Provisoire** | [ ] |
| halls de tranche (1–50…251, 15 fichiers `arena_brackets/*.json`) | Hall + ring de chaque tranche | Utilisent `arene.nbt` | [ ] |

## 3. Examen Hunter (structures NBT `hxh:exam/<fichier>`, dimension d'examen)
Aucun fichier n'est livré pour l'instant : **chaque site est provisoire** (généré par le code, un par exemplaire d'examen).
Une structure `hxh:exam/<fichier>` (ou `<fichier>_<n>` pour un exemplaire précis) passe avant le provisoire. Détail des
repères : `examen-constructions.md`.

| # | Site | Fichier | Contenu attendu | Validation |
|---|---|---|---|---|
| 1 | Bateau vers Dolle (tempête) | `bateau` | pont, capitaine | [ ] |
| 2 | Île de Dolle (maison de la Mamie, quiz) | `dolle` | port, auberge, Navigateur | [ ] |
| 3 | Forêt des Kiriko | `foret_kiriko` | chemin, maison du mari | [ ] |
| 4 | Zaban : restaurant | `zaban_restaurant` | gargote, cuisinier, ascenseur | [ ] |
| 5 | Hall d'inscription souterrain | `hall_inscription` | Beans, Satotz, départ du marathon | [ ] |
| 6 | Marais de Numele | `numele` | brouillard, parcours, mobs pièges | [ ] |
| 7 | Réserve de Biska | `biska` | Buhara, Menchi, zone de chasse | [ ] |
| 8 | Île de Zevil | `zevil` | chasse aux plaques, points d'apparition | [ ] |
| 9 | Mont Split | `mont_split` | faille, œufs d'aigle-araignée | [ ] |
| 10 | Dirigeable (jeu de la balle) | `dirigeable` | salle de Netero | [ ] |
| 11 | Salon du dirigeable (retour entre épreuves) | `lobby_dirigeable` | hôtesse | [ ] |
| 12 | Tour de Trick | `tour_de_trick` | descente du toit | [ ] |
| 13 | Île-hôtel (OVA) | `ile_hotel` | chambre, épave, vague | [ ] |
| 14 | Plaine d'ouverture du 288e examen (OVA) | `ouverture_288` | candidats à assommer | [ ] |
| 15 | Salle de la licence | `hotel` | Netero | [ ] |

## 4. Structures absentes (à construire quand tu veux ; stub = contour minimal)
| Lieu | Prévu dans | Ancre proposée | État |
|---|---|---|---|
| Greed Island : château, ruines, donjon, arène, bibliothèque magique, tour de sorcier, sanctuaire, grotte aux trésors (les 6 villes sont en stub, §1 n°17) | `feuille-route-monde.md`, `greed-island.md` | sur l'île (15 cases) | Absent |
| Port de l'île de la Baleine (quai, bateaux), sanctuaire du lac | idem | côte de l'île, à fixer avec la carte | Absent |
| Yorknew : hôtel de luxe, banque, musée, gare centrale… (le quartier résidentiel est en stub, §1 n°18) | idem | autour de Yorknew (2000 × 2000 à venir) | Absent |
| East Gorteau : quartiers, hôpital, école, souterrains (palais, caserne et centre de sélection sont en stub) | idem | autour du palais (§1 n°16) | Absent |
| Intérieurs de la Baleine Noire (cabines, cuisines, prison…) | idem | dans le navire | Absent |
| Padokea : quartier résidentiel, centre commercial, hôtel, restaurant, terrain d'entraînement (gare, hôpital, commissariat, école d'assassins, prison en stub, §1 n°19) | idem | autour de la Tour | Absent |
| Continent Noir / Nouveau Continent : rives, ruines, villes | `continent-cache.md` | à définir | Absent |

*Stubs générés par `tools/gen_stub_places.py` (un bâtiment creux avec porte, sans destination de dirigeable). Pour en ajouter : une ligne dans le script, ou un fichier à la main.*
