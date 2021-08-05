package org.jetbrains.research.ml.kotlinAnalysis

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.research.ml.kotlinAnalysis.psi.PsiProvider
import org.jetbrains.research.ml.kotlinAnalysis.util.*
import java.nio.file.Path

class Slicer(private val outputDir: Path, private val slice: Path) : AnalysisExecutor() {

    // TODO: Add debugWriter? UPD: DONE
    private val infoResourceManager = PrintWriterResourceManager(outputDir, "slicing_info.txt")
    override val controlledResourceManagers = mutableSetOf<ResourceManager>(infoResourceManager)

    override fun analyse(project: Project) { // initializes dataWriter.writer !!!
        val psiFiles: MutableSet<PsiFile> = PsiProvider.extractPsiFiles(project)
        if (psiFiles.isEmpty()) return

        val infoWriter = infoResourceManager.writer
        // TODO: Accept path to slice.log as an input. UDP: DONE
        val map = parse(slice) // main.kt -> MainKt

        // TODO: Remove debug prints
        infoWriter.println("map: $map\n") // DEBUG

        val documentManager = PsiDocumentManager.getInstance(project)

        for (psiFile in psiFiles) {
            val keyName = toKey(psiFile.name)
            val lineNumbers = map[keyName] ?: continue
            val lines = unpackSlices(lineNumbers, psiFile.text.lines())
            val document = documentManager.getDocument(psiFile)!!
            infoWriter.println("keyName:\n${lines.entries.joinToString("\n")}\n") // DEBUG
            SliceFormatter(psiFile, lineNumbers, document, outputDir).execute()
        }
    }

    // TODO: Research how to get the correct filename -> class/package name mapping
    private fun toKey(psiFileName: String) =
        psiFileName.replace(".kt", "").replace(psiFileName[0], psiFileName[0].toUpperCase())
}
