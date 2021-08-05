package org.jetbrains.research.ml.kotlinAnalysis

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.research.ml.kotlinAnalysis.psi.getBlock
import org.jetbrains.research.ml.kotlinAnalysis.psi.isAlwaysRetained
import org.jetbrains.research.ml.kotlinAnalysis.psi.isContainer
import org.jetbrains.research.ml.kotlinAnalysis.util.getPrintWriter
import java.nio.file.Path

class SliceFormatter(private val psiFile: PsiFile,
                     private val lineNumbers: MutableSet<Int>,
                     private val document: Document,
                     private val outputDir: Path) {

    private val sliceElements = mutableSetOf<PsiElement>()

    private val name = psiFile.name

    private val debugWriter = getPrintWriter(outputDir, "${name}_debug.txt")

    fun execute() {
        psiFile.acceptChildren(SlicingVisitor()) // including elements from sliceElements
        debugWriter.println("\n==========SLICE ELEMENTS==========\n")
        sliceElements.forEach { debugWriter.println("$it:\n ${it.text}\n") }

        debugWriter.println("\n==========FORMATTING==========\n")
        CommandProcessor.getInstance().executeCommand(
            null, { psiFile.acceptChildren(FormatterVisitor()) }, null, null
        )

        val sliceWriter = getPrintWriter(outputDir, name)
        sliceWriter.println(psiFile.text)

        debugWriter.flush()
        sliceWriter.flush()
    }

    // VISITORS:
    private inner class SlicingVisitor : PsiRecursiveElementVisitor() { // KtTreeVisitorVoid -- ?
        override fun visitElement(element: PsiElement) {
            if (lineNumbers.isEmpty()) return // if all lines from lineNumbers were found

            val lineNumber = getLineNumber(element)

            debugWriter.println(
                "i = $lineNumber    rawName = $element    class = ${element::class}    text:\n${element.text}\n"
            )

            when {
                lineNumber in lineNumbers -> {
                    debugWriter.println("ADD: SLICE_ELEMENT\n")
                    sliceElements.add(element)
                    lineNumbers.remove(lineNumber)
                }
                isAlwaysRetained(element) -> {
                    debugWriter.println("ADD: ALWAYS_RETAINED_PSI_ELEMENT\n")
                    sliceElements.add(element)
                }
                checkContainer(element, lineNumber) -> {
                    val block = getBlock(element)
                    // adding container & braces {}
                    sliceElements.addAll(listOf(element, block.firstChild, block.lastChild))
                    super.visitElement(block)
                }
                else -> debugWriter.println("SKIP\n")
            }
        }
    }

    private inner class FormatterVisitor : PsiRecursiveElementVisitor() {

        override fun visitElement(element: PsiElement) {
            debugWriter.println("rawName = $element    class = ${element::class}    text:\n${element.text}\n")
            when {
                element in sliceElements && isContainer(element) -> super.visitElement(getBlock(element))
                element !in sliceElements && !isAlwaysRetained(element) -> {
                    debugWriter.println("DELETE\n")
                    safeDelete(element)
                }
                else -> debugWriter.println("SKIP\n")
            }
        }

        private fun safeDelete(element: PsiElement) = element.deleteChildRange(element.firstChild, element.lastChild)
    }

    private fun getLineNumber(element: PsiElement) = document.getLineNumber(element.startOffset) + 1

    private fun checkContainer(element: PsiElement, startLine: Int): Boolean {
        if (!isContainer(element)) return false
        val endLine = document.getLineNumber(element.endOffset)
        return (startLine..endLine).any { it in lineNumbers }
    }
}

//    private inner class IncludeVisitor : PsiRecursiveElementVisitor() {
//        override fun visitElement(element: PsiElement) {
//            sliceElements.add(element)
//            if (element.toString() == "BLOCK") {
//                return
//            }
//            super.visitElement(element)
//        }
//    }

//    private fun includeParents(element: PsiElement) {
//        var parent = element.parent
//        while (!sliceElements.contains(parent)) {
//            sliceElements.add(parent)
//            if (parent::class in CONTAINERS) { // instead of do{}while
//                break
//            } else {
//                parent = parent.parent
//            }
//        }
//    }
