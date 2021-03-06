package ai.flowstorm.core.type.value

import com.fasterxml.jackson.annotation.JsonProperty

abstract class Value {
    @JsonProperty("values")
    var alternativeValues: List<Value> = listOf()
    val type: String = ""
}