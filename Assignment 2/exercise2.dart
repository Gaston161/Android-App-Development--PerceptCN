void main() {
  final words = ["apple", "cat", "banana", "dog", "elephant"];

  // Step 1 – Build a Map<String, int> : word -> its length
  final Map<String, int> wordLengths = {
    for (var w in words) w: w.length
  };

  print("=== Full map ===");
  wordLengths.forEach((word, length) {
    print("  $word -> $length");
  });

  // Step 2 – Filter entries whose length > 4 and display them
  print("\n=== Words with length > 4 ===");
  wordLengths.entries
      .where((entry) => entry.value > 4)
      .forEach((entry) => print("${entry.key} has length ${entry.value}"));

  // Step 3 sort by length descending
  print("\n=== Sorted by length (desc) ===");
  final sorted = wordLengths.entries.toList()
    ..sort((a, b) => b.value.compareTo(a.value));
  for (var entry in sorted) {
    print("${entry.key} -> ${entry.value}");
  }
}
