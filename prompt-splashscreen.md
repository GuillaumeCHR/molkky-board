---

## Écran de démarrage et éditeur « Yogi Studio »

### Éditeur
- L'éditeur de l'application est **Yogi Studio**. Les fichiers de marque sont dans `docs/brand/` :
  `yogi-studio-icon.svg` (logo carré) et `yogi-studio-logo.svg` (logo horizontal avec le nom).
  Convertis-les en VectorDrawable pour l'app (pas de PNG si le vectoriel suffit)
- « Yogi Studio » apparaît dans l'animation de démarrage, dans l'écran « À propos »
  (avec la version de l'app) et dans les métadonnées de publication
- Le logo Yogi Studio n'est **pas** l'icône de l'application : l'icône de **Molkky Board** reste à concevoir
  séparément, dans la direction artistique « Nordic Bold » (proposition attendue : une quille stylisée
  et un « MB » ou un tableau de score, lisible en 48dp)
- Logotype **Molkky Board** à créer pour l'intro et l'en-tête de l'accueil : « Molkky » en Bricolage Grotesque
  800 vert forêt, « Board » en safran, chiffres et lettres sur une même ligne de base

### Principe technique
- L'écran de démarrage système d'Android 12+ (API SplashScreen) est très limité en durée et en animation :
  il n'affiche que l'icône de l'app sur fond bouleau `#F4EBDD`, le temps strictement nécessaire
- Il enchaîne sans coupure visible (mêmes fond et position) sur un **écran d'intro Compose**
  qui joue l'animation complète
- **Chargement en tâche de fond** pendant l'animation : initialisation de Room, lecture du DataStore,
  préchargement des polices et des données de l'accueil, restauration de la session et
  synchronisation si un compte est connecté. Le chargement démarre dès `Application.onCreate`
  et ne dépend jamais de l'animation
- Passage au menu quand **l'animation ET le chargement** sont terminés :
  - si le chargement finit avant, l'animation va à son terme (pas d'attente supplémentaire) ;
  - s'il est plus long, le logo Yogi Studio reste affiché avec un indicateur discret ;
  - si le chargement échoue, le menu s'ouvre quand même en mode hors ligne, avec un message non bloquant
- Durée totale de l'animation : **4,5 s maximum**, passable d'un tap à tout moment
  (on saute alors à la transition vers le menu)
- À partir du deuxième lancement, proposer dans les réglages une version courte (environ 1,5 s :
  impact puis logo) ; l'animation complète reste la valeur par défaut
- « Réduire les animations » activé : simple fondu du logo Yogi Studio vers le menu (environ 1 s)

### Storyboard (≈ 4,5 s, entièrement en Canvas Compose, sans librairie d'animation externe)
1. **0 – 0,6 s — Mise en place** : les 12 quilles apparaissent dans leur formation de départ
   sur fond bouleau, avec un léger rebond (spring)
2. **0,6 – 1,2 s — Le lancer** : la quille de lancer (le mölkky) arrive du bas de l'écran
   en tournoyant, avec une traînée de mouvement
3. **1,2 – 2,6 s — L'impact** : les quilles volent en éclats. Chaque quille se fragmente en
   8 à 15 éclats de bois (polygones irréguliers dans les teintes bois de la palette),
   projetés avec une physique simple (vitesse initiale, rotation, gravité, rebond amorti au sol).
   Petit tremblement de l'écran et vibration forte au moment de l'impact. Les numéros des
   quilles s'envolent avec les éclats
4. **2,6 – 3,6 s — La signature** : pendant que les éclats retombent et s'estompent,
   le logo Yogi Studio apparaît au centre (le cercle safran se trace, puis le personnage
   apparaît avec un rebond), accompagné de la mention « Yogi Studio présente »
5. **3,6 – 4,5 s — Le titre et l'ouverture** : le logo Yogi Studio s'efface, et les derniers éclats
   de bois s'assemblent en vol pour former le logotype **« Molkky Board »** au centre. Après un court
   temps de pause, le logotype glisse et se réduit vers l'en-tête de l'accueil pendant que le menu
   apparaît derrière (transition à éléments partagés : c'est le même logotype qui sert de titre à l'accueil)

### Contraintes
- 60 images/s visées sur une machine modeste : nombre de particules borné (≈ 150 au total),
  aucune allocation dans la boucle de dessin, calcul des trajectoires précalculé ou en arithmétique simple
- Résultat déterministe (graine aléatoire fixe) pour que les tests de capture Roborazzi soient stables
- Fonctionne en mode clair et sombre (fond `#14201A` en sombre)
- Son d'impact optionnel, désactivé par défaut, respectant le mode silencieux du téléphone
- Test UI : vérifier qu'un tap passe bien l'animation et que le menu s'affiche même si le chargement échoue
- Montre-moi une capture de chacune des 5 étapes (via `android screen capture` ou Roborazzi)
  avant de finaliser les réglages de timing
