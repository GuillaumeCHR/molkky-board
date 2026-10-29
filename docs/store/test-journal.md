# Journal du test fermé

Sert à suivre le test fermé obligatoire (12 testeurs ou plus pendant 14 jours consécutifs) et à répondre aux questions
de Google lors de la **demande d'accès à la production**. À tenir au fil de l'eau : chaque retour, sa suite, la version
qui le corrige.

## Suivi du test
| Date | Testeurs inscrits | Jour du compte à rebours | Version | Événement |
|---|---|---|---|---|
| 2026-10-29 | — | — | 0.8.1 (9) | Test interne publié (porteur du projet) |
| 2026-10-29 | — | — | 0.8.2 (10) | Test fermé envoyé à l'examen de Google |
| | | | 0.8.3 (11) | Prête : grands écrans, bouton « Send feedback » |

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

## Réponses préparées pour la demande d'accès à la production
À compléter à la fin des 14 jours avec les chiffres réels. Les questions de Google évoluent : les relire au moment de la
demande.

**Comment avez-vous recruté vos testeurs ?**
Famille, amis et joueurs de Mölkky de l'entourage du développeur [à compléter : club, réseaux, service de testeurs le
cas échéant].

**Quelle a été la facilité de recrutement ?** [à compléter]

**Décrivez l'engagement des testeurs.**
[nombre de testeurs actifs, parties jouées, retours reçus par e-mail et par « Send feedback »]

**Résumé des retours reçus.** [à compléter à partir du tableau ci-dessus]

**Public visé.** Joueurs de Mölkky, amateurs et clubs, adolescents et adultes (13 ans et plus), qui veulent compter les
points sans papier ni calcul, sur un ou plusieurs téléphones.

**Valeur apportée.** Calcul des points et application des règles officielles, correction de n'importe quel lancer,
plusieurs téléphones sur la même partie sans compte ni Internet, historique et statistiques ; aucune publicité, aucun
traceur.

**Installations attendues la première année.** [estimation à donner]

**Modifications faites grâce au test.** Voir le tableau « Retours et corrections ».

**Comment avez-vous décidé que l'app est prête ?**
Tous les retours bloquants corrigés, tests automatisés (moteur de score, protocole, captures d'écran) et tests sur
téléphones réels au vert, aucun plantage remonté pendant [N] jours.
