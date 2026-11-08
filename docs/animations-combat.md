# Modifier les animations de combat

Tout se passe dans **`client/CombatAnimClient.java`** (le dessin) et **`combat/CombatAnim.java`** (la liste et les durées).
Le serveur choisit seulement *quelle* animation part ; changer leur aspect ne touche qu'au client.

## Tester vite

1. Lancer le client (`runClient`), monde en créatif.
2. `/hunter spawn mannequin` puis, en le regardant de profil :
   `/hunter anim @e[type=husk,limit=1,sort=nearest] charged_punch` (ou `punch_main`, `punch_off`, `charge`, `kick`).
3. Sur soi en vue F5 : `/hunter anim @s punch_main`.
4. Après une modification du code : **Build → Build Project** dans IntelliJ, puis le *hot swap* applique souvent le
   changement sans relancer le jeu (sinon relancer `runClient`).

## Les repères

- **Angles en radians** : 1 rad ≈ 57°, `Mth.HALF_PI` = 90°. `Mth.DEG_TO_RAD * 30` = 30°.
- **Bras / jambes (`xRot`)** : `0` = pendant le long du corps, **négatif = vers l'avant/le haut**
  (`-HALF_PI` = bras tendu devant), **positif = vers l'arrière**.
- **`yRot`** : rotation autour de l'axe vertical (bras qui part vers le centre ou vers l'extérieur).
- **`zRot`** : écarte le bras sur le côté (signe opposé entre bras droit et gauche).
- **`model.body.yRot`** : rotation du buste ; appeler ensuite `shoulder(model, …)` pour que les épaules suivent.
- **`main` / `off`** : bras de la main principale / l'autre (gère les gauchers tout seul).

## Le temps : la fonction `strike(t, sortie, maintien, retour)`

Renvoie un coefficient **k** qui va de 0 (pose normale) à 1 (coup au maximum) puis revient à 0 :

```
k  1 |    ______
     |   /      \
     |  /        \___
  0  |_/              \____
       sortie maintien retour   (en ticks, 20 ticks = 1 s)
```

Exemple, coup de poing de base : `strike(t, 2f, 1f, 4f)` → part en 2 ticks, reste 1 tick, revient en 4.
**Pour un coup plus sec**, baisser la sortie ; **plus lourd**, monter le maintien et le retour.

La durée totale doit rester ≤ à celle de `CombatAnim` (`PUNCH_MAIN(7)` = 7 ticks), sinon l'animation est coupée :
si on allonge l'une, allonger l'autre.

## Où changer quoi (vue extérieure : `applyToModel`)

| Animation | Bloc | Ce qu'on peut changer |
|---|---|---|
| Poings droit/gauche | `case PUNCH_MAIN, PUNCH_OFF` | `strike(...)` (rythme), `0.25f` (rotation du buste), `guardArm(..., 0.8f)` (garde) |
| Armement | `case CHARGE` | `0.75f` (bras en arrière), `0.35f` (bras écarté), `-0.35f` (buste tourné), tremblement `0.04f` |
| Frappe chargée | `case CHARGED_PUNCH` | `strike(t, 2f, 3f, 7f)`, `0.55f` (autre bras en arrière), `0.45f` (buste) |
| Coup de pied | `case KICK` | `-1.45f` (hauteur de jambe, ~83°), `0.5f` (bras écartés) |
| Garde | `guardArm` | `-1.2f` (hauteur du bras de garde) |

Exemple : **un uppercut à la place du poing droit** — dans `case PUNCH_MAIN, PUNCH_OFF`, viser plus haut :

```java
float aim = -Mth.PI * 0.75f;        // bras levé à ~135° au lieu de tendu devant
punchArm(hit, aim, look, k);
```

## Vue à la première personne : `onRenderHand` et `arm(...)`

`arm(renderer, event, côté, thrust, back, reach)` :
- `thrust` (0 → 1) : le poing part vers le centre de l'écran et vers l'avant ;
- `back` (0 → 1) : le bras recule et descend (armement) ;
- `reach` : distance du coup (`0.45f` poing de base, `0.7f` frappe chargée).

Les déplacements sont dans `arm` : `pose.translate(x, y, z)` (x = côté, y = haut, z négatif = vers l'avant)
et `pose.mulPose(Axis.XP.rotationDegrees(...))` pour incliner.

## Ajouter une nouvelle animation

1. Ajouter une ligne dans `CombatAnim` : `UPPERCUT(8),`.
2. Ajouter un `case UPPERCUT -> { ... }` dans `applyToModel` (et dans `onRenderHand` pour la vue subjective).
3. La déclencher côté serveur : `CombatAnimations.play(entité, CombatAnim.UPPERCUT)` là où l'attaque a lieu
   (par exemple dans `Combat.chargedStrike` pour une frappe parfaite).
4. Elle est aussitôt disponible dans `/hunter anim`.
