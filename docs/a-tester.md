# À tester (ajouts non vérifiés)

Run vert du 14 nov (8afd8ea) : 168 GameTests, formes de l'En depuis l'écran, yeux du Gyo et écarlates, clans, types de Nen.

La CI a été bloquée (facturation) le 12 novembre ; le dépôt est public depuis le soir et elle tourne de nouveau.
Run vert du 13 nov soir (faf9745) : 148 GameTests, gourmet revu, Great Stamp, ratés destructeurs.
Run vert du 13 nov soir (c4cc6d8) : 145 GameTests, Hunter gourmet, Hisoka.
Run vert du 13 nov fin d'après-midi (c291c3d) : 144 GameTests, rang Hunter, Blacklist, quêtes d'organisation.
Run vert du 13 nov après-midi (08c021d) : 142 GameTests, paris, exorcisme, contrats.
Run vert du 13 nov (89b5221) : 139 GameTests et le scénario passent (coups secrets, serments, enchères, combattants du 200e).
Run vert du 12 nov au soir (08913eb) : compilation, tous les GameTests et le scénario passent. Les lignes « CI » sont cochées ;
les lignes « En jeu » restent à regarder à la main.

Légende : **CI** = vérifié par les GameTests ou une capture du scénario · **En jeu** = à regarder à la main.

## Graphe des techniques du Nen, chute, Kick, vie, Jajanken (19 nov)
- **CI** Gyo : demande aussi In 3 (le Gyo révèle l'In), lien In → Gyo dans le graphe ; `advancedTechniquesUnlockFromTheGraph` vérifie « Ren 5 + In 2 : pas de Gyo ». **En jeu** : ouvrir le graphe du Nen, le Gyo a deux parents (Ren et In). Les valeurs à décider sont dans `a-trancher.md`.
- **CI** Les techniques avancées se débloquent toutes seules aux niveaux requis, sans voir Wing (GameTest `advancedTechniquesUnlockFromTheGraph`) : Zetsu = In 5 · Shu = Ten 8 · Ken = Ten 5 + Ren 3 · En = Ten 10 + Ren 8 · Gyo = Ren 5 + In 3 · Ko = Ten 10 + In 10 + Ren 10 + Gyo 5 + Hatsu appris · Ryu = Gyo 15 + Ko 10 + Ken 10.
- **CI** Wing enseigne Ten → In → Ren → Hatsu (divination puis choix du Hatsu), puis donne les 7 quêtes `nen_adv_*` d'un coup (`wingGivesAllAdvancedQuestsAtOnceAfterHatsu`) ; chacune se termine dès que la technique est débloquée, sans retourner voir Wing. Les anciennes sauvegardes (leçon du Ko faite) restent valides.
- **CI** Onglet « Graphe du Nen » du carnet (touche O) : capture `99f_graphe_du_nen` du scénario.
- **CI** Passage de niveau plus visible : message dans le chat (niveau de Nen et plafond des maîtrises ; « plafond atteint » pour une maîtrise), son et onde d'aura aux pieds, et bandeau du graphe (niveau de Nen, plafond, barre d'XP) — `nenGraphPayloadCarriesLevelAndCap`. **En jeu** juger la lisibilité.
- **CI** Examen : 5 à 7 épreuves tirées (au lieu de 4 à 6), toujours en crescendo par phases ; le salon du dirigeable (`lobby_dirigeable`) est bâti avec son hôtesse (test `examSitesAreBuiltWithNpcsAndCourse`). **En jeu** : après une épreuve réussie on revient dans le salon (repos, soin, repas), l'hôtesse propose d'embarquer pour la suivante (lien cliquable) ; la fin de l'Examen ramène toujours chez soi.
- **En jeu** Le graphe : survoler ou cliquer une technique affiche conditions et avancement ; les liens s'allument ; Ken → Ryu en pointillé (incertain).
- **En jeu** Un joueur qui n'a jamais parlé à Wing après le Hatsu débloque quand même le Gyo à Ren 5 (message + bandeau « Technique débloquée »).
- **CI** Coup de pied fort dans la direction visée (`strongKickFollowsTheAim`) ; **En jeu** viser le sol et frapper : la cible s'enfonce.
- **CI** Chute : dégâts × 0,6 × (1 − Volonté/100), nuls à la Volonté 100 (`fallDamageMeltsWithWillpowerAndLandingMarksTheGround`) ; impact au sol selon la hauteur : éclats dès 4 blocs, onde dès 8, secousse dès 14, cratère dès 20 (règle de destruction). **En jeu** sauter de haut avec différentes Volontés.
- **CI** La vie courante est sauvegardée (`lastHealthSurvivesSaveAndLoad`) ; **En jeu** se déconnecter avec Vitalité élevée ou en Youpi (+20 PV) et revenir : la vie n'est plus rabotée à 20.
- **CI** Jajanken ultime : rayon d'explosion plafonné à 16 blocs, blocs retirés par paquets de 1500 par tick (`hugeOrbBlastIsCappedAndSpreadOverTicks`) ; **En jeu** refaire l'ultime avec serments et très grosse aura : plus de lag.

## Recettes dans l'ordinateur et le livre de recettes (14 nov, soir)
- [x] CI (captures `97b`, `97c`, `97d`) : catégorie « Recettes » de l'ordinateur Hunter : grille d'artisanat avec les icônes (variantes qui défilent), flèche, résultat et quantité, info-bulles au survol ; liste des ingrédients comptés dessous ; cuisson : une case et le temps.
- [x] CI : GameTest `modRecipesAreInTheRecipeBook` : toutes les recettes du mod, ordinateur compris, débloquées à la connexion.
- [ ] En jeu : ouvrir le livre de recettes (inventaire ou établi) : l'ordinateur Hunter y est, avec les haltères, tapis, mannequin, bain glacé, plats…

## Retours du 14 nov (fin d'après-midi) : fins d'épreuve, Tour, dirigeable, Zevil, coups physiques
- [x] CI (capture `34_mamie_attente_avant_depart`) : 3,5 s d'attente avant la téléportation de fin d'épreuve, « Départ dans N s… » ; 6 s après avoir démasqué le Kiriko. Pas d'attente pour `/hunter quest skip` ni au premier dirigeable du guide.
- [x] CI : GameTest `overworldTowerIsAvailable` : la tour a un structure_set (générée aussi naturellement, tous les ~1000 blocs, dans les biomes autorisés) en plus de celle posée près du spawn.
- [x] CI : GameTest `zevilCandidatesHuntEachOtherForPlates` ; capture `34f2`.
- [x] CI : GameTest `physicalMovesIgnoreTheHeldTool`.
- [ ] En jeu : `/locate structure hxh:heavens_arena_tower` dans un nouveau monde (la génération naturelle ne vaut que pour les tronçons pas encore générés).
- [ ] En jeu : dirigeable posé sur un sol plat et dégagé, au-dessus des arbres, sans rien couper ; on monte par l'échelle du mât.
- [ ] En jeu : île de Zevil : les candidats se battent entre eux ; « X a ses plaques et quitte l'île (n/4) » ; un nouveau candidat débarque toutes les 25 s (18 au plus) ; à 4 qualifiés, épreuve terminée (éliminé sans ses 6 points). Le porteur de la plaque de ta cible ne se qualifie jamais.
- [ ] En jeu : hache ou épée en main : « Attaque basique », coup de pied, coup chargé font les mêmes dégâts qu'à mains nues (sauf armes du mod) et l'arme repart de zéro (pas de coup d'épée chargé juste après).

## Tri du choix de Hatsu par type, /hunter hatsu give (14 nov, soir)
- [x] CI (capture `16f_filtre_par_type_transformation`) : pastilles des six types en haut à droite ; un clic n'affiche que ce type (cadre doré), un second clic enlève le filtre. Chaque personnage a sa pastille.
- [x] CI : GameTest `anyTechniqueCanBeGivenByName` (nom anglais, français, identifiant ; chaque technique a ses deux noms) ; capture `99e` : Jajanken et Doctor Blythe donnés apparaissent dans la liste.
- [ ] En jeu : survoler une pastille : nom français + anglais.
- [ ] En jeu : `/hunter hatsu give Dev ` puis Tab : la liste propose les techniques par leur nom ; `/hunter hatsu take Dev <technique>` les retire.

## Bestiaire dans l'ordinateur, famille dans le carnet (14 nov, fin d'après-midi)
- [x] CI (capture `97_ordinateur_bestiaire_fourmi_chimere`) : catégorie « Bestiaire » de l'ordinateur Hunter, une fiche par créature (danger, où, description) ; l'onglet Bestiaire du carnet a disparu.
- [x] CI : GameTest : famille, niveau de clan et héritage dans l'onglet Personnage.
- [ ] En jeu : `/hunter max` met le niveau de clan à 100 ; carnet → Personnage, faire défiler sous les stats : famille, niveau de clan, stats offertes, héritage (XP en plus, stats au clan 100, poison, foudre), coup secret.

## Formes de l'En, yeux, Colère écarlate, clans, types de Nen (14 nov, après-midi)
- [x] CI : GameTest `everyEnShapeWorksAtEveryMasteryFrom50` (réglage par défaut faisable pour chaque forme de la maîtrise 50 à 100 ; forme gardée si l'En a grandi).
- [x] CI (captures `74` à `78`) : sphère complète vue d'en haut (dessous visible), écran demi-sphère → cube (réglage par défaut, bouton Appliquer actif), « » » +5, cube, cylindre, demi-sphère appliqués depuis l'écran.
- [x] CI (captures `98a` à `98d`) : yeux du Gyo de face, tête tournée à 45° et 90°, tête baissée.
- [x] CI (captures `99a` à `99d`) : yeux écarlates rouges avec éclairs ; carnet : niveau de clan Kurta 100 et durée 300 s ; Nen affiché « Spécialisation (yeux écarlates) ».
- [x] CI : GameTests `techniqueTypesPhysicalAndScarletEyes` et `kurtaClanLevelLengthensScarletEyes`.
- [ ] En jeu : changer de forme dans l'écran à plusieurs maîtrises ; le texte dit que le volume grandit avec la maîtrise.
- [ ] En jeu : la sphère complète se voit sous soi dans une grotte ou au-dessus du vide.
- [ ] En jeu : Gyo en vue de face (F5 deux fois), tourner la tête : les yeux restent sur le visage. Pareil pour un mob copié.
- [ ] En jeu : Colère écarlate : yeux rouges, éclairs, plus de Force ni de Résistance. `/hunter clan level 100` puis relancer : 5 min.
- [ ] En jeu : pastilles de type dans l'encart de technique (強 vert, 変 violet, 具 rouge, 特 bleu, 操 gris, 放 jaune, 体 marron) et dans la liste des techniques (« Type : … · efficacité pour toi »).
- [ ] À trancher : seconds types des techniques (estimation de départ dans `NenAffinity`), part de l'XP de stat qui va au clan (1/5), +30 XP de clan par coup secret, bonus de stats du clan (xp_bonus × 50 au niveau 100, réglable par `clan_stats`).

## En : cibles en surbrillance (14 nov)
- [x] CI (capture `73_en_niveau_0_cible_en_surbrillance`) : En maîtrise 0, cochon à 2 blocs : contour couleur d'aura.
- [ ] En jeu : En niveau 0 (rayon 2 blocs à l'arrêt), puis plus haut : tout ce qui vit dans l'En brille, à travers les murs. Le Gyo révèle aussi par le contour.

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
- [x] Testé par Claude en CI (captures 94–95) : quête « Le plat légendaire » jusqu'au bout, plats débloqués en boutique. Corrigé : la licence vaut l'Examen pour les prérequis ; 14 PNJ affichaient une clé brute au lieu d'une réplique.
- [x] Testé par Claude en CI (capture 85) : renard-ours et son petit, Great Stamp. Modèles et textures à faire (provisoires).
- [x] Testé par Claude en CI (capture 84) : l'arbitre annonce Hisoka au 235e, cote et paris ouverts, combat lancé. Reste à toi : la difficulté.

## Rang Hunter et Blacklist (13 nov, fin d'après-midi)
- [x] CI : GameTest `hunterStarsAndBlacklist` (licence exigée, une étoile par domaine : 200e étage, 3 captures, Nen 60 ; 1000 J × étoile ; étoiles jamais perdues ; traques réservées selon le rang ; une traque à la fois).
- [x] CI : capture `60_blacklist_et_rang` (liste cliquable [Traquer] dans le chat, traques d'un rang supérieur grisées).
- [x] Testé par Claude en CI (capture 93) : traque de la Blacklist prise, bulle + boussole, cible capturée.
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
- [x] Testé par Claude en CI (captures 73–74) : contrat pris, bulle + boussole, cible rejointe et éliminée. Corrigé : la cible brûlait au soleil ; en Paisible, contrats refusés (la cible disparaissait).

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
- [x] Testé par Claude en CI (captures 90–92) : deux guichets à 600 blocs, destination listée (60 J), « En vol », arrivée, Jenny débités. Bug corrigé : un guichetier posé par /summon n'avait pas de place (aéroport en 0, 0).
- [x] Testé par Claude en CI : `/hunter airport name Alpha` puis vol vers « Alpha ».

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
- [x] Testé par Claude en CI (captures 66 et 79) : niveaux 4 et 5, lueur, anneau doré, « +1 Volonté ».
- [x] CI : carnet (O) mis à jour en direct ; Ko persistant (effet « Ko ») ; serments (effet, stats multipliées, pause, coup de dos sans dégâts) ; cratère en demi-sphère ; coup de pied qui envoie la cible sur un autre mob.
- [x] Testé par Claude en CI (capture 76) : « PV · Armure » au-dessus d'un mob ordinaire au Gyo 40 (bug corrigé : ne marchait que sur les mobs à Nen).
- [x] Testé par Claude en CI (capture 78) : serment prêté, mis en pause, écran des serments.

## Retours du 13 novembre — lot B
- [x] CI : GameTest `questCompassPointsAtTheStepTarget` + captures 68/69 (boussole de quête à côté de la bulle).
- [x] Testé par Claude en CI (captures 73–74) : bulle du contrat + boussole, cible rejointe et éliminée, « Contrat rempli ».
- [x] CI : GameTest `doctorBlytheIsHeldAndDrainsAura` (Docteur Blythe maintenu).
- [x] CI : GameTest `strongFightersHitInAnArea` (coups de base et Nen en zone selon les stats).
- [ ] En jeu : sensation des attaques de zone (rayon, onde visible, pas trop fort) ; valider la liste des techniques à cible unique.

## Retours du 13 novembre — lot C (formes de l'En)
- [x] CI : GameTest `enShapesKeepTheSameVolume` (volume gardé, formes impossibles refusées, détection, déblocage à 50) + captures 70 (cube), 71 (cylindre), 72 (écran).
- [x] Testé par Claude en CI (captures 70–72) : cube, cylindre, écran de la forme.
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
- [x] CI : GameTest `eachOrganizationHasAThirdQuest` — nouvelles quêtes : Examinateur (Beans : 3 Great Stamp + 3 œufs d'aigle-araignée), Meteor City (Chrollo : 2 renards-ours, 32 or, un totem), Cartes de Greed Island (Genthru : 15 creepers, 16 poudres, 32 papiers), Conseil des Zodiaques (Cheadle : 2 contrats, 3 captures).
- [x] CI : GameTest `dayAndNightVowsSwitchWithTheSun` (Heure supplémentaire et Service de jour, jamais ensemble) + capture 88 (écran des serments sur deux colonnes).
- [x] CI : GameTest `numeleApeAndChimeraAntsHunt` + capture 96 (singe à visage humain, Fourmi-Chimère soldat). GameTest des traductions étendu à toutes les entités, objets et mobs d'exemple (corrigé : « suspect de la Brigade » sans nom).
- [ ] À juger : modèles provisoires (singe : momie ; fourmi : araignée des cavernes ×1,5).
- [x] CI : GameTest `unmaskedKirikoTransformsAndFliesAway` — dans l'épreuve des Kiriko, le faux mari démasqué devient un vrai Kiriko qui s'envole (capture 96). Modèle provisoire : perroquet vert ×3.
- [x] CI : onglet « Bestiaire » du carnet (capture 97) ; GameTest des traductions : chaque créature du bestiaire existe et a sa description.

## Entraînement : mannequin cogneur et bain glacé (14 nov)
- [x] CI : GameTests `strikingDummyHitsAnyCreatureButNeverKills`, `strikingDummyDodgeTrainsSpeed`, `iceBathTrainsWillpowerWithoutFreezeDamage` + captures 48c (objets posés), 48c2 (givre dans le bain), 48c3 (mannequin qui cogne).
- [ ] En jeu : rythme du mannequin (un coup toutes les 1,5 s, 1 cœur, jamais sous 2 cœurs) ; esquiver en s'accroupissant ou au bouclier donne de la Vitesse.
- [ ] À juger : modèles et textures provisoires (mannequin en paille sur pied, cuve en métal) ; recettes (mannequin : botte de foin + bâtons + piston ; bain : chaudron + 2 glaces compactées + seau d'eau).

## Leçons de Wing, maîtrises et coût d'aura (15 nov)
- [x] CI : GameTest `wingLessonsAreChainedAndZetsuIsTheInTier` (chaîne Ten → In → Zetsu → Ren → Gyo → Ken → En → Ko, Hatsu après le Ren ; Zetsu = palier de l'In, Ken = palier du Ten ; In et Zetsu partagent la même XP).
- [x] CI : GameTest `nenMustBeLearnedFromWing` étendu (niveau 1 du Ten requis, Wing dit « entraîne-toi encore » sinon, l'In est proposé ensuite et passe en suivi) + capture 38c.
- [x] CI : GameTest `beginnersPayMoreAura` (×2,5 à la maîtrise 0, normal au niveau 20) ; `questsRunInParallelAndCanBeTracked` : la quête commencée ou continuée chez un PNJ passe en suivi.
- [ ] CI : capture 22n7c (liseré de Gyo, couleur de l'aura, ~10 px qui s'efface vers le centre).
- [ ] En jeu : rythme des leçons (In 2, palier du Zetsu = In 5 ≈ 4 min d'In, palier du Ken = Ten 5 ≈ 4 min de Ten) ; surcoût débutant trop fort ou pas assez ?
- [ ] À juger : intensité du liseré de Gyo (opacité 0xB0 au bord, renforcée après la 1re capture trop pâle).

## Retours du 19 novembre (soir) — carnet, coût d'aura, coup de pied, examen
- [x] CI : GameTests `costCollapsesBrutallyBeforeLevelFive` (coût ×5 à la maîtrise 0, ×2 au niveau 5, ×1 au niveau 20 ; Ten et In coûtent de l'aura à tenir de 0 à 5, rien ensuite), `strongKickSlamsIntoTheGround` (cuvette au sol ; le coup de pied dans le vide n'abîme que les blocs à portée de pied), `profileDetailsShowEffectsAndEfficiencyPerLevel` (fiches par niveau, efficacité marquée, paquet du carnet).
- [x] CI : captures 99g à 99q — fiches Ten, Gyo, Hatsu, Niveau de Nen ; onglet Personnage (cartes de stats), fiches Force, Volonté, Type de Nen ; onglet Aura, fiches Soin et Régénération.
- [ ] En jeu : tenir le Ten au niveau 0 vide l'aura en ≈ 30 s (5/s − 2/s de régénération) ; à 5 il ne coûte plus rien. Trop dur / trop doux ?
- [ ] En jeu : coup de pied fort visé vers le bas → la cible s'écrase et creuse une cuvette ; coup de pied dans le vide vers un mur lointain → éclats, trou à partir d'un certain niveau de Force (règle hxhNenDestruction).
- [ ] En jeu : mourir dans l'Examen → message jaune, on sort au point de départ, aucun objet perdu, l'épreuve est ratée.
- [ ] En jeu : après `/hunter reset`, la divination redonne un type de Nen ; Wing commente ce type avant « choisis ton Hatsu » (Spécialisation : il est surpris).
- [ ] À juger : lisibilité des cartes et de la barre de paliers (survol d'un palier = effets en bas), onglets Nen / Aura.

## Retours du 20 novembre (nuit) — aura ×10, talent, sommeil, quêtes, équilibrage
- **CI** Aura maximale = (base + Volonté) × talent de Nen : Prometteur ×1 = 1000 affichés, Prodige ×2 = 2000 (`auraMaxScalesWithNenTalent`). L'affichage est ×10 (HUD, carnet, `/hunter show`, coûts des techniques) ; les coûts internes ne changent pas.
- **CI** Ten du débutant ≈ 40 aura/s affichés au niveau 0 (≈ 2 barres du niveau 1), nul au niveau 5.
- **CI** Maîtrise « Régénération d'aura » (`mastery regen`) : aucune régénération naturelle de 0 à 4, puis démarre au niveau 5 (`auraRegenIsNullBeforeLevelFive`). Dormir rend 100 % de l'aura et donne de l'XP de régénération ; la méditation aussi. À 0 d'aura : message qui conseille de dormir. **En jeu** vérifier le ressenti (très dur au début, voulu).
- **CI** Quête de départ « Découvrir la Heaven Arena » (`discoverArenaQuestCompletesOnceTheDeskIsPassed`) : donne les coordonnées de la Tour dans le chat ; terminée dès qu'on s'approche à 60 blocs ou qu'on passe au guichet.
- **CI** Wing : nouvelle première étape « trouver un meilleur endroit pour méditer » (30 s de méditation près d'une pierre de sanctuaire, `stone_meditation`), avec sa réplique sur les pores et la survie. Étape sautée si le Nen est déjà éveillé. En méditation avant l'éveil, la pierre accélère le compteur selon son palier.
- **En jeu** La Tour Céleste n'est posée que sur terre ferme (recherche en spirale d'un terrain sec au-dessus du niveau de la mer). Ne s'applique qu'aux nouveaux mondes : une tour déjà posée dans l'eau ne bouge pas.
- **CI** Carte Famille : le coup secret est expliqué (capture `99r_famille_coup_secret_explique`). Boutons Hatsu et Ryu dans leurs fiches, onglet Nen (`99s`, `99t`).
- **CI** Rééquilibrage : Force +0,5 dégât/niveau (+50 au niveau 100), Vitalité +0,4 PV/niveau (+40), Endurance 0,1 (10 PV plats) (`strengthIsMuchStrongerAtMaxLevel`). Attention : un fichier de config existant garde les anciennes valeurs.

## Lot 2 des techniques de ton fichier (20 nov)
- [x] CI : `zoldyckAssassinHitsIsolatedTargetsHarder`, `botobaiVeteranIsStrongerWhenHealthy`, `zeroHandSpendsAllAura` ; `neteroSpendsPrayerOnKannon` mis à jour (Bodhisattva = 10 de Prière, Poings de Kannon retirés).
- [ ] En jeu : Silva, Zeno ou Kalluto frappent +25 % une cible seule (rien d'autre à 6 blocs) ; Killua n'a pas encore ce bonus.
- [ ] En jeu : Netero n'a plus les Poings de Kannon ; Bodhisattva coûte 10 de Prière ; Main zéro : dégâts selon l'aura qui restait (aura vide après), blocs détruits le long du rayon si `hxhNenDestruction`.
- [ ] En jeu : Botobai en forme (≥ 70 % PV) a Force I et Résistance I ; Cookie donne aussi Force I aux alliés ; Pierre sans retenue de Gon demande Nen 60 ; aiguille d'Illumi : 2 s.

## Lot 3 des techniques de ton fichier (20 nov)
- [x] CI : `godspeedSlowsTheWorldExceptFastOnes`, `memoryBombFiresSixBulletsThenReloads`, `risingSunGrowsWithDamageTaken`.
- [ ] En jeu : Godspeed de Killua : tout ce qui est à 50 blocs ralentit 10 s, sauf un autre Godspeed ou une cible de Vitesse 90+ ; plus de minage rapide.
- [ ] En jeu : Tête de dragon de Zeno : la cible est mordue, repoussée puis retenue environ 2 s.
- [ ] En jeu : Bombe de mémoire de Pakunoda : 6 tirs à la suite sans recharge, puis 12 s ; elle se recharge aussi après 60 s sans tir.
- [ ] En jeu : Soleil levant de Feitan : se faire frapper avant le lancer ; plus on a reçu de dégâts, plus le soleil fait mal (8 à 48 au total).
- [ ] En jeu : Biscuit en grande forme est un peu ralentie.
- Déjà dans le jeu comme tu le décris, donc rien à changer : Big Bang Impact (charge, cratère) et Ripper Cyclotron (tours de bras). Pas encore faits : Double mitrailleuse en continu (c'est une rafale fixe), Compte à rebours au hasard avec désamorçage.

## Lot 4 des techniques de ton fichier (20 nov)
- [x] CI : `countdownBombIsRandomAndDefusedByContact`.
- [ ] En jeu : Compte à rebours de Genthru : délai au hasard de 1 à 5 s, explosion plus forte (24) ; si la cible frappe Genthru trois fois avant l'explosion, la bombe est désamorcée (message).

## Lot 5 des techniques de ton fichier (20 nov)
- [x] CI : `lot5RoyalPressureStanceAndFocusedShot` + `everyTechniqueOfEveryKitRuns`.
- [ ] En jeu, Meruem : Frappe du Roi (20 aura, 5 s), Queue du Roi (25, 8 s, projette + ralentit), Accélération royale (30, 15 s, Vitesse III + Force 6 s), Pression royale (30, 13 s, 8 blocs, ralentit + affaiblit).
- [ ] En jeu, Nobunaga : Coupe instantanée (15, 3 s), Posture du samouraï (20, 15 s, Résistance + Force 8 s), Pas du sabreur (15, 10 s, Vitesse III 4 s).
- [ ] En jeu, Franklin : Tir concentré (25, 8 s, niveau Nen 10), Tir perforant (35, 12 s, niveau Nen 20).
- Valeurs proposées par moi : corrige-les dans `docs/a-trancher.md`.

## Lot 6 des techniques de ton fichier (20 nov)
- [x] CI : `lot6HeatWaveBurnsAndPhantomStepHides` + `everyTechniqueOfEveryKitRuns`.
- [ ] En jeu, Feitan : Vague de chaleur (20 aura, 10 s, 6 blocs, enflamme), Éclat solaire (30, 15 s, niveau Nen 15), Pas fantôme (20, 12 s, Vitesse III + invisible 3 s).
- [ ] En jeu, Youpi : Poing colossal (25, 7 s), Lames organiques (20, 5 s, poison), Ailes monstrueuses (25, 15 s, chute lente + Vitesse II 8 s).
- Valeurs proposées par moi : corrige-les dans `docs/a-trancher.md`.

## Lot 7 des techniques de ton fichier (20 nov)
- [x] CI : `lot7ElectricFistSlowsAndSmashHits` + `everyTechniqueOfEveryKitRuns`.
- [ ] En jeu : Silva Frappe du patriarche (20 aura, 5 s) ; Zeno Aura Blast (30, 13 s) ; Razor Smash (30, 8 s, ralentit) ; Kalluto Éventail de papier (20, 6 s, affaiblit) ; Killua Coup de yoyo (10, 3 s, 5 blocs) et Poing électrique (20, 7 s, quasi paralysie 2 s).
- Valeurs proposées par moi : corrige-les dans `docs/a-trancher.md`.

## Lot 8 des techniques de ton fichier (20 nov)
- [x] CI : `lot8CloudBlindsAndGuardResists` + `everyTechniqueOfEveryKitRuns`.
- [ ] En jeu : Pitou Griffure (15 aura, 3 s) ; Illumi Pluie d'aiguilles (35, 12 s) ; Leorio Percussion interne (20, 6 s) ; Hisoka Poing élastique (20, 5 s, portée 6 blocs) ; Tsezguerra Défense instantanée (20, 12 s, Résistance II 3 s) ; Morel Nuage aveuglant (30, 15 s) ; Machi Fil étrangleur (25, 9 s).
- Valeurs proposées par moi : corrige-les dans `docs/a-trancher.md`.

## Lot 9 des techniques de ton fichier (20 nov)
- [x] CI : `lot9ChargeBuffsUvogin` + `everyTechniqueOfEveryKitRuns`.
- [ ] En jeu : Biscuit Gros coup de poing (30 aura, 10 s) ; Phinks Coup de bélier (20, 6 s) ; Uvogin Charge (25, 14 s, Vitesse II + Force II 5 s) ; Knuckle Toritaten (20, 7 s, étourdit).
- Valeurs proposées par moi : corrige-les dans `docs/a-trancher.md`.

## Lot 10 des techniques de ton fichier (21 nov)
- [x] CI : `lot10UltrasoundRevealsAndPounceLeaps` + `everyTechniqueOfEveryKitRuns`.
- [ ] En jeu : Kurapika Lancer de chaîne (20 aura, 6 s, 10 blocs) ; Leorio Détection par ultrasons (15, 20 s, 16 blocs, Lueur 6 s) ; Machi Toile invisible (30, 16 s, 5 blocs) ; Pitou Bond (20, 7 s, 6 blocs vers l'avant).
- Valeurs proposées par moi : corrige-les dans `docs/a-trancher.md`.

## Hatsu des mobs, lot 1 (21 nov)
- [x] CI : `newSpeciesGetTheirOwnKit` + `eachMobGetsItsOwnHatsu` (lance toutes les techniques de mob).
- [ ] En jeu : 15 nouveaux Hatsu, un par espèce : gardien (Rayon), shulker (Projectile lévitant), phantom (Piqué), abeille (Dard), pillard (Salve), piglin (Cupidité), vex (Lame spectrale), warden (Onde sonique), wither (Dépérissement), Ender Dragon (Souffle), tortue (Carapace), dauphin (Grâce), panda (Roulade), poulpe (Nuage d'encre). Le grand gardien partage celui du gardien.
- Ajoutés ensuite : poisson d'argent (Frénésie) et chauve-souris (Cri strident). Pas encore : warden à deux techniques ; l'araignée des cavernes garde celui de l'araignée. Coûts et recharges proposés par moi : corrige-les dans `docs/a-trancher.md`.

## Poing élastique de Hisoka : ramène la cible (22 nov)
- [x] CI : `elasticFistPullsTheTargetBack`.
- [ ] En jeu : le Poing élastique tire maintenant l'ennemi touché vers Hisoka.

## Passifs de ton fichier, lot 1 (22 nov)
- [x] CI : `passivesOfTheFileReduceDamageAndBoostFists` (inclut la rage de Youpi).
- [ ] En jeu : Phinks Bagarreur (+15 % à mains nues, résistance au recul 0,4) ; Meruem Corps du Roi (1 PV / 2 s, dégâts physiques −15 %) ; Youpi Corps monstrueux (physiques −15 %, explosions −50 %) ; Morel Capacité pulmonaire (respire sous l'eau). Valeurs proposées par moi.
- [ ] En jeu : Youpi Colère grandissante : la jauge de rage monte en encaissant (dégâts ×2, −2/s), poings +10/+25/+40 % dès 20/50/80, la Rafale de rage la consomme (jusqu'à ×2). Valeurs proposées par moi.
- [ ] En jeu (passifs lot 2, valeurs proposées par moi) : Meruem Génie du combat (+5 % par coup déjà porté au même ennemi, 20 % au plus) et Synthèse d'aura (achever un utilisateur du Nen rend 25 % d'aura) ; Pouf Esprit calculateur (ennemi observé : −15 % de dégâts reçus, 8 retenus) ; Machi Sens du fil (ennemis ralentis à 16 blocs brillent, Vitesse I si un ennemi à 4 blocs) ; Palm Wink Blue (3 cibles suivies, la plus ancienne remplacée) ; Neferpitou Garde royal (vitesse +20 %, aura +1,5/s, physiques −10 %).
- Pas encore : récupération des capacités de Pouf et Youpi par Meruem ; +20 % d'aura max, taille de En et Nen post mortem de Pitou.

## Monde créé par les menus (23 nov)
- [x] CI : second client `runCiMenuClient` (ci/scenario-menu.txt) : écran titre → Solo → Créer un monde → Créer un monde, puis surveillance du chargement (capture toutes les 20 s, échec après 150 s). Résultats : `ci-report-menu.txt`, `menu-screenshots/`, `client-menu-latest.log` sur la branche ci-results.
- [ ] Chez toi : si le jeu charge en boucle, dis-moi à quel écran (barre de chargement, « Préparation du monde »…) et colle-moi la fin de `logs/latest.log`.

## Dernières techniques et corrections (23 nov)
- [x] CI : `stealChainDrainsAuraAndCocoonImmobilizes`, `leaderJumpsInTheAirWithinItsLimit`, `newSpeciesGetTheirOwnKit` (Warden à deux Hatsu), `strongKickSlamsIntoTheGround`.
- [ ] En jeu : Cocon de renaissance (Pouf, 3 blocs : cible immobile, protégée et régénérée 8 s) ; Chaîne voleuse (Kurapika, 8 blocs : vole la moitié de l'aura de la cible) ; Cœur du séisme (second Hatsu du Warden, onde de 7 blocs).
- [ ] En jeu : doubles sauts du Meneur (Tsezguerra) : appuie sur saut en l'air ; 1 saut + 1 par 20 de Volonté, 5 au plus ; le compteur revient au sol. Le paquet client n'est testé qu'en jeu.
- [ ] En jeu : coup de pied dans le vide : n'abîme plus que les blocs à portée de pied (3 blocs), plus rien au loin.
- [ ] En jeu : monde créé par le menu : se charge en ~50 s (la Tour Céleste n'est plus cherchée en générant des chunks).
- Pas encore : Cocon (éveil au Nen, hybride), Chaîne voleuse (vol temporaire d'un Hatsu), changement d'apparence de l'Aiguille de métamorphose d'Illumi.
- CI : un run commencé n'est plus annulé par un push suivant (il attend).

## Dirigeable (carte du monde)
- Poser une « Borne du dirigeable » (onglet créatif), clic droit : la carte s'ouvre avec 11 repères (gris = pas assez de Jenny).
- Clic sur un repère → confirmation avec le prix → téléportation sur une plateforme (sol de briques + borne pour repartir). Le billet est retiré des Jenny.
- Sans borne à portée (6 blocs), le serveur refuse ; `/hunter airship` (opérateur) ouvre la carte de n'importe où.
- Pour l'instant la destination est dans le même monde (coordonnées de la carte) ; la dimension « monde HxH » viendra ensuite.
- Le dirigeable mène maintenant dans la dimension « monde HxH » (`/hunter airship <destination>` en opérateur pour y aller directement, ex. `whale_island`). Vérifier : mer autour des îles, plages, biomes par pays, grottes sous terre.
- Règle `hxhSpawnInWorld` (vrai par défaut) : un nouveau joueur apparaît sur l'île de la Baleine dans le monde HxH (et y réapparaît). Faux : il reste dans le monde normal. À tester en créant un monde neuf.
- Montagne Kukuroo : `/hunter airship kukuroo_mountain` puis aller vers le sud-est (domaine Zoldyck). Vérifier la porte de fer fermée, la petite porte des visiteurs (boutons), Zebro et sa maison, Mike (loup géant, enclos à l'ouest), le manoir et ses majordomes, le cimetière. Liste complète : `docs/kukuroo.md`.
- `/hunter place export <nom> <de> <à>` (opérateur) : exporte une construction en fichier de lieu (coin « de » = couche de sol).
- Les 7 portes (Kukuroo) : entrer par la grande porte, clic droit sur chaque porte. Force requise : 4, 10, 18, 28, 42, 60, 80 (portes de 2 à 128 t). Trop faible : message avec le poids ; réussite : toute la plaque s'ouvre 5 s puis se referme, et la Force s'entraîne. `/hunter stat` ou commandes de niveau pour tester.
- Mike (Kukuroo) : il attaque les intrus (150 PV, 14 de dégâts) mais reste près de son enclos. Lui donner de la viande (clic droit) : après 3 offrandes il devient ami et n'attaque plus ce joueur.
- Yorknew City : `/hunter airship yorknew_city` (borne sur une place pavée). Vérifier les deux avenues noires à marquage blanc, les tours de verre de hauteurs variées, la grande maison de ventes au centre (colonnes, tapis rouge, estrade, commissaire-priseur) et, au sud-est, l'entrepôt en ruine de la Brigade Fantôme (13 sièges autour d'un feu). Générateur : `tools/gen_yorknew.py`.
