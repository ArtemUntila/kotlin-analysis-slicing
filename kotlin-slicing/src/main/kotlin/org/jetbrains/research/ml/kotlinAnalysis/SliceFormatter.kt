package org.jetbrains.research.ml.kotlinAnalysis

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.research.ml.kotlinAnalysis.util.getPrintWriter
import java.nio.file.Path

class SliceFormatter(private val psiFile: PsiFile,
                     private val lineNumbers: MutableSet<Int>,
                     private val document: Document,
                     private val outputDir: Path) {

    private val sliceElements = mutableSetOf<PsiElement>()

    private val name = psiFile.name

    private val debugWriter = getPrintWriter(outputDir, "${name}_debug.txt")

    companion object {
        val ALWAYS_RETAINED_PSI_ELEMENTS = setOf(
            KtPackageDirective::class, KtImportList::class, PsiWhiteSpaceImpl::class
        )
        val CONTAINERS = setOf( // KtClassBody & KtBlockExpression ?
            // KtClass & KtObjectDeclaration are Kotlin Classes
            KtClass::javaClass, KtObjectDeclaration::javaClass, KtNamedFunction::class
        )
    }

    fun execute() {
        psiFile.acceptChildren(SlicingVisitor()) // including elements from sliceElements
        debugWriter.println("\n==========SLICE ELEMENTS==========\n")
        sliceElements.forEach { debugWriter.println("$it:\n ${it.text}\n") }

        CommandProcessor.getInstance().executeCommand(
            null, { psiFile.acceptChildren(FormatterVisitor()) }, null, null
        )
        val sliceWriter = getPrintWriter(outputDir, name)
        sliceWriter.println(psiFile.text)

        debugWriter.flush()
        sliceWriter.flush()
    }

//    private fun start(psiFile: PsiFile) { // check PsiVisitor -- udp: works wonderful -- thanks Marat
//        psiFile.acceptChildren(SlicingVisitor()) // including elements from sliceElements
//        debugWriter.println("\n==========SLICED ELEMENTS==========\n")
//        sliceElements.forEach { debugWriter.println("$it:\n ${it.text}\n") }
//    }

    // VISITORS:
    private inner class SlicingVisitor : PsiRecursiveElementVisitor() { // KtTreeVisitorVoid -- ?
        override fun visitElement(element: PsiElement) {
            if (lineNumbers.isEmpty()) return // if all lines from lineNumbers were found

            val lineNumber = getLineNumber(element)

            debugWriter.println(
                "i = $lineNumber    rawName = $element    class = ${element::class}    text:\n${element.text}"
            )

            when {
                lineNumber in lineNumbers -> {
                    sliceElements.add(element)
                    lineNumbers.remove(lineNumber)
                    debugWriter.println("ADDED: SLICE_ELEMENT\n")
                }
                isAlwaysRetained(element) -> {
                    sliceElements.add(element)
                    debugWriter.println("ADDED: ALWAYS_RETAINED_PSI_ELEMENT\n")
                }
                else -> {
                    if (checkContainer(element, lineNumber)) { // visit CONTAINERS if any of lineNumber is in its range
                        val block = getBlock(element)
                        // adding container & braces {}
                        sliceElements.addAll(listOf(element, block.firstChild, block.lastChild))
                        block.acceptChildren(this)
                    } else {
                        debugWriter.println("SKIPPED\n")
                    }
                }
            }
        }
    }

    private inner class FormatterVisitor : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            debugWriter.println("rawName = $element    class = ${element::class}    text:\n${element.text}")
            when {
                element in sliceElements -> {
                    if (isContainer(element)) {
                        getBlock(element).acceptChildren(this)
                    }
                }
                isAlwaysRetained(element) -> super.visitElement(element)
                else -> {
                    debugWriter.println("DELETING")
                    element.delete()
                }
            }
        }
    }

    private fun getLineNumber(element: PsiElement) = document.getLineNumber(element.startOffset) + 1

    private fun checkContainer(element: PsiElement, startLine: Int): Boolean {
        if (!isContainer(element)) return false
        val endLine = document.getLineNumber(element.endOffset)
        return (startLine..endLine).any { it in lineNumbers }
    }

    private fun getBlock(element: PsiElement): PsiElement {
        lateinit var block: PsiElement
        for (child in element.children) {
            if (isBlock(child)) { // ~child::class is KtBlockExpression~ doesn't work -> isBlock()
                block = child
                break
            }
        }
        return block
    }

    private fun isBlock(element: PsiElement) = element is KtBlockExpression

    private fun isAlwaysRetained(element: PsiElement) = element::class in ALWAYS_RETAINED_PSI_ELEMENTS

    private fun isContainer(element: PsiElement) = element::class in CONTAINERS
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
