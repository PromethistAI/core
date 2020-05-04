package com.promethist.core.runtime

import com.promethist.core.*
import com.promethist.core.dialogue.Dialogue
import com.promethist.core.builder.IrModel
import com.promethist.util.LoggerDelegate
import kotlin.math.roundToInt
import org.slf4j.Logger
import javax.inject.Inject
import com.promethist.core.model.Session.DialogueStackFrame as Frame

class DialogueManager : Component {
    @Inject
    lateinit var dialogueFactory: DialogueFactory

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context = with(context) {
        this@DialogueManager.logger.info("processing DM")
        if (session.dialogueStack.isEmpty()) {
            session.dialogueStack.push(
                    Frame(session.application.dialogueName, context.application.properties, 0))
        }

        proceed(context)

        return context
    }

    private fun getIrModels(currentFrame: Frame, context: Context): List<IrModel> {
        val currentDialogue = dialogueFactory.get(currentFrame)
        val node = currentDialogue.node(currentFrame.nodeId)

        require(node is Dialogue.UserInput)

        val models = mutableListOf(IrModel(currentDialogue.buildId, currentDialogue.name, node.id))
        if (!node.skipGlobalIntents) {
            //current global intents
            models.add(IrModel(currentDialogue.buildId, currentDialogue.name))
            //parents global intents
            context.session.dialogueStack.distinctBy { it.name } .forEach {
                val dialogue = dialogueFactory.get(it)
                models.add(IrModel(dialogue.buildId, dialogue.name))
            }
        }
        return models
    }

    private fun getIntentFrame(models: List<IrModel>, frame: Frame, context: Context): Frame {
        val (modelId, nodeId) = context.input.intent.name.split("#")
        val dialogueName = models.first { it.id == modelId }.dialogueName

        // intent is from current dialogue
        if (dialogueName == frame.name) {
            return frame.copy(nodeId = nodeId.toInt())
        }
        //intent is from parent dialogue
        context.session.dialogueStack.firstOrNull { it.name == dialogueName }?.let {
            return it.copy(nodeId = nodeId.toInt())
        }
        error("Can not find intent dialogue")
    }

    private fun getNode(frame: Frame): Dialogue.Node = dialogueFactory.get(frame).node(frame.nodeId)

    /**
     * @return true if next user input requested, false if session ended
     */
    private fun proceed(context: Context): Boolean = with(context) {
        var frame = session.dialogueStack.pop()
        var inputRequested: Boolean? = null
        var node: Dialogue.Node
        val processedNodes = mutableListOf<Dialogue.Node>()

        try {
            while (inputRequested == null) {
                if (processedNodes.size > 20) error("Too much steps in processing dialogue (infinite loop?)")

                node = getNode(frame)
                processedNodes.add(node)

                when (node) {
                    is Dialogue.UserInput -> {
                        if (processedNodes.size == 1) {
                            //first user input in turn
                            val irModels = getIrModels(frame, context)
                            context.irModels = irModels

                            val transition = node.process(context)
                            frame = if (transition != null) {
                                frame.copy(nodeId = transition.node.id)
                            } else {
                                // intent recognition
                                processPipeline()
                                getIntentFrame(irModels, frame, context)
                            }
                        } else {
                            //last user input in turn
                            addExpectedPhrases(context, node.intents.asList())
                            frame.copy(nodeId = node.id).let {
                                turn.endFrame = it
                                session.dialogueStack.push(it)
                            }
                            inputRequested = true
                        }
                    }
                    is Dialogue.Repeat -> {
                        if (session.dialogueStack.isEmpty()) inputRequested = false
                        frame = session.dialogueStack.pop()
                        session.turns.last { it.endFrame == frame }
                                .responseItems.forEach { if (it.repeatable) turn.responseItems.add(it) }
                    }
                    is Dialogue.Function -> {
                        val transition = node.exec(context)
                        frame = frame.copy(nodeId = transition.node.id)
                    }
                    is Dialogue.StopSession -> {
                        session.dialogueStack.clear()
                        inputRequested = false
                    }
                    is Dialogue.GoBack, is Dialogue.StopDialogue -> {
                        if (session.dialogueStack.isEmpty()) inputRequested = false
                        frame = session.dialogueStack.pop()
                        if (node is Dialogue.GoBack && node.repeat) {
                            session.turns.last { it.endFrame == frame }
                                    .responseItems.forEach { if (it.repeatable) turn.responseItems.add(it) }
                        }
                    }
                    is Dialogue.SubDialogue -> {
                        val args = node.getConstructorArgs(context)
                        session.dialogueStack.push(frame.copy(nodeId = node.next.id))
                        frame = Frame(node.name, args, 0)
                    }
                    is Dialogue.TransitNode -> {
                        when (node) {
                            is Dialogue.Response -> {
                                val text = node.getText(context)
                                turn.addResponseItem(text, node.image, node.audio, node.video, repeatable = node.isRepeatable)
                            }
                            is Dialogue.GlobalIntent -> {
                                session.dialogueStack.push(session.turns.last().endFrame)
                            }
                        }
                        frame = frame.copy(nodeId = node.next.id)
                    }
                }
            }
            return inputRequested

        } finally {
            logNodes(processedNodes, logger)
        }
    }

    private fun logNodes(nodes: List<Dialogue.Node>, logger:Logger) {
        var dialogueNodes = listOf<Dialogue.Node>()
        var rest = nodes
        do {
            dialogueNodes = rest.takeWhile { it.dialogue.name == rest.first().dialogue.name }
            logger.info("passed nodes ${dialogueNodes.first().dialogue.name} >> " +
                    dialogueNodes.map { it.toString() }.reduce { acc, s -> "$acc > $s" })
            rest = rest.drop(dialogueNodes.size)
        } while (rest.isNotEmpty())
    }

    private fun addExpectedPhrases(context: Context, intents: Collection<Dialogue.Intent>) {
        //note: google has limit 5000 (+100k per whole ASR request), we use lower value to be more comfortable with even longer phrases
        val maxPhrasesPerIntent = 2000 / intents.size
        intents.forEach { intent ->
            if (intent.utterances.size > maxPhrasesPerIntent) {
                val rat = intent.utterances.size / maxPhrasesPerIntent.toFloat()
                var idx = 0.0F
                for (i in 0 until maxPhrasesPerIntent) {
                    context.expectedPhrases.add(ExpectedPhrase(intent.utterances[idx.roundToInt()]))
                    idx += rat
                }
            } else {
                context.expectedPhrases.addAll(intent.utterances.map { text -> ExpectedPhrase(text) })
            }
        }
        logger.info("${context.expectedPhrases.size} expected phrase(s) added")
    }
}