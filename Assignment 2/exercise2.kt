fun main() {
    val words = listOf("apple", "cat", "banana", "dog", "elephant")

    // Step 1 – Build a Map<String, Int> : word -> its length
    val wordLengths: Map<String, Int> = words.associateWith { it.length }

    println("=== Full map ===")
    wordLengths.forEach { (word, length) ->
        println("  $word -> $length")
    }

    // Step 2 – Filter entries whose length > 4 and display them
    println("\n=== Words with length > 4 ===")
    wordLengths
        .filter  { (_, length) -> length > 4 }
        .forEach { (word, length) -> println("$word has length $length") }

    // Step 3 – Bonus: sort by length descending
    println("\n=== Sorted by length (desc) ===")
    wordLengths
        .entries
        .sortedByDescending { it.value }
        .forEach { (word, length) -> println("$word -> $length") }
}
