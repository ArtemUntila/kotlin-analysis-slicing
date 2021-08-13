package scripts

import java.util.*

fun main() {
    val v = Scanner(System.`in`).nextInt()

    if (v == 0) {
        } else if (v % 2 == 0) {
        } else {
        out1 = "not even"
    }

    when {
        v % 2 == 0 -> {
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

