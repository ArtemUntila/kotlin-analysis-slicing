package scripts

class SomeClass {

    val a: Int

    constructor(a: Int) {
        this.a = if (a % 2 == 0) {
            a * 2
        } else {
            a
        }
    }

    override fun toString(): String {
        return a.toString()
    }
}