package com.promethist.core.runtime

import com.promethist.core.*
import com.promethist.core.dialogue.Dialogue
import com.promethist.core.model.*
import com.promethist.core.provider.LocalFileStorage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

object DialogueLoadTest {

    @JvmStatic
    fun main(args: Array<String>) {
        val logger: Logger = LoggerFactory.getLogger("dialogue-model-load-test")
        val loader = FileResourceLoader(LocalFileStorage(File("test")), "dialogue")
        val dialogueName = "product/some-dialogue/1"
        val dialogue = loader.newObject<Dialogue>("$dialogueName/model", "ble", 1, false)
        dialogue.loader = loader

        dialogue.validate()
        println(dialogue.describe())
        val user = User(username = "tester@promethist.ai", name = "Tester", surname = "Tester", nickname = "Tester")
        val context = Context(
                object : Pipeline {
                    override val components = LinkedList<Component>()
                    override fun process(context: Context): Context = components.pop().process(context)
                },
                Profile(user_id = user._id),
                Session(
                        datetime = Date(),
                        sessionId = "T-E-S-T",
                        user = user,
                        application = Application(name = "test", dialogueName = "product/some-dialogue/1", ttsVoice = "Grace")
                ),
                Turn(Input(transcript = Input.Transcript("some message"))),
                logger,
                dialogue.locale,
                SimpleCommunityResource()
        )

        val func = dialogue.functions.first()
        println("calling $func:")
        println(func.exec(context))

        dialogue.subDialogues.first().apply {
            val dialogueArgs = getConstructorArgs(context)
            val subDialogue = loader.newObjectWithArgs<Dialogue>("${this.name}/model", dialogueArgs)
            println("sub-dialogue: $subDialogue")
            println(subDialogue.describe())
        }
    }
}