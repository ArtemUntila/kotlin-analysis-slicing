package org.jetbrains.research.ml.kotlinAnalysis.slicing

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
import org.jetbrains.research.ml.kotlinAnalysis.util.KtConditionalVisitor
import org.jetbrains.research.ml.kotlinAnalysis.util.getPrintWriter
import java.io.PrintWriter
import java.nio.file.Path

class KtStaticAnalyzer(override val psiFile: PsiFile,
                       override val lineNumbers: MutableSet<Int>,
                       override val document: Document,
                       override val outputDir: Path) : Slicer {

    override val sliceElements = mutableSetOf<KtElement>()

    private val staticLineNumbers = mutableSetOf<Int>()

    fun getStaticLineNumbers() = staticLineNumbers.sorted()

    override val debugWriter: PrintWriter = getPrintWriter(outputDir, "${name}_static_debug.txt")

    override fun execute() {
        psiFile.accept(KtStaticAnalysisPreprocessor())
        debugWriter.flush()
    }

    private inner class KtStaticAnalysisPreprocessor : KtConditionalVisitor() {

        override val condition: Condition<KtElement> = Condition { it.containsSliceElement() }

        /**Included properties*/
        override fun visitProperty(property: KtProperty) {
            if (property.containsSliceElement()) processProperty(property)
            else processSkippedProperty(property)
        }

        private fun processProperty(property: KtProperty) {
            when (val initializer = property.delegateExpressionOrInitializer) {
                is KtIfExpression -> visitIfExpressionSafely(initializer)
                is KtWhenExpression -> visitWhenExpressionSafely(initializer)
            }
        }

        /**If and When as an expressions*/
        private fun visitIfExpressionSafely(expression: KtIfExpression) {
            visitExpressionSafely(expression.then)
            visitExpressionSafely(expression.`else`)
        }

        private fun visitWhenExpressionSafely(expression: KtWhenExpression) {
            expression.entries.forEach { visitExpressionSafely(it.expression) }
        }

        private fun visitExpressionSafely(expression: KtExpression?) {
            if (expression == null) return

            when (expression) {
                is KtBlockExpression -> {
                    val lastStatement = expression.lastBlockStatementOrThis()
                    if (lastStatement !is KtBlockExpression) visitExpressionSafely(lastStatement)
                }
                is KtIfExpression -> visitIfExpressionSafely(expression)
                is KtWhenExpression -> visitWhenExpressionSafely(expression)
                else -> processLastStatement(expression)
            }
        }

        private fun processLastStatement(expression: KtExpression) {
            when {
                // expression.containsSliceElement() -> return
                expression.isPrimitive() -> expression.addToStaticSlice()
                else -> processElement(expression)
            }
        }

        private fun KtExpression.isPrimitive() = this is KtConstantExpression || this is KtStringTemplateExpression

        private fun processSkippedProperty(property: KtProperty) {
            // if property is included by dynamic slicing or was processed already
            debugWriter.println("PROCESS PROPERTY: ${property.getDebugText()}")
            debugWriter.print("SKIPPED = ")
            if (property.containsSliceElement() || property in sliceElements) {
                debugWriter.println("false\n")
                return
            }

            debugWriter.println("true")

            val initializer = property.delegateExpressionOrInitializer

            val references = ReferencesSearch.search(property).map { it.element.parent as KtElement }

            val used = references.any {
                it.containsSliceElement() || it in sliceElements || it.getStartLineNumber() in staticLineNumbers
            }

            debugWriter.println("USED = $used\n")
            if (!used) return
            else property.addToStaticSlice()

            if (initializer != null) processProperty(property)
            else {
                debugWriter.println("SEARCH INITIALIZERS\n")
                references
                    .filter {
                        it is KtBinaryExpression && it.asAssignment() != null &&
                                property as PsiElement == (it.left as? KtNameReferenceExpression)?.resolve()
                    } // initializing assignments
                    .forEach { processElement(it) }
            }
        }

        private fun processElement(element: KtElement) {
            if (element in sliceElements) return // containsSliceElement isn't used because of one-string expressions
            // else
            element.addToStaticSlice()

            val dataDependencies = getDataDependencies(element)
            val controlDependencies = getControlDependencies(element)
            debugWriter.println("PROCESS ELEMENT: ${element.getStartLineNumber()}. ${element.getDebugText()}")
            debugWriter.println("DATA DEPENDENCIES: ${dataDependencies.map { it.getDebugText() }}")
            debugWriter.println("CONTROL DEPENDENCIES: ${controlDependencies.map { it.getDebugText() }}\n")

            val dependencies = (dataDependencies + controlDependencies).filter { it !in sliceElements }
            for (dependency in dependencies) {
                if (dependency is KtProperty) processSkippedProperty(dependency)
                else processElement(dependency)
            }
        }

        // ~ resolved dataDependencies
        private fun getDataDependencies(element: KtElement): Set<KtElement> {
            val references = findNameReferences(element)
            val resolves = mutableSetOf<KtElement>()
            for (reference in references) {
                val resolve = reference.resolve() as? KtElement ?: continue
                resolves.add(resolve)
            }
            return resolves
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

        // conditions from control-statements
        // TODO: Research FOR and WHILE behaviour
        private fun getControlDependencies(element: KtElement): Set<KtElement> {
            var current = element
            val controlDependencies = mutableSetOf<KtElement>()
            while (current !is KtNamedFunction && current !is KtClassOrObject) {
                //if (parent !in sliceElements) {
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
                }
                //}
                current = (current as PsiElement).parent as KtElement
            }
            return controlDependencies
        }

        private fun KtElement.addToStaticSlice() {
            sliceElements.add(this)
            staticLineNumbers.addAll(this.getStartLineNumber()..this.getEndLineNumber())
        }
    }
}
