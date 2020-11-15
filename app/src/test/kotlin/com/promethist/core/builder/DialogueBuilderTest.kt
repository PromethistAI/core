package com.promethist.core.builder

import com.promethist.core.builder.DialogueSourceCodeBuilder.*
import io.mockk.mockk
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.TestInstance
import java.io.InputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DialogueBuilderTest {

    val dialogueBuilder = DialogueBuilder().apply {
        fileResource = mockk(relaxed = true)
        intentModelBuilder = mockk(relaxed = true)
    }

    @Test
    fun `test dialogue building`() {
        dialogueBuilder.create("dialogue1", "product/dialogue", 1).apply {
            source.apply {
                parameters = mapOf("str" to "bla", "num" to 123, "chk" to true)
                initCode = "data class Test(val i: Int)\nfun time() = System.currentTimeMillis()\nval i = 1"

                var nodeId = 0
                addNode(Speech(--nodeId, "response1", null, true, listOf("hello, say some animal", "hi, say some animal")))
                addNode(Intent(--nodeId, "intent1", 0F, listOf("no", "nope", "quit", "stop"), listOf()))
                addNode(Intent(--nodeId, "intent2", 0F, listOf("dog", "cat", "tiger"), listOf()))
                addNode(Function(--nodeId, "function1", mapOf("trans1" to "stop"), "println(trans1)\ntrans1"))
                addNode(Speech(--nodeId, "response2", null, true, listOf("Your response was \${input.transcript.text}. Intent node \${input.intent.name}.")))
                addNode(UserInput(--nodeId, "input1", listOf("intent1", "intent2"), listOf(), null, false, mapOf(), ""))

                addTransition("start" to "response1")
                addTransition("response1" to "input1")
                addTransition("intent1" to "function1")
                addTransition("intent2" to "response2")
                addTransition("response2" to "stop")
            }

            addResource(object : DialogueBuilder.Resource {
                override val stream: InputStream
                    get() = """{"data":"value"}""".byteInputStream()
                override val filename: String
                    get() = "data.json"

            })
            source.build()
        }
    }
}