# Résultats du test sur téléphone — fin du J4 (2026-10-26, Fairphone 6)

Questions : `J4-plan-de-test.md`. Réponses recueillies au fil du test.

## Incident avant le test
- Plantage au lancement : `IllegalStateException: Room cannot verify the data integrity` (base J3 présente sur le
  téléphone, schéma modifié au J4 — table `game_player` — sans changement de version). Contourné par la
  désinstallation ; **à corriger** : version 2 + migration + test de migration.

## Réponses
| Question | Réponse | Commentaire |
|---|---|---|
| Q1.1 Intro fluide | A. Parfaitement fluide | |
| Q1.2 Transition vers l'accueil | A. Fluide et joli | |
| Q2.1 Logotype compact | A. Bonne taille | |
| Q10.1 Démarrage à froid | A. Rapide | |
| Q3.1 Joueurs habituels | A. Clair et rapide | |
| Q3.2 Avatars | Emoji OK | Photo pas testée |
| Q3.3 Glisser-déposer | A. Naturel et précis | |
| Q3.4 Mode équipes | Problème | (détail ci-dessous) |
| Q3.4 détail | Ajout de joueurs confus | **Ajout de joueurs à une équipe possible mais peu clair** (d'abord pas trouvé) : le champ « Nom du joueur » est en haut de l'écran, hors de vue une fois les cartes d'équipe affichées ; interface à améliorer. À corriger : « + Ajouter un joueur » (et joueurs habituels) dans chaque carte d'équipe. |
| Q3.5 Reprise d'équipe | B. Message pas clair | Reformuler le message de reprise |
| Q3.6 Règles à la création | A. Clair | |
| Q3.7 Bouton Lancer | A. Oui | |
| Q4.4 Tableau des scores | Problème | **Le geste « tirer vers le haut » ne marche pas** sur le téléphone ; le tableau s'ouvre en touchant le bandeau, mais **ce n'est pas intuitif**. Le score en cours se voit sur l'accueil. À revoir : geste fiable + accès visible (bouton « Scores » explicite ?). |
| Q4.1 Tient sur un écran | A. Oui | |
| Q4.2 Saisie + Valider | A. Rapide et sûre | |
| Q4.3 Pastille Tour N | A. Oui | |
| Q4.5 Animation « 12 ! » | A. Spectaculaire et rapide | |
| Q4.6 Dépassement | A. Bien | |
| Q4.7 Élimination | A. Clair et drôle | |
| Q4.8 Annulation de l'élimination | Pas testé | |
| Q4.9 Écran allumé | A. Oui | |
| Q5.1 Déroulé lisible | C. Infos manquantes | Détail à préciser |
| Q5.2 Correction d'un lancer ancien | Pas testé | |
| Q5.3 Suppression d'un lancer | Pas testé | |
| Q5.4 Correction refusée expliquée | Pas testé | |
| Q6b Règles du jeu | Texte clair | **Mise en forme pas assez structurée** : manque d'intertitres pour aérer et structurer (à faire : regrouper en parties « Préparer / Jouer / Gagner / Variantes / FAQ », intertitres visibles, cartes par section, plus d'espace, points clés en gras ou en listes). |
| Q6.1 Changer l'ordre | Problème | **Flèches inactives, l'ordre ne se change pas** (message affiché non précisé : blocage « entre deux tours » incompris ou défaut). À investiguer et rendre explicite. |
| Global | — | **Une partie complète se joue de bout en bout.** |
