package com.penguinstudio.safecrypt.ui.verify

import androidx.lifecycle.ViewModel

class PatternUnlockViewModel : ViewModel() {
    var isRegistering: Boolean = false
    var isRegisteringCounter: Int = 1
    var pattern: String = ""
}