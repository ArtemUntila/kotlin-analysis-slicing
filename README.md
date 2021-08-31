# Kotlin-Analysis-Slicing

[![build](https://github.com/Artyom-IWT/kotlin-analysis-slicing/actions/workflows/build.yml/badge.svg)](https://github.com/Artyom-IWT/kotlin-analysis-slicing/actions/workflows/build.yml)

This project represents "hybrid" slicer for programs written in Kotlin, 
that generates sliced and compilable .kt files.

The project works around following projects:
- [Slicer4J](https://github.com/resess/Slicer4J) — Dynamic slicing tool for Java.
- [Kotlin-Analysis](https://github.com/JetBrains-Research/Kotlin-Analysis) — Getting the PSI of the input project.
- [SourceDecompiler](https://github.com/Artyom-IWT/SourceDecompiler) — Getting .kt file names from the compiled .class files.

The project requires 3 input values:
1. Path to the input project.
2. Path to the output directory.
3. Path to the `slice.log` file processed by [SourceDecompiler](https://github.com/Artyom-IWT/SourceDecompiler).

The project depends on [Slicer4J](https://github.com/resess/Slicer4J) as a dynamic slicer and contains own static analyzer, that includes statements required 
for the compilability of the sliced .kt files. All internal analysis is performed over the PSI of the input project.

The project consists of several modules:
- `kotlin-analysis-plugin` — plugin to run the required functionality from the CLI.
- `kotlin-analysis-core` — functions to get the PSI of the input project.
- `kotlin-slicing` — functionality related to the program slicing.

## Example

Source code:
```kotlin
1.  fun main() {
2.      val x = readLine()!!.toInt() // 1
3.      val y: Int
4.      val z: Int
5.      if (x > 0) {
6.          y = x + 1
7.          z = x - 1
8.      } else if (x == 0) {
9.          y = x + 2
10.         z = x - 2
11.     } else {
12.         y = x + 3
13.         z = x - 3
14.     }
15.     println(y)
16.     println(z)
17. }
```

Running [Slicer4J](https://github.com/resess/Slicer4J) w.r.t line 15.

`slice.log` created by Slicer4J:
```
MainKt:15
MainKt:2
MainKt:5
MainKt:6
```

`slice.log` processed by [SourceDecompiler](https://github.com/Artyom-IWT/SourceDecompiler):
```
main.kt:15
main.kt:2
main.kt:5
main.kt:6
```

If we write lines, directly included in `slice.log`, and lines with the elements, containing statements in included 
lines, we will have the following code:

```kotlin
fun main() {
    val x = readLine()!!.toInt() // 1
    if (x > 0) {
        y = x + 1
    }
    println(y)
}
```

The code above will not be compilable, and besides including the declaration of the variable `y` we also have to include 
all assignments to this variable, because it must be initialized. And this is the reason why we use static analyzer.

The final sliced and compilable program after static analysis:

```kotlin
fun main() {
    val x = readLine()!!.toInt() // 1
    val y: Int
    if (x > 0) {
        y = x + 1
    } else if (x == 0) {
        y = x + 2
    } else {
        y = x + 3
    }
    println(y)
}
```

More samples: [results/samples](https://github.com/Artyom-IWT/kotlin-analysis-slicing/tree/main/results/samples).

Learn more about the internal structure and analysis: [kotlin-slicing](https://github.com/Artyom-IWT/kotlin-analysis-slicing/tree/main/kotlin-slicing)