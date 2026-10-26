# Plan de test sur téléphone — fin du J4 (2026-10-26)

Objectif : valider sur ton Fairphone tout ce qui a été livré depuis le dernier test, avant de terminer le J4
(coach, écran « Règles », première partie guidée viendront ensuite).

**Comment répondre** : fais les étapes de chaque section, note au fil de l'eau la lettre qui correspond à ton
ressenti pour chaque question (A, B, C…). En revenant dans Claude Code, dis « retour du test » : je te poserai
ces mêmes questions en choix multiples, avec à chaque fois une case « Autre » pour un commentaire libre.
Durée estimée : 30 à 40 minutes. Une seule personne suffit, mais certains points sont plus parlants à 3 ou 4.

---

## 0. Installation

1. Branche le téléphone (débogage USB activé), puis dans Claude Code :
   `! adb -s d49c0560 install -r app/build/outputs/apk/debug/app-debug.apk`
2. Lance **Molkky Board (debug)**. Les données des tests précédents sont conservées (partie en cours éventuelle).

## 1. Démarrage

Étapes : ferme complètement l'app (balayage depuis les apps récentes), relance-la deux fois.

- **Q1.1** L'animation d'intro (version actuelle, avant la refonte « strike ») est-elle fluide ?
  A. Parfaitement fluide · B. Quelques saccades · C. Saccades nettes · D. Elle ne s'est pas jouée
- **Q1.2** Le passage de l'intro à l'accueil (le logotype qui glisse en haut à gauche) :
  A. Fluide et joli · B. Fluide mais le logotype « saute » · C. Saccadé · D. Je ne l'ai pas vu

## 2. Accueil

- **Q2.1** Le logotype compact en haut à gauche :
  A. Bien, à la bonne taille · B. Trop petit · C. Encore trop grand · D. Mal placé
- **Q2.2** S'il y avait une partie en cours, la carte « Partie en cours » et « Reprendre » :
  A. Fonctionnent · B. Les scores affichés sont faux · C. Ne reprend pas la bonne partie · D. Pas de partie en cours

## 3. Création de partie

Étapes : « Nouvelle partie ». Ajoute 3 joueurs en solo. Touche l'avatar d'un joueur : choisis un autre emoji,
puis essaie « Prendre une photo » et « Choisir dans la galerie ». Change l'ordre en faisant glisser la poignée ≡.
Retire un joueur (✕) : il doit réapparaître dans « Joueurs habituels ». Passe en **En équipes**.

- **Q3.1** Ajouter des joueurs et les retrouver en « Joueurs habituels » :
  A. Clair et rapide · B. Fonctionne mais pas évident · C. Les joueurs habituels n'apparaissent pas · D. Bug
- **Q3.2** Avatars : choix d'un emoji, photo prise, photo de la galerie :
  A. Tout fonctionne · B. L'emoji oui, la photo non · C. La photo s'affiche mal (tournée, floue, déformée) · D. Bug
- **Q3.3** Glisser-déposer de l'ordre (poignée ≡) :
  A. Naturel et précis · B. Fonctionne mais difficile à attraper · C. Les lignes sautent ou se mélangent · D. Ne fonctionne pas
- **Q3.4** Mode équipes : répartition automatique, nom 🎲, couleur, emblème (toucher l'icône), ⇄ pour changer d'équipe :
  A. Tout est clair · B. Fonctionne mais l'écran est chargé · C. Certaines actions sont introuvables · D. Bug
- **Q3.5** Reprise d'équipe : lance une partie en équipes, reviens, recompose **exactement** les mêmes équipes.
  L'app propose-t-elle « Ces joueurs ont déjà joué ensemble » ?
  A. Oui, et « Reprendre » fonctionne · B. Oui, mais le message n'est pas clair · C. Non · D. Pas testé
- **Q3.6** Règles : Officiel (+ interrupteur IMO), Ludique (résumé, « Modifier pour cette partie »), mode famille :
  A. Clair · B. Ludique pas assez compréhensible · C. Mode famille pas compréhensible · D. Bug
- **Q3.7** Le bouton « Lancer la partie » devient-il évident dès deux joueurs (ou deux équipes) ?
  A. Oui · B. Oui, mais on ne le voit pas assez · C. Non

## 4. Écran de jeu

Étapes : joue au moins 4 tours à 3 joueurs. Essaie : quilles et saisie rapide, un raté, la quille 12 seule,
les 12 quilles (strike), un dépassement de 50, trois ratés de suite pour un joueur. Tire le bandeau des scores
vers le haut. Annule un lancer avec le bouton ↶.

- **Q4.1** Tout tient sur un seul écran, sans défilement :
  A. Oui · B. Presque (un élément coupé) · C. Non
- **Q4.2** Saisie (quilles ou pavé rapide) puis « Valider » :
  A. Rapide et sûre · B. Un tap accidentel est passé · C. Trop de taps · D. Bug
- **Q4.3** Pastille « Tour N » : juste et lisible ?
  A. Oui · B. Le numéro est faux à un moment (préciser) · C. Pas assez visible · D. Trop visible
- **Q4.4** Tableau des scores (tirer le bandeau ou le toucher) :
  A. Facile à ouvrir, clair · B. Difficile à ouvrir au geste · C. Le classement est faux · D. Bug
- **Q4.5** Animation « 12 ! » (quille 12 seule, puis strike) :
  A. Spectaculaire et rapide · B. Trop longue · C. Trop discrète · D. Saccadée
- **Q4.6** Dépassement (« Aïe ! Retour à 25 ») :
  A. Bien · B. Trop long · C. Pas clair · D. Pas vu
- **Q4.7** Avertissement au 2e raté puis élimination au 3e (tampon « Éliminé », « Pourquoi ? ») :
  A. Clair et drôle · B. Trop long · C. Pas assez visible · D. Bug
- **Q4.8** Annuler (↶) le 3e raté : l'élimination se défait (animation à l'envers) :
  A. Oui · B. Le score revient mais pas d'animation · C. Non · D. Pas testé
- **Q4.9** L'écran reste allumé pendant la partie :
  A. Oui · B. Non, il s'éteint

## 5. Déroulé de la partie (⋮ › Déroulé de la partie)

Étapes : ouvre le déroulé, corrige un lancer ancien (ex. tour 1), puis supprime-en un autre.
Essaie aussi de corriger un lancer pour qu'un joueur atteigne 50 avant la fin.

- **Q5.1** Le déroulé groupé par tour est-il lisible ?
  A. Oui · B. Trop dense · C. Il manque des informations (préciser)
- **Q5.2** Corriger un lancer ancien : tout est recalculé (scores, retours à 25, éliminations) ?
  A. Oui · B. Recalcul faux (préciser) · C. L'éditeur n'est pas clair · D. Bug
- **Q5.3** Supprimer un lancer (avec l'avertissement sur la réattribution) :
  A. Clair et juste · B. La réattribution surprend · C. Bug
- **Q5.4** Une correction impossible est-elle bien expliquée ?
  A. Oui · B. Message pas clair · C. Pas réussi à la provoquer

## 6. Gestion des joueurs (⋮ › Gérer les joueurs)

Étapes : en début de tour, change l'ordre ; en plein tour, constate que c'est bloqué. Ajoute un retardataire.
Retire un joueur.

- **Q6.1** Changer l'ordre entre deux tours (↑/↓ puis confirmation) :
  A. Clair · B. Pas compris pourquoi c'est bloqué en plein tour · C. Bug
- **Q6.2** Joueur en retard : il démarre à 0 et lance à la fin du tour :
  A. Oui · B. Il lance au mauvais moment · C. Bug
- **Q6.3** Retirer un joueur (et victoire si un seul reste) :
  A. Oui · B. Comportement surprenant (préciser) · C. Bug

## 7. Fin de partie

- **Q7.1** Écran de fin (provisoire : l'animation de victoire arrive au J5), classement, « Nouvelle partie » :
  A. Suffisant pour l'instant · B. Classement faux · C. Bug

## 8. Réglages

Étapes : Réglages › Règles du mode Ludique (change une exception, vérifie qu'elle est proposée à la création),
Plein soleil, langue, intro courte.

- **Q8.1** Règles du mode Ludique dans les Réglages :
  A. Clair et pris en compte · B. Pas pris en compte à la création · C. Pas clair
- **Q8.2** Plein soleil (à tester dehors si possible) :
  A. Nettement plus lisible · B. Peu de différence · C. Pas testé

## 9. Fiabilité et accessibilité

Étapes : en pleine partie, ferme l'app depuis les apps récentes puis relance ; recommence en redémarrant le
téléphone. Passe la taille de police système au maximum. Essaie le thème sombre et « Supprimer les animations ».

- **Q9.1** Reprise après fermeture / redémarrage : la partie revient exactement ?
  A. Oui · B. Des lancers manquent · C. Non
- **Q9.2** Police système au maximum : aucun score coupé, tout utilisable ?
  A. Oui · B. Un texte coupé (préciser où) · C. Inutilisable
- **Q9.3** Thème sombre :
  A. Joli et lisible · B. Contraste insuffisant quelque part · C. Pas testé
- **Q9.4** « Supprimer les animations » : les animations sont remplacées par des fondus simples ?
  A. Oui · B. Certaines animations restent · C. Pas testé

## 10. Performance et réactivité

À ressentir tout au long du test (pas besoin d'outils) :

- **Q10.1** Démarrage à froid jusqu'à l'accueil (animation comprise) :
  A. Rapide · B. Un peu long · C. Trop long
- **Q10.2** Réaction aux taps (quilles, Valider, boutons) :
  A. Instantanée · B. Léger retard parfois · C. Retard gênant
- **Q10.3** Fluidité des animations et des feuilles (tableau des scores, déroulé) :
  A. Toujours fluide · B. Quelques saccades · C. Saccades fréquentes
- **Q10.4** Le téléphone chauffe-t-il ou la batterie baisse-t-elle anormalement pendant une partie de 15 min ?
  A. Non · B. Un peu · C. Oui, nettement · D. Pas testé

## 11. Bilan

- **Q11.1** Note globale de cette version :
  A. Prête pour jouer en vrai · B. Presque, quelques corrections · C. Encore loin
- **Q11.2** Ta priorité pour la suite :
  A. Corriger les défauts relevés · B. Terminer J4 (coach, règles, prise en main) · C. J5 (victoire, historique, Hall of Fame) · D. La nouvelle intro « strike »
