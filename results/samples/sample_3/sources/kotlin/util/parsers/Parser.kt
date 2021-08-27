package util.parsers

import java.io.File

object Parser {

    fun parse(path: String) {
        val slices = mutableMapOf<String, MutableList<Int>>()
        for (line in File(path).readLines()) {
            val pair = line.split(":")
            val klass = ktClassToFileName(pair[0])
            val lineNumber = pair[1].toInt()
            if (lineNumber > 0) {
                if (slices.containsKey(klass)) {
                    slices[klass]!!.add(lineNumber)
                } else {
                    slices[klass] = mutableListOf(lineNumber)
                }
            }
        }
        println(slices)
    }

    private fun ktClassToFileName(ktClassName: String): String {
        val index = ktClassName.length - 2
        return if (ktClassName.substring(index) == "Kt") {
            ktClassName.substring(0, index)
        } else {
            ktClassName
        }
    }

}