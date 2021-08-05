package scripts

fun main() {
    var a = intArrayOf(0, 1)
    a = addAndReturn(a)
    println(a.contentToString())
}

fun addAndReturn(a: IntArray): IntArray {
    a[0] += 1
    return a
}
