# Audit des formats d'écran — version 0.8.2 (2026-10-29)

## Contexte
L'app est bloquée en portrait (`screenOrientation="portrait"`) et ses écrans sont pensés pour un téléphone. Avec le
**targetSdk 37**, Android ignore l'orientation, le rapport d'aspect et la non-redimensionnabilité sur les écrans dont la
plus petite largeur est **≥ 600 dp** (tablettes, pliables ouverts, Chromebooks, fenêtres de bureau), **sans dérogation
possible** (la propriété `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` ne vaut plus au niveau 37). Sur téléphone
(< 600 dp), le portrait reste imposé.

## Méthode
Émulateur Android 17 passé en grand écran (`wm size 1600x2560`, `wm density 320`, soit 800 dp de plus petite largeur),
rotation forcée en paysage puis en portrait. Le profil tablette de l'outil `android` ne propose qu'Android 15, où le
portrait est encore respecté : il ne permet pas de voir le problème.

## Constats
| Écran | Paysage (≥ 600 dp) | Portrait tablette |
|---|---|---|
| Blocage en portrait | **Ignoré** (`requestedOrientation=PORTRAIT`, l'app occupe tout l'écran paysage) | — |
| Intro | Correcte (centrée) | Correcte |
| Accueil, création, réglages, historique, Hall of Fame | Fonctionnent, contenus **étirés sur toute la largeur** (boutons et champs de 2 000 px) | Fonctionnent, étirés |
| Écran de jeu | Fonctionne ; **grille de quilles minuscule, calée à gauche**, grand vide à droite ; boutons Raté/OK étirés | Fonctionne ; grille petite et décalée, grand vide en bas |
| Tableau des scores, déroulé (feuilles) | Corrects (largeur des feuilles limitée par Material) | Corrects |
| Rotation en pleine saisie | **Aucune perte** : la quille sélectionnée et l'aperçu sont conservés, la validation fonctionne | — |

Aucun plantage ni perte de données. Le défaut de rotation du test du 27 octobre ne s'est pas reproduit. Le problème est
d'**ergonomie et d'apparence** : l'app est utilisable mais paraît inachevée sur grand écran.

Captures : `form-factors/home-landscape.png`, `game-landscape.png`, `game-portrait.png`, `scoreboard-landscape.png`.

## Non couvert par cet audit
Pliables (passage plié/déplié en pleine partie), écran partagé et fenêtres redimensionnées, Chromebook (clavier et
souris), écrans de fin, de partage et « Gérer les téléphones » en paysage, partie à côté avec un hôte sur tablette.

## Options
1. **Mise en page adaptative (recommandé avant la production)** : largeur maximale du contenu centré (≈ 600 dp) pour les
   écrans de liste et de formulaire ; écran de jeu en deux colonnes en paysage (joueur actif et scores à gauche, grille
   et boutons à droite), grille dimensionnée sur la place réelle ; test Roborazzi sur un profil tablette. Estimation : 1 à
   2 jours.
2. **Repli au targetSdk 36** avec `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` : le portrait redevient respecté sur
   tablette. Conforme au Play Store jusqu'en août 2027 ; à revérifier : la permission « réseau local » d'Android 17, liée
   au targetSdk 37.
3. **Exclure les tablettes** de la distribution (catalogue d'appareils de la Play Console) : fastidieux et incomplet
   (les pliables sont des téléphones).

## Correctif — version 0.8.3 (2026-10-29)
- **Écrans de liste et de formulaire** (accueil, création, réglages, historique, Hall of Fame, statistiques, À propos,
  règles, placement, partage, Rejoindre, Gérer les téléphones) : contenu centré, largeur limitée à 640 dp
  (`ReadableWidth`). Plus étroit qu'un téléphone, sans effet sur téléphone.
- **Écran de jeu** : au-delà de 600 dp de large en paysage, deux colonnes (joueurs et joueur actif à gauche, grille et
  boutons à droite) ; une seule colonne sinon, inchangée.
- **Grille de quilles** centrée dans sa zone (elle était calée en haut à gauche) : seul changement visible sur
  téléphone, voulu.
- **Non-régression téléphone** : `verifyRoborazziDebug` sur tous les modules. Seules les 36 captures où figure la grille
  changent (designsystem et jeu), et uniquement à l'emplacement de la grille ; toutes les autres sont identiques au pixel.
- Vérifié sur l'émulateur Android 17 en grand écran (800 dp) : accueil et création centrés, écran de jeu sur deux
  colonnes en paysage, une colonne en portrait.
