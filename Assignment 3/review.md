Exercice 3 : Traitement de données complexes
SE 3242 : Android Application Development

Objectif : 
Manipuler une liste d'objets Person. Calculer l'âge moyen des personnes dont le nom commence par 'A' ou 'B', puis afficher le résultat arrondi à 1 décimale.
Résultat attendu :

=== Personnes filtrées ===
  KEDE (25)
  MPODE (30)
  BISSIE (22)
  JANE (28)

Âges : [25, 30, 22, 28]

Âge moyen des personnes dont le nom commence par A ou B :
  → 26.3
Calcul : (25 + 30 + 22 + 28) / 4 = 105 / 4 = 26.25 → arrondi à 26.3
Data Class vs Classe Standard
Fonctionnalité	Kotlin data class	Dart class
toString()	Généré automatiquement	Doit être surchargé manuellement
equals() / ==	Comparaison par valeur automatique	Comparaison par référence par défaut
copy()	Nativement intégré	Doit être implémenté manuellement
Immuabilité	Encouragée via val	Obtenue via final
La data class de Kotlin est plus puissante dès l'instanciation. En Dart, vous définissez une classe classique et surchargez les méthodes nécessaires selon vos besoins.
Pipeline : Filtrage → Transformation → Agrégation
people (List<Person>)
   │
   ▼  filter { le nom commence par A ou B }
filtered (List<Person>)
   │
   ▼  map { extraction de l'âge }
ages (List<Int/int>)
   │
   ▼  average() / somme + division
moyenne (Double/double)
Calcul de la moyenne
Langage	Méthode
Kotlin	.average() — Intégré sur les Iterable<Number>
Dart	ages.reduce((a, b) => a + b) / ages.length

La bibliothèque standard de Kotlin est plus riche sur ce point. En Dart, il n'y a pas de méthode .average() native, on effectue donc la somme avec reduce avant de diviser manuellement.
Formatage des décimales
Langage	Syntaxe	Résultat
Kotlin	"%.1f".format(average)	"26.3"
Dart	average.toStringAsFixed(1)	"26.3"

**Erreurs courantes à éviter:

Utiliser filter sur une liste Dart	Dart utilise .where(), pas .filter().
Division entière pour la moyenne	Assurez-vous qu'au moins un opérande est un double. En Dart, / retourne toujours un double.
Sensibilité à la casse (startsWith)	"alice".startsWith("A") retourne false. Utilisez .toLowerCase() si nécessaire.
Calculer la moyenne d'une liste vide	Vérifiez toujours if (ages.isEmpty) avant le calcul pour éviter des erreurs de division par zéro.

Version sécurisée (gestion de liste vide)
// Kotlin
val average = if (ages.isEmpty()) 0.0 else ages.average()
// Dart
final average = ages.isEmpty ? 0.0 : ages.reduce((a, b) => a + b) / ages.length;

