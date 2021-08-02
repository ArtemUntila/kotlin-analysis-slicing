package org.jetbrains.research.ml.kotlinAnalysis

import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.io.PrintWriter

class SliceFormatter(private val psiFile: PsiFile,
                     private val lines: MutableMap<Int, String>, // line <number, text>
                     private val writer: PrintWriter) {

    // val constants = arrayOf("PACKAGE_DIRECTIVE", "IMPORT_LIST", "PsiWhiteSpace") // should not be deleted (not full)

    private val psiElementsToDelete = mutableListOf<PsiElement>()

    private val containers = arrayOf( // objects containing "BLOCK"
        "CLASS",
        "OBJECT_DECLARATION",
        "FUN"
    )

    fun execute() {
        writer.println("PSI FILE NAME = ${psiFile.name}:\n${psiFile.text}\n")
        start(psiFile)
        writer.println("\npsiElementsToDelete = $psiElementsToDelete") // DEBUG
        CommandProcessor.getInstance().executeCommand(
            null, kotlinx.coroutines.Runnable { psiElementsToDelete.forEach { it.delete() } },
            null, null
        )
        writer.println("Slice:\n${psiFile.text}")
    }

    private fun start(psiElement: PsiElement) {
        if (toDelete(psiElement)) {
            psiElementsToDelete.add(psiElement)
            return
        }

        for (child in psiElement.children) {
            if (child.toString() == "BLOCK") {
                for (subChild in child.children) {
                    walkInDepth(subChild)
                }
            } else if (containers.contains(child.toString())) {
                start(child)
            }
        }
    }

    private fun walkInDepth(psiElement: PsiElement) {
        if (toDelete(psiElement)) {
            psiElementsToDelete.add(psiElement)
            return
        }

        for ((key, value) in lines) {
            if (psiElement.text.trim() == value.trim()) {
                lines.remove(key)
                return
            }
        }

        for (child in psiElement.children) {
            if (containers.contains(child.toString())) {
                start(child)
            } else {
                walkInDepth(child)
            }
        }
    }

    private fun toDelete(psiElement: PsiElement): Boolean {
        if (psiElement.toString() == "PsiWhiteSpace") {
            return false
        }

        val textLines = psiElement.text.lines().map { it.trim() } // trim will be removed -> parse.unpackSlices()
        var decision = true
        for (line in lines.values) {
            if (textLines.contains(line)) {
                decision = false
                break
            }
        }

        return decision
    }

    /*private fun fixTextSize(element: PsiElement): Int {
        val lines = element.text.lines()
        val size = lines.size
        val fixedSize = lines.filter { it.isNotEmpty() }.size
        return when {
            (fixedSize > 0) -> size
            element.toString() == "PsiWhiteSpace" -> size - 2 // PsiWhiteSpace \n = 3 * \n, idk why ¯\_(ツ)_/¯
            else -> 0
        }
    }*/

    /*private fun funTextSize(funElement: PsiElement): Int {
        val children = funElement.children
        val stringChildren = funElement.children.map { it.toString() }
        val block = children[stringChildren.indexOf("BLOCK")]
        return getLinesNumber(funElement) - (getLinesNumber(block) - 1)
    }*/

    // private fun getLinesNumber(element: PsiElement) = element.text.lines().size

    // private fun check(elementName: String) = blocks.contains(elementName) || elementName.contains("KtFile")
}
