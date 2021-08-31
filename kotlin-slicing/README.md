# kotlin-slicing

This module provides functionality related to the whole PSI-analysis, static analysis and formatting the output sliced
.kt files.

## `SlicingExecutor`

Module entry point.

- Extracts .kt files from the input project.
- If this file's name is contained in `slice.log` processed by [SourceDecompiler](https://github.com/Artyom-IWT/SourceDecompiler), 
executes `SliceFormatter` to get the sliced PSI of this file.
- Writes output .kt file.

## `SliceFormatter`

Main Slicer. 

- Executes `KtStaticAnalyzer`.
- Visits PSI of the .kt file — analyzing elements.
  - If element contains slice element (class, fun, etc) or is slice element — going further.
  - Else — adding this element to the elements-to-delete.
- Deletes unused elements.
- Returns sliced PSI.

## `KtStaticAnalyzer`

Analyses PSI and includes line numbers with the elements required for the compilability of the output sliced .kt file.

The main analysis-criteria are variables (properties), that were not included in `slice.log`, but are used.

There are 3 main cases:

1. Property was interpreted as a soot-constant: 
```kotlin
val i = 1
val s = "String"
```

2. Property initializer is `if` or `when` expression (property declaration is included in `slice.log`):
```kotlin
val a = if (x % 2 == 0) {
    "even"
} else {
    "not even"
}
```

Here we have to include all possible assigned values.

3. Property has not initializer in declaration:

```kotlin
val a: String
if (x % 2 == 0) {
    a = "even"
} else {
    a = "not even"
}
```

Here we also have to include all assignments to the variable `a`.

### Processing elements

#### Getting element data-dependencies
- Find all name references.
- Resolve name references: properties, functions, classes.
- Process properties separately.
- Process other elements from data-dependencies in the same way.

#### Getting element control-dependencies
- Find all parents with types: `if`, `when`, `for`, `while`/`do while`.
- Get condition-expressions of these parents.
- Process these expressions.

### Processing properties

- Property line number is included in `slice.log`:
  - If property initializer is `if` or `when` expression:
    - `if` expression — process last statement in body.
    - `when` expression — process last statement in each `when entry` body.
  - Else — skip.
  
```kotlin
val a = if (x == 0) {
    println(0)
    "zero" // last statement
} else if (x % 2 == 0) { // process this if statement in the same way
    "even" // last statement
} else "not even" // last statement
```

- Property line number is not included in `slice.log`:
  - Property is used in elements, which line numbers are included in`slice.log` or which are included 
  during static analysis:
    - Property has initializer — process initializer.
    - Property has not initializer — find assignments to this variable and process them.
  - Property is not used — skip.

```kotlin
val zero = "zero" // property with initializer
val result: String // property without initializer — must be initialized
if (x != 0) { 
    result = "not zero" // initializing assignment
} else { 
    result = zero // initializing assignment
}
```

# Examples

If you want to look at the examples of this module's work: [results/samples](https://github.com/Artyom-IWT/kotlin-analysis-slicing/tree/main/results/samples)

You can learn more about the module's internal work using debug files from samples:
- `slicing_info.txt` — `SlicingExecutor`
- `_debug.txt` — `SliceFormatter`.
- `_static_debug.txt` — `StaticAnalyzer`.
