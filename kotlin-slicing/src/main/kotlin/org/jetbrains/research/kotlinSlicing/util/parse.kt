package org.jetbrains.research.kotlinSlicing.util

import java.io.File
import java.nio.file.Path

fun parse(path: Path): MutableMap<String, MutableSet<Int>> {
    val slices = mutableMapOf<String, MutableSet<Int>>()
    for (line in File(path.toString()).readLines()) {
        val pair = line.split(":")
        val className = pair[0]
        val lineNumber = pair[1].toInt()
        if (lineNumber > 0) slices.getOrPut(className) { mutableSetOf() }.add(lineNumber)
    }
    return slices
}

fun unpackSlices(lineNumbers: Set<Int>, lines: List<String>): MutableMap<Int, String> { // debug
    val result = mutableMapOf<Int, String>()
    for (i in lines.indices) if (lineNumbers.contains(i + 1)) {
        result[i + 1] = lines[i]
    }
    return result
}
