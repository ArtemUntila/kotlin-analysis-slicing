package org.jetbrains.research.ml.kotlinAnalysis

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.research.ml.kotlinAnalysis.psi.PsiProvider
import org.jetbrains.research.ml.kotlinAnalysis.util.*
import java.nio.file.Path

class Slicer(outputDir: Path) : AnalysisExecutor() {

    // TODO: Add debugWriter?
    private val dataWriter = PrintWriterResourceManager(outputDir, "slice.txt")
    override val controlledResourceManagers: Set<ResourceManager> = setOf(dataWriter)

    override fun analyse(project: Project) { // initializes dataWriter.writer !!!
        val psiFiles: MutableSet<PsiFile> = PsiProvider.extractPsiFiles(project)
        if (psiFiles.isEmpty()) return

        // TODO: Accept path to slice.log as an input
        val map = parse(
            "/home/artyom/IdeaProjects/Kotlin-Analysis/kotlin-slicing/results/samples/sample1/slice.log"
        ) // main.kt -> MainKt

        // TODO: Remove debug prints
        dataWriter.writer.println("map: $map\n") // DEBUG

        val documentManager = PsiDocumentManager.getInstance(project)

        for (psiFile in psiFiles) {
            val keyName = toKey(psiFile.name)
            val lineNumbers = map[keyName] ?: continue
            val lines = unpackSlices(lineNumbers, psiFile.text.lines())
            dataWriter.writer.println("${lines.entries.joinToString("\n")}\n") // DEBUG
            SliceFormatter(psiFile, lineNumbers, dataWriter.writer, documentManager.getDocument(psiFile)!!).execute()
        }
    }

    // TODO: Research how to get the correct filename -> class/package name mapping
    private fun toKey(psiFileName: String) =
        psiFileName.replace(".kt", "").replace(psiFileName[0], psiFileName[0].toUpperCase())
}
