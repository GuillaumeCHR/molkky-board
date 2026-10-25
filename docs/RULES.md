# Règles du Mölkky — référence pour l'application

> Document reformulé à partir de plusieurs sources (voir § 9). Il sert de référence
> fonctionnelle pour le moteur de score ; la spécification testable est dans
> [`SCORING_SPEC.md`](SCORING_SPEC.md).
>
> Vocabulaire : **lancer** = un jet du Mölkky ; **tour** = chaque équipe a lancé une fois ;
> **manche** (*set* en anglais) = une partie jusqu'à 50 ; **match** (*game* en anglais IMO) =
> une série de manches (ex. 2 manches gagnantes). Un joueur seul compte comme une équipe d'un joueur.

---

## 1. Matériel et mise en place (informatif)

- 12 quilles numérotées de 1 à 12, une quille de lancer (le *Mölkky*), un *mölkkaari*
  (cadre qui délimite l'avant de la zone de lancer).
- Les quilles sont placées en bloc serré à **3,50 m** (± 10 cm) du mölkkaari, dans cette disposition
  (vue depuis le lanceur, rangée du fond en haut) :

  ```
       7   9   8
     5  11  12   6
       3  10   4
         1   2
  ```

  → Cette disposition servira de base à la grille de saisie de l'écran de jeu.
- Après chaque lancer, chaque quille tombée est **relevée là où elle s'est arrêtée**, numéro tourné
  vers la zone de lancer. Le jeu s'étale donc au fil de la manche. Une quille sortie d'un terrain à
  bordure fixe est replacée à une longueur de Mölkky de la bordure.

## 2. Décompte des points d'un lancer

| Quilles tombées | Points marqués |
|---|---|
| aucune | 0 (c'est un **raté**) |
| exactement une | la **valeur inscrite** sur la quille (1 à 12) |
| deux ou plus | le **nombre** de quilles tombées (2 à 12) |

Exemples : la 12 seule → 12 pts ; la 12 et la 4 → 2 pts ; les 12 quilles → 12 pts.

**Quand une quille compte comme tombée** : uniquement si elle est entièrement au sol. Une quille
en appui sur une autre quille, sur le Mölkky ou sur un élément naturel (racine, souche) ne compte
pas ; une quille en appui sur un élément artificiel (bordure, banc) compte. L'IMO précise en plus
qu'une quille appuyée contre un objet fixe (mur) est écartée et compte comme non tombée, et qu'une
quille déplacée par autre chose que le Mölkky du jeu est remise en place sans compter.
→ C'est un jugement humain sur le terrain : l'app enregistre simplement les quilles déclarées tombées.

## 3. Objectif : 50 points exactement

- La première équipe qui atteint **exactement 50** gagne la manche, qui s'arrête **immédiatement**
  (les autres ne finissent pas le tour).
- Si un lancer ferait **dépasser 50**, le score de l'équipe **redescend à 25** et le jeu continue.
  Ce lancer n'est **pas** un raté (des quilles sont tombées).

## 4. Ratés et élimination

- Un **raté** (*misthrow*) est un lancer qui ne fait tomber aucune quille ; il rapporte 0.
  Certaines fautes (§ 5) sont aussi comptées comme des ratés.
- Après **3 ratés consécutifs**, l'équipe est **éliminée** de la manche. Tout lancer qui fait tomber au
  moins une quille remet le compteur à 0.
- **En équipe** : le compteur est **celui de l'équipe**, pas celui de chaque joueur. Trois ratés
  consécutifs de l'équipe (même par des joueurs différents) l'éliminent (IMO § 2.3 / 5.3 ; la copie
  FFMO dit « le joueur seul ou l'équipe »).
- **Fin par élimination** : quand il ne reste qu'une seule équipe en jeu, elle gagne la manche, quel
  que soit son score. Dans le règlement IMO, elle est alors créditée de 50 et l'équipe éliminée de 0.
- Avec 3 équipes ou plus, une élimination ne termine pas la manche : on saute l'équipe éliminée
  et le jeu continue entre les autres.
- « À égalité de score » : deux équipes peuvent avoir le même score sans conséquence ; aucune
  règle officielle ne prévoit d'effet (la variante « on fait redescendre celui qu'on rejoint »
  est listée au § 8).

## 5. Fautes (règlement de tournoi IMO, ch. 5)

Les règles grand public (molkky.world, copie FFMO) se limitent à : *lancer derrière le mölkkaari
sans le toucher*. La copie FFMO précise qu'un **lancer « mordu » compte comme un raté**.
Le règlement IMO détaille :

| Faute | Conséquence |
|---|---|
| **Mordu** (*trespassing*) : toucher ou déplacer le mölkkaari, ou poser une partie du corps au sol au-delà du mölkkaari ou des lignes latérales, entre l'entrée et la sortie (par l'arrière) de la zone de lancer | Le lancer est un **raté** (0 pt, compte pour les 3 ratés) |
| **Mölkky lâché** dans la zone de lancer (hors ramassage / passage de main) | Raté |
| **Temps dépassé** (60 s par défaut) : 1er dépassement = avertissement, 2e dans le même match = lancer perdu | Raté |
| **Joueur absent** / ne peut pas lancer | Raté |
| **Mauvais joueur** de l'équipe qui lance | Raté ; l'ordre continue **comme si le bon joueur avait lancé** (un joueur peut donc lancer deux fois de suite) |
| **Mauvaise équipe** qui lance hors de son tour | Points du lancer annulés ; l'équipe fautive **perd son prochain lancer régulier** (raté) ; l'ordre reprend comme si personne n'avait lancé |

**Règle des 37 points (IMO § 5.7)** : si l'équipe fautive a **37 points ou plus** et commet un
mordu, une erreur de joueur ou une erreur d'équipe, son score **redescend à 25** et la faute compte
**aussi** comme raté. Les fautes « Mölkky lâché », « temps » et « absence » ne déclenchent pas
cette règle. Une contestation doit être faite avant le lancer suivant, sinon elle est irrecevable.

Les **sanctions disciplinaires** (avertissement, perte de lancer, de manche, de match,
disqualification) relèvent de l'arbitre ; l'app ne les traitera que via « perte de manche / match »
saisie par l'organisateur (hors v1).

## 6. Équipes et ordre de passage

- Chaque équipe a **un seul lancer par tour** ; les équipes lancent à tour de rôle dans un ordre fixé.
- Au sein d'une équipe, les joueurs lancent **à tour de rôle selon l'ordre annoncé** par le capitaine
  en début de manche. L'ordre (et la composition) peut changer entre deux manches, pas pendant.
- Les équipes éliminées sont sautées.
- Taille d'équipe libre en jeu amateur ; en tournoi IMO : 2, 3 ou 4 joueurs (+ remplaçants).

## 7. Fin de manche et déroulé d'un match

**Fin de manche** : une équipe atteint exactement 50, ou il ne reste qu'une équipe non éliminée.

**Match (série de manches)** :
- Habituellement en **2 manches gagnantes** (meilleur des 3) ; l'IMO cite aussi le meilleur des 5.
- **Qui commence** :
  - 1re manche : tirage au sort (consensus) ou ordre fixé par l'organisateur (IMO).
  - Manches suivantes, selon les sources :
    - *Consensus (molkky.world)* : si c'est 1–1 après deux manches, chaque équipe additionne ses
      points des deux manches et **celle qui a le plus de points commence la belle** ; à égalité,
      **Mölkkout**.
    - *IMO* : laissé à l'organisateur ; exemples donnés : alternance, ou tirage au sort puis
      alternance avec Mölkkout pour la 5e manche.
- **Classement en tournoi** (IMO) : à définir par l'organisateur, ex. manches gagnées puis
  différence de points ; d'où l'intérêt de stocker les points de chaque manche.

**Mölkkout** (départage / choix de l'équipe qui commence) :
- Quilles **6, 4, 12, 10, 8** alignées dans cet ordre, espacées d'une longueur de Mölkky, la 6 à 3,50 m.
  Les quilles tombées sont **remises à leur position de départ**.
- Décompte habituel (une quille = sa valeur, plusieurs = leur nombre) ; on additionne. Pas de règle
  des 50, pas d'élimination.
- Lancers par joueur : seul → 3 ; équipes de 2 → 2 chacun ; 3 joueurs ou plus → 1 chacun.
- Ordre des équipes : `A BB AA BB …`, terminé par un seul lancer
  (seul et 3 joueurs : `A BB AA B` ; 2 et 4 joueurs : `A BB AA BB A`).
- Le plus haut total gagne ; à égalité, chaque joueur relance une fois jusqu'à départage.

## 8. Variantes courantes → options paramétrables

| Option | Valeur officielle | Variantes répandues |
|---|---|---|
| Score cible | 50 | 30, 40, 100… (parties courtes/enfants) |
| Dépassement de la cible | retour à 25 | retour à 0 ; retour à la moitié de la cible ; lancer annulé (on garde son score) ; rebond (cible − excédent) |
| Ratés consécutifs avant sanction | 3 | 2, 4, 5 |
| Sanction des ratés | élimination | aucune ; retour à 0 ; retour à 25 |
| Règle des 37 points | IMO : oui ; jeu amateur : non | — |
| Mordu = raté | oui | mordu ignoré (on ne pénalise pas en amateur) |
| Fin de manche | dès que quelqu'un atteint 50 | **jouer les places** (on continue pour la 2e, 3e place…) ; **finir le tour** pour que chacun ait lancé autant de fois (non retenue en v1, trop de cas d'égalité) |
| Rejoindre le score d'un autre | sans effet | l'équipe rejointe redescend à 25 (variante rare, façon « fléchettes ») |
| Match | 2 manches gagnantes | manche unique, 3 manches gagnantes |
| Qui commence la manche suivante | voir § 7 | alternance ; le perdant de la manche précédente commence ; tirage au sort |
| Départage | Mölkkout | aucun (somme des points) |
| Score de fin par élimination | IMO : gagnant 50 / éliminé 0 | scores réels conservés |

Deux préréglages en découlent : **« Classique »** (consensus des fédérations, sans règle des 37,
scores réels conservés) et **« Tournoi IMO »** (règle des 37, fautes détaillées, 50/0 en cas
d'élimination).

## 9. Sources et divergences

**Sources consultées (25/10/2026)** :
1. **IMO – International Mölkky Organisation, *Tournament Rules*, révision du 30/03/2019** (PDF sur
   international-molkky.org). Règlement de tournoi le plus détaillé ; la version anglaise fait foi.
2. **molkky.world, *Règles du Mölkky*** (PDF FR et page EN, 2022) : règles « acceptées par la plupart
   des associations et fédérations officiellement reconnues ».
3. **Règles du jeu – Fédération Française de Mölkky (FFMO)** : copie hébergée par la circonscription
   de Luxeuil (académie de Besançon, 2017). ⚠ Je n'ai pas trouvé le règlement officiel à jour sur
   le site de la FFMO ; cette copie peut être ancienne.
4. **Tuoterengas / Tactic Games** : Tuoterengas (devenu Lahden Paikka) a cédé la marque et la
   production à **Tactic Games Oy** en 2016 ; aucune page de règles publiée par le fabricant n'a été
   trouvée en ligne. Les règles IMO renvoient au matériel Tactic.

**Divergences relevées** :

| Sujet | IMO | molkky.world | Copie FFMO |
|---|---|---|---|
| Distance des quilles | 3,5 m ± 0,1 | 3,5 m ± 0,1 (≈ 3–4 m dans le résumé) | ≈ 4 m |
| 3 ratés | équipe hors de la manche **avec 0 point** | joueur éliminé | joueur ou équipe éliminé |
| Gagnant par élimination | crédité de 50 | « le dernier restant gagne » | « le dernier restant gagne » |
| Mordu | raté (+ règle des 37) | seulement « lancer sans toucher le mölkkaari » | raté |
| Fautes d'ordre, temps, Mölkky lâché | détaillées | non traitées | non traitées |
| Qui commence la belle | à l'organisateur | plus de points sur les 2 manches, sinon Mölkkout | non traité |
| Rôle du Mölkkout | décider qui commence une manche | départager une égalité de points | non traité |

**Points d'interprétation à valider** (repris dans la spec, marqués ⚠) :
- Faute « mauvaise équipe » : on applique le retour à 25 de la règle des 37 au moment de la faute, et
  le raté sur le prochain lancer régulier de l'équipe fautive (un seul raté au total).
- Mölkkout à égalité : « chaque joueur relance » = un tour supplémentaire complet (un lancer par
  joueur, même alternance), comparaison à la fin de chaque tour.
- La faute « mauvais joueur » ne se présentera presque jamais dans l'app (elle impose le joueur
  courant) ; elle reste saisissable pour l'arbitrage.
