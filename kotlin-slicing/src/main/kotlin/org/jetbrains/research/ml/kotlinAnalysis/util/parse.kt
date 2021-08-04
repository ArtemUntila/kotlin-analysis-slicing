package org.jetbrains.research.ml.kotlinAnalysis.util

import java.io.File

fun parse(path: String): MutableMap<String, MutableList<Int>> {
    val slices = mutableMapOf<String, MutableList<Int>>()
    for (line in File(path).readLines()) {
        val pair = line.split(":")
        val klass = ktClassToFileName(pair[0])
        val lineNumber = pair[1].toInt()
        if (lineNumber > 0) {
            slices.getOrPut(klass) { mutableListOf() }.add(lineNumber)
        }
    }
    return slices
}

fun unpackSlices(lineNumbers: List<Int>, lines: List<String>): MutableMap<Int, String> { // unnecessary
    val result = mutableMapOf<Int, String>()
    for (i in lines.indices) {
        if (lineNumbers.contains(i + 1)) {
            result[i + 1] = lines[i].trim() // ".trim()" will be removed
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
