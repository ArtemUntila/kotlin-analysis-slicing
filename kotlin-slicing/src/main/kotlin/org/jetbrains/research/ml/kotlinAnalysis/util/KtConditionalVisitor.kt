package org.jetbrains.research.ml.kotlinAnalysis.util

import com.intellij.openapi.util.Condition
import org.jetbrains.kotlin.psi.*

abstract class KtConditionalVisitor: KtTreeVisitorVoid() {

    abstract val condition: Condition<KtElement>

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        if (condition.value(classOrObject)) classOrObject.body?.accept(this, null)
    }

    override fun visitClassBody(classBody: KtClassBody) {
        if (condition.value(classBody)) classBody.acceptChildren(this, null)
    }

    override fun visitClassInitializer(initializer: KtClassInitializer) {
        if (condition.value(initializer)) initializer.body?.accept(this, null)
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        if (condition.value(constructor)) constructor.bodyExpression?.accept(this, null)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (condition.value(function)) function.bodyExpression?.accept(this, null)
    }

    override fun visitIfExpression(expression: KtIfExpression) {
        if (condition.value(expression)) {
            expression.then?.accept(this, null)
            expression.`else`?.accept(this, null)
        }
    }

    override fun visitWhenExpression(expression: KtWhenExpression) {
        if (condition.value(expression)) for (entry in expression.entries) visitWhenEntry(entry)
    }

    override fun visitWhenEntry(whenEntry: KtWhenEntry) {
        if (condition.value(whenEntry)) whenEntry.expression?.accept(this, null)
    }

    override fun visitLoopExpression(loopExpression: KtLoopExpression) {
        if (condition.value(loopExpression)) loopExpression.body?.accept(this, null)
    }

    override fun visitBlockExpression(expression: KtBlockExpression) {
        if (condition.value(expression)) expression.acceptChildren(this, null)
    }
}
