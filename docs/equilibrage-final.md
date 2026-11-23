# Équilibrage final : à revoir ensemble

Valeurs posées par Claude pour avancer. Elles marchent en jeu, mais on les revoit ensemble au grand rééquilibrage.

## Hunter gourmet et cuisine (13 nov)
| Quoi | Valeur actuelle | Où |
|---|---|---|
| Plat II / III / IV en boutique (après « Le plat légendaire ») | 300 / 900 / 2500 J | `data/hxh/shops/hunter_gourmet.json` |
| XP de Vitalité par plat | I 25 · II, III, IV selon les gammes (×1,6 / ×2,5 / ×4 environ) | `NutritiousDishItem`, `TrainingTier` |
| Nourriture des plats | 6 + 2 × gamme, saturation 0,6 + 0,1 × gamme | `NutritiousDishItem` |
| Viande de renard-ours crue / cuite | 4 (sat. 0,3) / 10 (sat. 0,9) | `ModItems` |
| Rachat de la viande crue / cuite | 8 / 12 J | boutique du gourmet |
| Recette du plat III | carnet + plat II + 2 viandes de renard-ours cuites + carotte dorée + baies lumineuses | `data/hxh/recipe/nutritious_dish_3.json` |
| Recette du plat IV | carnet + plat III + 2 viandes cuites + pomme d'or + saumon cuit + fiole de miel | `data/hxh/recipe/nutritious_dish_4.json` |
| Quête « Le plat légendaire » | 16 carottes dorées, 16 saumons cuits, 1 pomme d'or enchantée → plat IV + 500 J | `quests/gourmet_plat_legendaire.json` |
| Quête « La recette secrète » | 6 renards-ours, 12 viandes cuites, 4 pommes d'or, 2 plats III, 1 cœur de la mer → carnet + 1000 J | `quests/gourmet_recette_secrete.json` |

## Renard-ours
| Quoi | Valeur actuelle |
|---|---|
| Vie, dégâts, vitesse, recul | 60 PV, 9, 0,27, résistance au recul 0,6 |
| Apparition | forêts, poids 6, 1 à 2 |
| Butin | 2 à 4 viandes crues, 1 à 3 cuirs |
| Comportement | celui de l'ours polaire (neutre, la mère défend ses petits) : décidé par Evan, pas d'apprivoisement |

## Great Stamp
| Quoi | Valeur actuelle |
|---|---|
| Vie, dégâts, recul | 50 PV, 7, projection 1,2, résistance au recul 0,7 |
| Museau blindé | coups de face × 0,3 |
| Apparition | forêts (chênes, bouleaux, forêt noire), poids 4, 2 à 3 |
| Butin | 4 à 7 côtelettes de porc, 1 à 2 cuirs |

## Techniques ratées
| Quoi | Valeur actuelle |
|---|---|
| Seuils (dégâts d'un coup) | éclats ≥ 8, trou de rayon 1 ≥ 15, cratère de rayon 1,8 ≥ 25 |

## Coup de pied fort et bras de fer
| Quoi | Valeur actuelle |
|---|---|
| Seuil « fort » | Force ≥ 30 (config `combat.strongFromStrength`) |
| Poussée du coup de pied fort | (2,6 + Force × 0,02) × (1 − résistance au recul), légère montée 0,25 |
| Trou dans les murs | rayon 1,6, 2 murs au plus, la cible garde 70 % de sa vitesse entre deux |
| Bras de fer | mise 100 J (gain ×2), 40 XP de Force ; champion = Force du joueur + 10 ; zone 0,2 + 0,003 × écart de Force |

## Tour Céleste
| Quoi | Valeur actuelle |
|---|---|
| Hisoka | 235–240e étages, son kit de joueur, stats de l'étage + 5 |
| Gido, Sadaso, Riehlvelt, Kastro | 200–209, 210–219, 220–224, 225–234 ; techniques dans `ArenaChallengers` |
| Paris | cote = niveau Hunter + ½ niveau de Nen, bornée 15–85 %, 10 % au guichet, mises 100/500/1000 J |

## Nen, familles, économie
| Quoi | Valeur actuelle |
|---|---|
| Coups secrets de famille | recharges 15 s / 90 s / 40 s / 45 s ; Main-griffe 6 + Force × 0,08 + Vitesse × 0,06 |
| Serments | ×1,25 à ×3 ; sceau 10 min, 1 jour après « Tout sacrifier » |
| Malédiction de Nen | 10 % sur un manieur tué (Nen ≥ 20), 1 jour, dégâts −15 %, aura ÷2 ; exorcisme 300 J (÷2 avec licence) |
| Enchères | lots et prix dans `data/hxh/auctions/yorknew.json` ; limite des enchérisseurs 75–125 % de la valeur |
| Contrats Zoldyck | 200 à 800 J, 15 à 25 min, +50 % pour un Zoldyck |
| Blacklist et rang Hunter | 400 à 5000 J ; étoiles : 200e étage, 3 captures, Nen 60 ; 1000 J × étoile |
| Seuils des impacts | faible < 8 PV, moyen 8–15, moyen + 15–25, très fort ≥ 25 |

## Retours du 13 novembre (lots A et B)
| Quoi | Valeur actuelle |
|---|---|
| Sanctuaires | chance par tronçon : niv. 1 1/700, 2 1/1400, 3 1/2800, 4 1/6000, 5 1/25000 ; au moins 400 blocs entre deux |
| Ko persistant | 10 s à maîtrise 0 → 2 min à maîtrise 100 (linéaire), plus consommé au premier coup |
| Serments | les stats sont multipliées pour de vrai (au-delà de 100) ; « Tout sacrifier » scelle tout le Nen 240 000 ticks (~10 jours) |
| PV et armure des cibles | Gyo 40, En 30 |
| Cratère de la frappe parfaite | demi-sphère de rayon 3,2 |
| Cible projetée qui percute un mob | 4 dégâts, garde 70 % de sa vitesse |
| Recharge des coups secrets | −0,5 % par niveau d'Endurance |
| Docteur Blythe maintenu | 20 d'aura au départ, puis 10 d'aura/s ; 2 PV toutes les 0,5 s ; 30 s au plus ; recharge 20 s au relâchement |
| Attaques de zone | coups de base dès Force + Vitesse + Endurance ≥ 180 ; Nen dès Volonté + niveau de Nen ≥ 120 ; voisins à 50 % des dégâts ; rayon 1,5 → 4 blocs |
| Techniques jamais en zone | liste `AreaScaling.SINGLE_TARGET` (aiguilles d'Illumi, chaîne-prison, fils de Machi, Bungee Gum, Hakoware, dettes, vol de Chrollo…) |

## Lot D (13 nov, soir)
| Quoi | Valeur actuelle |
|---|---|
| Serment de Kurapika | Chaîne-prison ×3, contre la Brigade seulement (joueurs de la Brigade, mobs dont le personnage est dans `member_kits` de ryodan.json) ; sinon le coup ne porte pas, serment rompu, mort |
| Serment de Feitan | Soleil levant ×1,8, seulement sous 50 % de vie (refus sinon, jamais de rupture) |
| Aigle-araignée | 16 PV, vol 0,7 ; montagnes au-dessus de y 80, poids 5, 1–2 ; pond un œuf toutes les 10–20 min ; butin 1–3 plumes + 25 % d'œuf |
| Heure supplémentaire / Service de jour | ×1,3 dans sa période, ×0,7 dans l'autre (nuit = 13000–23000 ticks ; pas de cycle = jour) ; un seul serment à la fois |
| Singe à visage humain | 24 PV, 4 dégâts, vitesse 0,3 ; marais, poids 20, 1–2 ; une réplique trompeuse ~toutes les 30 s à 24 blocs |
| Fourmi-Chimère soldat | 40 PV, 7 dégâts, armure 6 ; jungles, poids 12, 2–3 ; 30 % au Nen (niveau 35, prime 600 J, mob d'exemple `fourmi_chimere_soldat`) |

## Maîtrises et leçons de Wing (15 nov)
- Palier du Zetsu : In (maîtrise partagée avec le Zetsu) niveau **5** (`senses.zetsuUnlockInLevel`), ≈ 4 min d'In tenu. L'exemple « In jusqu'au niveau 30 » demanderait ≈ 6 h d'In **et** le niveau de Nen 30 (plafond de verre des maîtrises), soit ≈ 9 h : trop long pour une leçon de début. À relever si on veut un vrai palier tardif.
- Palier du Ken : Ten niveau **5** (`senses.kenUnlockTenLevel`).
- Étapes des autres leçons : niveau 1 de la maîtrise (In : niveau 2).
- Surcoût débutant : ×**2,5** à la maîtrise 0, redescend en ligne droite jusqu'à ×1 au niveau **20** (`aura.beginnerCostMultiplier`, `aura.beginnerCostUntil`). Postures et sens : leur maîtrise ; Ko : maîtrise du Ko ; Hatsu : niveau de Nen. Le soin par l'aura garde son propre barème.

## Graphe des techniques du Nen (19 nov)
Techniques de base enseignées par Wing : Ten, In, Ren, Hatsu. Le reste se débloque aux niveaux de maîtrise (plafond de verre : niveau de Nen, minimum 5). Valeurs **proposées**, à trancher (`NenTree.nodes()`) :
- Zetsu : In 5 (`senses.zetsuUnlockInLevel`) · Shu : Ten 8 (Ken + 3) · Ken : Ten 5 (`senses.kenUnlockTenLevel`) + Ren 3 · En : Ten 10 + Ren 8 · Gyo : Ren 5 + In 3 (le Gyo révèle l'In, vrai prérequis depuis le 20 nov).
- Ko : Ten 10, In 10, Ren 10, Gyo 5, Hatsu appris. Ryu : Gyo 15, Ko 10, Ken 10 (le lien Ken → Ryu est incertain dans le schéma de référence).
- Ces niveaux au-dessus de 5 demandent un niveau de Nen équivalent.

## Chute et impact au sol (19 nov)
- Dégâts de chute = vanilla × **0,6** × (1 − Volonté/100) : 0 à la Volonté 100 (`FallImpact.BASE_FACTOR`).
- Impact : éclats ≥ 4 blocs, onde ≥ 8, secousse ≥ 14, cratère (rayon 1 → 3,2) ≥ 20.

## Jajanken ultime (19 nov)
- Rayon d'explosion plafonné à **16** blocs (`NenProjectile.MAX_BLAST`) ; l'aura en trop n'ajoute plus que des dégâts. Blocs retirés du centre vers l'extérieur, 1500 par tick, sans particules ni mises à jour de voisins.

## Coût d'aura des débutants et fiches du carnet (19 nov, soir)
- Multiplicateur de coût : ×5 à la maîtrise 0 (`aura.beginnerCostMultiplier`), ×2 au niveau 5 (`aura.beginnerCostAtLevel5`), puis descente douce jusqu'à ×1 au niveau 20 (`aura.beginnerCostUntil`). Valable pour Ren, Ken, Gyo, En, Ko, Shu (maîtrise) et Hatsu (niveau de Nen).
- Ten et In : maintien payant de 0 à 5 (`aura.novicePostureDrain` = 5 aura/s à 0, nul à 5 ; la régénération rend 2/s) ; un joueur à court d'aura retombe en posture neutre sans pénalité. Ne s'applique qu'aux joueurs en apprentissage.
- Coup de pied fort : portée sur les blocs 8 + 0,15 × Force (24 au plus), dégâts ×1,8 pour les éclats/trous ; écrasement au sol dès 0,8 bloc/tick de chute, cuvette de 1 à 3,2 blocs selon la vitesse.
- Les valeurs affichées dans les fiches du carnet viennent des mêmes formules (donc de la configuration du serveur).

## Aura ×10, régénération et rééquilibrage du niveau max (20 nov)
| Quoi | Valeur actuelle | Où |
|---|---|---|
| Aura maximale | (100 + 4 × Volonté) × talent (Prometteur ×1 = 1000 affichés ; ×0,8 à ×4 selon le talent) | `NenModes.computeAuraMax` |
| Échelle d'affichage | ×10 (`NenCost.DISPLAY_UNIT`), coûts internes inchangés | `NenCost` |
| Ten / In débutant | 4 internes = 40 aura/s affichés à la maîtrise 0, nul au niveau 5 | `aura.novicePostureDrain` |
| Régénération d'aura | 0 jusqu'au niveau 4 ; (niveau − 4)/16 de la base du niveau 5 à 19 ; ×1 au niveau 20 ; ×3 au niveau 100 | `NenCost.regenFactor`, maîtrise `REGEN` |
| Sommeil | rend 100 % de l'aura, +80 XP de régénération | `NenModes.sleepAndEmpty` |
| Méditation | +1 XP de régénération toutes les 10 s ; avant l'éveil, la pierre multiplie le compteur par son palier (3/5/8/12/20) | `NenAwakening` |
| Force | +0,5 dégât/niveau (était 0,1) : +50 au niveau 100 | `stats.strengthDamagePerLevel` |
| Vitalité | +0,4 PV/niveau (était 0,2) : +40 au niveau 100 | `stats.vitalityHealthPerLevel` |
| Endurance | 0,1 PV plat/niveau (était 0,05) : 10 au niveau 100 | `stats.enduranceFlatPerLevel` |
| Repères | niveau 100 ≈ Meruem / Gon adulte ; Netero ≈ niveau 80–90 (comme les gardes royaux) | à régler avec les PNJ |

## Passifs et techniques ajoutés le 22–23 nov (valeurs proposées, à corriger dans `a-trancher.md`)
| Élément | Valeur | Où |
|---|---|---|
| Colère grandissante (Youpi) | rage = 2 × dégâts reçus (0–100), −2/s ; poings +10/+25/+40 % dès 20/50/80 ; Rafale ×(1 + rage/100) | `Characters.YOUPI_GROWING_RAGE` |
| Génie du combat (Meruem) | +5 % par coup déjà porté au même ennemi, 20 % au plus (16 ennemis retenus) | `Characters.MERUEM_KING` |
| Synthèse d'aura (Meruem) | +25 % d'aura max en achevant un utilisateur du Nen | `Characters.MERUEM_KING` |
| Esprit calculateur (Pouf) | −15 % de dégâts d'un ennemi observé, 8 retenus | `Characters.POUF_CALCULATING` |
| Sens du fil (Machi) | 16 blocs (ennemis ralentis brillent) ; Vitesse I si ennemi à 4 blocs | `Characters.MACHI_THREAD_SENSE` |
| Wink Blue (Palm) | 3 cibles suivies, lueur renouvelée chaque seconde | `Characters.PALM_WINK_BLUE` |
| Garde royal (Pitou) | vitesse +20 %, aura +1,5/s, physiques −10 % | `Characters.PITOU_ROYAL_GUARD` |
| Cocon de renaissance (Pouf) | 70 aura, 90 s, Nen 30, 3 blocs, 8 s d'incubation | `Characters.POUF_COCOON` |
| Chaîne voleuse (Kurapika) | 40 aura, 30 s, Nen 20, 8 blocs, vole 50 % de l'aura de la cible | `Characters.KURAPIKA_STEAL_CHAIN` |
| Cœur du séisme (Warden) | 40 aura, 12 s, 7 blocs, 9 dégâts, recul 1,6 | `MobTechniques.WARDEN_QUAKE` |
| Sauts en l'air du Meneur | 1 + Volonté/20, 5 au plus | `Characters.airJumpsAllowed` |
| Coup de pied dans le vide | portée sur le décor 3 blocs (était jusqu'à 24) | `Combat.KICK_BLOCK_REACH` |
