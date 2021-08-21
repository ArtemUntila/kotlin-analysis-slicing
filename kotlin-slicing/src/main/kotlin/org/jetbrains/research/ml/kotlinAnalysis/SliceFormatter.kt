package org.jetbrains.research.ml.kotlinAnalysis

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.singleLambdaArgumentExpression
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.psiUtil.asAssignment
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

    // TODO: Research references and properties to include soot constants in slice.
    private inner class SlicingKtVisitor : KtTreeVisitorVoid() {

        /**Always retained elements*/
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

        /**Functions related to visiting Classes or Objects*/
        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            if (analyzeElement(classOrObject)) classOrObject.body?.accept(this, null)
        }

        override fun visitClassBody(classBody: KtClassBody) {
            if (analyzeElement(classBody)) classBody.acceptChildren(this, null)
        }

        override fun visitClassInitializer(initializer: KtClassInitializer) {
            if (analyzeElement(initializer)) initializer.body?.accept(this, null)
        }

        override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
            if (analyzeElement(constructor)) constructor.bodyExpression?.accept(this, null)
        }

        /**Functions related to visiting body containers*/
        override fun visitNamedFunction(function: KtNamedFunction) {
            if (analyzeElement(function)) function.bodyExpression?.accept(this, null)
        }

        override fun visitIfExpression(expression: KtIfExpression) {
            // What happens with:
            // foo(
            //   if (true)
            //     42
            //   else {
            //     ...
            //     ...
            //     "hello" -> mk<String>()
            //   }
            // )

            if (!analyzeElement(expression)) return

            val then = expression.then
            then?.accept(this, null)

            val els = expression.`else` ?: return
            els.accept(this, null)
            if ((els as PsiElement) in elementsToDelete) elementsToDelete.add(expression.elseKeyword!!)
        }

        override fun visitWhenExpression(expression: KtWhenExpression) {
            if (analyzeElement(expression)) for (entry in expression.entries) visitWhenEntry(entry)
        }

        override fun visitWhenEntry(whenEntry: KtWhenEntry) {
            if (analyzeElement(whenEntry)) whenEntry.expression?.accept(this, null)
        }

        override fun visitLoopExpression(loopExpression: KtLoopExpression) {
            if (analyzeElement(loopExpression)) loopExpression.body?.accept(this, null)
        }

        override fun visitBlockExpression(expression: KtBlockExpression) {
            /*if (analyzeElement(expression))*/ expression.acceptChildren(this, null)
        }

        override fun visitProperty(property: KtProperty) {
            // can be sliced if "REF TYPE ELEMENT" is null or contains "Any" -- but is it necessary?
//            debugWriter.println("HAS TYPE: ${property.hasType}    TYPE REF: ${property.typeReference}    " +
//                    "REF TYPE ELEMENT: ${property.typeReference?.typeElement?.getDebugText()}")

            // What happens with:
            // val lambda = { ... 42 }
            // val listOfFortyTwos = listOf(1, 2, 3).map(lambda)

            if (analyzeElement(property)) visitExpressionSafely(property.delegateExpressionOrInitializer)
            else { // including constant: val a = 0 -- undo deleting
                val references = ReferencesSearch.search(property)
                // PsiReference -> KtNameReference -> Expression -> ArgumentList || Condition || ...
                val used = references.any { (it.element.parent.parent as KtElement).containsSliceElement() }
                if (used) {
                    elementsToDelete.remove(property)
                    debugWriter.println("UNDO DELETING USED CONSTANT\n")
                }
            }
        }

        override fun visitBinaryExpression(expression: KtBinaryExpression) {
            if (!analyzeElement(expression)) {
                val assignment = expression.asAssignment() ?: return
                val left = assignment.left as KtNameReferenceExpression
                val property = left.resolve() as KtProperty
                if ((property as PsiElement) !in elementsToDelete && property.delegateExpressionOrInitializer == null) {
                        elementsToDelete.remove(expression as PsiElement)
                } // example: val a: Int; if (...) a = 1 else a = 2 -- both expressions should be included in slice
                debugWriter.println("THIS EXPRESSION INITIALIZES PROPERTY, MUST NOT BE REMOVED\n")
            }
            visitExpressionSafely(expression.right)
        }

        private fun visitExpressionSafely(expression: KtExpression?) {
            when (expression) {
//                is KtIfExpression -> visitIfExpressionSafely(expression)
//                is KtWhenExpression -> visitWhenExpressionSafely(expression)
                is KtDotQualifiedExpression -> visitDotQualifiedExpression(expression)
            }
        }

        /**Functions related to visiting Lambda Expressions*/
        override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
            if (!analyzeElement(expression)) return
            val lambda = expression.callExpression?.singleLambdaArgumentExpression() ?: return
            lambda.accept(this, null)
            expression.receiverExpression.accept(this, null) // slicing previous lambda
            // expression.callExpression?.singleLambdaArgumentExpression()?.accept(this, null)
        }

        override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
            if (analyzeElement(lambdaExpression)) lambdaExpression.bodyExpression?.accept(this, null)
        }

        /**For all that left*/
        override fun visitKtElement(element: KtElement) {
            analyzeElement(element) // ignore return type
        }

//        override fun visitConstantExpression(expression: KtConstantExpression) {
//            super.visitConstantExpression(expression)
//        }
//
//        override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
//            super.visitStringTemplateExpression(expression)
//        }

//        private fun visitIfExpressionSafely(expression: KtIfExpression) { // variable declaration with if
//            analyzeElement(expression)
//
//            val then = expression.then
//            if (then != null) analyzeElement(then)
//
//            val els = expression.`else` ?: return
//            visitElseSafely(els)
//        }
//
//        private fun visitWhenExpressionSafely(expression: KtWhenExpression) {
//            analyzeElement(expression)
//
//            for (entry in expression.entries) {
//                if (!entry.isElse) analyzeElement(entry)
//                else visitElseSafely(entry.expression ?: return)
//            }
//        }
//
//        private fun visitElseSafely(els: KtElement) {
//            when (els) {
//                is KtIfExpression -> visitIfExpressionSafely(els)
//                is KtWhenExpression -> visitWhenExpressionSafely(els)
//                is KtBlockExpression -> els.acceptChildren(this, null)
//            }
//        }
    }

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

    private fun KtElement.containsSliceElement(): Boolean {
        val startLineNumber = this.getStartLineNumber()
        val endLineNumber = this.getEndLineNumber()
        return (startLineNumber..endLineNumber).any { it in lineNumbers }
    }

    private fun KtElement.getStartLineNumber() = document.getLineNumber(this.pureStartOffset) + 1

    private fun KtElement.getEndLineNumber() = document.getLineNumber(this.pureEndOffset) + 1
}
