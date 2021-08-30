fun Complex(s: String) : Complex {
    val ss = s.substring(0, s.length - 1)
    val list = ss.split(Regex("""[+\- ]"""))
    val symbol = ss.replace(Regex("""[\d.i]+"""), "")
    var real = 0.0
    var imaginate = 0.0
    if (symbol.length == 1) {
        real = list[0].toDouble()
        imaginate = if (symbol == "+") list[1].toDouble() else list[1].toDouble() * -1
    } else {
        real = list[1].toDouble() * -1
        imaginate = if (symbol == "-+") list[2].toDouble() else list[2].toDouble() * -1
    }
    return Complex(real, imaginate)
}

class Complex(val re: Double, val im: Double) {
    /**
     * Конструктор из вещественного числа
     */
    constructor(x: Double) : this(re = x, im = 0.0)

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

    /**
     * Вычитание
     */
    operator fun minus(other: Complex): Complex = Complex(re - other.re, im - other.im)

    /**
     * Умножение
     */
    operator fun times(other: Complex): Complex =
        Complex(re * other.re + im * other.im * -1, re * other.im + im * other.re)

    /**
     * Деление
     */
    operator fun div(other: Complex): Complex {
        val real = (re * other.re + im * other.im) / (other.re * other.re + other.im * other.im)
        val imaginate = (im * other.re - re * other.im) / (other.re * other.re + other.im * other.im)
        return Complex(real, imaginate)
    }

    /**
     * Сравнение на равенство
     */
    override fun equals(other: Any?): Boolean {
        if (other is Complex)
            return other.re == re && other.im == im
        return false
    }

    /**
     * Преобразование в строку
     */
    override fun toString(): String =
        when {
            re == 0.0 && im != 0.0 -> "${im}i"
            im == 0.0 && re != 0.0 -> "$re"
            im == 1.0 -> "$re+i"
            im == -1.0 -> "$re-i"
            im > 0.0 -> "$re+${im}i"
            im < 0.0 -> "$re${im}i"
            else -> "0.0"
        }
}