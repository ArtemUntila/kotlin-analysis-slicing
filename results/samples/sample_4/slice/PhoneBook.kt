class PhoneBook {

    private val book = mutableMapOf<String, MutableSet<String>>()
    private val phones = mutableMapOf<String, String>()

    /**
     * Добавить человека.
     * Возвращает true, если человек был успешно добавлен,
     * и false, если человек с таким именем уже был в телефонной книге
     * (во втором случае телефонная книга не должна меняться).
     */

    fun addHuman(name: String): Boolean {
        if (book.containsKey(name)) return false
        book[name] = mutableSetOf()
        return true
    }

    /**
     * Добавить номер телефона.
     * Возвращает true, если номер был успешно добавлен,
     * и false, если человек с таким именем отсутствовал в телефонной книге,
     * либо у него уже был такой номер телефона,
     * либо такой номер телефона зарегистрирован за другим человеком.
     */

    fun addPhone(name: String, phone: String): Boolean {
        if (!book.containsKey(name)) return false
        for ((n, p) in book)
            if (p.contains(phone)) return false
        book[name]!!.add(phone)
        phones[phone] = name
        return true
    }

    /**
     * Убрать номер телефона.
     * Возвращает true, если номер был успешно удалён,
     * и false, если человек с таким именем отсутствовал в телефонной книге
     * либо у него не было такого номера телефона.
     */

    fun removePhone(name: String, phone: String): Boolean {
        if (!book.containsKey(name)) return false
        if (!book[name]!!.contains(phone)) return false
        return true
    }

    /**
     * Вернуть имя человека по заданному номеру телефона.
     * Если такого номера нет в книге, вернуть null.
     */

    fun humanByPhone(phone: String): String? = phones[phone]

    }