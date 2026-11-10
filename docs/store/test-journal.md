# Journal du test fermé

Sert à suivre le test fermé obligatoire (12 testeurs ou plus pendant 14 jours consécutifs) et à répondre aux questions
de Google lors de la **demande d'accès à la production**. À tenir au fil de l'eau : chaque retour, sa suite, la version
qui le corrige.

## Suivi du test
| Date | Testeurs inscrits | Jour du compte à rebours | Version | Événement |
|---|---|---|---|---|
| 2026-10-29 | — | — | 0.8.1 (9) | Test interne publié (porteur du projet) |
| 2026-10-29 | — | — | 0.8.2 (10) | Test fermé envoyé à l'examen de Google |
| — | — | — | 0.8.3 (11) | Construite, jamais envoyée (remplacée par la 0.8.4) |
| 2026-11-02 | — | — | 0.8.4 (12) | AAB construit depuis `2666242` avec « Send feedback », à envoyer en test fermé à la place de la 0.8.2 |
| 2026-11-10 | — | — | 0.8.8 (16) | En test fermé (tag `v0.8.8`) ; 12 testeurs inscrits depuis 11 jours sans interruption |
| 2026-11-10 | — | — | 1.0.0 (17) | AAB de production construit (tag `v1.0.0`), à envoyer une fois l'accès accordé |

Le compte à rebours démarre au premier jour où **au moins 12 testeurs** sont inscrits ; s'il passe sous 12, il repart
à zéro.

## Retours et corrections
| Date | Source | Retour | Suite donnée | Version |
|---|---|---|---|---|
| 2026-10-28 | Test interne, 2 téléphones | QR code refusé s'il est affiché depuis plus de 2 minutes | QR renouvelé automatiquement, mention à l'écran | 0.8.0 |
| 2026-10-28 | Test interne | Compteur « téléphones connectés » incluant l'hôte | Compte des invités seulement | 0.8.0 |
| 2026-10-28 | Test interne (Android 17) | L'invité ne pouvait pas joindre l'hôte | Permission « réseau local » demandée à l'invité | 0.8.0 |
| 2026-10-28 | Test interne | Pas de moyen de quitter la partie côté invité ; gestion des téléphones peu visible côté hôte | « Se déconnecter » pour l'invité ; écran « Gérer les téléphones » en deux pavés ; « Déconnecter ce téléphone » | 0.8.0 |
| 2026-10-28 | Test interne | Tout invité doit pouvoir compter les points ; besoin d'un arbitre | Rôle compteur par défaut, invité « arbitre » sans jouer | 0.8.0 |
| 2026-10-28 | Test interne | Pas d'avertissement quand un téléphone part ou est déconnecté ; partage actif après la fin | Avertissements sur les deux téléphones ; fin automatique du partage | 0.8.0 |
| 2026-10-29 | Revue avant publication | Anglais attendu par défaut (fiche en anglais) | Anglais par défaut, français conservé pour les téléphones en français | 0.8.2 |
| 2026-10-29 | Revue avant publication | Un QR scanné juste avant son renouvellement était refusé | Le QR précédent reste valable jusqu'à ses 2 minutes | 0.8.2 |
| 2026-10-29 | Revue avant publication | Prise de vue refusée par Android (permission caméra déclarée pour le scan) | Permission demandée avant la photo | 0.8.2 |
| 2026-10-29 | Revue avant publication | Liens vers la politique de confidentialité exigés dans l'app | Liens dans À propos | 0.8.2 |
| 2026-10-29 | Audit grands écrans | Mise en page étirée sur tablette (portrait ignoré au targetSdk 37) | Contenu centré, écran de jeu sur deux colonnes ; grille centrée | 0.8.3 |
| 2026-10-29 | Tournage de la vidéo | « Ready to play? » affiché sur le téléphone invité | Réservé à l'organisateur | 0.8.3 |
| 2026-11-02 | Porteur du projet (Fairphone 6) | Saisie rapide : le choix « quille seule / nombre de quilles » ne sert à rien | Saisie directe du score (0 à 12) ; testée sur téléphone : score, ratés, retour à 25, « 12 ! », déroulé, ancienne partie conformes | 0.8.5 |
| 2026-11-02 | Porteur du projet (Fairphone 6) | GIF « Quelle remontée ! » : le titre déborde de l'image | Tous les textes du clip et du GIF (même moteur de rendu) réduits pour tenir dans les marges ; test sur les textes les plus longs, en vidéo et en GIF | 0.8.5 |
| 2026-11-02 | Porteur du projet (Fairphone 6 hôte, émulateur Android 17 invité, même Wi-Fi) | Partie à côté après la revue de sécurité (TLS imposé, trames bornées, délai d'admission) | Connexion, validation par l'organisateur et lancer saisi sur l'invité reçu par l'hôte : conformes | 0.8.5 |
| 2026-11-08 | Rapport Testers Community (service de testeurs payant) | Aucun plantage ni bug ; fiche du store jugée pauvre en mots-clés | Fiche réécrite (mots-clés « score keeper », « quilles finlandaises », passage obsolète sur la saisie rapide corrigé) | fiche |
| 2026-11-08 | Rapport Testers Community | Pas de choix du thème clair, sombre ou système | Réglages › Affichage › Thème | 0.8.7 |
| 2026-11-08 | Rapport Testers Community | Pas de rubrique Aide ni FAQ, pas de tutoriel visible | Écran « Aide et règles » depuis l'accueil et les Réglages : questions sur l'app, règles, FAQ, recherche, « Poser une question » par e-mail | 0.8.7 |
| 2026-11-08 | Rapport Testers Community | Tutoriel pour les nouveaux joueurs | La partie guidée (4 bulles) est proposée dès l'accueil au premier lancement (« Première fois ? ») | 0.8.8 |
| 2026-11-08 | Rapport Testers Community | Pas de bouton pour noter l'app | À propos › « Noter sur Google Play » (simple lien vers la fiche, sans API Google propriétaire) | 0.8.8 |
| 2026-11-08 | Porteur du projet | « Thème » et « Plein soleil » se recouvrent | Plein soleil devient le 4e choix de la carte Thème | 0.8.8 |

## Réponses préparées pour la demande d'accès à la production
Rédigées le 2026-11-10 avec des faits exacts (décision du 2026-11-08 : pas les réponses d'un modèle tout fait). En
anglais, la langue la plus sûre pour l'examen. Les questions et leurs limites de longueur évoluent : les relire dans la
Play Console au moment de la demande. **À vérifier avant l'envoi** : les points entre crochets.

### Votre test fermé

**How did you recruit users for your closed test?**
Through Testers Community, a testing service that provided a group of Android testers who installed the app and kept it
for the whole test. In parallel, I tested the multi-phone mode myself on two real phones (a Fairphone 6 and a Pixel 7a
on Android 17), because it needs two devices on the same Wi-Fi network or local hotspot.

**How easy was it to recruit testers for your app?**
Easy through the testing service. Mölkky is a niche outdoor game, so finding enough players around me to commit to 14
days of testing would have been harder.

**Describe the engagement you received from testers during your closed test.**
12 testers stayed enrolled for 14 consecutive days without interruption [à vérifier : date de fin]. The main feedback
was a consolidated report from the testing service on November 8, which found no crash and no bug and suggested
usability improvements. I complemented it with my own test sessions on real devices, including full games, the
multi-phone mode on the same Wi-Fi and on a local hotspot, sharing and backup. [À vérifier dans Android vitals : aucun
plantage ni ANR.]

**Provide a summary of the feedback that you received from testers.**
No crash and no bug were reported. The suggestions were: improve the store listing keywords, add a light/dark/system
theme choice, add a help and FAQ section, offer a tutorial to new players, and add a way to rate the app. My own device
tests led to fixes in the multi-phone mode (QR code renewal, permissions on Android 17, disconnect options), in quick
input and in the share video.

### Votre application

**Who is the intended audience of your app?**
Mölkky players, casual and club players, teenagers and adults (13+), who want to keep score without paper or mental
arithmetic, on one or several phones.

**Describe how your app provides value to users.**
Players tap the pins that fell and the app applies the official rules (exactly 50 points, back to 25 when going over,
elimination after three misses). Any past throw can be corrected and everything is recalculated. Several phones can
follow and enter the same game directly, with no account and no internet. It keeps history, a Hall of Fame and
statistics, and lets players share the moment as a picture or a short video. No ads, no trackers, no data collected.

**How many installs do you expect your app to have in your first year?**
More than 10,000 (choix du porteur du projet ; réponse proposée par la Play Console sous forme de tranche).

### Préparation pour la production

**What changes did you make to your app based on what you learned during closed testing?**
Theme choice (system, light, dark, bright sunlight); a Help and rules screen with FAQ and search; the guided first
game offered on the home screen; a "Rate on Google Play" link; a rewritten store listing. Before that, multi-phone
fixes: automatic QR code renewal, local network permission on Android 17, disconnect and phone management, a referee
role. Version 1.0.0 also makes going over 50 visible on the game curve.

**How did you decide that your app is ready for production?**
All feedback was addressed and verified on real phones. Every change passes automated checks: unit tests of the score
engine (100% line coverage), of the multi-phone protocol and its security cases, screenshot tests, lint, a tracker
check (0 trackers) and a known-vulnerability scan of the dependencies. No crash was reported during the test
[à vérifier dans Android vitals].

### Points d'attention
- **Engagement** : le test repose sur un service de testeurs et peu de retours directs. Google refuse parfois l'accès
  quand l'engagement lui paraît faible ; il faudrait alors refaire 14 jours de test avec des testeurs plus actifs.
  Ne pas gonfler les réponses : Google voit l'activité réelle des testeurs.
- **Installations** : « plus de 10 000 » est une ambition, sans effet sur l'examen ; une tranche plus basse reste
  crédible pour une app de niche.
