fun Complex(s: String) : Complex {
    val ss = s.substring(0, s.length - 1)
    val list = ss.split(Regex("""[+\- ]"""))
    val symbol = ss.replace(Regex("""[\d.i]+"""), "")
    var real = 0.0
    var imaginate = 0.0
    if (symbol.length == 1) {
        real = list[0].toDouble()
        imaginate = if (symbol == "+") list[1].toDouble() else list[1].toDouble() * -1
    }
    return Complex(real, imaginate)
}

class Complex(val re: Double, val im: Double) {

    /**
     * Конструктор из строки вида x+yi
     */
    //constructor(s: String) : this(re = abc(s)[0], im = abc(s)[1])

    /**
     * Сложение.
     */
    operator fun plus(other: Complex): Complex = Complex(re + other.re, im + other.im)

    /**
     * Смена знака (у обеих частей числа)
     */
    operator fun unaryMinus(): Complex = Complex(-re, -im)

    }