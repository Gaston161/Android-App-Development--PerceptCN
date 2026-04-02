Exercice 2 : Transformation entre types de collections
SE 3242 : Android Application Development

 Objectif :
À partir d'une liste de chaînes de caractères (strings), créez une Map (clé : mot → valeur : longueur), puis affichez uniquement les entrées dont la longueur du mot est supérieure à 4.

Résultat attendu :
apple a une longueur de 5
banana a une longueur de 6
elephant a une longueur de 8

Transformation de Liste vers Map

Langage	Syntaxe
Kotlin	list.associateWith { it.length }
Dart	{ for (var w in list) w: w.length }
La fonction associateWith de Kotlin utilise chaque élément comme clé et le résultat de la lambda comme valeur. Dart utilise un littéral de Map avec une boucle for, ce qui est la méthode idiomatique recommandée.

Filtrage d'une Map
// Kotlin – Décomposition (destructuring) dans le paramètre lambda
wordLengths.filter { (_, length) -> length > 4 }
// Dart – Accès via .entries, puis .where()
wordLengths.entries.where((entry) => entry.value > 4)

En Kotlin, le caractère _ est utilisé pour ignorer la clé lors de la décomposition d'une entrée (Map.Entry). En Dart, vous devez toujours nommer le paramètre, mais les propriétés entry.key et entry.value rendent le code explicite.
Itération sur une Map
Langage	Syntaxe d'itération
Kotlin	.forEach { (key, value) -> ... } (décomposition)
Dart	.forEach((key, value) { ... }) ou .entries.forEach(...)

Bonus : Trier par valeur
// Kotlin
wordLengths.entries
    .sortedByDescending { it.value }
    .forEach { (word, len) -> println("$word -> $len") }
// Dart
final sorted = wordLengths.entries.toList()
  ..sort((a, b) => b.value.compareTo(a.value));
L'opérateur .. en Dart est l'opérateur de cascade. Il appelle .sort() sur la liste et retourne la liste elle-même (contrairement à .sort() seul qui retourne void).
