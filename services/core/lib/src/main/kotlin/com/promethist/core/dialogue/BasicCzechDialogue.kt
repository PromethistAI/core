package com.promethist.core.dialogue

import com.promethist.core.model.DialogueEvent
import java.util.*

abstract class BasicCzechDialogue() : BasicDialogue() {

    var basicId = 1
    //Nodes
    val _goBack = GoBack(basicId++, repeat = true)
    val _basicVersionGlobalIntent = GlobalIntent(basicId++, "basicVolumeUpGlobalIntent", "verze")
    val _basicVersionResponse = Response(basicId++, { "\$version, dialogue $dialogueName" })

    val _basicVolumeUpGlobalIntent = GlobalIntent(basicId++, "basicVolumeUpGlobalIntent", "zvýšit hlasitost", "mluv hlasitěji")
    val _basicVolumeUpResponse = Response(basicId++, { "\$volume_up setting volume up" })

    val _basicVolumeDownGlobalIntent = GlobalIntent(basicId++, "basicVolumeDownGlobalIntent", "snížit hlasitost", "mluv tišeji")
    val _basicVolumeDownResponse = Response(basicId++, { "\$volume_down setting volume down" })

    val _basicLogApplicationErrorGlobalIntent = GlobalIntent(basicId++, "basicLogApplicationErrorGlobalIntent", "chyba aplikace", "problém aplikace")
    val _basicLogApplicationErrorResponse1 = Response(basicId++, {"O co jde?"})
    val _basicLogApplicationErrorResponse2 = Response(basicId++, {"Díky, pojďme zpátky."})
    val _basicLogApplicationErrorUserInputTransition = Transition(_basicLogApplicationErrorResponse2)
    val _basicLogApplicationErrorUserInput = UserInput(basicId++, arrayOf()) {
        val transition = Transition(_basicLogApplicationErrorResponse2)
        dialogueEvent = DialogueEvent(datetime = Date(), type = DialogueEvent.Type.UserError, userId = user._id, sessionId = session._id, applicationName = application.name, dialogueName = application.dialogueName, nodeId = turn.endFrame?.nodeId, text = input.transcript.text)
        transition
    }

    val _basicLogApplicationCommentGlobalIntent = GlobalIntent(basicId++, "basicLogApplicationCommentGlobalIntent", "komentář aplikace")
    val _basicLogApplicationCommentResponse1 = Response(basicId++, {"Nyní máte prostor přidat komentář."})
    val _basicLogApplicationCommentResponse2 = Response(basicId++, {"Děkujeme za komentář, následuje návrat ke konverzaci."})
    val _basicLogApplicationCommentUserInputTransition = Transition(_basicLogApplicationCommentResponse2)
    val _basicLogApplicationCommentUserInput = UserInput(basicId++, arrayOf()) {
        val transition = Transition(_basicLogApplicationCommentResponse2)
        dialogueEvent = DialogueEvent(datetime = Date(), type = DialogueEvent.Type.UserComment, userId = user._id, sessionId = session._id, applicationName = application.name, dialogueName = application.dialogueName, nodeId = turn.endFrame?.nodeId, text = input.transcript.text)
        transition
    }

    init {
        _basicVersionGlobalIntent.next = _basicVersionResponse
        _basicVersionResponse.next = _goBack

        _basicVolumeUpGlobalIntent.next = _basicVolumeUpResponse
        _basicVolumeUpResponse.next = _goBack

        _basicVolumeDownGlobalIntent.next = _basicVolumeDownResponse
        _basicVolumeDownResponse.next = _goBack

        _basicLogApplicationErrorGlobalIntent.next = _basicLogApplicationErrorResponse1
        _basicLogApplicationErrorResponse1.next = _basicLogApplicationErrorUserInput
        _basicLogApplicationErrorResponse2.next = _goBack

        _basicLogApplicationCommentGlobalIntent.next = _basicLogApplicationCommentResponse1
        _basicLogApplicationCommentResponse1.next = _basicLogApplicationCommentUserInput
        _basicLogApplicationCommentResponse2.next = _goBack
    }
}