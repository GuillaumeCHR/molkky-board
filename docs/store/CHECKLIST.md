# Checklist de publication sur le Google Play Store

Mise à jour : 2026-11-05 (version 0.8.5, build 13). Statuts : ✅ fait · 🟡 en cours · ⬜ à faire · 👤 action du porteur
du projet (compte, identité, décision). Décisions : V1 **sans compte** (phase 4 en V2), **modes de jeu après la
publication**, **F-Droid après le Play Store**.

## Décisions
| Point | Statut | Note |
|---|---|---|
| Phase 4 (comptes) reportée en V2 | ✅ | validé le 2026-10-28 |
| Modes de jeu après la publication | ✅ | validé le 2026-10-28 |
| Marque « Mölkky » vérifiée, nom validé | ✅ | nom conservé ; non-affiliation dans À propos, README, `TRADEMARKS.md`, fiche du store, site ; nom de repli à garder sous le coude |
| Public cible : 13 ans et plus, hors programme « Familles » | ✅ | déclaré dans la Play Console |
| Langue par défaut : anglais (fiche et app) | ✅ | un téléphone en français reste en français (0.8.2) |
| Lien de soutien (« Paye-moi un café ») | ⬜ | seulement sur le site, jamais dans l'app ; règle Play et statut DSA à vérifier |
| F-Droid | ⬜ | après le Play Store (voir `CLAUDE.md`, décisions du 2026-10-29) |

## Comptes et clés
| Point | Statut | Note |
|---|---|---|
| Compte développeur personnel « Yogi Studio » | ✅ | validé le 2026-10-29 |
| Adresse de contact publique dédiée | ✅ | yogistudiofr@gmail.com |
| Clé d'envoi créée et sauvegardée | ✅ | 2026-10-28 ; archive chiffrée sur le NAS et par e-mail |
| Play App Signing accepté | ✅ | au premier envoi (test interne 0.8.1) |

## Application
| Point | Statut | Note |
|---|---|---|
| Fiche créée (`com.yogistudio.molkkyboard`, Application, Sports, gratuite) | ✅ | 2026-10-29 |
| AAB signé | ✅ | 0.8.4 (12), construit le 2026-11-02 avec `-Pmolkky.feedback=true` pour le test fermé |
| Test interne | ✅ | 0.8.1 (9) publié le 2026-10-29 |
| Test fermé | 🟡 | 0.8.2 (10) envoyé le 2026-10-29 ; 0.8.4 (12) à envoyer (notes : `docs/store/release-notes/0.8.4.md`) |
| Release minifiée testée sur téléphone | 🟡 👤 | 0.8.0 sur Fairphone (partie, reprise, partage, sauvegarde) ; à refaire avec la 0.8.3 depuis le Play Store, dont la partie à côté |
| targetSdk (API 36 minimum) | ✅ | 37 |
| Grands écrans (≥ 600 dp, portrait ignoré au targetSdk 37) | ✅ | mise en page adaptative 0.8.3 (`docs/qa/FORM_FACTORS.md`) |
| Permission « réseau local » (Android 17), hôte et invité | ✅ | testé sur Pixel 7a |
| Permissions minimales, aucune localisation | ✅ | liste blanche en CI (`checkReleasePermissions`) |
| Permission caméra avant une photo de joueur ou de vignette | ✅ | 0.8.2 (à vérifier sur téléphone) |
| Icône adaptative avec variante monochrome | ✅ | |
| Aucun traceur | ✅ | contrôle Exodus en CI ; analyse Exodus Privacy après publication ⬜ |
| Langues : anglais (défaut) et français | ✅ | |
| Liens vers la politique de confidentialité et les conditions dans l'app | ✅ | À propos (0.8.2) |
| Bouton « Send feedback » pour les testeurs | ✅ | release construite avec `-Pmolkky.feedback=true` (0.8.3) |
| Version publique : versionName 1.0.0 | ⬜ | à la demande d'accès à la production |
| Suppression du compte | ✅ sans objet | pas de compte en V1 |

## Sécurité et vie privée
| Point | Statut | Note |
|---|---|---|
| Modèle de menaces à jour (partie à côté livrée : compteur, arbitre, départ, fin automatique) | ✅ | `docs/security/THREAT_MODEL.md`, 2026-11-02 (N13, N14) |
| Revue MASVS (`docs/security/MASVS_CHECKLIST.md`) | ✅ | 2026-11-02 ; 5 corrections (TLS imposé, trames bornées, connexions sans jeton fermées, fermeture accusée, tapjacking) |
| Jeton de session de l'invité | ✅ | en mémoire seulement, jamais sur le disque (plus strict que le chiffrement prévu) |
| Protection contre le tapjacking sur « accepter un téléphone » | ✅ | et sur « Supprimer toutes mes données » |
| Vérification des dépendances, osv-scanner, SBOM | ✅ | 2026-11-02 ; `verification-metadata.xml`, SBOM CycloneDX + osv-scanner en CI ; Guava relevée (GHSA-xxph-c9ww-hj94) |
| Analyse statique MobSF (ou mobsfscan) | ⬜ 👤 | outil externe à installer (Docker pour MobSF, `pipx` pour mobsfscan) : à lancer avec ton accord |
| Registre des traitements (`docs/privacy/REGISTRE.md`) | 🟡 👤 | 2026-11-05 : 4 traitements (e-mails, rapports de plantage, vulnérabilités, testeurs) ; durées de conservation et double authentification du compte Google à valider |
| `SECURITY.md` et `security.txt` | ✅ | `security.txt` sur le site |
| Test d'intrusion léger | ⬜ 👤 | |

## Site web (`GuillaumeCHR/molkky-board-site`, GitHub Pages)
| Point | Statut | Note |
|---|---|---|
| Dépôt public et publication | ✅ | https://guillaumechr.github.io/molkky-board-site/ |
| Accueil (en, fr) | ✅ | |
| Politique de confidentialité | ✅ | `/privacy.html`, `/fr/confidentialite.html` ; déclarée dans la Play Console |
| Conditions d'utilisation, âge minimum | ✅ | `/terms.html`, `/fr/conditions.html` |
| `security.txt` | ✅ | |
| Lien vers le Play Store sur l'accueil | ⬜ | à la publication |

## Fiche du Play Store
| Point | Statut | Note |
|---|---|---|
| Textes en anglais (défaut) et en français | ✅ | `docs/store/listing/en-US.md`, `fr-FR.md` ; autres variantes d'anglais reprises automatiquement |
| Icône 512 × 512 | ✅ | `docs/store/icon-512.png` |
| Image de présentation 1 024 × 500 | ✅ | `docs/store/screenshots/<langue>/feature-graphic.png` |
| Captures (8, en et fr) | ✅ | grille centrée (0.8.3) ; `tools/store/compose_screenshots.py` ; **à remplacer dans la Play Console** |
| Vidéo YouTube de la fiche | ⬜ | facultatif |

## Déclarations Play Console
| Point | Statut | Note |
|---|---|---|
| Sécurité des données | ✅ | aucune donnée collectée ni partagée |
| Service de premier plan `connectedDevice` (justification et vidéo) | ✅ | vidéo de démonstration (non répertoriée) |
| Classification du contenu (IARC) | ✅ | interactions entre utilisateurs : oui, limitées aux invités acceptés |
| Public cible et contenu | ✅ | 13 ans et plus |
| Publicités : aucune | ✅ | |
| Applications de santé | ✅ | non concernée |
| Accès à l'app | ✅ sans objet | pas de compte |
| Statut DSA : non-commerçant | 🟡 👤 | à vérifier dans le profil du compte |

## Test fermé (obligatoire pour un compte personnel)
| Point | Statut | Note |
|---|---|---|
| Liste de testeurs | ✅ | « Molkky board test list » |
| Instructions pour les testeurs | ✅ | retirer la remarque sur les tablettes avec la 0.8.3 |
| Plan de test | ✅ | `docs/store/closed-test-plan.md` (14 jours, 30 questions) |
| 12 testeurs ou plus pendant 14 jours consécutifs (viser 15 à 20) | ⬜ 👤 | démarre après l'approbation de Google |
| Journal des retours et des corrections | 🟡 | `docs/store/test-journal.md` |
| Demande d'accès à la production | ⬜ 👤 | réponses préparées dans le journal |

## Tests sur téléphone encore ouverts
| Point | Statut | Note |
|---|---|---|
| Partie à côté : même Wi-Fi et point d'accès | ✅ | Fairphone 6 + Pixel 7a, 2026-10-28 |
| Point d'accès sans réseau mobile (plage) | ⬜ 👤 | |
| Coupures, hôte écran éteint, refus de permissions | ⬜ 👤 | `docs/qa/PARTIE_A_COTE.md` |
| Version Play Store 0.8.3 : photo de joueur, langue, liens d'À propos, partie à côté | ⬜ 👤 | |
