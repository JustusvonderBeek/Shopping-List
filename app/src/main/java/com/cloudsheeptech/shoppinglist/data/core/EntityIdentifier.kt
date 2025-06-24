package com.cloudsheeptech.shoppinglist.data.core

interface EntityIdentifier<out ID : Any> {
    fun getId(): ID
}
