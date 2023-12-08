package com.cloudsheeptech.shoppinglist

open class SingleEvent<out T>(private val content : T) {

    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled)
            null
        else {
            hasBeenHandled = true
            return content
        }
    }

    fun peekContent() : T = content

}