package org.jetbrains.research.ml.kotlinAnalysis.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.*

val ALWAYS_RETAINED_PSI_ELEMENTS = setOf(
    KtPackageDirective::class, KtImportList::class, PsiWhiteSpaceImpl::class
)

val CONTAINERS = setOf( // KtClassBody & KtBlockExpression ?
    // KtClass & KtObjectDeclaration are Kotlin Classes
    KtClass::javaClass, KtObjectDeclaration::javaClass, KtNamedFunction::class
)

fun getBlock(element: PsiElement): PsiElement {
    lateinit var block: PsiElement
    for (child in element.children) {
        if (isBlock(child)) { // ~child::class is KtBlockExpression~ doesn't work -> isBlock()
            block = child
            break
        }
    }
    return block
}

fun isBlock(element: PsiElement) = element is KtBlockExpression

fun isAlwaysRetained(element: PsiElement) = element::class in ALWAYS_RETAINED_PSI_ELEMENTS

fun isContainer(element: PsiElement) = element::class in CONTAINERS
