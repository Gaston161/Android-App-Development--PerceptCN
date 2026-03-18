Exercice 1  : Fonctions d'Ordre Supérieur
SE 3242  Android Application Development

 Objectif
L'objectif est de rédiger une fonction nommée processList. Celle-ci doit accepter une liste d'entiers ainsi qu'une lambda (ou fonction passée en paramètre), afin de retourner une nouvelle liste filtrée selon le prédicat fourni
•	Signature de la fonction : Utilisation correcte d'un type fonctionnel comme paramètre.
o	En Kotlin : (Int) -> Boolean
o	En Dart : bool Function(int)
•	Logique interne : Structure claire basée sur l'itération, le test de condition et la collecte des résultats, garantissant une maintenance aisée.
•	Validation : Couverture de plusieurs cas d'usage (nombres pairs, impairs et filtrage par seuil).

Fonction d'ordre supérieur
Il s'agit d'une fonction qui reçoit une autre fonction en paramètre (ou en retourne une). C'est l'un des piliers de la programmation fonctionnelle.
Langage	Syntaxe du type fonctionnel
Kotlin	(Int) -> Boolean
Dart	bool Function(int)
Lambda / Fonction anonyme
Une fonction sans nom, définie directement au moment de l'appel.
// Kotlin – utilise 'it' pour un paramètre unique
processList(nums) { it % 2 == 0 }

// Dart – nom de paramètre explicite requis
processList(nums, (n) => n % 2 == 0);
Collections mutables vs immuables
•	Kotlin : On utilise mutableListOf() pour la construction, puis listOf() pour une version en lecture seule.
•	Dart : Une liste déclarée via <int>[] est mutable par défaut.
 Alternative : Utilisation de la bibliothèque standard
Les deux langages possèdent déjà des méthodes natives (filter ou where). 

L'exercice consiste à réimplémenter cette logique pour comprendre le fonctionnement interne des itérateurs.
Exemple avec les fonctions natives :
// Kotlin – Raccourci intégré
val even = nums.filter { it % 2 == 0 }
// Dart – Raccourci intégré
final even = nums.where((n) => n % 2 == 0).toList();

