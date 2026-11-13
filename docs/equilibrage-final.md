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
