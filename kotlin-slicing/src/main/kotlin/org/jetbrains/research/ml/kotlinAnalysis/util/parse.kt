package org.jetbrains.research.ml.kotlinAnalysis.util

import java.io.File
import java.nio.file.Path

fun parse(path: Path): MutableMap<String, MutableSet<Int>> {
    val slices = mutableMapOf<String, MutableSet<Int>>()
    for (line in File(path.toString()).readLines()) {
        val pair = line.split(":")
        val klass = ktClassToFileName(pair[0])
        val lineNumber = pair[1].toInt()
        if (lineNumber > 0) {
            slices.getOrPut(klass) { mutableSetOf() }.add(lineNumber)
        }
    }
    return slices
}

fun unpackSlices(lineNumbers: Set<Int>, lines: List<String>): MutableMap<Int, String> { // unnecessary
    val result = mutableMapOf<Int, String>()
    for (i in lines.indices) {
        if (lineNumbers.contains(i + 1)) {
            result[i + 1] = lines[i]
        }
    }
    return result
}

fun ktClassToFileName(className: String): String {
    // PsiFile.name doesn't contain package
    val fileName = className.replace(Regex("""([\w]+\.)*"""), "")
    return if (className.endsWith("Kt")) {
        fileName.substring(0, fileName.length - 2)
    } else {
        fileName
    }
}
