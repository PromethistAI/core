package com.promethistai.port.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.promethistai.common.DataObject
import java.util.*

data class Message(

    /**
     * Message id.
     */
    var _id: String? = createId(),

    /**
     * Reference id to previous logically preceded message (question).
     */
    var _ref: String? = null,

    /**
     * Each message has none or more items
     */
    var items: MutableList<Item> = mutableListOf(),

    /**
     * Message language.
     */
    var language: Locale = Locale.ENGLISH,

    /**
     * When message was created.
     */
    val datetime: Date? = Date(),

    /**
     * Identification of bot service processing message behind port.
     * Client does not set it (it is determined by platform customer contract).
     * Bot service will put its name there (e.g. helena, illusionist, ...)
     */
    var bot: String? = null,

    /**
     * Application key. Must identify valid app contract.
     */
    var appKey: String? = null,

    /**
     * Sending client identification (determined by client application - e.g. device ID, user auth token etc.)
     */
    var sender: String? = null,

    /**
     * Receiver identification. When message send by client, it is optional (can address specific part of bot
     * service, depending on its type).
     * Bot service is using this to identify client (when not set, it can work as broadcast to all clients
     * (if this will be supported by port in the future).
     */
    var recipient: String? = null,

    /**
     * Conversation session identification. Set by port. todo: control if the session was generated by port and not client
     */
    var sessionId: String? = null,

    /**
     * Identification of the end of session (graph in dialog editor)
     */
    var sessionEnded: Boolean = false,

    /**
     * Expected phrases. It will be provided to Speech-to-text engine as a hint of more probable words
     */
    var expectedPhrases: MutableList<ExpectedPhrase>? = mutableListOf(),

    /**
     * Extension properties for message. Determined by bot service and/or client application.
     */
    val extensions: PropertyMap = PropertyMap()

) {

    @JsonDeserialize(using = PropertyMap.Deserializer::class)
    class PropertyMap : DataObject() {

        class Deserializer : DataObject.Deserializer<PropertyMap>(PropertyMap::class.java) {
            //TODO support for specific data types used in message properties (if needed)
        }

    }

    data class ResourceLink(val type: String? = null, val ref: String? = null)

    data class ExpectedPhrase(val text: String? = null, val boost: Float = 1.0F) // boost can be used in google stt v1p1beta1


    data class Item (
        /**
         * Message text. Can contain specific XML tags (which can be interpered or stripped by channel).
         */
        var text: String? = null,

        /**
         * Ssml text - Google based by default. Can contain specific XML tags
         */
        var ssml: String? = null,

        /**
         * Confidence score. Client usually does not set (if there is human behind ;-)
         */
        var confidence: Double? = 1.0,

        /**
         * Resource links.
         */
        val links: MutableList<ResourceLink> = mutableListOf(),

        /**
         * Extension properties for message. Determined by bot service and/or client application.
         */
        val extensions: PropertyMap = PropertyMap()
    )

    fun response(items: MutableList<Item>, confidence: Double): Message {
        val sender = this.recipient
        val recipient = this.sender
        return this.copy(_id = createId(), _ref = _id, sender = sender, recipient = recipient, items = items, datetime = Date(), sessionId = this.sessionId)
    }

    companion object {

        @JvmStatic
        fun createId(): String {
            return UUID.randomUUID().toString()
        }
    }
}