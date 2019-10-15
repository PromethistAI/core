package com.promethistai.common

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.text.SimpleDateFormat

object ObjectUtil {

    @JvmStatic
    val defaultMapper = ObjectMapper()
            .registerModule(KotlinModule())
            .setDateFormat(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}