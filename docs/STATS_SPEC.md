# Spécification des statistiques et du Hall of Fame (`core:stats`)

> Contrat du module `core:stats` (Kotlin pur). Chaque ligne d'un tableau est un test de
> `core/stats/src/test/.../StatsTest.kt`, sous le même identifiant. Rien n'est stocké : tout est recalculé
> à partir des journaux des manches (`ARCHITECTURE.md` § 10), pour qu'un nouveau classement s'applique
> rétroactivement à tout l'historique.

## 1. Modèle

- **Entrée** : `StatsGame` (identifiant, date, `GameConfig`, équipes avec leur `memberKey`, manches avec
  leur composition et leur `ReplayResult`, drapeau « abandonnée »).
- **`summarize(game)`** → un `SetSummary` par manche : équipes (identité, nom, joueurs, score final,
  lancers), vainqueur, éligibilité, règles officielles ou non, lignes de chaque joueur.
- **Identité** : une équipe = son `memberKey` (empreinte de ses joueurs). Un joueur seul = une équipe d'un
  joueur. Le nom affiché est **le plus récent**.
- **Éligible** (compte pour les victoires) : manche `FINISHED`, partie non abandonnée, pas de modification
  après la fin (l'annulation immédiate du lancer gagnant est tolérée).
- **Officielle** (compte pour les records « purs ») : `GameConfig` égal au préréglage Officiel ou au
  règlement IMO, sans surcharge par équipe (mode famille).
- **Lancers d'une équipe** : lancers, fautes et lancers hors tour qui lui sont attribués (attribution par
  position). Les lancers perdus automatiquement ne sont pas des événements et ne comptent pas.
- **Ligne d'un joueur** : lancers (fautes exclues), ratés, points bruts, quilles touchées seules (mode
  « Quilles » seulement), plus longue série sans raté.

## 2. Résumé d'une manche (`R`)

| # | Cas | Attendu |
|---|---|---|
| R1 | une faute puis 5 lancers gagnants | l'équipe gagnante compte 6 lancers ; le joueur 5 |
| R2 | manche en cours | pas de vainqueur, non éligible |
| R3 | abandon · correction après la fin · partie marquée abandonnée | non éligible |
| R4 | annulation immédiate du lancer gagnant, puis nouveau lancer gagnant | éligible |
| R5 | raté, 7, deux quilles, 9 | 4 lancers, 1 raté, 18 points, quilles seules {7, 9}, série 3 |
| R6 | saisie rapide « 7, quille seule » | compte dans les lancers et les points, pas dans les quilles seules |

## 3. Victoires (`W`)

Tri : victoires ↓, puis **date de la victoire qui a donné ce total** ↑ (le premier à l'avoir atteint garde
le rang), puis identité. « Parties jouées » = manches éligibles disputées ; taux = victoires / jouées.

| # | Cas | Attendu |
|---|---|---|
| W1 | A gagne 2 fois, B 1 fois sur 3 manches | A (2, 3 jouées, 67 %, lien vers la dernière victoire) puis B |
| W2 | 1 victoire chacun, B la première | B devant A |
| W3 | manches corrigées après la fin ou abandonnées | aucune ligne |
| W4 | mode Ludique | compte dans les victoires |
| W5 | une partie solo, une partie à deux contre un | Joueurs : la partie solo ; Équipes : l'autre ; filtre de taille appliqué aux équipes |
| W6 | période « depuis » | seules les manches de la période |
| W7 | équipe renommée | le nom le plus récent |

## 4. Victoire la plus rapide (`F`)

Manches éligibles **aux règles officielles** seulement ; une ligne par équipe (sa meilleure). Tri : lancers ↑,
date ↑, identifiant de partie ↑. **5 lancers** (4 × 12 + 2) = « partie parfaite ».

| # | Cas | Attendu |
|---|---|---|
| F1 | 12, 12, 12, 12, 2 | 5 lancers, partie parfaite |
| F2 | A en 9 puis en 5, B en 9 | A (5) puis B (9) |
| F3 | deux records en 5 lancers | le plus ancien d'abord ; même date → identifiant de partie |
| F4 | mode Ludique, mode famille (handicap) | aucun record ; les victoires comptent |
| F5 | règlement de tournoi IMO | compte comme officiel |

## 5. « 🏆 Nouveau record ! » (`N`)

Annoncé quand la manche devient la victoire la plus rapide de son onglet **alors qu'un record existait**.

| # | Cas | Attendu |
|---|---|---|
| N1 | record en 9, nouvelle victoire en 5 | oui |
| N2 | toute première victoire · égalité du record | non |
| N3 | manche en mode Ludique | non |

## 6. Statistiques d'un joueur (`P`)

Toutes ses manches (solo, équipes, Ludique) ; victoires = manches éligibles gagnées par son équipe ;
victoire la plus rapide = règles officielles seulement.

| # | Cas | Attendu |
|---|---|---|
| P1 | une victoire parfaite, une défaite à 4 × 1 | 2 manches, 1 victoire, 9 lancers, 54 points, moyenne 6, 0 % de ratés, progression [10 ; 1], record 5, série 5 |
| P2 | aucun lancer | moyenne et taux à 0, pas de record |

## 7. Série (`Z`)

Une série = les manches **terminées** consécutives jouées par exactement les mêmes équipes (revanches et
manches d'un match), jusqu'à la manche affichée. Pas de série pour une seule manche. Lignes triées par
victoires ↓ puis total de points ↓.

| # | Cas | Attendu |
|---|---|---|
| Z1 | A, B, A gagnent | 3 manches ; A 2 victoires, scores [50, 8, 50] |
| Z2 | une manche à trois équipes au milieu | la série repart après elle |
| Z3 | une seule manche · manche inconnue | pas de série |
| Z4 | 1 victoire chacun | le total de points départage |

## 8. Moments partageables (`M`, J6)

Détectés depuis le journal effectif d'une manche, classés par force : record (100), partie parfaite (90),
remontée (70), strike des 12 quilles (60), victoire (50), « 12 ! » quille seule (40), retour depuis 49 (30),
élimination (20). L'écran de fin propose le plus fort ; les autres restent accessibles d'un glissement.

| # | Cas | Attendu |
|---|---|---|
| M1 | 12, 12, 12, 12, 2 avec record | record, parfaite, victoire (5 lancers, tour 5), quatre « 12 » |
| M2 | les 12 quilles d'un coup | un « 12 » strike, plus fort qu'une victoire |
| M3 | à 49, un 12 fait dépasser | pas de « 12 » pour ce lancer ; un « retour depuis 49 » |
| M4 | mené de 35 points puis victoire | remontée (retard ≥ 15) |
| M5 | trois ratés | élimination, puis victoire du dernier en lice |
| M6 | mode Ludique | pas de partie parfaite, la victoire reste |
| M7 | manche inconnue | aucun moment |
