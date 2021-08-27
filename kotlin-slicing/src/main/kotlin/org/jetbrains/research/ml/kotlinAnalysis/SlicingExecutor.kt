package org.jetbrains.research.ml.kotlinAnalysis

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.tools.projectWizard.core.asPath
import org.jetbrains.research.ml.kotlinAnalysis.psi.PsiProvider
import org.jetbrains.research.ml.kotlinAnalysis.slicing.SliceFormatter
import org.jetbrains.research.ml.kotlinAnalysis.util.getPrintWriter
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
            var psiFile = ktFile as PsiFile

            val packageName =
                (ktFile.packageFqName.asString().replace('.', '/') + '/').takeIf { it != "/" } ?: ""

            val pathName = packageName + psiFile.name

            val lineNumbers = map[pathName]

            if (lineNumbers != null) {
                val lines = unpackSlices(lineNumbers, ktFile.getDebugText().lines()) // DEBUG
                infoWriter.println("$pathName:\n${lines.entries.joinToString("\n")}\n") // DEBUG

                val document = documentManager.getDocument(psiFile)!!
                val debugOutputDir = "$outputDir/debug/$packageName".asPath()
                val sliceFormatter = SliceFormatter(psiFile, lineNumbers, document, debugOutputDir)
                sliceFormatter.execute()
                psiFile = sliceFormatter.getSlicedPsi()
            }

            writePsi(psiFile, packageName)
        }
    }

    private fun writePsi(psiFile: PsiFile, packageName: String) {
        val dir = "$outputDir/slice/$packageName".asPath()
        val writer = getPrintWriter(dir, psiFile.name)
        writer.write(psiFile.text)
        writer.flush()
    }
}
