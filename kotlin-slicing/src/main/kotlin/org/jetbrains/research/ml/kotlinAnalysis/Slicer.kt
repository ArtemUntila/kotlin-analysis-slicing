package org.jetbrains.research.ml.kotlinAnalysis

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.research.ml.kotlinAnalysis.psi.PsiProvider
import java.nio.file.Path

class Slicer(outputDir: Path) : AnalysisExecutor() {

    private val dataWriter = PrintWriterResourceManager(outputDir, "slice.txt")
    override val controlledResourceManagers: Set<ResourceManager> = setOf(dataWriter)

    override fun analyse(project: Project) { // initializes dataWriter.writer !!!
        val psiFiles: MutableSet<PsiFile> = PsiProvider.extractPsiFiles(project)
        for (psiFile in psiFiles) {
            val map = parse("/home/artyom/IdeaProjects/kotlin-gradle/slice.log")
            dataWriter.writer.println("$map\n") // DEBUG
            val keyName = toKey(psiFile.name)
            if (map.containsKey(keyName)) {
                val lines = unpackSlices(map[keyName]!!, psiFile.text.lines())
                lines.forEach { dataWriter.writer.println(it) } // DEBUG
                dataWriter.writer.println() // DEBUG
                SliceFormatter(psiFile, lines, dataWriter.writer).execute()
            }
        }
    }

    private fun toKey(psiFileName: String) =
        psiFileName.replace(".kt", "").replace(psiFileName[0], psiFileName[0].toUpperCase())

    // printAndWriteInDepth(psiFile, "", 1)
    /*private fun printAndWriteInDepth(element: PsiElement, prefix: String, index: Int) {
        var i = index
        dataWriter.writer.println("i = $i\n${prefix}rawName = $element    class = ${element::class.java}")

        val text = element.text
        if (text.isNotEmpty()) {
            dataWriter.writer.println("${prefix}text:")
            for (line in text.lines()) {
                dataWriter.writer.println("$prefix$line")
            }
        }

        val children = element.children
        if (children.isNotEmpty()) {
            dataWriter.writer.println("${prefix}Children:")
            for (child in children) {
                printAndWriteInDepth(child, "$prefix    ", i)
                if (check(element.toString())) {
                    i += fixTextSize(child) // in future line number will checked as: (lineNumber > i)
                }
            }
        }

        dataWriter.writer.println()
    }*/

    /*private fun fixTextSize(element: PsiElement): Int {
        val lines = element.text.lines()
        val size = lines.size
        val fixedSize = lines.filter { it.isNotEmpty() }.size
        return when {
            (fixedSize > 0) -> size
            element.toString() == "PsiWhiteSpace" -> size - 2 // PsiWhiteSpace \n = 3 * \n, idk why ¯\_(ツ)_/¯
            else -> 0
        }
    }*/

    /*private fun funTextSize(funElement: PsiElement): Int {
        val children = funElement.children
        val stringChildren = funElement.children.map { it.toString() }
        val block = children[stringChildren.indexOf("BLOCK")]
        return getLinesNumber(funElement) - (getLinesNumber(block) - 1)
    }*/

    // private fun getLinesNumber(element: PsiElement) = element.text.lines().size

    /*private val indexChangers = arrayOf(
        "CLASS",
        "OBJECT_DECLARATION",
        "FUN",
        "BLOCK"
     )*/

    // private fun check(elementName: String) = indexChangers.contains(elementName) || elementName.contains("KtFile")

    // delete unused PsiElement; in future "element" will be replaced to list of elements
    /*if (javaClass == org.jetbrains.kotlin.psi.KtReturnExpression::class.java) {
            CommandProcessor.getInstance().executeCommand(
                null, kotlinx.coroutines.Runnable { element.delete() }, null, null
            )
        }*/
}
