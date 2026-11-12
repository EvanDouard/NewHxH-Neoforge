# À tester (ajouts non vérifiés)

La CI a été bloquée (facturation) le 12 novembre ; le dépôt est public depuis le soir et elle tourne de nouveau.
Run vert du 12 nov au soir (08913eb) : compilation, tous les GameTests et le scénario passent. Les lignes « CI » sont cochées ;
les lignes « En jeu » restent à regarder à la main.

Légende : **CI** = vérifié par les GameTests ou une capture du scénario · **En jeu** = à regarder à la main.

## Gyo par paliers (12 nov)
- [x] CI : GameTest `gyoTiersFollowEvansTable` (paliers 5/10/20/25/35/50/100, invisibilité potion vs Nen, Zetsu au max).
- [x] CI : capture `24b_gyo_50_type_invisible_et_trajectoire` (type de Nen à côté du niveau, contour du momifié invisible, trait rouge devant la flèche).
- [ ] En jeu : Bungee Gum invisible pour les autres sans Gyo 5, toujours visible pour Hisoka.
- [ ] En jeu : In pâle à Gyo 25, net à 50 ; poing chargé en Ryu qui brille à Gyo 20.
- [ ] En jeu : Texture Surprise, nuée de Pouf, fumée de Morel percées seulement à Gyo 50.

## Mobs de Minecraft avec une technique de personnage (12 nov)
- [x] CI : GameTest `vanillaMobsOnlyGetWeakCopiesOfCharacterTechniques` (copie affaiblie, ultime refusé, joueur et kits `mob_*` non touchés).
- [x] CI : GameTest `gonMobsHaveNoUltimateAndWeakerJajanken` toujours vert.
- [ ] En jeu : un zombie au kit de Zeno ne lance jamais la Plongée du dragon.

## Quête de Netero : soin par le Nen (12 nov)
- [x] CI : GameTest `neteroQuestUnlocksHealing` (prière en Ten seulement, paumes jamais fatales, méditation blessé, soin débloqué).
- [x] CI : captures `54a_netero_cercle_et_paume_de_kannon`, `54b_netero_la_paume_s_abat`.
- [ ] En jeu : difficulté des paumes (22 % de la vie, ÷2 en Ken, une paume toutes les 1,2 s) — trop dur, trop facile ?
- [ ] En jeu : poser le PNJ `netero_maitre` avec le repère de PNJ et faire la quête en entier.

## Tour Céleste : Hisoka au 200e (12 nov)
- [x] CI : GameTest `hisokaAuraWallNeedsTen`.
- [ ] En jeu : au 200e, sans Ten, on est repoussé et apeuré près de l'hôtesse ; Hisoka apparaît devant elle ; en Ten on passe.

## Éclats du sol (12 nov)
- [x] CI : captures d'impacts du scénario (`13l…`, `13m…`, coups de pied et frappes chargées) : éclats de la couleur du bloc, plus de particules de Minecraft.

## Éditeur de quêtes (12 nov)
- [x] Testé dans un navigateur (formulaire → JSON, vérifications). Lien : https://claude.ai/artifact/SvjkskuBnnVLBEUYbnoV9G
- [ ] En jeu : une quête générée par l'éditeur passe `/hunter quest check` sans erreur.

## Voyages en dirigeable (12 nov)
- [x] CI : GameTest `airportsOpenWhenVisitedAndCostJenny` (aéroport ouvert à la visite, prix 1 Jenny / 10 blocs, min 50, gratuit avec la licence).
- [ ] En jeu : poser deux PNJ `guichetier` loin l'un de l'autre, parler aux deux, cliquer « ✈ destination » : écran noir « En vol », arrivée devant l'autre guichet, Jenny débités.
- [ ] En jeu : `/hunter airport name <nom>` renomme l'aéroport le plus proche.

## Plats nutritifs (Vitalité) (12 nov)
- [x] CI : GameTest `nutritiousDishGivesVitalityXp`.
- [ ] En jeu : recettes (I = bol + steak cuit + pomme de terre cuite + carotte + pain ; II = plat I + carotte dorée + saumon cuit + miel), le bol revient après le repas, textures I–IV.

## Sanctuaires de méditation (Volonté) (12 nov)
- [x] CI : GameTest `meditationShrineMultipliesWillpower`.
- [ ] En jeu : explorer de nouvelles terres → des sanctuaires apparaissent (niveau 1 ~1 tronçon sur 120 … niveau 5 ~1 sur 4000) ; pas de lag ni de sanctuaire coupé ou dans l'eau.
- [ ] En jeu : méditer (M) à côté de la pierre : Volonté ×3/×5/×8/×12/×20 selon le niveau.
- [ ] En jeu : PNJ `archeologue` (repère de PNJ) : liste cliquable niveaux 1–4, coordonnées du plus proche, Jenny débités ; niveau 5 jamais vendu.
- [ ] En jeu : `/hunter shrine build <1-5>` (admin) pose un sanctuaire devant soi.

## Organisations (12 nov, soir)
- [x] CI : GameTest `organizationsMakeEnemiesHittable` (Association, Zodiaques, Brigade Fantôme, Bombers ; ennemis = techniques qui touchent).
- [ ] En jeu : quêtes de recrutement — Beans (Association, après l'examen), PNJ `chrollo` (Brigade Fantôme), PNJ `genthru` (Bombers) à poser.
- [ ] En jeu : carnet (organisation affichée), `/hunter org leave`, `/hunter org list`, `/hunter org join <id>` (admin).
- [x] PvP entre organisations ennemies : validé par Evan le 12 nov.

## Familles (12 nov, soir)
- [x] CI : GameTest `familiesGiveTheirInheritance` (Zoldyck poison/foudre, Kurta yeux écarlates, Freecss XP, ~70 % sans famille).
- [ ] En jeu : message de naissance à la première connexion, famille dans le carnet, `/hunter family list`, `/hunter family set <id|none>` (admin), règle `/gamerule randomFamily`.
- [ ] À valider avec Evan : les effets proposés pour chaque famille et les poids du tirage (data/hxh/families).
