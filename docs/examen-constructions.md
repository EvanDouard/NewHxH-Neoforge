# Examen Hunter : construire les sites

Ce guide liste les structures de l'Examen Hunter à construire et les repères à y poser.
Le principe est le même que pour la Tour Céleste : construis en créatif, pose les repères, sauvegarde,
envoie les fichiers.

## Les repères

Tous sont dans l'onglet créatif du mod. Pose-les **sur le sol, à hauteur des pieds** : ils sont remplacés par
de l'air quand la structure est posée.

| Bloc | Lettre | Réglage | Rôle |
| --- | --- | --- | --- |
| Repère : PNJ (réglable) | P (violet) | Clic droit = PNJ suivant, accroupi = précédent. Une étiquette flottante montre le PNJ choisi. | Place d'un PNJ : Navigateur, Cuisinier de Zaban, Beans, Satotz, Buhara, Menchi, Netero, hôtesse, arbitre |
| Repère : point de parcours | # (orange) | Clic droit = +1, accroupi = −1. Étiquette « Parcours n°… ». | Chemin que suit un guide (Satotz). Le **plus grand numéro est l'arrivée** de l'épreuve. |
| Repère : apparition de proies | G (marron) | — | Où apparaissent les proies d'une épreuve de chasse |
| Repère : arrivée | E (vert) | — | Où le joueur apparaît en arrivant dans le site |

Les repères de la Tour Céleste (H, R, J, A) restent valables là-bas.

## Le marathon de Satotz

- Satotz part de **sa place** (repère PNJ réglé sur Satotz), puis passe par les points de parcours n°1, n°2, n°3…
  dans l'ordre, jusqu'au plus grand numéro, qui est l'arrivée.
- Il va à peu près à la vitesse d'un joueur sous Vitesse I : un joueur qui marche décroche, il faut sprinter.
  La vitesse est réglable dans la config serveur (`exam.marathonSpeed`).
- Rester plus de 5 secondes trop loin de lui (`exam.lostSeconds`) = **examen raté** : « Retentez l'année
  prochaine ! ». Le joueur doit attendre un jour Minecraft (`exam.retryTicks`) **et** trouver un autre centre
  d'examen : le Navigateur du centre qui l'a recalé refuse de le reprendre.
- Conseils pour le tracé : des points tous les 10 à 20 blocs, pas de saut de plus d'un bloc entre deux points,
  et un chemin assez large (3 blocs) pour que Satotz ne se coince pas. Les escaliers marchent.

Pour essayer un parcours sans structure : `/hunter exam setup` pose les PNJ, et Satotz suit une boucle de
démonstration de 8 points.

## Les sites à construire

Les noms de fichier sont des propositions ; garde le préfixe `hxh:exam/`.

| Ordre | Site | Fichier | Repères | Contenu |
| --- | --- | --- | --- | --- |
| 1 | Port de Dolle | `hxh:exam/dolle` | E, P = Navigateur | Petit port, quais, auberge. Le Navigateur lance l'examen : **chaque site avec un Navigateur est un « centre d'examen »**. |
| 2 | Zaban : restaurant | `hxh:exam/zaban_restaurant` | E, P = Cuisinier de Zaban | La gargote d'entrée. Plus tard : le bon plat fait descendre la salle (ascenseur). |
| 3 | Hall d'inscription souterrain | `hxh:exam/hall_inscription` | E, P = Beans, P = Satotz | Grande salle sous terre. Beans donne la plaque, Satotz attend au départ du tunnel. |
| 4 | Tunnel du marathon | `hxh:exam/tunnel` (ou dans le hall) | Points de parcours 1 → N | Long tunnel puis escalier sans fin. Le dernier point est la sortie. Le plus simple : tunnel et hall dans **la même structure**, pour que Satotz enchaîne. |
| 5 | Réserve de Biska | `hxh:exam/biska` | E, P = Buhara, P = Menchi, G (plusieurs) | Clairière, cuisine en plein air, grand portail, zone de chasse avec les proies. |
| 6 | Salle de la licence | `hxh:exam/hotel` | E, P = Netero | Salle de l'hôtel de l'Association où Netero remet la licence. |
| plus tard | Marais de Numele | `hxh:exam/numele` | E, points de parcours | Sortie du tunnel, brouillard, mobs pièges. |
| plus tard | Mont Split, dirigeable, Tour de Trick, île de Zevil | `hxh:exam/…` | selon l'épreuve | Voir la page « Structures à construire » du schéma. |

## Sauvegarder et envoyer

```
/hunter structure save hxh:exam/<nom> X1 Y1 Z1 X2 Y2 Z2
```

Le fichier arrive dans `saves/<monde>/generated/hxh/structures/exam/<nom>.nbt`. Envoie-le, avec une phrase
sur ce que tu veux à chaque endroit si besoin : je branche les repères sur les étapes de la quête et je vérifie
en CI. Les repères réglables gardent leur réglage dans le fichier.
