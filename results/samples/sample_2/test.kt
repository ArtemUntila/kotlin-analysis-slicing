package scripts

import java.util.*

fun main() {
    val input = Scanner(System.`in`).nextInt()

    val output1 = if (input == 0) {
        "zero"
    } else if (input % 2 == 0) {
        "even"
    } else {
        }

    val output2 = when {
        input % 2 == 0 -> {
            "even"
        }
        else -> {
            }
    }

    val array = intArrayOf(1, 2, 3)
    val filter = array.map {
        it + 1
    }
    println("$output1\n$output2\n$filter")
}
