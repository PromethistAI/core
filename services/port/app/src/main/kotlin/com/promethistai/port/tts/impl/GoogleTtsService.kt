package com.promethistai.port.tts.impl

import com.google.cloud.texttospeech.v1beta1.*
import com.promethistai.port.tts.TtsService
import com.promethistai.port.tts.TtsVoice
import java.util.concurrent.TimeUnit

class GoogleTtsService : TtsService {

    private val client = TextToSpeechClient.create()

    override val voices: List<TtsVoice>
        get() {
            val request = ListVoicesRequest.getDefaultInstance()
            val voices = mutableListOf<com.promethistai.port.tts.TtsVoice>()
            for (voice in client.listVoices(request).voicesList)
                voices.add(
                    TtsVoice(voice.name, voice.ssmlGender.name,
                        if (voice.languageCodesCount > 0) voice.getLanguageCodes(0) else "undef"))
            return voices
        }

    override fun speak(text: String, voiceName: String, language: String): ByteArray {
        // Set the text input to be synthesized
        val input = SynthesisInput.newBuilder().setText(text).build()

        // Build the voice request, select the language code ("en-US") and the ssml voice gender ("neutral")
        val voice = VoiceSelectionParams.newBuilder()
                .setName(voiceName)
                .setLanguageCode(language)
                //.setSsmlGender(SsmlVoiceGender.MALE)
                .build()

        // Select the type of stream file you want returned
        val audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build()

        // Perform the text-to-speech request on the text input with the selected voice parameters and stream file type
        val response = client.synthesizeSpeech(input, voice, audioConfig)

        // Get the stream contents from the response
        val audioContents = response.audioContent

        return audioContents.toByteArray()
    }

    override fun close() {
        try {
            client.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        client.close()
    }

}