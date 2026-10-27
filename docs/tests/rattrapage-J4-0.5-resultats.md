# Résultats du test de rattrapage (J4 → 0.5.0) — 2026-10-27

Fairphone 6, version 0.5.0 (build 5), plan `rattrapage-J4-0.5-plan-de-test.md`. Journaux : seule la dernière
minute a été capturée (journal vidé au lancement de la capture) ; aucune erreur de Molkky Board dessus. Le
téléphone était en **vibreur**, ce qui explique l'absence de voix et de sons (voir C9).

## Réponses

| Q | Sujet | Réponse | Suite |
|---|---|---|---|
| 1 | Intro « strike » | Spectaculaire, lisible | — |
| 2 | Timings de l'intro | Signature Yogi Studio trop courte | C1 |
| 3 | Accueil | Clair, à garder ; aime les petites icônes | C2 |
| 4 | Raccourcis de l'icône | Les trois marchent | — |
| 5 | Placement pas à pas | Trop long, sans plus-value : vue globale épurée | C3 |
| 6 | Vue de côté / lire à voix haute | Vue de côté à retirer ; la voix ne lit pas | C3, C9 |
| 7 | Carte « Prêts à jouer ? » | Pas testé | — |
| 8 | Mode débutant | Trop de messages ; « Relevez les quilles » gênant et inutile | C4 |
| 9 | Bulles de prise en main | Pas testé | — |
| 10 | Bouton « Quilles » | Oui | — |
| 11 | Coach | Envahissant | C5 |
| 12 | Bandeau des scores | Le geste ne marche jamais ; icône podium à gauche du bandeau | C2 |
| 13 | Avatars | Les emoji font enfantin | C6 |
| 14 | Paysage | L'affichage bugue | C7 |
| 15 | Animation de victoire | Belle et fluide | — |
| 16 | Écran de fin | Clair, mais pas de bouton pour arrêter | C8 |
| 17 | Revanche | Oui | — |
| 18 | Annuler le lancer gagnant | Rouvre bien ; tap non testé | — |
| 19 | Match en 2 manches | Clair | — |
| 20 | Mölkkout | Jamais vu (se déclenche seulement à égalité de points à 1-1) | C10 |
| 21 | Historique | Complet et fiable | — |
| 22 | Hall of Fame | Semble fonctionner (toutes les combinaisons non testées) | — |
| 23 | Autres records | Justes et amusants | — |
| 24 | Statistiques | Bien (données non vérifiées) | — |
| 25 | Badges | Justes et motivants | — |
| 26 | Moment proposé | Oui | — |
| 27 | Vignettes | Idée : photo du moment avec score ou badge incrusté (façon Strava) | N1 |
| 28 | Masquer un nom | Jugé inutile | D1 |
| 29 | QR code | Inutile, à supprimer | C11 |
| 30 | Vidéo | Moins de 10 s, belle | — |
| 31 | GIF | S'anime bien | — |
| 32 | Notification | Tout marche | — |
| 33 | Annonces vocales | Aucune annonce (vibreur) | C9 |
| 34 | Sons | Aucun son (vibreur) | C9 |
| 35–39 | Sauvegarde, lecture, restauration, suppression, retour | Tout fonctionne, rien n'est doublé | — |
| 40 | Mauvais fichier | Non sélectionnable (grisé par le sélecteur) | — |
| 41 | Langue | Bascule immédiate | — |
| 42 | Anglais | Tout en anglais, naturel | — |
| 43 | Voix en anglais | Non testable (vibreur) | C9 |
| 44 | Règles du jeu | On trouve vite | — |
| 45 | À propos | Insister sur « aucune collecte » et sur l'open source | C12, D2 |
| 46 | Écran Crédits | Sans objet (crédits intégrés à À propos) | — |
| 47 | Envoyer un retour | Marche, mais aucun destinataire | À créer : adresse de contact dédiée |
| 48 | Fluidité | Fluide, aucune latence | — |
| 49 | Reprise | Rien n'est perdu | — |
| 50 | Plein soleil, police maximale | Lisible, rien de coupé | — |
| 51 | TalkBack | Pas testé, vérification automatique par Claude | C13 |
| 52 | Réduire les animations | Pas testé | — |

Remarques hors questionnaire : Ludique › retirer « Mordu compté comme raté » (C14) ; au retour à 25, le
tremblement vise le joueur suivant (C15) ; multiplier les petites animations, par exemple les quilles touchées
qui s'envolent à chaque lancer (N2) ; vérifier le contrôle du JSON importé (fait, voir C16).

## Corrections à faire
- **C1** Signature Yogi Studio ≈ 1,5 s (au lieu de 1,1 s), total ≤ 4,5 s : temps pris sur la fin de l'envol et l'entrée du logotype.
- **C2** Icône podium (style des icônes de l'app) à gauche du bandeau des scores, qui ouvre le tableau ; le mot « Scores » disparaît de la barre ; geste de glissement retiré.
- **C3** Placement : les 12 quilles d'emblée, vue épurée ; pas à pas et vue de côté retirés ; distance conservée.
- **C4** Mode débutant : rappel « Relevez les quilles » retiré ; restent les deux explications uniques.
- **C5** Coach allégé : quille surlignée et « Il faut 7 » court ; plus de « Zone de danger ».
- **C6** Avatars sobres : initiale en pastille ou emblèmes vectoriels au trait (captures à choisir).
- **C7** Écran bloqué en portrait (paysage en V2).
- **C8** Écran de fin : bouton « Terminer » (retour à l'accueil).
- **C9** Voix et sons : coupés seulement en mode silencieux, plus en vibreur (volume média) ; vérifier la lecture à voix haute.
- **C10** Mölkkout : une ligne d'explication sur l'écran de fin de la manche 2, et une petite démo dans les règles.
- **C11** Vignettes : QR code supprimé (logotype et « Comptez vos parties avec » restent).
- **C12** À propos : « Aucune donnée collectée. Molkky Board n'a même pas accès à Internet. » en tête de l'encart.
- **C13** Accessibilité : vérification automatique (tests Compose et contrôles d'accessibilité).
- **C14** Règles du mode Ludique : retirer « Mordu compté comme raté ».
- **C15** Retour à 25 : tremblement et autocollant sur le joueur qui a dépassé.
- **C16** Noms : refuser les caractères Unicode de mise en forme (inversion du sens de lecture…), à la saisie et à la restauration.

## Nouvelles fonctions proposées
- **N1** Photo du moment : photo prise sur le terrain (appareil photo système ou galerie), score, badge ou moment incrusté, sans EXIF ni conservation après le partage. Maquette avant le code.
- **N2** Petite animation à chaque lancer : les quilles touchées s'envolent (≈ 0,4 s), le « 12 » gardant sa bulle.

## Décisions en attente
- **D1** Masquer un nom avant le partage (exigé par la spec « Vie privée ») : retirer, ou garder en plus discret.
- **D2** « Open source » : le code est privé ; l'écrire suppose de publier le code sous licence libre.
- **D3** Voix et sons coupés seulement en mode silencieux (C9) : à confirmer.
