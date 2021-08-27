package util

class Person(val name: String, var age: Int, var occupation: String) {

    var index = 0

    fun mature() {
        mature(1)
    }

    fun mature(years: Int) {
        age += years
    }

    fun changeOccupationTo(newOccupation: String) {
        occupation = newOccupation
        index++
    }

    fun introduce() {
        var output = "My name is $name. I am $age years old"
        if (occupation.isNotEmpty()) {
            output += " $occupation"
        }
        println("$output.")
    }

    override fun toString(): String {
        val str = "$name ${Char(8212)} $age years old"
        return if (occupation.isNotEmpty()) {
            "$str $occupation"
        } else {
            str
        }
    }

}