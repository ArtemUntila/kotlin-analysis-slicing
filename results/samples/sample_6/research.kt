package util

fun main() {
    val v = 1

    var out1: String
    if (v == 0) {
        out1 = "zero"
    } else if (v % 2 == 0) {
        out1 = "even"
    } else {
        out1 = "not even"
    }

    val out2 = if (v == 0) {
        0
    } else if (v % 2 == 0) {
        2
    } else {
        1
    }

    val out3 = sum(
        1, 2, 3,
        4, 5, 6
    )

    println("$out1\n$out2\n$out3")
}

fun sum(vararg args: Int) =
    args.sum()
