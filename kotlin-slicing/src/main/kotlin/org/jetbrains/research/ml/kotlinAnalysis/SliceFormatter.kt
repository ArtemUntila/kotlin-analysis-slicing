package org.jetbrains.research.ml.kotlinAnalysis

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.research.ml.kotlinAnalysis.util.getPrintWriter
import java.nio.file.Path

class SliceFormatter(private val psiFile: PsiFile,
                     private val lineNumbers: MutableSet<Int>,
                     private val document: Document,
                     private val outputDir: Path) {

    private val sliceElements = mutableSetOf<KtElement>()

    private val elementsToDelete = mutableSetOf<PsiElement>()

    private val name = psiFile.name

    private val debugWriter = getPrintWriter(outputDir, "${name}_debug.txt")

    fun execute() {
        psiFile.accept(SlicingKtVisitor())
        debugWriter.println("\n==========SLICE ELEMENTS==========\n")
        sliceElements.forEach { debugWriter.println("$it:\n ${it.getDebugText()}\n") }

        debugWriter.println("\n==========FORMATTING==========\n")
        try {
            CommandProcessor.getInstance().executeCommand(
                null, { elementsToDelete.forEach { it.delete() } }, null, null
            )

            val sliceWriter = getPrintWriter(outputDir, name)
            sliceWriter.println(psiFile.text)
            sliceWriter.flush()
        } finally {
            debugWriter.flush()
        }
    }

    private inner class SlicingKtVisitor : KtTreeVisitorVoid() {

        override fun visitPackageDirective(directive: KtPackageDirective) {
            printDebug(directive, directive.getStartLineNumber())
            debugWriter.println("ADD: ALWAYS_RETAINED_PSI_ELEMENT\n")
            sliceElements.add(directive)
        }

        override fun visitImportList(importList: KtImportList) {
            printDebug(importList, importList.getStartLineNumber())
            debugWriter.println("ADD: ALWAYS_RETAINED_PSI_ELEMENT\n")
            sliceElements.add(importList)
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            if (!analyzeElement(classOrObject)) return

            val body = classOrObject.body ?: return
            sliceElements.add(classOrObject)
            sliceElements.add(body)
            super.visitKtElement(body)
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            if (!analyzeElement(function)) return

            val body = function.bodyExpression ?: return
            sliceElements.add(function)
            sliceElements.add(body)
            super.visitKtElement(body)
        }

        override fun visitIfExpression(expression: KtIfExpression) {
            if (!analyzeElement(expression)) return

            val then = expression.then
            then?.accept(this, null)

            val els = expression.`else`
            els?.accept(this, null)
            if ((els as PsiElement) in elementsToDelete) elementsToDelete.add(expression.elseKeyword!!)
        }

        override fun visitWhenExpression(expression: KtWhenExpression) {
            if (!analyzeElement(expression)) return

            for (entry in expression.entries) visitWhenEntry(entry)
        }

        override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) {
            val analyzeResult = analyzeElement(jetWhenEntry)
            when {
                analyzeResult -> jetWhenEntry.expression?.accept(this, null)
                !analyzeResult && jetWhenEntry.isElse -> elementsToDelete.remove(jetWhenEntry as PsiElement)
            }
        }

        override fun visitLoopExpression(loopExpression: KtLoopExpression) {
            if (!analyzeElement(loopExpression)) return

            loopExpression.body?.accept(this, null)
        }

        override fun visitBlockExpression(expression: KtBlockExpression) {
            if (analyzeElement(expression)) expression.acceptChildren(this, null)
        }

        override fun visitKtElement(element: KtElement) {
            analyzeElement(element) // ignore return type
        }
    }

//  TODO: PsiElement has a *range* of line numbers, not a single line number
//  TODO: +- done, because KtElement ? declaration / assignment includes everything after "="
    private fun analyzeElement(element: KtElement): Boolean {
        val startLineNumber = element.getStartLineNumber()
        printDebug(element, startLineNumber)

        return when {
            (startLineNumber in lineNumbers) -> {
                debugWriter.println("ADD: SLICE_ELEMENT\n")
                sliceElements.add(element)

                true
            }
            element.containsSliceElement() -> {
                debugWriter.println("ADD: CONTAINER\n")
                sliceElements.add(element)

                true
            }
            else -> {
                debugWriter.println("SKIP\n")
                elementsToDelete.add(element as PsiElement)

                false
            }
        }
    }

    private fun printDebug(element: KtElement, startLineNumber: Int) =
        debugWriter.println(
            "$element: i = $startLineNumber   class = ${element::class}    text:\n${element.getDebugText()}\n"
        )

    private fun KtElement.getStartLineNumber() = document.getLineNumber(this.pureStartOffset) + 1

    private fun KtElement.getEndLineNumber() = document.getLineNumber(this.pureEndOffset) + 1

    private fun KtElement.containsSliceElement(): Boolean {
        val startLineNumber = this.getStartLineNumber()
        val endLineNumber = this.getEndLineNumber()
        return (startLineNumber..endLineNumber).any { it in lineNumbers }
    }
}
