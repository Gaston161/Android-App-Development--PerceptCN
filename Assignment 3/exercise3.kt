//  Data class 
data class Person(val name: String, val age: Int)

fun main() {
    val people = listOf(
        Person("KEDE",   25),
        Person("MPODE",     30),
        Person("CLARKSON", 35),
        Person("BISSIE",    22),
        Person("JANE",     28)
    )

    // Step 1 – Filter: keep people whose name starts with 'A' or 'B'
    val filtered = people.filter { person ->
        person.name.startsWith("A") || person.name.startsWith("B")
    }

    println("=== Filtered people ===")
    filtered.forEach { println("  ${it.name} (${it.age})") }

    // Step 2 – Extract ages
    val ages = filtered.map { it.age }
    println("\nAges : $ages")

    // Step 3 – Calculate average
    val average = ages.average()

    // Step 4 – Print rounded to 1 decimal place
    println("\nAverage age of people whose name starts with A or B:")
    println("  → ${"%.1f".format(average)}")

    // --- Bonus: show each person's contribution ---
    println("\n=== Breakdown ===")
    filtered.forEach { p ->
        println("  ${p.name.padEnd(8)} | age ${p.age}")
    }
    println("  ${"Average".padEnd(8)} | ${"%.1f".format(average)}")
}
