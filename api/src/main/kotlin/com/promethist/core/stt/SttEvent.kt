package com.promethist.core.stt

import java.io.Serializable
import java.util.HashMap

data class SttEvent(
        var type: Type? = null,
        var params: MutableMap<String, Any?> = HashMap()) : Serializable {

    enum class Type {
        Connected,
        Listening,
        Recognized,
        Error
    }

}