# Checklist de publication sur le Google Play Store

Mise à jour : 2026-10-28 (version 0.8.0, build 8). Statuts : ✅ fait · 🟡 en cours · ⬜ à faire · 👤 action du porteur
du projet (compte, identité, décision). Décisions : V1 **sans compte** (phase 4 en V2), **modes de jeu après la
publication**.

## Décisions
| Point | Statut | Note |
|---|---|---|
| Phase 4 (comptes) reportée en V2 | ✅ | validé le 2026-10-28 |
| Modes de jeu après la publication | ✅ | validé le 2026-10-28 |
| Marque « Mölkky » vérifiée, nom validé ou nom de repli | ✅ | nom conservé (usage toléré par les apps tierces) ; non-affiliation dans À propos, README, `TRADEMARKS.md` ; à reprendre dans la fiche ; nom de repli à garder sous le coude |
| Public cible : 13 ans et plus, hors programme « Familles » | ⬜ 👤 | à confirmer au questionnaire |
| Lien de soutien (« Paye-moi un café ») | ⬜ | seulement sur le site, jamais dans l'app ; règle Play et statut DSA à vérifier |

## Comptes et clés
| Point | Statut | Note |
|---|---|---|
| Compte développeur personnel « Yogi Studio » (25 $, identité vérifiée) | ⬜ 👤 | |
| Adresse de contact publique dédiée | ✅ | yogistudiofr@gmail.com |
| Clé d'envoi créée et sauvegardée | 🟡 👤 | créée le 2026-10-28 (empreinte SHA-256 `71:A7:37:72:…:54:A8:DF:B0`) ; sauvegarde à faire, `docs/RELEASE.md` § 2 |
| Play App Signing accepté | ⬜ 👤 | au premier envoi |

## Application
| Point | Statut | Note |
|---|---|---|
| AAB signé (`bundleRelease`) | ✅ | 0.8.0 (build 8) signé avec la clé d'envoi le 2026-10-28 ; APK release signé (v2), 0 traceur |
| Release minifiée testée sur téléphone (dont partie à côté) | 🟡 👤 | 0.8.0 sur Fairphone 6 (2026-10-28) : partie, reprise, partage, sauvegarde, À propos OK ; **partie à côté en release non testée** (release sur les deux téléphones) |
| targetSdk au niveau exigé (API 36 minimum) | ✅ | targetSdk 37 |
| Permission « réseau local » (Android 17) gérée, hôte et invité | ✅ | testé sur Pixel 7a |
| Permissions minimales, aucune localisation | ✅ | liste blanche vérifiée en CI (`checkReleasePermissions`) |
| Icône adaptative avec variante monochrome | ✅ | |
| Aucun traceur | ✅ | contrôle Exodus en CI (0 traceur) ; analyse Exodus Privacy après publication ⬜ |
| Langues : français et anglais | ✅ | |
| Version : versionCode +1 à chaque envoi, versionName 1.0.0 | ⬜ | à la première version publique |
| Bouton « Envoyer un retour » (versions de test seulement) | ✅ | |
| Suppression du compte dans l'app et sur le site | ✅ sans objet | pas de compte en V1 |

## Sécurité et vie privée
| Point | Statut | Note |
|---|---|---|
| Modèle de menaces à jour (partie à côté livrée : compteur, arbitre, départ) | 🟡 | `docs/security/THREAT_MODEL.md` |
| Revue MASVS (`docs/security/MASVS_CHECKLIST.md`) | ⬜ | |
| Jeton de session de l'invité chiffré (Keystore) et exclu des sauvegardes | ⬜ | à vérifier dans le code |
| Protection contre le tapjacking sur « accepter un téléphone » | ⬜ | prévue par la spec |
| Vérification des dépendances, osv-scanner, SBOM, MobSF | ⬜ | |
| Registre des traitements (`docs/privacy/REGISTRE.md`) | ⬜ | presque vide sans compte |
| `SECURITY.md` | ✅ | `/.well-known/security.txt` sur le site ⬜ |
| Test d'intrusion léger | ⬜ 👤 | |

## Site web (dépôt public `molkky-board-site`, GitHub Pages)
| Point | Statut | Note |
|---|---|---|
| Création du dépôt public | ⬜ 👤 | accord nécessaire |
| Accueil (fr, en) | ⬜ | |
| Politique de confidentialité (URL exigée par Google) | ⬜ | |
| Conditions d'utilisation, âge minimum | ⬜ | |
| Page de suppression de compte | ✅ sans objet | pas de compte en V1 |
| `security.txt` | ⬜ | |
| Liens d'À propos branchés sur le site | ⬜ | |

## Fiche du Play Store (`docs/store/`, fr et en)
| Point | Statut | Note |
|---|---|---|
| Titre (30), description courte (80), longue (4 000) | ⬜ | |
| Icône 512 × 512, image de présentation 1 024 × 500 | ⬜ | |
| 4 captures ou plus (accueil, jeu, fin de partie, Hall of Fame) | ⬜ | depuis Roborazzi |
| Vignette de partage d'exemple | ⬜ | |
| « Plusieurs téléphones, sans compte ni Internet » mis en avant | ⬜ | |

## Déclarations Play Console (`docs/store/declarations.md`)
| Point | Statut | Note |
|---|---|---|
| Sécurité des données | ⬜ | aucune collecte ; qualification du transfert direct entre téléphones à vérifier |
| Service de premier plan `connectedDevice` : justification (+ vidéo) | ⬜ | |
| Classification du contenu (IARC) | ⬜ 👤 | |
| Public cible et contenu | ⬜ 👤 | |
| Publicités : aucune | ⬜ 👤 | |
| Accès à l'app | ✅ sans objet | pas de compte à fournir |
| Statut DSA : non-commerçant | ⬜ 👤 | |

## Test fermé (obligatoire pour un compte personnel)
| Point | Statut | Note |
|---|---|---|
| Kit testeurs (`docs/store/testeurs.md`) | ⬜ | |
| 12 testeurs ou plus pendant 14 jours consécutifs (viser 15 à 20) | ⬜ 👤 | |
| Journal des retours et des corrections | ⬜ | pour le questionnaire d'accès à la production |

## Tests sur téléphone encore ouverts
| Point | Statut | Note |
|---|---|---|
| Partie à côté : même Wi-Fi et point d'accès | ✅ | Fairphone 6 + Pixel 7a, 2026-10-28 |
| Point d'accès sans réseau mobile (plage) | ⬜ 👤 | |
| Coupures, hôte écran éteint, refus de permissions | ⬜ 👤 | `docs/qa/PARTIE_A_COTE.md` |
| Plan J7b (0.6.0) | ⬜ 👤 | `docs/tests/J7b-plan-de-test.md` |
