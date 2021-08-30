fun main() {
    val x = readLine()!!.toInt()
    val zero = "zero"

    val evenOrNot = if (x % 2 == 0) {
        "even"
    } else {
        "not even"
    }

    val result: String
    if (x != 0) {
        result = evenOrNot
    } else {
        result = zero
    }

    println(result)
}