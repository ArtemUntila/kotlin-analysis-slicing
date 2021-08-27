package org.jetbrains.research.ml.kotlinAnalysis.slicing

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Condition
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.singleLambdaArgumentExpression
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.research.ml.kotlinAnalysis.util.KtConditionalVisitor
import org.jetbrains.research.ml.kotlinAnalysis.util.getPrintWriter
import java.nio.file.Path

class SliceFormatter(override val psiFile: PsiFile,
                     override val lineNumbers: MutableSet<Int>,
                     override val document: Document,
                     override val outputDir: Path) : Slicer {

    override val sliceElements = mutableSetOf<KtElement>()

    private val elementsToDelete = mutableSetOf<PsiElement>()

    override val debugWriter = getPrintWriter(outputDir, "${name}_debug.txt")

    override fun execute() {
        debugWriter.println("LINE NUMBERS: ${lineNumbers.sorted()}\n")
        val ktStaticAnalyzer = KtStaticAnalyzer(psiFile, lineNumbers, document, outputDir)
        ktStaticAnalyzer.execute()
        val staticLineNumbers = ktStaticAnalyzer.getStaticLineNumbers()
        lineNumbers.addAll(staticLineNumbers)
        debugWriter.println("LINE NUMBERS AFTER STATIC ANALYSIS: ${lineNumbers.sorted()}\n")

        psiFile.accept(KtSlicingVisitor())

        debugWriter.println("\n==========SLICE ELEMENTS==========\n")
        sliceElements.forEach { debugWriter.println("$it:\n ${it.getDebugText()}\n") }
        debugWriter.println("\n==========ELEMENTS TO DELETE==========\n")
        elementsToDelete.forEach { debugWriter.println("$it:\n ${it.text}\n") }
        debugWriter.flush()
        CommandProcessor.getInstance().executeCommand(
            null, { elementsToDelete.forEach { it.delete() } }, null, null
        )
    }

    fun writeSlice() {
        val sliceWriter = getPrintWriter(outputDir, name)
        sliceWriter.println(psiFile.text)
        sliceWriter.flush()
    }

    fun getSlicedPsi() = psiFile

    private inner class KtSlicingVisitor : KtConditionalVisitor() {

        /**Always retained elements*/
        override fun visitPackageDirective(directive: KtPackageDirective) {
            printDebug(directive)
            debugWriter.println("ADD: ALWAYS_RETAINED_PSI_ELEMENT\n")
            sliceElements.add(directive)
        }

        override fun visitImportList(importList: KtImportList) {
            printDebug(importList)
            debugWriter.println("ADD: ALWAYS_RETAINED_PSI_ELEMENT\n")
            sliceElements.add(importList)
        }

        override val condition: Condition<KtElement> = Condition { analyzeElement(it) }

        override fun visitIfExpression(expression: KtIfExpression) {
            if (!analyzeElement(expression)) return

            expression.then?.accept(this, null)

            val els = expression.`else` ?: return
            els.accept(this, null)
            if ((els as PsiElement) in elementsToDelete) elementsToDelete.add(expression.elseKeyword!!) // ok
        }

        override fun visitProperty(property: KtProperty) {
            if (analyzeElement(property)) property.delegateExpressionOrInitializer?.accept(this, null)
        }

        /**Functions related to visiting Lambda Expressions*/
        override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
            if (!analyzeElement(expression)) return
            val lambda = expression.callExpression?.singleLambdaArgumentExpression() ?: return
            lambda.accept(this, null)
            expression.receiverExpression.accept(this, null) // slicing previous lambda
        }

        override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
            if (analyzeElement(lambdaExpression)) lambdaExpression.bodyExpression?.accept(this, null)
        }

        /**For all that left*/
        override fun visitKtElement(element: KtElement) {
            analyzeElement(element) // ignore return type
        }
    }

    private fun analyzeElement(element: KtElement): Boolean {
        val startLineNumber = element.getStartLineNumber()
        printDebug(element)

        return when {
            startLineNumber in lineNumbers -> {
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
}
