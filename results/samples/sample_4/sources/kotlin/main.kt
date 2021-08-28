fun main() {
    val a = Complex(2.2, 2.1)
    val b = -Complex("3.2+1.1i")
    val c = a + b

    val pb = PhoneBook()
    val artyom = "Artyom"
    val number1 = "+7 978 888 88 88"
    pb.addHuman(artyom)
    pb.addPhone(artyom, number1)
    pb.addPhone(artyom, "+7 981 111 11 11")

    val alexander = "Alexander"
    pb.addHuman(alexander)
    pb.addPhone(alexander, number1)

    pb.removePhone(artyom, number1)
    pb.addPhone(alexander, number1)

    println("$c\n${pb.humanByPhone(number1)}")
}