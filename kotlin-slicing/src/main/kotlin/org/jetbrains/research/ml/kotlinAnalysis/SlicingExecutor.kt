package org.jetbrains.research.ml.kotlinAnalysis

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.research.ml.kotlinAnalysis.psi.PsiProvider
import org.jetbrains.research.ml.kotlinAnalysis.slicing.SliceFormatter
import org.jetbrains.research.ml.kotlinAnalysis.util.parse
import org.jetbrains.research.ml.kotlinAnalysis.util.unpackSlices
import java.nio.file.Path

class SlicingExecutor(private val outputDir: Path, private val slice: Path) : AnalysisExecutor() {

    private val infoResourceManager = PrintWriterResourceManager(outputDir, "slicing_info.txt")
    override val controlledResourceManagers = mutableSetOf<ResourceManager>(infoResourceManager)

    override fun analyse(project: Project) { // initializes dataWriter.writer !!!
        val ktFiles = PsiProvider.extractElementsOfTypeFromProject(project, KtFile::class.java)

        if (ktFiles.isEmpty()) return

        val map = parse(slice)

        val infoWriter = infoResourceManager.writer
        infoWriter.println("map: $map\n") // DEBUG

        val documentManager = PsiDocumentManager.getInstance(project)

        for (ktFile in ktFiles) {
            val psiFile = ktFile as PsiFile

            var name = psiFile.name
            val packageFqName = ktFile.packageFqName.asString()
            if (packageFqName.isNotEmpty()) name = "$packageFqName.$name"

            val lineNumbers = map[name] ?: continue

            val lines = unpackSlices(lineNumbers, ktFile.getDebugText().lines()) // DEBUG
            infoWriter.println("${psiFile.name} -- $name:\n${lines.entries.joinToString("\n")}\n") // DEBUG

            val document = documentManager.getDocument(psiFile)!!
            SliceFormatter(psiFile, lineNumbers, document, outputDir).execute()
        }
    }
}
