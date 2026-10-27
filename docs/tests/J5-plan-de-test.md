# Plan de test sur téléphone — fin du J5 (2026-10-27)

Objectif : valider sur ton Fairphone ce qui a été livré au J5 (fin de partie, revanche, match et Mölkkout,
historique, Hall of Fame, statistiques, notification, annonces vocales), ainsi que les points de fiabilité et
d'accessibilité restés en suspens au J4.

**Comment répondre** : fais les étapes, note la lettre qui correspond à ton ressenti pour chaque question.
En revenant dans Claude Code, dis « retour du test » : je te poserai ces questions en choix multiples, avec
« Pas testé » et une case libre pour commenter. Durée estimée : 30 minutes. 15 questions.

---

## 0. Installation

1. Branche le téléphone (débogage USB activé), puis dans Claude Code :
   `! adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. Lance **Molkky Board (debug)**. La base passe en version 3 : tes parties en cours et tes joueurs habituels
   doivent être conservés.

## 1. Fin de partie (3 joueurs en solo, règles Officielles)

Étapes : joue une partie jusqu'à la victoire. À la fin, regarde l'animation jusqu'au bout. Touche « Annuler
le dernier lancer », puis rejoue le lancer gagnant et **touche l'écran** pendant l'animation. Choisis un ordre
de revanche puis touche « Revanche ». Gagne aussi la revanche (la série apparaît).

- **Q1** Animation de victoire (« 50 » qui explose, pluie de quilles, trophée, double vibration) :
  A. Belle et fluide · B. Saccadée · C. Trop longue · D. Trop discrète · E. Pas testé
- **Q2** Écran de fin (chiffre clé, classement, statistiques de la manche, « Revanche » bien visible) :
  A. Clair · B. Il manque une information (préciser) · C. Trop chargé · D. Pas testé
- **Q3** « Revanche » : mêmes joueurs, ordre choisi respecté, puis récapitulatif de série juste :
  A. Oui · B. Mauvais ordre · C. Série absente ou fausse · D. Pas testé
- **Q4** Annuler le lancer gagnant rouvre la partie ; toucher l'écran passe l'animation :
  A. Les deux marchent · B. L'annulation ne rouvre pas · C. Le tap ne passe pas l'animation · D. Pas testé

## 2. Match en 2 manches et Mölkkout

Étapes : nouvelle partie à 2 joueurs, Format « Match en 2 manches ». Chacun gagne une manche. Pour forcer le
Mölkkout, faites le même total de points sur les deux manches (par exemple 50 + 4 de chaque côté).

- **Q5** Enchaînement des manches (« Manche 2 », qui commence, score du match) :
  A. Clair · B. L'ordre de la manche 2 est faux · C. Pas compris · D. Pas testé
- **Q6** Mölkkout (quilles 6, 4, 12, 10, 8 ; à qui le tour ; qui commence la belle) :
  A. Clair et juste · B. Faux (préciser) · C. Pas réussi à le déclencher · D. Pas testé

## 3. Mémoire : historique, Hall of Fame, statistiques

Étapes : onglet **Historique** : ouvre une partie, puis supprimes-en une. Onglet **Hall of Fame** : Joueurs,
Équipes, filtres de période ; touche un record. Icône 📊 : **Statistiques**, change de joueur.

- **Q7** Historique (toutes les parties terminées, ouverture du déroulé, suppression confirmée) :
  A. Complet et fiable · B. Une partie manque · C. La suppression ne marche pas · D. Pas testé
- **Q8** Hall of Fame (classements justes, lien vers la partie, filtres) :
  A. Juste et clair · B. Un classement est faux (préciser) · C. Pas clair · D. Pas testé
- **Q9** Statistiques (moyenne, ratés, victoires, graphique) :
  A. Justes et lisibles · B. Valeurs fausses · C. Graphique illisible · D. Pas testé

## 4. Notification et annonces vocales

Étapes : à la première partie, accepte l'affichage des notifications. Pendant la partie, descends le volet :
touche « Saisir le lancer ». Puis Réglages › Annonces vocales : active-les, règle le débit et le volume,
joue quelques lancers (téléphone en mode sonore, puis en silencieux).

- **Q10** Notification de partie en cours (tour, joueur, scores à jour, « Saisir le lancer » ouvre la partie) :
  A. Tout marche · B. Pas à jour · C. Le bouton n'ouvre pas la partie · D. Pas de notification · E. Pas testé
- **Q11** Annonces vocales (« Au tour de… », « Retour à 25 ! », « Douze ! », victoire ; muettes en silencieux) :
  A. Justes et utiles · B. Trop bavardes · C. Pas muettes en silencieux · D. Aucune annonce · E. Pas testé

## 5. Fiabilité, accessibilité, extérieur

- **Q12** Fluidité générale et retours haptiques (sélection, validation, élimination, victoire) :
  A. Fluide, vibrations bien dosées · B. Saccades (où ?) · C. Vibrations gênantes ou absentes · D. Pas testé
- **Q13** Reprise : en pleine partie, ferme l'app depuis les apps récentes, relance-la, « Reprendre » :
  A. Rien n'est perdu · B. Le dernier lancer est perdu · C. Ne reprend pas · D. Pas testé
- **Q14** Extérieur et grande police : Réglages › « Plein soleil », puis taille de police système au maximum
  (Paramètres › Affichage › Taille de la police). Écran de jeu, écran de fin, Hall of Fame :
  A. Lisible, rien de coupé · B. Un score ou un texte coupé (où ?) · C. Peu lisible au soleil · D. Pas testé
- **Q15** À une main et avec TalkBack (quilles, scores, bouton « Scores », écran de fin) ; écran allumé
  pendant toute la partie :
  A. Tout est annoncé et atteignable · B. Un élément muet ou mal annoncé (préciser) · C. Hors de portée du
  pouce · D. L'écran s'éteint · E. Pas testé
