
/// processList – filters a list of integers using a function predicate.

/// [numbers]   The list of integers to process.
/// [predicate] A function that returns true for elements to keep.
/// Returns a new list containing only the matching elements.
List<int> processList(List<int> numbers, bool Function(int) predicate) {
  final result = <int>[];
  for (var n in numbers) {
    if (predicate(n)) result.add(n);
  }
  return result;
}

void main() {
  final nums = [1, 2, 3, 4, 5, 6];

  // Test 1: keep even numbers ---
  final even = processList(nums, (n) => n % 2 == 0);
  print("Even numbers   : $even");          // [2, 4, 6]

  //  Test 2: keep numbers greater than 3 ---
  final greaterThan3 = processList(nums, (n) => n > 3);
  print("Greater than 3 : $greaterThan3");  // [4, 5, 6]

  //  Test 3: keep odd numbers ---
  final odd = processList(nums, (n) => n % 2 != 0);
  print("Odd numbers    : $odd");           // [1, 3, 5]
}
