package scripts

import java.util.*

fun main() {
    val v = Scanner(System.`in`).nextInt() // 1

    val out1: String
    if (v == 0) {
        out1 = "zero"
    } else if (v % 2 == 0) {
        out1 = "even"
    } else {
        out1 = "not even"
    }

    val out2: String
    when {
        v == 0 -> {
            out2 = "zero"
        }
        v % 2 == 0 -> {
            out2 = "even"
        }
        else -> {
            out2 = "not even"
        }
    }

    var i = 0
    while (i < 10) {
        i++
        println(i)
    }

    var j = 0
    for (k in 0..9) {
        j++
        println(j)
    }

    println("$out1\n$out2\n$i\n$j\n")
}
