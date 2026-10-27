# Plan de test sur téléphone — rattrapage du J4 à la 0.5.0 (2026-10-27)

Une seule session pour tout ce qui n'a pas encore été joué sur téléphone : restes du J4, J5, J6, J7 et les lots
faits ensuite (aides aux débutants, À propos et crédits, sons, records secondaires et badges). Il remplace
`J5-plan-de-test.md`, `J6-plan-de-test.md` et `J7-plan-de-test.md` (leurs questions sont reprises ici, mises à
jour). L'ordre suit un parcours naturel, pour ne pas sauter d'un écran à l'autre.

**Comment répondre** : fais les étapes, note une lettre par question. En revenant dans Claude Code, dis
« retour du test » : je te poserai les questions en choix multiples, avec « Pas testé » et un commentaire libre.
Tu peux t'arrêter à la fin de n'importe quelle partie et reprendre plus tard. Durée estimée : 1 h 30 en tout.
52 questions.

Pendant la session, laisse le téléphone branché : je surveille les journaux (plantages, saccades, et l'absence
totale de requête réseau).

---

## 0. Préparation
- La version **0.5.0** est installée (À propos › Version 0.5.0). Si besoin : `! adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- Réglages › « Revoir la prise en main », et vérifie que la carte « Prêts à jouer ? » est activée.
- Pour la partie 9 (données), garde quelques parties terminées : ne supprime rien avant.

## 1. Démarrage
- **Q1** Intro « strike » (lignes de vitesse, image figée safran, quilles qui s'envolent, scintillement, signature,
  logotype qui claque), **sans la bulle « 12 ! »** :
  A. Spectaculaire et lisible · B. Trop rapide · C. Trop lente · D. Saccadée · E. Pas testé
- **Q2** Timings (≈ 4,5 s ; version courte ≈ 1,7 s dans les Réglages, à partir du 2e lancement) :
  A. Bons · B. À raccourcir (où ?) · C. À allonger (où ?) · D. Pas testé
- **Q3** Accueil : « Placer les quilles » et « Règles du jeu » en bas, partie en cours proposée en premier :
  A. Clair · B. Encombré · C. Il manque quelque chose · D. Pas testé
- **Q4** Raccourcis de l'icône (appui long sur l'icône de l'app : Nouvelle partie, Placer les quilles, Règles) :
  A. Les trois ouvrent le bon écran · B. Un raccourci ne marche pas (lequel ?) · C. Pas de raccourcis · D. Pas testé

## 2. Débuter : placement, carte « Prêts à jouer ? », mode débutant
Étapes : accueil › « Placer les quilles » : suis le pas à pas jusqu'au bout, essaie la vue de côté et « Lire à
voix haute ». Puis nouvelle partie à 2 joueurs ; sur la carte « Prêts à jouer ? », active le mode débutant.
- **Q5** Placement pas à pas (une quille à la fois, position en clair, formation complète à la fin) :
  A. On arrive à poser les quilles sans autre aide · B. Une position est fausse ou peu claire · C. Trop lent · D. Pas testé
- **Q6** Vue de côté et lecture à voix haute :
  A. Utiles · B. Vue de côté peu parlante · C. La voix ne lit pas · D. Pas testé
- **Q7** Carte « Prêts à jouer ? » (texte, « Voir le placement », « C'est parti », « Ne plus afficher ») :
  A. Claire, au bon moment · B. Gênante · C. « Voir le placement » ne revient pas à la carte · D. Pas testé
- **Q8** Mode débutant (rappel « Relevez les quilles » après les lancers, explication au premier retour à 25 et au
  premier deuxième raté, une seule fois) :
  A. Utile, pas envahissant · B. Trop de messages · C. Explication absente · D. Pas testé
- **Q9** Les 4 bulles de prise en main (joueurs, quilles, « Valider », annuler) :
  A. Claires, elles ne gênent pas · B. Une bulle cache quelque chose d'utile · C. Trop nombreuses · D. Pas testé

## 3. Pendant la partie
- **Q10** Bouton « Quilles » (icône à trois quilles dans la barre) : ouvre le placement sans perdre la saisie :
  A. Oui · B. Introuvable · C. La saisie est perdue · D. Pas testé
- **Q11** Coach à partir de 38 points (« Il te faut 7… », quille surlignée, « Zone de danger ») :
  A. Utile et juste · B. Faux (préciser) · C. Envahissant · D. Pas testé
- **Q12** Tirer le bandeau des scores (vers le haut ou le bas) ouvre le tableau ; le bouton « Scores » aussi :
  A. Oui, à chaque fois · B. Parfois · C. Jamais · D. Pas testé
- **Q13** Avatars (emoji du joueur, emblème de l'équipe) sur l'écran de jeu :
  A. Bien · B. Manquants · C. Gênants · D. Pas testé
- **Q14** Téléphone tourné à l'horizontale pendant la partie :
  A. Je joue toujours en vertical, à bloquer en portrait · B. L'écran paysage me convient · C. Pas testé

## 4. Fin de partie (3 joueurs en solo, règles Officielles)
Étapes : jouez jusqu'à la victoire, regarde l'animation. Touche « Annuler le dernier lancer », rejoue le lancer
gagnant et **touche l'écran** pendant l'animation. Choisis un ordre de revanche puis « Revanche » ; gagne aussi
la revanche.
- **Q15** Animation de victoire (« 50 » qui explose, pluie de quilles, trophée, double vibration) :
  A. Belle et fluide · B. Saccadée · C. Trop longue · D. Trop discrète · E. Pas testé
- **Q16** Écran de fin (chiffre clé, classement, statistiques, rappel de placement avec le schéma, « Revanche ») :
  A. Clair · B. Il manque une information · C. Trop chargé · D. Pas testé
- **Q17** « Revanche » : mêmes joueurs, ordre choisi respecté, puis récapitulatif de série juste :
  A. Oui · B. Mauvais ordre · C. Série absente ou fausse · D. Pas testé
- **Q18** Annuler le lancer gagnant rouvre la partie ; toucher l'écran passe l'animation :
  A. Les deux marchent · B. L'annulation ne rouvre pas · C. Le tap ne passe pas l'animation · D. Pas testé

## 5. Match en 2 manches et Mölkkout
Étapes : nouvelle partie à 2 joueurs, Format « Match en 2 manches ». Chacun gagne une manche, avec le même
total de points sur les deux manches pour forcer le Mölkkout.
- **Q19** Enchaînement des manches (« Manche 2 », qui commence, score du match) :
  A. Clair · B. L'ordre de la manche 2 est faux · C. Pas compris · D. Pas testé
- **Q20** Mölkkout (quilles 6, 4, 12, 10, 8 ; à qui le tour ; qui commence la belle) :
  A. Clair et juste · B. Faux (préciser) · C. Pas réussi à le déclencher · D. Pas testé

## 6. Mémoire : historique, Hall of Fame, statistiques, badges
- **Q21** Historique (toutes les parties terminées, déroulé, suppression confirmée, dates en français) :
  A. Complet et fiable · B. Une partie manque · C. La suppression ne marche pas · D. Pas testé
- **Q22** Hall of Fame (Joueurs, Équipes, filtres de période, un tap ouvre la partie du record) :
  A. Juste et clair · B. Un classement est faux · C. Pas clair · D. Pas testé
- **Q23** « Autres records » en bas du Hall of Fame (plus de « 12 », plus longue série, retours à 25, remontée) :
  A. Justes et amusants · B. Un record est faux (lequel ?) · C. Absents · D. Pas testé
- **Q24** Statistiques (moyenne, ratés, victoires, graphique, « 12 » réussis, retours à 25) :
  A. Justes et lisibles · B. Valeurs fausses · C. Graphique illisible · D. Pas testé
- **Q25** Badges (Première victoire, Partie parfaite, Phénix, Sniper ; grisés tant qu'ils ne sont pas obtenus) :
  A. Justes et motivants · B. Un badge manque ou est faux · C. Pas compris · D. Pas testé

## 7. Partage (fin de partie › « Partager ce moment »)
- **Q26** Le moment proposé en premier est-il le bon (record, partie parfaite, « 12 ! »…) ?
  A. Oui · B. Un autre aurait été mieux (lequel ?) · C. Moment absent ou faux · D. Pas testé
- **Q27** Vignettes : glisser entre les 4 modèles, changer de format (9:16, 4:5, 1:1) :
  A. Belles et lisibles · B. Un texte coupé (où ?) · C. Pas assez « Nordic Bold » · D. Pas testé
- **Q28** Masquer un joueur (« Joueur 2 ») puis partager dans WhatsApp ou une autre app :
  A. Nom bien masqué, l'image arrive · B. Le nom apparaît encore · C. Le partage échoue · D. Pas testé
- **Q29** Le QR code de la vignette, scanné avec un autre téléphone :
  A. Ouvre la fiche Play Store · B. Illisible · C. Pas testé
- **Q30** « Créer une vidéo » : temps de création (objectif moins de 10 s) et rendu :
  A. Moins de 10 s, belle · B. 10 à 20 s · C. Plus de 20 s · D. Vidéo couchée ou abîmée · E. Pas testé
- **Q31** « GIF (sans vidéo) » dans une messagerie :
  A. S'anime bien · B. Couleurs dégradées · C. Trop lourd · D. Pas testé

## 8. Notification, voix, sons
Étapes : accepte les notifications à la première partie. Réglages › Annonces vocales (débit, volume) et « Sons » ;
joue en mode sonore, puis en silencieux.
- **Q32** Notification de partie en cours (tour, joueur, scores à jour, « Saisir le lancer » ouvre la partie) :
  A. Tout marche · B. Pas à jour · C. Le bouton n'ouvre pas la partie · D. Pas de notification · E. Pas testé
- **Q33** Annonces vocales (« Au tour de… », « Retour à 25 ! », « Douze ! », victoire ; muettes en silencieux) :
  A. Justes et utiles · B. Trop bavardes · C. Pas muettes en silencieux · D. Aucune annonce · E. Pas testé
- **Q34** Sons maison (choc de l'intro, « 12 ! », victoire, élimination ; muets en silencieux et en vibreur) :
  A. Agréables · B. Trop forts ou agaçants (lequel ?) · C. Pas muets en silencieux · D. Aucun son · E. Pas testé

## 9. Données (Réglages › Données)
- **Q35** « Sauvegarder mes données » dans un dossier (Téléchargements, Drive…) :
  A. Fichier créé, message « Sauvegarde enregistrée » · B. Échec (message ?) · C. Pas testé
- **Q36** Ouvrir le fichier JSON (gestionnaire de fichiers ou ordinateur) :
  A. Lisible, on y retrouve parties et joueurs · B. Illisible · C. Pas testé
- **Q37** « Restaurer une sauvegarde » avec ce même fichier, sans rien supprimer :
  A. « 0 partie ajoutée · N déjà présentes », rien n'est doublé · B. Doublons · C. Pas testé
- **Q38** « Supprimer toutes mes données » puis confirmer :
  A. Historique, Hall of Fame, joueurs habituels et partie en cours ont disparu ; réglages conservés ·
  B. Il reste des données (lesquelles ?) · C. Pas testé
- **Q39** Restaurer ensuite la sauvegarde :
  A. Tout revient (sans les photos) et la partie en cours se reprend · B. Il manque quelque chose · C. Pas testé
- **Q40** Restaurer un fichier qui n'est pas une sauvegarde (une photo, un PDF) :
  A. Message clair, rien n'est modifié · B. Plantage ou message obscur · C. Pas testé

## 10. Langues (Réglages › Langue)
- **Q41** Choix 🇫🇷 Français / 🇬🇧 English, avec les drapeaux, sans « Langue du téléphone » :
  A. Bascule immédiate, la bonne case est cochée · B. Il faut relancer l'app · C. Mauvaise case · D. Pas testé
- **Q42** En anglais, une partie complète (création, jeu, fin, partage, règles) :
  A. Tout est en anglais et naturel · B. Restes de français (où ?) · C. Formulations bizarres · D. Pas testé
- **Q43** Annonces vocales en anglais :
  A. Voix anglaise (« Round 2 », « Alice to throw ») · B. Voix française qui lit l'anglais · C. Pas testé

## 11. Règles, À propos, crédits
- **Q44** Règles du jeu : sommaire, recherche (essaie « regle » sans accent, « 50 »), schémas animés :
  A. On trouve vite, les schémas aident · B. La recherche rate des règles · C. Schémas peu clairs · D. Pas testé
- **Q45** À propos : encart « Vie privée », polices, bibliothèques open source :
  A. Clair et sincère · B. Une phrase me semble fausse ou floue (laquelle ?) · C. Pas testé
- **Q46** Crédits (« Voir le générique ») : défilement automatique, arrêt au toucher, scintillement final :
  A. Drôle et lisible · B. Trop rapide ou trop lent · C. Un texte à revoir (lequel ?) · D. Pas testé
- **Q47** Version de test : À propos › « Envoyer un retour » ouvre l'e-mail prérempli (version et modèle seulement) :
  A. Oui · B. Rien ne s'ouvre · C. Pas testé

## 12. Fiabilité, performance, accessibilité
- **Q48** Fluidité et réactivité (saisie d'un lancer, animations, ouverture des feuilles) ; retours haptiques :
  A. Fluide, aucune latence perceptible · B. Saccades ou lenteurs (où ?) · C. Vibrations gênantes · D. Pas testé
- **Q49** Reprise : en pleine partie, ferme l'app depuis les apps récentes, relance-la, « Reprendre » :
  A. Rien n'est perdu · B. Le dernier lancer est perdu · C. Ne reprend pas · D. Pas testé
- **Q50** « Plein soleil », puis police système au maximum : jeu, fin, Hall of Fame, placement, crédits :
  A. Lisible, rien de coupé · B. Un texte coupé (où ?) · C. Peu lisible au soleil · D. Pas testé
- **Q51** À une main et avec TalkBack (quilles, scores, « Scores », placement, terminal des crédits lu en une phrase) :
  A. Tout est annoncé et atteignable · B. Un élément muet ou mal annoncé · C. Hors de portée du pouce · D. Pas testé
- **Q52** « Réduire les animations » (Paramètres Android › Accessibilité) : intro, victoire, schémas, crédits :
  A. Tout passe en version calme · B. Une animation reste (laquelle ?) · C. Pas testé
