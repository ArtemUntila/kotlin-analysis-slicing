package scripts

fun main() {
    var a = intArrayOf(0, 1)
    add(a)
    println(a.contentToString())
}

fun add(a: IntArray) {
    a[0] += 1
}


