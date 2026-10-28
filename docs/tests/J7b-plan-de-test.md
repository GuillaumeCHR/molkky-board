# Plan de test sur téléphone — J7b, version 0.6.0 (2026-10-28)

Corrections du test de rattrapage (C1 à C16, D1) et nouveautés N1, N2, N3, C6. Une session courte, dans l'ordre d'une
partie normale.

**Comment répondre** : fais les étapes, note une lettre par question. En revenant dans Claude Code, dis
« retour du test » : je te poserai les questions une à une, avec « Pas testé » et un commentaire libre.
Durée estimée : 30 à 40 min. 20 questions.

Laisse le téléphone branché pendant la session : je lis les journaux (plantages, saccades) sans les vider.

---

## 0. Préparation
- Installe la 0.6.0 : `! adb install -r app/build/outputs/apk/debug/app-debug.apk` (À propos › Version 0.6.0).
- Mets le téléphone **en vibreur** (pas en silencieux) et active Réglages › Annonces vocales et Sons.

## 1. Démarrage et accueil
- **Q1** Intro : la signature Yogi Studio dure maintenant ≈ 1,5 s (C1) :
  A. Bonne durée · B. Encore trop courte · C. Trop longue · D. Pas testé
- **Q2** Accueil › « Placer les quilles » : les 12 quilles d'emblée, vue depuis la ligne, et la distance (C3) :
  A. Clair et suffisant · B. Il manque quelque chose (quoi ?) · C. Pas testé

## 2. Création de partie
Étapes : nouvelle partie en équipes (2 × 2 joueurs), puis une partie « Chacun pour soi » à 3.
- **Q3** Avatars et emblèmes au trait (C6), sur la création, la puce des joueurs habituels et le sélecteur d'avatar :
  A. Sobres et lisibles · B. Trop petits · C. Une icône est peu claire (laquelle ?) · D. Pas testé
- **Q4** Pastilles de couleur des équipes, plus grandes, sur deux lignes si besoin (C13) :
  A. Faciles à toucher · B. Encombrantes · C. Pas testé
- **Q5** Un nom avec des caractères spéciaux (emoji, accents) passe toujours ; un texte copié qui contient des
  caractères invisibles est nettoyé (C16) :
  A. Rien d'anormal · B. Un caractère voulu a disparu (lequel ?) · C. Pas testé

## 3. Pendant la partie
- **Q6** Icône podium à gauche du bandeau : elle ouvre le tableau des scores (C2) :
  A. Évidente · B. Peu visible · C. Pas testé
- **Q7** À chaque lancer, les quilles touchées s'envolent ≈ 0,4 s puis reviennent (N2) :
  A. Sympa, ne gêne pas · B. Trop rapide pour être vue · C. Gêne la saisie suivante · D. Pas testé
- **Q8** Dépassement de 50 : l'autocollant « Aïe ! » montre le **joueur qui a dépassé**, avec son score qui retombe
  à 25 (C15) :
  A. Clair · B. Confus (pourquoi ?) · C. Pas testé
- **Q9** Après deux ratés de suite, la carte du joueur bat comme un cœur (contour corail, vibration légère) avec
  « Dernière chance · 1 raté = éliminé » (N3) :
  A. Bonne tension · B. Trop discret · C. Trop insistant · D. Pas testé
- **Q10** Le même avertissement en mode Ludique avec « retour à 0 » au lieu de l'élimination (« 1 raté = retour
  à 0 ») ; aucun avertissement si la règle est « on continue » :
  A. Juste · B. Faux (quel cas ?) · C. Pas testé
- **Q11** Avatars au trait sur l'écran de jeu, dans le bandeau, le tableau des scores et l'animation d'élimination :
  A. Cohérents · B. Un endroit affiche encore un emoji ou une initiale inattendue · C. Pas testé
- **Q12** Voix et sons **en vibreur** (C9) : annonce « Tour 2 », « Douze ! », son de victoire :
  A. Tout s'entend · B. La première annonce manque · C. Rien ne sort · D. Pas testé
- **Q13** Même chose **en silencieux** : rien ne doit sortir :
  A. Silence total · B. Un son ou une voix passe · C. Pas testé

## 4. Fin de partie et match
- **Q14** Bouton « Terminer » sur l'écran de fin (C8) :
  A. On sait comment sortir · B. Pas clair · C. Pas testé
- **Q15** Match en 2 manches, une manche chacun : l'écran de fin explique qui commence la belle et pourquoi (C10) :
  A. Compris · B. Peu clair · C. Pas testé
- **Q16** Règles du jeu › « La belle et le Mölkkout » (texte et démo animée) :
  A. Clair · B. Trop long · C. Démo peu parlante · D. Pas testé

## 5. Partage et À propos
- **Q17** Vignettes et clip **sans QR code** (C11), avec « Comptez vos parties avec » et le logotype :
  A. Bien · B. Il manque quelque chose · C. Pas testé
- **Q18** Photo du moment (N1) : « Prendre une photo » ou « Choisir une photo », puis le modèle « Photo » façon Strava,
  dans les trois formats :
  A. Belle et lisible · B. Texte peu lisible sur ma photo · C. La photo est mal orientée ou coupée · D. Pas testé
- **Q19** Après le partage d'une photo du moment, rien ne reste dans la galerie (sauf « Enregistrer ») :
  A. Vérifié · B. Une copie est restée · C. Pas testé
- **Q20** À propos › Vie privée : « Aucune donnée collectée. Molkky Board n'a même pas accès à Internet. » en tête,
  et la mention des bibliothèques open source (C12) :
  A. Clair et rassurant · B. À reformuler · C. Pas testé
