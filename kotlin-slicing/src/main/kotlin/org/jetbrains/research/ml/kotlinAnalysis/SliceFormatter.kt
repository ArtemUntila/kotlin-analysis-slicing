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

//      TODO: PsiElement has a *range* of line numbers, not a single line number
//      TODO: +- done, because KtElement ? declaration / assignment includes everything after "="
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

        override fun visitNamedFunction(function: KtNamedFunction) {
            if (!analyzeElement(function)) return

            if (function.containsSliceElement()) {
                val body = function.bodyExpression ?: return
                sliceElements.add(function)
                sliceElements.add(body)
                super.visitKtElement(body)
            }
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            if (!analyzeElement(classOrObject)) return

            if (classOrObject.containsSliceElement()) {
                val body = classOrObject.body ?: return
                sliceElements.add(classOrObject)
                sliceElements.add(body)
                super.visitKtElement(body)
            }
        }

        override fun visitIfExpression(expression: KtIfExpression) {
            if (!analyzeElement(expression)) return

            val then = expression.then
            val els = expression.`else`
            if (then != null) {
                if (then.containsSliceElement()) {
                    if (then is KtBlockExpression) super.visitKtElement(then)
                    else if (then is KtIfExpression) visitIfExpression(then)
                } else elementsToDelete.add(then as PsiElement)
            }
            if (els != null) {
                if (els.containsSliceElement()) {
                    if (els is KtBlockExpression) super.visitKtElement(els)
                    else if (els is KtIfExpression) visitIfExpression(els)
                } else {
                    elementsToDelete.add(els as PsiElement)
                    elementsToDelete.add(expression.elseKeyword!!) // everything is fine
                }
            }
        }

        override fun visitLoopExpression(loopExpression: KtLoopExpression) {
            if (!analyzeElement(loopExpression)) return

            val body = loopExpression.body ?: return
            if (body is KtBlockExpression) super.visitKtElement(body) // no need to check a single expression
        }

        override fun visitWhenExpression(expression: KtWhenExpression) {
            if (!analyzeElement(expression)) return

            for (entry in expression.entries) {
                if (entry.containsSliceElement()) { // "else" entry is necessary, therefore analyzeElement() isn't used
                    val block = (entry as PsiElement).lastChild // Sequence<KtElement> is more complicated
                    if (block is KtBlockExpression) super.visitKtElement(block) // no need to check a single expression
                } else if (!entry.isElse) {
                    elementsToDelete.add(entry as PsiElement)
                }
            }
        }

        override fun visitKtElement(element: KtElement) {
            // More optimized than adding this condition in analyzeElement()
            if (element is KtPackageDirective || element is KtImportList) {
                printDebug(element, element.getStartLineNumber())
                debugWriter.println("ADD: ALWAYS_RETAINED_PSI_ELEMENT\n")
                sliceElements.add(element)
            } else {
                analyzeElement(element) // ignore return type
            }
        }
    }

    // TODO: Maybe it is better to do -1 in util.parse
    private fun KtElement.getStartLineNumber() = document.getLineNumber(this.pureStartOffset) + 1

    private fun KtElement.getEndLineNumber() = document.getLineNumber(this.pureEndOffset) + 1

    private fun KtElement.containsSliceElement(): Boolean {
        val startLineNumber = this.getStartLineNumber()
        val endLineNumber = this.getEndLineNumber()
        return (startLineNumber..endLineNumber).any { it in lineNumbers }
    }
}
