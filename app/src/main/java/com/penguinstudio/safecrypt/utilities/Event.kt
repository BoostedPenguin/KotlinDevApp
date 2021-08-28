package com.penguinstudio.safecrypt.utilities

import java.lang.Exception

open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}

class ff() {
    private var smthBase: DoingSomething = DoingSomething()

    fun callbackFunction() : String {
        return ""
    }

    fun callingSmth() {
        smthBase.smth {
            callbackFunction()
        }

        smthBase.smthReturning( { callbackFunction() }, { callbackFunction() })
    }
}

class DoingSomething : ISomething {
    override fun smth(onSomething: (Unit) -> String) {
        val g = onSomething.invoke(Unit)
    }

    override fun smthReturning(onSomething: (Unit) -> Unit, onError: (Exception) -> Unit) {
        try {
            onSomething.invoke(Unit)
        }
        catch (ex: Exception) {
            onError.invoke(ex)
        }
    }
}

interface ISomething {
    fun smth(onSomething: (Unit) -> String)
    fun smthReturning(onSomething: (Unit) -> Unit, onError: (Exception) -> Unit)
}