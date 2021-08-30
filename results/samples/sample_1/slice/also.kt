import java.io.File

fun main() {
    //File("./dir/slice").mkdirs()
    val file = File("./dir/slice/file.txt")
    val writer = file.writer()
    writer.write("Hello")
    writer.flush()
}