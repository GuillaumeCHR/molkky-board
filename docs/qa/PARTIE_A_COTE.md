# Plan de test sur téléphones — Partie à côté, version 0.7.0 (2026-10-28)

Première partie à plusieurs téléphones, sans serveur ni Internet (phase 3 bis). Il faut **deux téléphones** :
l'**hôte** (Fairphone 6, celui de l'organisateur) et l'**invité** (Galaxy S23). Un troisième téléphone est un plus
pour Q9.

**Comment répondre** : fais les étapes, note une lettre par question. En revenant dans Claude Code, dis
« retour du test » : je te poserai les questions une à une, avec « Pas testé » et un commentaire libre.
Durée estimée : 45 min. 18 questions.

Laisse les téléphones branchés quand c'est possible (au moins l'hôte) : je lis les journaux (plantages, connexions)
sans les vider.

---

## 0. Préparation
- Branche les deux téléphones en USB (débogage USB activé), puis `! adb devices -l` : je repère lequel est lequel.
- Installe la 0.7.0 sur chacun : `! adb -s <numéro> install -r app/build/outputs/apk/debug/app-debug.apk`
  (À propos › Version 0.7.0). Je peux le faire pour toi une fois les deux téléphones visibles.
- Les deux téléphones en vibreur, Wi-Fi activé. Sur l'invité, laisse les données mobiles activées (elles ne doivent
  pas servir : c'est vérifié par les journaux).
- Sur l'hôte : une partie à 3 joueurs, par exemple Léa, Tom et Max, puis lance-la.

## 1. Même Wi-Fi (les deux téléphones sur ta box)
Étapes : hôte › écran de jeu › ⋮ › « Inviter des téléphones » › « Même Wi-Fi » › « Partager la partie ».
Invité › accueil › « Rejoindre » › scanne le QR code › pseudo « Tom » › « Rejoindre ».
- **Q1** Écran « Inviter » de l'hôte (texte d'explication, QR code, luminosité poussée, liste vide) :
  A. Clair · B. Une étape est confuse (laquelle ?) · C. Rien ne s'affiche / erreur (laquelle ?) · D. Pas testé
- **Q2** Demandes de permission vues (hôte et invité) et leur explication :
  A. Aucune ou appareil photo seulement, explication claire · B. Une permission surprend (laquelle ?) ·
  C. Pas testé
- **Q3** La carte « Tom veut rejoindre la partie » apparaît chez l'hôte ; touche « C'est Tom ». L'invité arrive sur
  l'écran de jeu, à jour :
  A. Oui · B. La carte n'apparaît pas · C. L'invité reste bloqué (sur quel message ?) · D. Pas testé
- **Q4** Temps entre le scan et l'écran de jeu de l'invité (objectif : moins de 10 s, validation comprise) :
  A. Moins de 10 s · B. 10 à 30 s · C. Plus de 30 s · D. Pas testé

## 2. Jouer à deux téléphones
Étapes : jouez une dizaine de lancers. Tom saisit ses lancers sur l'invité, l'hôte saisit les autres.
- **Q5** Un lancer saisi sur un téléphone apparaît sur l'autre (score, tour, animations « 12 ! », envol des quilles,
  élimination) :
  A. Immédiatement, partout · B. Avec un retard gênant · C. Il manque quelque chose (quoi ?) · D. Pas testé
- **Q6** Sur l'invité, essaie de saisir quand ce n'est **pas** le tour de Tom :
  A. Refusé avec un message clair · B. Accepté (anormal) · C. Message peu clair · D. Pas testé
- **Q7** Sur l'hôte, ouvre le déroulé et **corrige un ancien lancer de Tom** ; sur l'invité, Tom peut corriger
  son dernier lancer mais pas celui d'un autre :
  A. Correction visible partout, droits respectés · B. Pas propagée · C. Droits incorrects (lesquels ?) ·
  D. Pas testé
- **Q8** Pastille de connexion en haut de l'écran de jeu (« 2 téléphones » sur l'hôte, « Connecté » sur l'invité) :
  A. Claire · B. Peu visible · C. Incorrecte · D. Pas testé

## 3. Arrivées, refus, exclusion
- **Q9** Un troisième téléphone (ou l'invité après « Arrêter », puis un nouveau scan) rejoint en
  « **Nouveau joueur** » avec un pseudo déjà pris (« Léa ») : il arrive à 0 en fin d'ordre, sous le nom « Léa 2 » :
  A. Oui · B. Autre comportement (lequel ?) · C. Pas testé
- **Q10** Nouvelle demande, l'hôte touche « Refuser » ; puis scan d'un QR code **déjà utilisé** ou vieux de plus de
  2 minutes :
  A. Deux messages clairs · B. Un message manque ou est confus · C. Pas testé
- **Q11** Sur l'hôte, « Exclure » le téléphone de Tom : l'invité est prévenu et ne peut plus saisir ; un nouveau scan
  repasse par la validation de l'organisateur :
  A. Oui · B. L'invité continue à saisir · C. Autre (quoi ?) · D. Pas testé

## 4. Coupures
- **Q12** Sur l'invité, coupe le Wi-Fi 20 s puis rallume-le : bandeau « Hors ligne », saisie suspendue, puis
  reconnexion **sans** nouvelle validation et rattrapage des lancers saisis entre-temps sur l'hôte :
  A. Oui · B. Pas de reconnexion automatique · C. Lancers manquants · D. Pas testé
- **Q13** Hôte écran éteint pendant 10 minutes (notification « Partie partagée en cours » visible) : l'invité
  continue à saisir, et l'hôte retrouve tout en se rallumant :
  A. Oui · B. Connexion perdue (après combien de temps ?) · C. Pas testé
- **Q14** Ferme complètement l'app sur l'hôte (balayage dans les applis récentes) puis rouvre-la. *Le redémarrage
  automatique du partage n'est pas encore fait : on note le comportement réel.*
  A. L'invité passe « Hors ligne », la partie reste lisible · B. Plantage ou partie perdue · C. Pas testé

## 5. Point d'accès local (dehors, sans Wi-Fi)
Étapes : coupez le Wi-Fi de la box sur les deux téléphones (ou sortez). Nouvelle partie sur l'hôte › Inviter ›
« Point d'accès » › Partager. L'invité scanne et accepte la fenêtre système « Se connecter au réseau ».
- **Q15** Le point d'accès démarre sur l'hôte (permission « Appareils à proximité » demandée) et l'invité s'y
  connecte :
  A. Oui · B. L'hôte échoue (message ?) · C. L'invité n'arrive pas à se connecter (à quelle étape ?) · D. Pas testé
- **Q16** Pendant ce temps, l'invité garde ses données mobiles (un message reçu ailleurs, une page web) :
  A. Oui · B. Plus d'Internet sur l'invité · C. Pas testé
- **Q17** Refuse la permission « Appareils à proximité » sur l'hôte (Réglages Android › Applis › Molkky Board ›
  Autorisations, puis relance le partage) :
  A. Message clair, la partie continue sur l'hôte · B. Plantage ou blocage · C. Pas testé

## 6. Fin de partie
- **Q18** Victoire : l'animation se joue sur les deux téléphones et la partie est dans l'historique des deux.
  *Le partage reste ouvert après la victoire (manche suivante, revanche) : arrête-le avec « Arrêter le partage »
  (écran « Inviter » ou notification), puis note aussi ce qu'a fait « Revanche » sur l'invité.*
  A. Tout est cohérent · B. Il manque quelque chose (quoi ?) · C. Pas testé

## Ce que je vérifie de mon côté (journaux)
- Aucune connexion vers une adresse hors du réseau local (adresses privées et lien-local uniquement).
- Aucun plantage, aucune erreur TLS hors du test « mauvais téléphone ».
- Durées de connexion et de rattrapage.
