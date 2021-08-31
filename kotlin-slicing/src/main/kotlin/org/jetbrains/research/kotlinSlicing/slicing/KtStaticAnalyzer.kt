package org.jetbrains.research.kotlinSlicing.slicing

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.psiUtil.asAssignment
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.research.kotlinSlicing.util.KtConditionalVisitor
import org.jetbrains.research.ml.kotlinAnalysis.util.getPrintWriter
import java.io.PrintWriter
import java.nio.file.Path

class KtStaticAnalyzer(override val psiFile: PsiFile,
                       override val lineNumbers: MutableSet<Int>,
                       override val document: Document,
                       override val outputDir: Path) : Slicer {

    /**Elements included during static analysis*/
    override val sliceElements = mutableSetOf<KtElement>()

    private val staticLineNumbers = mutableSetOf<Int>()

    override val debugWriter: PrintWriter = getPrintWriter(outputDir, "${name}_static_debug.txt")

    override fun execute() {
        psiFile.accept(KtStaticAnalysisPreprocessor())
        debugWriter.flush()
    }

    fun getStaticLineNumbers() = staticLineNumbers.sorted()

    private inner class KtStaticAnalysisPreprocessor : KtConditionalVisitor() {

        override val condition: Condition<KtElement> = Condition { it.containsSliceElement() }

        /**Included properties*/
        override fun visitProperty(property: KtProperty) {
            if (property.containsSliceElement()) {
                sliceElements.add(property)
                processIncludedPropertyInitializer(property.delegateExpressionOrInitializer ?: return)
            } else processSkippedProperty(property)
        }

        private fun processIncludedPropertyInitializer(initializer: KtExpression) {
            when (initializer) {
                is KtIfExpression -> visitIfExpressionSafely(initializer)
                is KtWhenExpression -> visitWhenExpressionSafely(initializer)
            }
        }

        /**IF as an expressions*/
        private fun visitIfExpressionSafely(expression: KtIfExpression) {
            visitExpressionSafely(expression.then)
            visitExpressionSafely(expression.`else`)
        }

        /**WHEN as an expression*/
        private fun visitWhenExpressionSafely(expression: KtWhenExpression) {
            expression.entries.forEach { visitExpressionSafely(it.expression) }
        }

        /**Property initializer expression*/
        private fun visitExpressionSafely(expression: KtExpression?) {
            if (expression == null) return

            when (expression) {
                is KtBlockExpression -> {
                    val lastStatement = expression.lastBlockStatementOrThis()
                    if (lastStatement !is KtBlockExpression) visitExpressionSafely(lastStatement)
                }
                is KtIfExpression -> visitIfExpressionSafely(expression)
                is KtWhenExpression -> visitWhenExpressionSafely(expression)
                else -> processElement(expression)
            }
        }

        /**
         * Processing property, not included in slice.log, but used in sliced lines or used in elements,
         * included by this static analyzer
         */
        private fun processSkippedProperty(property: KtProperty) {
            // if the property is included by a dynamic slicer or has already been processed
            debugWriter.println("PROCESSING PROPERTY: ${property.getDebugText()}")
            debugWriter.print("SKIPPED = ")
            if (property in sliceElements) {
                debugWriter.println("false\n")
                return
            }

            debugWriter.println("true")

            val references = ReferencesSearch.search(property).map { it.element.parent as KtElement }

            val used = references.any {
                it.containsSliceElement() || it in sliceElements || it.getStartLineNumber() in staticLineNumbers
            }

            debugWriter.println("USED = $used\n")
            if (!used) return
            else property.addToStaticSlice()

            val initializer = property.delegateExpressionOrInitializer

            if (initializer != null) visitExpressionSafely(initializer)
            else {
                debugWriter.println("SEARCHING INITIALIZERS\n")
                references
                    .filter {
                        it is KtBinaryExpression && it.asAssignment() != null &&
                                property as PsiElement == (it.left as? KtNameReferenceExpression)?.resolve()
                    } // assignments-initializers
                    .forEach { processElement(it) }
            }
        }

        /**Processing element and it's data- and control-dependencies*/
        private fun processElement(element: KtElement) {
            if (element in sliceElements) return // containsSliceElement isn't used because of one-string expressions

            element.addToStaticSlice()

            val dataDependencies = getDataDependencies(element)
            val controlDependencies = getControlDependencies(element)
            val dependencies = (dataDependencies + controlDependencies).filter { it !in sliceElements }

            debugWriter.println("PROCESSING ELEMENT: ${element.getStartLineNumber()}. ${element.getDebugText()}")
            debugWriter.println("DATA DEPENDENCIES: ${dataDependencies.map { it.getDebugText() }}")
            debugWriter.println("CONTROL DEPENDENCIES: ${controlDependencies.map { it.getDebugText() }}")
            debugWriter.println("NOT PROCESSED DEPENDENCIES: ${dependencies.map { it.getDebugText() }}\n")

            for (dependency in dependencies) {
                if (dependency is KtProperty) processSkippedProperty(dependency)
                else processElement(dependency)
            }
        }

        // ~ resolved dataDependencies
        private fun getDataDependencies(element: KtElement): Set<KtElement> {
            val references = findNameReferences(element)
            val dataDependencies = mutableSetOf<KtElement>()
            for (reference in references) {
                val resolve = reference.resolve() as? KtElement ?: continue
                dataDependencies.add(resolve)
            }
            return dataDependencies
        }

        // ~ used dataDependencies
        private fun findNameReferences(element: KtElement): Set<KtNameReferenceExpression> {
            val references = mutableSetOf<KtNameReferenceExpression>()

            class KtNameReferencesFinder : KtTreeVisitorVoid() {

                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    references.addIfNotNull(expression as? KtNameReferenceExpression)
                    super.visitSimpleNameExpression(expression)
                }
            }

            element.acceptChildren(KtNameReferencesFinder(), null)

            return references
        }

        // conditions from control-expressions
        private fun getControlDependencies(element: KtElement): Set<KtElement> {
            var current = element
            val controlDependencies = mutableSetOf<KtElement>()

            while (current !is KtNamedFunction && current !is KtClassOrObject) {
                current = (current as PsiElement).parent as KtElement

                when (current) {
                    is KtIfExpression -> {
                        sliceElements.add(current)
                        controlDependencies.add(current.condition ?: continue)
                    }
                    is KtWhenExpression -> {
                        sliceElements.add(current)
                        controlDependencies.add(current.subjectVariable ?: continue)
                    }
                    is KtWhenEntry -> {
                        sliceElements.add(current)
                        controlDependencies.addAll(current.conditions)
                    }
                    is KtForExpression -> {
                        sliceElements.add(current)
                        controlDependencies.add(current.loopRange ?: continue)
                    }
                    is KtWhileExpressionBase -> { // While + DoWhile
                        sliceElements.add(current)
                        controlDependencies.add(current.condition ?: continue)
                    }
                }
            }

            return controlDependencies
        }

        private fun KtElement.addToStaticSlice() {
            sliceElements.add(this)
            staticLineNumbers.addAll(this.getStartLineNumber()..this.getEndLineNumber())
        }
    }
}
