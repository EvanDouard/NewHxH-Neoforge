# À tester (ajouts non vérifiés)

La CI a été bloquée (facturation) le 12 novembre ; le dépôt est public depuis le soir et elle tourne de nouveau.
Run vert du 13 nov soir (faf9745) : 148 GameTests, gourmet revu, Great Stamp, ratés destructeurs.
Run vert du 13 nov soir (c4cc6d8) : 145 GameTests, Hunter gourmet, Hisoka.
Run vert du 13 nov fin d'après-midi (c291c3d) : 144 GameTests, rang Hunter, Blacklist, quêtes d'organisation.
Run vert du 13 nov après-midi (08c021d) : 142 GameTests, paris, exorcisme, contrats.
Run vert du 13 nov (89b5221) : 139 GameTests et le scénario passent (coups secrets, serments, enchères, combattants du 200e).
Run vert du 12 nov au soir (08913eb) : compilation, tous les GameTests et le scénario passent. Les lignes « CI » sont cochées ;
les lignes « En jeu » restent à regarder à la main.

Légende : **CI** = vérifié par les GameTests ou une capture du scénario · **En jeu** = à regarder à la main.

## Coup de pied fort et bras de fer (13 nov, soir)
- [x] CI (capture `64_coup_de_pied_fort_mur_creuse`) : la cible projetée creuse un trou dans le mur, éclats de la couleur des briques.
- [x] CI : GameTest `strongKickSendsFarAndDigsWalls` (poussée loin de l'attaquant > 2 blocs/tick ; le choc contre un mur creuse un trou autour de la cible).
- [x] CI : GameTest `armWrestlingRewardsSkillAndStrength` (suivre la zone = victoire, ne pas pousser = défaite, la Force élargit la zone, mise prise au départ) ; capture `65_bras_de_fer`.
- [ ] En jeu : coup de pied avec Force ≥ 30 : projection très lointaine ; contre un mur, trou autour de la cible puis elle continue (2 murs au plus). Ressenti du son et des effets ?
- [ ] En jeu : poser le PNJ `lutteur` (repère de PNJ), clic droit : bras de fer à 100 J (clic gauche ou Espace maintenu). Victoire = 200 J et 40 XP de Force. Commande admin : `/hunter armwrestle <force>`.

## Retours d'Evan du 13 nov (soir) : gourmet, renard-ours ; Great Stamp, ratés destructeurs
- [x] CI : boutique du gourmet : plats II/III/IV verrouillés « Après sa quête » (capture `61_boutique_hunter_gourmet`), achetables ensuite (GameTest `gourmetSellsDishThreeAndQuestGivesFour`).
- [x] CI : quête finale « La recette secrète » valide (renards-ours, viande cuite, pommes d'or, plats III, cœur de la mer → carnet de recettes) ; le carnet n'est pas consommé à l'établi.
- [x] CI : renard-ours neutre comme l'ours polaire (GameTest `foxbearActsLikeAPolarBear`), donne de la viande de renard-ours.
- [x] CI : GameTest `greatStampHasAnArmoredSnout` (coups de face ÷3, jamais de zoglin) ; capture `63_great_stamp`.
- [x] CI : GameTest `missedTechniquesDamageScenery` (un raté ≥ 15 de dégâts troue le mur, < 8 ne fait rien).
- [ ] En jeu : faire les deux quêtes du gourmet ; cuisiner plats III et IV à l'établi avec le carnet ; cuire la viande de renard-ours (four, fumoir, feu de camp).
- [ ] En jeu : Great Stamp dans les forêts : il charge ; le frapper de côté, de dos ou d'en haut.
- [ ] En jeu : une technique ratée contre un mur (règle hxhNenDestruction) : éclats, trou ou cratère selon les dégâts.
- Valeurs à revoir ensemble : `docs/equilibrage-final.md`.

## Hunter gourmet, Hisoka, renard-ours (13 nov, soir)
- [x] CI : GameTest `gourmetSellsDishThreeAndQuestGivesFour` (plat III en boutique à 900 J, plat IV jamais en boutique mais dans la quête et aux enchères) ; capture `61_boutique_hunter_gourmet`.
- [x] CI : GameTest `namedChallengersAbove200` étendu : Hisoka (son vrai kit) aux 235–240e étages.
- [x] CI : GameTest `foxbearIsAToughPredator` (60 PV, 9 dégâts, petits renards-ours, butin) ; capture `62_renard_ours`.
- [ ] En jeu : poser le PNJ `hunter_gourmet` ; clic droit = boutique, accroupi + clic droit = sa quête « Le plat légendaire » (16 carottes dorées, 16 saumons cuits, une pomme d'or enchantée → plat IV).
- [ ] En jeu : renards-ours dans les forêts (nouveaux tronçons), agressifs, la mère défend ses petits. Modèle et texture à faire (provisoire : ours polaire agrandi).
- [ ] En jeu : combattre Hisoka au 235e étage.

## Rang Hunter et Blacklist (13 nov, fin d'après-midi)
- [x] CI : GameTest `hunterStarsAndBlacklist` (licence exigée, une étoile par domaine : 200e étage, 3 captures, Nen 60 ; 1000 J × étoile ; étoiles jamais perdues ; traques réservées selon le rang ; une traque à la fois).
- [x] CI : capture `60_blacklist_et_rang` (liste cliquable [Traquer] dans le chat, traques d'un rang supérieur grisées).
- [ ] En jeu : ouvrir l'ordinateur Hunter avec la licence → la Blacklist s'affiche dans le chat ; prendre une traque, rejoindre la cible, la capturer.
- [ ] En jeu : carnet, ligne « Rang Hunter : ★☆☆ · Blacklist · Contrats ».
- Nouvelle commande admin pour tes tests : `/hunter license give|remove`.

## Quêtes propres aux organisations (13 nov, fin d'après-midi)
- [x] CI : GameTest `organizationQuestsFollowMembership` (prérequis `org:`, l'étape `blacklist_catch` compte les captures, pas les contrats).
- [ ] En jeu : après le recrutement, reparler au recruteur : Beans (2 captures de la Blacklist), Chrollo (10 gardes + une étoile du Nether des enchères), Genthru (16 TNT + 3 golems) ; Cheadle (PNJ `cheadle`, à poser) ouvre les Zodiaques après la mission de Beans (230e étage + 1 capture).

## Paris sur les combats de la Tour (13 nov, après-midi)
- [x] CI : GameTest `betsPayWinnersAtFightEnd` (cote bornée, pas de pari contre soi, mise prise tout de suite, fermeture après 20 s, gain versé, pari perdu).
- [x] CI : scénario au 200e : « Guichet des paris », mise de 100 J sur soi, « Pari gagné » à la victoire (capture 45).
- [ ] En jeu : en multijoueur, un spectateur reçoit les liens [100 J] [500 J] [1000 J] et peut parier sur l'adversaire.
- [ ] À juger : la cote (niveau Hunter + ½ niveau de Nen, bornée 15–85 %, 10 % gardés par le guichet).

## Exorcisme de Nen (13 nov, après-midi)
- [x] CI : GameTest `exorcistRemovesForeignAuras` (le lait n'enlève pas la malédiction ; malédiction, dette du Hakoware, bombe de Genthru retirées ; prix payé ; le sceau d'un serment reste).
- [x] CI : captures `58a_malediction_de_nen` (icône d'œil rouge), `58b_exorcisme` (300 J).
- [ ] En jeu : tuer des mobs à Nen (niveau de Nen ≥ 20) : 10 % de chance d'être maudit (dégâts −15 %, régénération d'aura ÷2, une journée).
- [ ] En jeu : poser le PNJ `exorciste` ; désamorcer une bombe de Genthru avant l'explosion.

## Contrats Zoldyck (13 nov, après-midi)
- [x] CI : GameTest `zoldyckContractsPayTheHolder` (cible nommée, récompense au seul titulaire, délai dépassé = cible partie, +50 % pour un Zoldyck).
- [x] CI : capture `59_contrat_zoldyck` (contrat, coordonnées, délai, récompense dans le chat).
- [ ] En jeu : poser le PNJ `majordome_zoldyck`, prendre un contrat, rejoindre les coordonnées (100 à 300 blocs), éliminer la cible avant la fin.

## Coups secrets de famille (13 nov)
- [x] CI : GameTest `familySecretMovesWork` (coup donné avec la famille, juste avant les coups de base, sans Nen, recharge, change avec la famille).
- [x] CI : captures `55a_famille_zoldyck_main_griffe` (encart « Main-griffe » 4/7, griffure) et `55b_famille_freecss_instinct_du_chasseur` (onde verte, vache repérée).
- [ ] En jeu : Main-griffe (bond de 8 blocs + saignement) agréable ou trop fort ? Colère écarlate 15 s / recharge 90 s ; Instinct 32 blocs / 40 s ; Premiers soins 4 PV / 45 s.
- [ ] En jeu : le carnet affiche « Coup secret : … » sous la famille.

## Serments et restrictions (13 nov)
- [x] CI : GameTest `vowsStrengthenAndSeal` (technique unique ×1,6 et refus des autres, rupture = sceau, payer de sa vie −2 PV, monstres seulement rompu sur un cochon, tout sacrifier ×3 puis scellé un jour).
- [x] CI : captures `56a_serments_liste`, `56b_serment_ultime_prete`.
- [ ] En jeu : bouton « Serments » (écran K, fiche de son personnage) → prêter, renoncer (confirmation), sceau affiché.
- [ ] En jeu : « Combat loyal » se rompt bien en frappant de dos, pas de face.

## Enchères de York Shin (13 nov)
- [x] CI : GameTest `auctionsOutbidThenSell` (4 lots par jour sans doublon, surenchères sans rien payer, adjugé à la dernière mise, lot vendu fermé).
- [x] CI : captures `57a_encheres_lots_du_jour`, `57b_encheres_apres_une_mise`.
- [ ] En jeu : poser le PNJ `commissaire_priseur` (repère de PNJ), enchérir jusqu'à l'adjudication ; nouveaux lots le jour suivant.

## Combattants du 200e (13 nov)
- [x] CI : GameTest `namedChallengersAbove200` (Gido 200–209, Sadaso 210–219, Riehlvelt 220–224, Kastro 225–234, kits non proposés au joueur, toupies de Gido qui touchent).
- [x] CI : capture `45_combat_maitre_du_nen_sans_objets` : l'arbitre annonce « Dans ce coin, Gido ! » (vérifié dans le chat).
- [ ] En jeu : combattre Gido, Sadaso, Riehlvelt, Kastro : difficulté, techniques lisibles.

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

## Retours du 13 novembre — lot A
- [x] CI : sanctuaires plus rares et espacés, effets de méditation selon le niveau, `/hunter shrine nearest`.
- [ ] En jeu : méditer près d'une pierre (niveau 1 à 5) : lueur, onde dès le niveau 3, crépitement au niveau 5 ; message d'XP dans la barre d'action.
- [x] CI : carnet (O) mis à jour en direct ; Ko persistant (effet « Ko ») ; serments (effet, stats multipliées, pause, coup de dos sans dégâts) ; cratère en demi-sphère ; coup de pied qui envoie la cible sur un autre mob.
- [ ] En jeu : PV et armure au-dessus des mobs au Gyo 40 / En 30 ; mobs surlignés dans l'En à tout niveau.
- [ ] En jeu : écran Serments → bouton pause pour « Combat loyal », « Monstres seulement », « Mains nues ».

## Retours du 13 novembre — lot B
- [x] CI : GameTest `questCompassPointsAtTheStepTarget` + captures 68/69 (boussole de quête à côté de la bulle).
- [ ] En jeu : boussole pendant un contrat Zoldyck ou une traque de la Blacklist (elle suit la cible qui bouge, mise à jour toutes les 5 s).
- [x] CI : GameTest `doctorBlytheIsHeldAndDrainsAura` (Docteur Blythe maintenu).
- [x] CI : GameTest `strongFightersHitInAnArea` (coups de base et Nen en zone selon les stats).
- [ ] En jeu : sensation des attaques de zone (rayon, onde visible, pas trop fort) ; valider la liste des techniques à cible unique.

## Retours du 13 novembre — lot C (formes de l'En)
- [x] CI : GameTest `enShapesKeepTheSameVolume` (volume gardé, formes impossibles refusées, détection, déblocage à 50) + captures 70 (cube), 71 (cylindre), 72 (écran).
- [ ] En jeu : carnet (O) → onglet Personnage → « Forme de l'En » ; essayer les 4 formes, vérifier que les mobs surlignés correspondent à la forme.
- [ ] À juger : les parois sont discrètes avec une aura claire (arêtes et cercles encrés bien visibles) — plus marquées ?

## Tests en jeu faits par Claude en CI (lot T1, 13 nov)
Scénario « Tests en jeu faits par Claude » dans `ci/scenario.txt`, captures 73 à 82 :
contrat Zoldyck (bulle + boussole, cible rejointe, contrat rempli), attaque de zone au poing, Gyo 40 (PV et armure),
Docteur Blythe maintenu, serment en pause (écran), méditation au sanctuaire de niveau 5, PNJ exorciste,
archéologue, Hunter gourmet (boutique), commissaire-priseur (enchères).

## Lot D (13 nov, soir)
- [x] CI : GameTest `characterVowsHaveTheirOwnRules` (Kurapika : Brigade seulement sinon la mort ; Feitan : seulement blessé ; Gon : pas de serment propre).
- [x] CI : GameTest `spiderEagleLaysEggsAndStaysWild` + capture 83 (aigle-araignée).
- [ ] À juger : modèle provisoire de l'aigle-araignée (perroquet gris ×2,5) en attendant le modèle Blockbench.
