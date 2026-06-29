package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.unit.Density

val Measurable.id get() = parentData
val Placeable.id get() = parentData

fun <T: Any> Modifier.id(obj: T) =
    this then IdModifierElement(obj)

@Immutable
data class IdModifierElement<T: Any>(val id: T) :
    ModifierNodeElement<IdModifierNode<T>>() {
    override fun create() = IdModifierNode(id)
    override fun update(node: IdModifierNode<T>) {
        node.id = this.id
    }
}

class IdModifierNode<T: Any>(var id: T) :
    ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?): Any = id
}