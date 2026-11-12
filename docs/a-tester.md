# À tester (ajouts non vérifiés)

La CI GitHub est bloquée (facturation) depuis le 12 novembre midi. Tout ce qui suit est poussé sur `main` mais
n'a été ni compilé ni lancé. Quand la CI repart : la lancer, corriger, puis cocher ici et dans la feuille de route.

Légende : **CI** = vérifié par les GameTests ou une capture du scénario · **En jeu** = à regarder à la main.

## Gyo par paliers (12 nov)
- [ ] CI : GameTest `gyoTiersFollowEvansTable` (paliers 5/10/20/25/35/50/100, invisibilité potion vs Nen, Zetsu au max).
- [ ] CI : capture `24b_gyo_50_type_invisible_et_trajectoire` (type de Nen à côté du niveau, contour du momifié invisible, trait rouge devant la flèche).
- [ ] En jeu : Bungee Gum invisible pour les autres sans Gyo 5, toujours visible pour Hisoka.
- [ ] En jeu : In pâle à Gyo 25, net à 50 ; poing chargé en Ryu qui brille à Gyo 20.
- [ ] En jeu : Texture Surprise, nuée de Pouf, fumée de Morel percées seulement à Gyo 50.

## Mobs de Minecraft avec une technique de personnage (12 nov)
- [ ] CI : GameTest `vanillaMobsOnlyGetWeakCopiesOfCharacterTechniques` (copie affaiblie, ultime refusé, joueur et kits `mob_*` non touchés).
- [ ] CI : GameTest `gonMobsHaveNoUltimateAndWeakerJajanken` toujours vert.
- [ ] En jeu : un zombie au kit de Zeno ne lance jamais la Plongée du dragon.

## Quête de Netero : soin par le Nen (12 nov)
- [ ] CI : GameTest `neteroQuestUnlocksHealing` (prière en Ten seulement, paumes jamais fatales, méditation blessé, soin débloqué).
- [ ] CI : captures `54a_netero_cercle_et_paume_de_kannon`, `54b_netero_la_paume_s_abat`.
- [ ] En jeu : difficulté des paumes (22 % de la vie, ÷2 en Ken, une paume toutes les 1,2 s) — trop dur, trop facile ?
- [ ] En jeu : poser le PNJ `netero_maitre` avec le repère de PNJ et faire la quête en entier.

## Tour Céleste : Hisoka au 200e (12 nov)
- [ ] CI : GameTest `hisokaAuraWallNeedsTen`.
- [ ] En jeu : au 200e, sans Ten, on est repoussé et apeuré près de l'hôtesse ; Hisoka apparaît devant elle ; en Ten on passe.

## Éclats du sol (12 nov)
- [ ] CI : captures d'impacts du scénario (`13l…`, `13m…`, coups de pied et frappes chargées) : éclats de la couleur du bloc, plus de particules de Minecraft.

## Éditeur de quêtes (12 nov)
- [x] Testé dans un navigateur (formulaire → JSON, vérifications). Lien : https://claude.ai/artifact/SvjkskuBnnVLBEUYbnoV9G
- [ ] En jeu : une quête générée par l'éditeur passe `/hunter quest check` sans erreur.

## Voyages en dirigeable (12 nov)
- [ ] CI : GameTest `airportsOpenWhenVisitedAndCostJenny` (aéroport ouvert à la visite, prix 1 Jenny / 10 blocs, min 50, gratuit avec la licence).
- [ ] En jeu : poser deux PNJ `guichetier` loin l'un de l'autre, parler aux deux, cliquer « ✈ destination » : écran noir « En vol », arrivée devant l'autre guichet, Jenny débités.
- [ ] En jeu : `/hunter airport name <nom>` renomme l'aéroport le plus proche.
