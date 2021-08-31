fun main() {
    val x = readLine()!!.toInt()
    val y: Int
    val z: Int
    if (x > 0) {
        y = 1
        z = 1
    } else if (x == 0) {
        y = x + 2
        z = x - 2
    } else {
        y = x + 3
        z = x - 3
    }
    println(y)
}
