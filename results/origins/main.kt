package scripts

fun main() {
    var a = intArrayOf(0, 1)
    a = addAndReturn(a)
    add(a)
    println(a.contentToString())
}

fun add(a: IntArray) {
    a[0] += 1
}

fun addAndReturn(a: IntArray): IntArray {
    a[0] += 1
    return a
}