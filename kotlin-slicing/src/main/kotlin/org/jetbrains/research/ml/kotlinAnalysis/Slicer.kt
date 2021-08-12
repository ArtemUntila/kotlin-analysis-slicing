package org.jetbrains.research.ml.kotlinAnalysis

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.research.ml.kotlinAnalysis.psi.PsiProvider
import org.jetbrains.research.ml.kotlinAnalysis.util.parse
import org.jetbrains.research.ml.kotlinAnalysis.util.unpackSlices
import java.nio.file.Path

class Slicer(private val outputDir: Path, private val slice: Path) : AnalysisExecutor() {

    private val infoResourceManager = PrintWriterResourceManager(outputDir, "slicing_info.txt")
    override val controlledResourceManagers = mutableSetOf<ResourceManager>(infoResourceManager)

    override fun analyse(project: Project) { // initializes dataWriter.writer !!!
        val psiFiles = PsiProvider.extractPsiFiles(project)
        if (psiFiles.isEmpty()) return

        val map = parse(slice)

        val infoWriter = infoResourceManager.writer
        infoWriter.println("map: $map\n") // DEBUG

        val documentManager = PsiDocumentManager.getInstance(project)

        for (psiFile in psiFiles) {
            val keyName = "${(psiFile as KtFile).packageFqName}.${psiFile.name}"
            val lineNumbers = map[keyName] ?: continue
            val lines = unpackSlices(lineNumbers, psiFile.text.lines()) // DEBUG
            val document = documentManager.getDocument(psiFile)!!
            infoWriter.println("$keyName:\n${lines.entries.joinToString("\n")}\n") // DEBUG
            SliceFormatter(psiFile, lineNumbers, document, outputDir).execute()
        }
    }
}
