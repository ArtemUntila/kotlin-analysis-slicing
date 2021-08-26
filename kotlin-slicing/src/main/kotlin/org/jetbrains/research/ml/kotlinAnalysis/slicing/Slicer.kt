package org.jetbrains.research.ml.kotlinAnalysis.slicing

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import java.io.PrintWriter
import java.nio.file.Path

interface Slicer {

    val psiFile: PsiFile

    val lineNumbers: MutableSet<Int>

    val document: Document

    val outputDir: Path

    val sliceElements: MutableSet<KtElement>

    val debugWriter: PrintWriter get() = PrintWriter(System.out)

    val name: String get() = psiFile.name

    fun execute()

    fun KtElement.getStartLineNumber() = document.getLineNumber(this.pureStartOffset) + 1

    fun KtElement.getEndLineNumber() = document.getLineNumber(this.pureEndOffset) + 1

    fun KtElement.containsSliceElement(): Boolean {
        val startLineNumber = this.getStartLineNumber()
        val endLineNumber = this.getEndLineNumber()
        return (startLineNumber..endLineNumber).any { it in lineNumbers }
    }

    fun printDebug(element: KtElement) =
        debugWriter.println(
            "$element: i = ${element.getStartLineNumber()}   class = ${element::class}    " +
                    "text:\n${element.getDebugText()}\n"
        )
}
