fun main(){
    val x = readLine()!!.toInt() // 1 !!!
    val y: Int
    val z: Int
    val w: Int
    if (x > 0) { // !!!
        y = 1
        z = 1 // !!!
        w = 1
    } else if (x == 0) {
        y = x + 2
        z = x - 2
        w = x * 2
    } else {
        y = x + 3
        z = x - 3
        w = x * 3
    }

    val t = if (z == 1) { // !!!
            "z"
        } else if (w == 1) {
            str(y)
        } else "a"

    println(t) // !!!
    str(y)
}

fun str(i: Int) =
    "$i"