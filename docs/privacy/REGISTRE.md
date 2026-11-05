# Registre simplifié des traitements — Molkky Board

Mise à jour : 2026-11-05 (version 0.8.5, V1 **sans compte**). Responsable : **Yogi Studio**, développeur individuel
établi en France. Contact : yogistudiofr@gmail.com. Référence : RGPD art. 30, recommandations de la CNIL pour les
applications mobiles.

> L'exemption de l'art. 30.5 (moins de 250 personnes) ne s'applique pas aux traitements non occasionnels ; ce
> registre est tenu dans tous les cas, par transparence. Il doit rester cohérent avec la politique de confidentialité
> du site (`molkky-board-site`, `privacy.html` et `fr/confidentialite.html`) : toute modification de l'un impose de
> relire l'autre.

## 1. Ce que l'app ne fait pas

Yogi Studio n'exploite **aucun serveur** pour l'app et n'en reçoit **aucune donnée** : ni compte, ni identifiant, ni
localisation, ni contacts, ni statistiques d'usage, ni identifiant publicitaire. Aucune bibliothèque de publicité,
d'analytics, d'attribution ou de rapport de plantage (contrôle Exodus en CI, `docs/privacy/RAPPORT_TRACEURS.md`).

## 2. Traitements dont Yogi Studio est responsable

| # | Traitement | Finalité | Base légale | Personnes | Données | Durée de conservation | Sous-traitants (localisation) |
|---|---|---|---|---|---|---|---|
| T1 | Messages reçus à l'adresse de contact (questions, retours, « Send feedback ») | Répondre, corriger l'app | Intérêt légitime (art. 6.1.f) | Utilisateurs qui écrivent | Adresse e-mail, nom affiché par la messagerie, contenu du message ; pour « Send feedback » : version de l'app et modèle du téléphone | **2 ans après le dernier échange** ⚠ à valider | Google (Gmail), États-Unis — garanties : DPF UE–États-Unis et clauses contractuelles types |
| T2 | Rapports de plantage envoyés par l'utilisateur | Corriger les plantages | Intérêt légitime (art. 6.1.f) | Utilisateurs qui envoient le rapport | Ceux de T1 + version d'Android, modèle, types d'exception et pile d'appels (aucun message d'exception, aucune donnée de partie) | **Jusqu'à la correction, 1 an au plus** ⚠ à valider | Google (Gmail), comme T1 |
| T3 | Signalements de vulnérabilités (`SECURITY.md`, `security.txt`) | Corriger, documenter la divulgation | Intérêt légitime (art. 6.1.f) | Chercheurs en sécurité | Ceux de T1 + description technique | **3 ans après la clôture** ⚠ à valider | Google (Gmail), comme T1 |
| T4 | Liste des testeurs du test fermé (Play Console) | Donner accès aux versions de test, atteindre le seuil de Google | Consentement de chaque testeur (art. 6.1.a), retirable à tout moment | Testeurs invités | Adresse e-mail du compte Google | **Jusqu'à la fin des tests ; retrait sur simple demande** | Google (Play Console), États-Unis, comme T1 |

Aucune décision automatisée, aucun profilage, aucune donnée sensible (art. 9), aucun mineur de moins de 13 ans visé.

## 3. Données sur le téléphone (hors registre, pour mémoire)

Joueurs et équipes (pseudos, couleurs, emblèmes), journaux des parties, photo de joueur facultative (réencodée, sans
EXIF), réglages, dernier rapport de plantage en attente de décision. Ces données **ne quittent jamais le téléphone**
sauf à l'initiative de l'utilisateur ; Yogi Studio n'y a pas accès. Exclues des sauvegardes cloud et du transfert
d'appareil ; supprimables par Réglages › Données › « Supprimer toutes mes données » ou en désinstallant l'app.

**Partie à côté** : pseudo, emblème, joueurs et équipes de la partie et événements de jeu échangés directement entre
les téléphones de la partie, en TLS, sur le réseau local, sans serveur ni Internet. Ni photo ni identifiant d'appareil.
Clés et jetons supprimés à la fin de la partie (jeton de session de l'invité en mémoire seulement). **Aucun
sous-traitant.**

## 4. Tiers agissant comme responsables distincts (hors registre)

| Tiers | Ce qu'il traite | Ce que voit Yogi Studio |
|---|---|---|
| Google Play | Installations, stabilité, avis, selon sa propre politique | Statistiques agrégées de la Play Console |
| GitHub (dépôt public, GitHub Pages du site) | Journaux techniques des visiteurs (adresse IP) selon sa propre politique | Rien (aucune mesure d'audience sur le site) |
| Moteur de synthèse vocale du téléphone | Texte des annonces (désactivées par défaut) | Rien |
| App choisie dans la feuille de partage | Image, vidéo ou GIF partagé | Rien |

## 5. Droits des personnes

Accès, rectification, effacement, opposition, limitation : par e-mail à yogistudiofr@gmail.com, réponse sous un
mois. Pour T1 à T3, effacement des messages concernés dans Gmail (corbeille vidée). Pour T4, retrait de la liste des
testeurs dans la Play Console. Réclamation possible auprès de la CNIL.

## 6. Sécurité des traitements T1 à T4

Compte Google de Yogi Studio protégé par une double authentification (⚠ à confirmer par le porteur du projet) ;
aucune copie des messages hors de Gmail ; les rapports de plantage cités dans un ticket ou un commit sont réduits
à la pile d'appels, sans adresse e-mail.

## 7. À revoir

- Phase 4 (comptes, jeu à distance, groupes, V2) : nouveaux traitements (authentification, données de groupe,
  journalisation de sécurité), hébergeur et sous-traitants à inscrire **avant** la version concernée.
- Lien de soutien (« Paye-moi un café ») sur le site : plateforme de paiement à inscrire s'il est ajouté.
- Mesure de viralité : aucune aujourd'hui ; à inscrire si elle est décidée.
