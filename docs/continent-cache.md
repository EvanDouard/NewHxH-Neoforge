# Continent caché : Dark Continent, Nouveau Continent, lac Mobius

Document de réflexion (27 nov). **Rien ici n'est décidé** : c'est une réserve d'idées tirées du manga, des deux images d'Evan et de recherches, « on verra ce qu'on fait ou non ». Les mentions « (spéculation) » sont des théories de fans, pas du canon.

## 1. État actuel dans le mod
- Variable par joueur `darkContinentKnown` (voir `docs/monde-hxh.md`) : à `false`, carte classique ; à `true`, carte « Lac Mobius » (`world_map_dark.png`, contours tracés sur la carte d'Evan : lac à deux lobes, monde connu au centre dans un cadre en pointillés, Nouveau Continent à l'est, Continent Noir tout autour).
- Cinq petits carrés (grisés, non cliquables) sur les côtes du Continent Noir : Saherta, Begerossé, Kukan'yu, Mimbo, Ochima. Info-bulle « pas encore accessible ».
- **Purement visuel** : le monde jouable s'arrête à la carte actuelle (7968 × 5632 blocs, mer profonde au-delà). On ne peut ni y aller ni le voir en jeu.

## 2. À faire pour le rendre réel (feuille de route, rien d'engagé)
1. **Étendre l'océan** du monde HxH jusqu'à atteindre les terres de la carte : le lac Mobius entoure le monde connu, puis le Continent Noir commence sur ses rives. Ordre de grandeur d'après la carte : le monde connu occupe ~45 % de la largeur du lac ; avec l'échelle actuelle (1 px de la toile ≈ 7,4 blocs) la toile fait environ **17 800 × 10 500 blocs**, dont ~65 % de lac. Le terrain étant calculé à la volée par `HxHTerrainDensity`, il faut surtout agrandir la carte de référence (`cells.txt`), pas stocker des chunks.
2. **Ajouter réellement le Nouveau Continent** (complexe d'îles dans le lobe est, forme tracée dans `tools/ref/new_continent_mask.png`) : terrain, côtes, biome, premier lieu (arrivée du Black Whale).
3. **Dark Continent et Nouveau Continent = biomes spéciaux** : arbres spéciaux (nouvelles essences, champignons géants, plantes carnivores…), mobs qui n'apparaissent que là, ciel / brume / ambiance propres, musique. Biomes sur mesure (JSON de worldgen), contrairement au reste du monde qui réutilise des biomes vanilla.
4. **Bordures de l'océan** : pour l'instant il n'y a pas de téléportation ni de voyage vers ces zones (le dirigeable ne les dessert pas). Prévoir comment on y arrive (voir §6).
5. **Créatures du lac Mobius** dans l'eau (voir §5), pas seulement sur terre.
6. Poser les **cinq rives** (une par expédition, voir §3) à la bonne direction du lac.

## 3. Ce que dit le canon (manga)
Sources : voir §8.

**Le lac Mobius et ses divisions.** Grand lac au centre du Continent Noir ; le monde connu n'en est qu'une petite partie. Trois zones : *eaux territoriales humaines* (petite zone occupée par l'humanité), *eaux sauvages* (où se trouve le Nouveau Continent), *eaux sombres*. Des *eaux frontières* séparent les deux dernières ; leur passage est régulé par le **Gardien** (une race de bêtes magiques). Pour franchir les frontières océaniques du monde connu il faut un **Guide**, invoqué par le Gardien. Des courants et d'autres facteurs gênent l'accès.

**Le Nouveau Continent n'est pas le Continent Noir.** C'est un complexe d'îles près de sa frontière, jamais colonisé. L'expédition financée par Kakin (menée par Beyond Netero) y aboutit : le V5 a autorisé le voyage à condition que le navire (le **Black Whale**) y débarque tous les passagers sauf les membres de l'Association des Hunters. Le public l'a pris pour le Continent Noir (au ch. 359, les passagers civils voient une carte de l'Afrique à l'envers).

**Chiffres.** Traité d'Inviolabilité du V5 (~200 ans) qui interdit les voyages. 149 voyages non officiels recensés, 5 seulement avec des survivants, soit 28 personnes (~0,04 %). Seuls 3 survivants ont passé le test d'évaluation et mené une vie normale. Netero, Zigg Zoldyck et Linne sont revenus de leur première expédition.

**Les cinq expéditions officielles (une par rive) = les cinq repères de la carte.**

| Pays (V6) | Rive du lac | Lieu | Ressource | Menace | Survivants |
|---|---|---|---|---|---|
| Begerossé | nord-est | chaîne de montagnes abruptes | *Unmanned Rock* (minerai qui produit de l'électricité sous l'eau : une perle ≈ 20 000 kW/jour) | **Pap** (bête qui garde des humains comme animaux) | 7 sur 1 000 |
| Saherta | nord | ruines d'une cité labyrinthe à 400 km dans une forêt | *Herbe contre toutes les maladies* | **Brion** (arme botanique : plante qui marche) | 2 |
| Ochima | sud-est lointain | marais | *Nitro Rice* (secret de longévité) | **Hellbell** (serpent à deux queues, rend les proies homicides) | 11 (99 % de pertes) |
| Mimbo | sud-est | inconnu | *Trinity Elixir* (« solution mère » de tous les liquides) | **Ai** (forme de vie gazeuse) | 3 |
| Kukan'yu | sud | inconnu | *Metallion* (plante d'alchimie) | **Zobae** (maladie de l'immortalité) | 6 dont Beyond |

Niveaux de menace cités (sources de fans) : Brion A à B+, Pap A, Ai A, Hellbell B+ à A, Zobae B+ à A, tous au-dessus des Fourmis-Chimères. Les victimes d'Ai sont revenues dans le monde connu (lien avec Nanika/Alluka). La Reine des Fourmis-Chimères ne serait pas née dans le monde connu (venue du Continent Noir ou d'une île du lac).

**Le livre « Voyage au Nouveau Monde »** (Don Freecss) : guide d'un voyageur qui a tenté de longer seul les rives du lac il y a ~300 ans ; deux éditions (Est et Ouest), la version Ouest est introuvable. Pistes d'objets / de quête.

**Faune et décor.** Créatures souvent gigantesques ; inspiration annoncée : forêt amazonienne ; tribus non contactées possibles, hostiles aux étrangers ; reliefs : montagnes, forêts, marais, ruines antiques.

## 4. Extraction des deux images d'Evan
**Image 1 — double page du manga (« This place… »).** Paysage montrant en bas une prairie humide / marécage avec des plantes étranges (tiges à bulbes, « lollipops », champignons, petites formes rondes) ; un grand prédateur pâle à crête dorsale en pointes, entouré de petits animaux blancs ronds (comme des petits, ou des moutons) ; en haut à gauche, deux colosses à gueules ouvertes (type dragon ou requin) qui émergent des falaises et de la forêt ; au centre-haut, une créature géante segmentée (anneaux, bouche ronde) hérissée de longs filaments ; à droite, une grosse bête à dents de castor, au museau sombre, avec une grande poche / membrane à motifs, et derrière elle un corps annelé massif ; un gros escargot / coquillage ; forêts très denses avec mousse pendante ; sur la gauche un oiseau à plumage qui ressemble à des flammes. Idées : mégafaune qui se croise de loin sans qu'elle attaque, **échelle écrasante** (créatures visibles à l'horizon), flore bizarre en tous genres.

**Image 2 — vue de la carte (anime), le lac et sa rive.** Le monde connu est un **disque bleu lumineux** en bas à gauche (dans l'eau pâle, peu profonde). Rangée de **créatures dans le lac, le long de la côte** : serpents de mer sombres, hérissés de piques ; trois blobs bleus ronds (méduses ou bulles à visage) ; une grosse créature verte ronde (type tortue ou hippopotame) avec une petite tête dessus ; une créature rose à tentacules (pieuvre / calmar) ; une baleine bleue qui souffle ; écume, vagues blanches et **tourbillons**. Une **plage jaune** : palmiers à feuilles épineuses, embouchures de rivières, falaises érodées orange (canyon), un cratère ou une grotte, un grand œuf blanc, des reptiles bruns (dragons / lézards) près du rivage, un crocodile vert dans l'eau. En haut à gauche : champs de champignons violets et de fleurs roses ; en haut : un **volcan** fumant. À droite : l'intérieur **sombre** (violet / rouge) avec des orages, des flèches noires épineuses (forêt ou ville), des **ruines de bâtiments** et des créatures rouges à tentacules qui volent. Structure utile : **zones concentriques** lac → plage → falaises et forêts → intérieur sombre.

## 5. Idées pour le mod (au choix)
**Lac Mobius.** Créatures aquatiques : serpents de mer, méduses bleues rondes, tortue géante verte, pieuvre rose, baleine, crocodile ; tourbillons ; zones d'eaux (territoriales / sauvages / sombres) de plus en plus dangereuses ; Gardien et Guides aux eaux frontières (barrière de passage tant qu'on n'a pas de Guide).

**Cinq rives, cinq biomes.**
- Saherta (nord) : forêt + ruines d'une cité labyrinthe (structure), plantes marcheuses, *Herbe contre toutes les maladies* (plante de soin rare), mob/boss **Brion**.
- Begerossé (nord-est) : montagnes abruptes, minerai *Unmanned Rock* (bloc qui génère de l'énergie dans l'eau), **Pap**.
- Ochima (sud-est lointain) : marais, culture *Nitro Rice* (nourriture qui change la longévité), **Hellbell** (serpent qui rend agressif : effet « homicidal » sur le joueur/les mobs).
- Mimbo (sud-est) : liquides étranges (*Trinity Elixir*, fluide spécial), **Ai** (nuage de gaz mortel, lien avec Nanika).
- Kukan'yu (sud) : plantes d'alchimie (*Metallion*), **Zobae** (effet de statut « immortalité maudite »).

**Mécanique générale.** Accès par voie maritime seulement (Black Whale, bateaux), **Traité d'Inviolabilité** (permis / interdiction), survie très difficile (taux de 0,04 % : mort définitive ou perte d'objets ?), mobs plus forts que le monde connu, effets de statut propres aux fléaux, ressources uniques (les cinq), tribus hostiles non contactées, ruines à explorer. Le Nouveau Continent est **plus sûr** que le Continent Noir (étape d'arrivée) mais présenté comme un leurre.

**Quêtes / scénario.** Découvrir l'existence du Continent Noir (ce qui passe `darkContinentKnown` à vrai : événement à définir), livre « Voyage au Nouveau Monde », expédition du Black Whale, enquête sur les fléaux, rencontre des Gardiens.

## 6. Questions ouvertes (pour Evan)
- Comment on y va : bateau, Black Whale, téléportation d'un PNJ, ou carte seulement pour l'instant ?
- Le Continent Noir est-il jouable en entier ou limité à quelques rives ?
- Que fait le joueur qui l'explore trop tôt (monstres trop forts, bloqué, ou libre) ?
- Quel événement passe `darkContinentKnown` à vrai ?
- Les cinq fléaux : boss à tuer, événements, ou menaces qu'on ne peut que fuir ?
- Nom à garder : « Minbo » ou « Mimbo » (la carte d'Evan écrit Mimbo ; le mod l'affiche en Mimbo depuis le 27 nov) ?

## 7. Théories de fans (spéculation, à ne pas prendre pour du canon)
- Nanika serait un être d'Ai ; Zigg Zoldyck l'aurait ramené de son expédition avec Netero, d'où la possession d'Alluka.
- L'Association des Hunters aurait été fondée pour atteindre le Continent Noir.
- Le clan Kurta et/ou la mère de Killua viendraient du Continent Noir.
- Le monde de HxH serait à l'intérieur d'un être gigantesque (une baleine) ; le symbole de l'infini sur la robe de Kurapika évoquerait le lac Mobius et le sort du Gardien.
- Greed Island serait une répétition ou un outil du scénario du Continent Noir.
- Les Fourmis-Chimères viendraient du Continent Noir (confirmé par Ging pour la Reine).
Ces idées peuvent inspirer des quêtes ou des indices, sans engagement.

## 8. Sources
- Fandom HxH, « Dark Continent » : https://hunterxhunter.fandom.com/wiki/Dark_Continent
- Fandom HxH, « Lake Mobius » : https://hunterxhunter.fandom.com/wiki/Lake_Mobius
- Fandom HxH, chapitre 344 (les cinq menaces) : https://hunterxhunter.fandom.com/wiki/Chapter_344
- Soapcentral, guide des êtres du Continent Noir : https://www.soapcentral.com/anime/hunter-x-hunter-a-guide-dark-continent-beings-mentioned-manga
- Sportskeeda, tout ce qu'on sait du Continent Noir : https://sportskeeda.com/anime/hunter-x-hunter-everything-revealed-dark-continent-far
- Ranker, théories de fans : https://www.ranker.com/list/interesting-fan-theories-about-hunter-x-hunter/keithmurphy
- Carte du lac Mobius : image d'Evan (de Sharpsider) ; images 1 et 2 : fournies par Evan.
