# Spécification du moteur de score

> Contrat du futur module `core:scoring` (Kotlin pur). Chaque ligne d'un tableau deviendra un test
> JUnit. Les règles d'origine sont dans [`RULES.md`](RULES.md) ; les points d'interprétation
> sont marqués ⚠.

## 1. Modèle

### 1.1 Configuration (`GameConfig`)

| Champ | Type | « Classique » (défaut) | « Tournoi IMO » |
|---|---|---|---|
| `targetScore` | Int | 50 | 50 |
| `overshoot` | `ResetTo(n)` · `Stay` · `Bounce` | `ResetTo(25)` | `ResetTo(25)` |
| `missLimit` | Int ≥ 1 | 3 | 3 |
| `onMissLimit` | `Eliminate` · `ResetScore(n)` · `Nothing` | `Eliminate` | `Eliminate` |
| `rule37` | Boolean (seuil 37 → 25) | false | true |
| `faultsCountAsMiss` | Boolean | true | true |
| `endCondition` | `FirstToTarget` · `PlayForPlaces` | `FirstToTarget` | `FirstToTarget` |
| `eliminationSetScore` | `Keep` · `WinnerTargetLoserZero` | `Keep` | `WinnerTargetLoserZero` |
| `catchUpReset` | Boolean (rejoindre un score fait redescendre l'autre) | false | false |

`ResetTo(n)` ramène au score `n` ; `Stay` garde le score d'avant le lancer ; `Bounce` donne
`target − (score + points − target)`.

### 1.2 État d'une manche (`SetState`, immuable)

```
SetState(
  teams:      List<TeamState>   // ordre de passage
  turn:       TeamId?           // équipe qui doit lancer (null si terminée)
  status:     IN_PROGRESS | FINISHED
  ranking:    List<TeamId>      // rempli à la fin (1er, 2e, …)
  log:        List<Event>       // journal accepté
)
TeamState(id, players: List<PlayerId>, nextPlayerIdx, score, misses, eliminated, finishedRank?, pendingForfeit)
```

### 1.3 Événements (entrée du moteur)

```
Throw(team, player, pins: Set<Int>)      // pins vide = raté
Fault(team, player, kind)                // kind ∈ TRESPASS, DROPPED, TIMEOUT, ABSENT, WRONG_PLAYER
WrongTeam(offendingTeam, player, pins)   // ⚠ avancé : lancer d'une équipe hors de son tour
Undo                                     // retire le dernier événement saisi
Abandon                                  // la manche est abandonnée (statut ABANDONED)
Correct(index, replacement: Event)       // remplace un événement passé (organisateur)
```

### 1.4 Invariants

- **Fonction pure** : `reduce(state, event) → Result<SetState, Error>`. L'état n'est jamais modifié sur place.
- **Rejeu** : `state == fold(initial(config, teams), log)`. Undo et correction reconstruisent l'état
  en rejouant le journal (pas de « calcul inverse »).
- Les événements dérivés (lancer perdu automatiquement après `WrongTeam`) ne sont **pas** stockés
  dans le journal ; ils sont recalculés au rejeu.
- Le score d'une équipe est toujours compris entre 0 et `targetScore`.

### 1.5 Points d'un lancer

```
points(pins) = 0             si pins vide
             = la valeur     si |pins| = 1
             = |pins|        si |pins| ≥ 2
```

## 2. Notation des cas

Sauf mention contraire : configuration « Classique », 2 équipes d'un joueur, **A** (Alice) puis **B** (Bruno).
`A 30/1` = A a 30 points et 1 raté consécutif. `→ B` = B doit lancer ensuite. `✗A` = A éliminé.
`{7}` = la quille 7 seule ; `{}` = raté.

## 3. Points d'un lancer (`P`)

| ID | État | Lancer | Résultat | Note |
|---|---|---|---|---|
| P1 | A 0/0 | A `{7}` | A 7/0 → B | une quille = sa valeur |
| P2 | A 0/0 | A `{12, 4}` | A 2/0 → B | plusieurs quilles = leur nombre |
| P3 | A 0/0 | A `{3, 5}` | A 2/0 | pas 8 |
| P4 | A 0/0 | A `{1}` | A 1/0 | |
| P5 | A 0/0 | A `{1..12}` | A 12/0 | toutes les quilles |
| P6 | A 0/0 | A `{}` | A 0/1 → B | raté |
| P7 | A 10/2 | A `{2}` | A 12/0 | un lancer réussi remet le compteur à 0 |

## 4. Score cible et dépassement (`O`)

| ID | État | Lancer | Résultat | Note |
|---|---|---|---|---|
| O1 | A 45/0 | A `{5}` | A 50, **FINISHED**, classement [A, B], turn = null | exactement 50 |
| O2 | A 45/0 | A `{6}` | A **25**/0 → B | 51 > 50 |
| O3 | A 45/0 | A `{6, 7}` | A 47/0 | 2 quilles = 2 pts |
| O4 | A 40/0 | A `{12}` | A 25/0 | 52 |
| O5 | A 49/0 | A `{2, 3}` | A 25/0 | 51 |
| O6 | A 48/0 | A `{1, 2}` | A 50, FINISHED | 50 via plusieurs quilles |
| O7 | A 38/0 | A `{12}` | A 50, FINISHED | |
| O8 | A 45/**2** | A `{9}` | A 25/**0** | le dépassement n'est pas un raté |
| O9 | A 50, FINISHED | B `{3}` | Erreur `SET_FINISHED` | aucun lancer après la fin |
| O10 | A 20/0 | A `{5}` | A 25/0 | atteindre 25 normalement est sans effet particulier |

## 5. Ratés et élimination (`E`)

| ID | Équipes | État | Lancer | Résultat |
|---|---|---|---|---|
| E1 | A, B | A 30/2, B 12/0 | A `{}` | ✗A ; **FINISHED**, classement [B, A] (B gagne avec 12) |
| E2 | A, B, C | A 30/2, tour de A | A `{}` | ✗A → B ; la manche continue, l'ordre devient B, C, B, C… |
| E3 | A, B, C | ✗A, B 40/2, C 10/0 | B `{}` | ✗B ; FINISHED, classement [C, B, A] (éliminés classés du dernier au premier éliminé) |
| E4 | A, B | A 30/1 | A `{}` puis B `{4}` puis A `{}` | ✗A (2 → 3 ratés) |
| E5 | A, B | A 30/1 | A `{}`, B…, A `{1}`, B…, A `{}` | A 31/1 : les ratés doivent être consécutifs |
| E6 | équipes A=[Alice, Arthur], B=[Bruno] | A 0/0 | Alice `{}`, Bruno `{3}`, Arthur `{}`, Bruno `{3}`, Alice `{}` | ✗A : **le compteur est celui de l'équipe** |
| E7 | A, B, preset IMO | A 30/2, B 12/0 | A `{}` | ✗A ; résultat de manche : B **50**, A **0** (`WinnerTargetLoserZero`) ; les scores en direct restent 30/12 dans le journal |

## 6. Ordre de passage (`T`)

| ID | Configuration | Séquence attendue des lanceurs |
|---|---|---|
| T1 | A=[Alice, Arthur], B=[Bruno, Béa, Benoît] | Alice, Bruno, Arthur, Béa, Alice, Benoît, Arthur, Bruno, … (chaque équipe fait tourner ses propres joueurs) |
| T2 | A, B, C ; ✗B au 2e tour | …, A, C, A, C, … |
| T3 | A, B, C ; A atteint 50 (`FirstToTarget`) | fin immédiate : B et C ne lancent pas |
| T4 | Lancer saisi pour une équipe dont ce n'est pas le tour (sans `WrongTeam`) | Erreur `NOT_YOUR_TURN` |
| T5 | Lancer saisi par un joueur qui n'appartient pas à l'équipe | Erreur `UNKNOWN_PLAYER` |

## 7. Fautes (`F`)

| ID | Config | État | Événement | Résultat |
|---|---|---|---|---|
| F1 | Classique | A 30/0 | `Fault(A, TRESPASS)` | A 30/**1** → B (mordu = raté) |
| F2 | IMO (rule37) | A 40/0 | `Fault(A, TRESPASS)` | A **25**/1 |
| F3 | IMO | A **37**/0 | `Fault(A, TRESPASS)` | A 25/1 (seuil inclus) |
| F4 | IMO | A 36/0 | `Fault(A, TRESPASS)` | A 36/1 |
| F5 | IMO | A 40/2 | `Fault(A, TRESPASS)` | A 25/3 → ✗A |
| F6 | IMO | A 40/0 | `Fault(A, DROPPED)` | A 40/1 (la règle des 37 ne s'applique pas) |
| F7 | IMO | A 40/0 | `Fault(A, TIMEOUT)` / `Fault(A, ABSENT)` | A 40/1 |
| F8 | IMO, A=[Alice, Arthur] | tour d'Alice, A 40/0 | `Fault(A, Arthur, WRONG_PLAYER)` | A 25/1 ; le prochain lanceur de A est **Arthur** (on avance comme si Alice avait lancé) |
| F9 | `faultsCountAsMiss = false` | A 30/1 | `Fault(A, TRESPASS)` | A 30/1 → B (lancer perdu, compteur inchangé) |
| F10 ⚠ | IMO | tour de A ; B 40/0 | `WrongTeam(B, {5})` | les points ne comptent pas ; B **25**/0 (règle des 37), `B.pendingForfeit = 1` ; toujours **tour de A** |
| F11 ⚠ | suite de F10 | A lance `{3}` | A +3 ; tour de B → le moteur applique automatiquement le lancer perdu : B 25/**1**, le joueur de B suivant est consommé → tour de A |
| F12 ⚠ | Classique, même cas que F10 | B 40/0 | `WrongTeam(B, {5})` | B 40/0, pendingForfeit = 1 (pas de règle des 37) |

## 8. Undo et correction (`U`)

| ID | Journal avant | Action | Résultat |
|---|---|---|---|
| U1 | …, A `{9}` (45 → 25) | Undo | A 45/x, tour de A, journal sans le dernier lancer |
| U2 | …, A `{5}` (victoire) | Undo | status IN_PROGRESS, A 45, tour de A, classement vide |
| U3 | …, A `{}` (élimination) | Undo | A n'est plus éliminé, A x/2, tour de A |
| U4 | vide | Undo | Erreur `NOTHING_TO_UNDO`, état inchangé |
| U5 | e1, e2, e3 | Undo, Undo | état = `fold(initial, [e1])` |
| U6 ⚠ | …, `WrongTeam(B…)`, A `{3}` (+ lancer perdu automatique de B) | Undo | retire A `{3}` **et** le lancer perdu dérivé ; état = juste après F10 |
| U7 | e1…e5 | `Correct(2, Throw(A, {8}))` | rejeu de e1, e2′, e3…e5 ; scores recalculés en cascade |
| U8 | e1…e5 | `Correct(3, …)` qui ferait gagner la manche dès e3 (e4, e5 deviendraient des lancers après la fin) | Erreur `CORRECTION_INVALIDATES_LOG` ; journal inchangé |
| U9 | — | pour tout journal valide L : `fold(L + e) puis Undo == fold(L)` | propriété (test paramétré / property-based) |

## 9. Variantes (`V`)

| ID | Option | État | Lancer | Résultat |
|---|---|---|---|---|
| V1 | `overshoot = ResetTo(0)` | A 45 | `{9}` | A 0 |
| V2 | `overshoot = Stay` | A 45 | `{9}` | A 45, compteur de ratés remis à 0 |
| V3 | `overshoot = Bounce` | A 45 | `{9}` | A 46 (50 − 4) |
| V4 | `onMissLimit = Nothing` | A 30/2 | `{}` | A 30/3, pas éliminé ; `{}` → 30/4 (compteur conservé pour les stats) |
| V5 | `onMissLimit = ResetScore(0)` | A 30/2 | `{}` | A **0/0**, toujours en jeu |
| V6 | `onMissLimit = ResetScore(25)` | A 40/2 | `{}` | A 25/0 |
| V7 | `targetScore = 30`, `ResetTo(15)` | A 25 | `{6}` | A 15 |
| V8 | `missLimit = 4` | A 30/3 | `{}` | ✗A |
| V9 | `endCondition = PlayForPlaces`, A, B, C | A 45 | A `{5}` | A 1er, **la manche continue** entre B et C ; A est sauté |
| V10 | suite de V9 | C 48 | C `{1, 2}` | C 2e ; il ne reste que B → FINISHED, classement [A, C, B] |
| V11 | `PlayForPlaces`, A 1er, B, C | ✗B | — | FINISHED, classement [A, C, B] |
| V12 | `catchUpReset = true` | A 20, B 30 | A `{10}` | A 30, **B 25** |
| V13 | `catchUpReset = true` | A 20, B 30 | A `{12}` | A 32, B 30 (pas d'effet sans égalité exacte) |

## 10. Match en plusieurs manches (`M`)

`MatchConfig(setsToWin = 2, firstStarter, nextStarter: Alternate | LoserStarts | DecisiveByPoints, tieBreak: Molkkout | None)`.
« Classique » : `DecisiveByPoints` + `Molkkout` ; « Tournoi IMO » : `Alternate`.

| ID | Config | Manches jouées | Résultat attendu |
|---|---|---|---|
| M1 | setsToWin 2 | A gagne, A gagne | match FINISHED, vainqueur A, pas de 3e manche |
| M2 | `DecisiveByPoints` | M1 : A 50–B 32 ; M2 : A 41–B 50 | 3e manche, **A commence** (91 > 82) |
| M3 | `DecisiveByPoints` | M1 : A 50–B 40 ; M2 : A 40–B 50 | égalité 90–90 → `MolkkoutRequired` ; le vainqueur du Mölkkout commence |
| M4 | `Alternate`, firstStarter A | — | M1 : A, M2 : B, M3 : A |
| M5 | `LoserStarts` | M1 gagnée par A | M2 : B commence |
| M6 | 3 équipes, `Alternate` | — | la première équipe tourne : A, B, C, A… ; l'ordre relatif est conservé |
| M7 | preset IMO | M1 gagnée par élimination de B (A avait 30) | points de manche enregistrés : A 50, B 0 |
| M8 | — | composition / ordre des joueurs modifiés entre deux manches | accepté ; refusé pendant une manche (`LINEUP_LOCKED`) |

## 11. Mölkkout (`K`)

Quilles possibles : {4, 6, 8, 10, 12}. Remises en place après chaque lancer ; aucune règle de 50 ni
d'élimination. Lancers par équipe : `T = 3` (1 joueur), `4` (2 joueurs, 2 chacun), `n` (n ≥ 3, 1 chacun).
Séquence de `2T` lancers : l'équipe du lancer *k* (k = 0…2T−1) est **A** si `⌊(k+1)/2⌋` est pair, sinon **B**.

| ID | Cas | Résultat attendu |
|---|---|---|
| K1 | équipes de 1 | `A B B A A B` |
| K2 | équipes de 2 (A1, A2 / B1, B2) | `A B B A A B B A` ; les joueurs alternent dans leur équipe |
| K3 | équipes de 3 | `A B B A A B` |
| K4 | équipes de 4 | `A B B A A B B A` |
| K5 | lancer `{12}` / `{6, 4}` / `{}` | 12 / 2 / 0 |
| K6 | lancer `{7}` | Erreur `INVALID_PIN` (la 7 n'est pas sur le terrain) |
| K7 | totaux 20–14 après tous les lancers | vainqueur A |
| K8 ⚠ | totaux 18–18 | tour supplémentaire : chaque joueur relance une fois (même alternance) ; comparaison à la fin du tour, on répète tant qu'il y a égalité |

## 12. Validation (`X`)

| ID | Entrée | Résultat |
|---|---|---|
| X1 | `Throw(A, {13})` ou `{0}` | `INVALID_PIN` |
| X2 | quilles en double (liste `[4, 4]` dans la représentation brute) | `DUPLICATE_PIN` |
| X3 | création avec moins de 2 équipes | `NOT_ENOUGH_TEAMS` |
| X4 | équipe sans joueur | `EMPTY_TEAM` |
| X5 | même joueur dans deux équipes | `PLAYER_IN_MULTIPLE_TEAMS` |
| X6 | `targetScore < 1`, `ResetTo(n)` avec n ≥ target, `missLimit < 1` | `INVALID_CONFIG` |
| X7 | tout événement sur une manche FINISHED (sauf Undo / Correct) | `SET_FINISHED` |

## 13. Statut et éligibilité aux records (`S`)

> Ajouté le 2026-10-25 avec le Hall of Fame (voir `ARCHITECTURE.md` § 10). Ces drapeaux sont dérivés
> du journal effectif : ils ne sont jamais stockés.

| ID | Journal effectif | Résultat |
|---|---|---|
| S1 | …, A atteint 50 | `FINISHED`, `corrected = false`, éligible aux records |
| S2 | …, `Abandon` | `ABANDONED`, plus aucun lancer accepté (`SET_FINISHED`), non éligible |
| S3 | …, `Correct(k, …)`, …, victoire | `FINISHED`, `corrected = true`, non éligible, reste dans l'historique |
| S4 | …, lancer, `Undo`, …, victoire | `FINISHED`, `corrected = false` (un Undo n'est pas une correction) |
| S5 | `Abandon` sur une manche FINISHED | Erreur `SET_FINISHED` |
| S6 | `Abandon` puis `Undo` | la manche redevient `IN_PROGRESS` |
