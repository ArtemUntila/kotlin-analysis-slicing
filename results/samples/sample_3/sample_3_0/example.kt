package scripts

import java.util.*

fun main() {
    val v = Scanner(System.`in`).nextInt()

    if (v == 0) {
        out1 = "zero"
    }

    when {
        v == 0 -> {
            out2 = "zero"
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

