package scripts

class SomeClass {

    constructor(a: Int) {
        this.a = if (a % 2 == 0) {
            a * 2
        } else {
            a
        }
    }

    }
