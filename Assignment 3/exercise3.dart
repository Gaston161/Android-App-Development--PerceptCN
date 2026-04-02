//  Data class equivalent 
class Person {
  final String name;
  final int age;

  const Person(this.name, this.age);

  @override
  String toString() => "Person(name: $name, age: $age)";
}

void main() {
  final people = [
    Person("KEDE",   25),
    Person("MPODE",     30),
    Person("BISSIE", 35),
    Person("JANE",    22),
    Person("NGAH",     28),
  ];

  // Step 1 – Filter: keep people whose name starts with 'A' or 'B'
  final filtered = people
      .where((p) => p.name.startsWith("A") || p.name.startsWith("B"))
      .toList();

  print("=== Filtered people ===");
  for (var p in filtered) {
    print("  ${p.name} (${p.age})");
  }

  // Step 2 – Extract ages
  final ages = filtered.map((p) => p.age).toList();
  print("\nAges : $ages");

  // Step 3 – Calculate average using reduce
  final sum     = ages.reduce((a, b) => a + b);
  final average = sum / ages.length;

  // Step 4 – Print rounded to 1 decimal place
  print("\nAverage age of people whose name starts with A or B:");
  print("  → ${average.toStringAsFixed(1)}");

  //  show each person's contribution 
  print("\n=== Breakdown ===");
  for (var p in filtered) {
    print("  ${p.name.padRight(8)} | age ${p.age}");
  }
  print("  ${"Average".padRight(8)} | ${average.toStringAsFixed(1)}");
}
