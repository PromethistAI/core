package com.promethist.client.standalone.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.type.TypeReference
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.PinPullResistance
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import com.promethist.client.BotClient
import com.promethist.client.BotConfig
import com.promethist.client.BotContext
import com.promethist.client.client.JwsBotClientSocket
import com.promethist.client.audio.WavFileAudioRecorder
import com.promethist.client.common.OkHttp3BotClientSocket
import com.promethist.client.signal.SignalGroup
import com.promethist.client.signal.SignalProvider
import com.promethist.client.standalone.Application
import com.promethist.client.standalone.DeviceClientCallback
import com.promethist.client.standalone.io.*
import com.promethist.client.standalone.ui.Screen
import com.promethist.client.util.InetInterface
import com.promethist.common.AppConfig
import com.promethist.common.ObjectUtil.defaultMapper
import com.promethist.common.ServiceUrlResolver
import com.promethist.core.model.SttConfig
import com.promethist.core.model.Voice
import com.promethist.core.type.Dynamic
import com.promethist.core.type.PropertyMap
import cz.alry.jcommander.CommandRunner
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.*
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class ClientCommand: CommandRunner<Application.Config, ClientCommand.Config> {

    enum class BotSocketType { OkHttp3, JWS }

    @Parameters(commandNames = ["client"], commandDescription = "Run client (press Ctrl+C to quit)")
    class Config : ClientConfig() {

        @Parameter(names = ["-c", "--configFile"], order = 0, description = "Config file")
        var configFile: String? = null

        @Parameter(names = ["-d", "--device"], order = 1, description = "Device type (e.g. desktop, rpi)")
        var device = "desktop"

        @Parameter(names = ["-e", "--environment"], order = 2, description = "Environment (develop, preview) - this superseeds -u value")
        var environment: String? = null

        @Parameter(names = ["-nc", "--noCache"], order = 3, description = "Do not cache anything")
        var noCache = false

        @Parameter(names = ["-s", "--sender"], order = 4, description = "Sender identification")
        var sender = "standalone_" + (InetInterface.getActive()?.hardwareAddress?.replace(":", "") ?: "default")

        @Parameter(names = ["-it", "--introText"], order = 5, description = "Intro text")
        var introText: String? = null

        @Parameter(names = ["-as", "--autoStart"], order = 6, description = "Start conversation automatically")
        var autoStart = false

        @Parameter(names = ["-ex", "--exitOnError"], order = 7, description = "Raise exceptions")
        var exitOnError = false

        @Parameter(names = ["-nol", "--noOutputLogs"], order = 8, description = "No output logs")
        var noOutputLogs = false

        @Parameter(names = ["-log", "--showLogs"], order = 9, description = "Show contextual logs")
        var showLogs = false

        // audio

        @Parameter(names = ["-stt", "--sttMode"], order = 30, description = "STT mode (Default, SingleUtterance, Duplex)")
        var sttMode = SttConfig.Mode.SingleUtterance

        @Parameter(names = ["-v", "--voice"], order = 31, description = "TTS voice")
        var voice: Voice? = null

        @Parameter(names = ["-pn", "--portName"], order = 32, description = "Audio output port name")
        var portName: String = "SPEAKER"

        @Parameter(names = ["-vo", "--volume"], order = 33, description = "Audio output volume")
        var volume: Int? = null

        @Parameter(names = ["-nia", "--noInputAudio"], order = 34, description = "No input audio (text input only)")
        var noInputAudio = false

        @Parameter(names = ["-noa", "--noOutputAudio"], order = 35, description = "No output audio (text output only)")
        var noOutputAudio = false

        @Parameter(names = ["-aru", "--audioRecordUpload"], order = 36, description = "Audio record with upload (none, local, night, immediate)")
        var audioRecordUpload = WavFileAudioRecorder.UploadMode.none

        @Parameter(names = ["-pm", "--pauseMode"], order = 37, description = "Pause mode (wake word or button will pause output audio instead of stopping it and listening)")
        var pauseMode = false

        // GUI

        @Parameter(names = ["-scr", "--screen"], order = 40, description = "Screen view (none, window, fullscreen)")
        var screen = "none"

        @Parameter(names = ["-nan", "--noAnimations"], order = 41, description = "No animations")
        var noAnimations = false

        // networking

        @Parameter(names = ["-sp", "--socketPing"], order = 80, description = "Socket ping period (in seconds, 0 = do not ping)")
        var socketPing = 10L

        @Parameter(names = ["-st", "--socketType"], order = 81, description = "Socket implementation type (okhttp3, jetty, jws)")
        var socketType = ClientCommand.BotSocketType.OkHttp3

        @Parameter(names = ["-aa", "--autoUpdate"], order = 82, description = "Auto update JAR file")
        var autoUpdate = false

        @Parameter(names = ["-du", "--distUrl"], order = 83, description = "Distribution URL for auto updates")
        var distUrl = "https://repository.promethist.ai/dist"
    }

    override fun run(globalConfig: Application.Config, config: Config) {
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.toLevel(globalConfig.logLevel)
        val writer = OutputStreamWriter(
            if (config.output == "stdout")
                System.out
            else
                FileOutputStream(config.output)
        )
        if (config.configFile != null) {
            FileInputStream(config.configFile).use {
                defaultMapper.readerForUpdating(config).readValue(JsonFactory().createParser(it), object : TypeReference<Config>() {})
            }
            println("{Using configuration ${config.configFile}}")
        }

        if (config.volume != null) {
            OutputAudioDevice.volume(config.portName, config.volume!!)
            writer.write("{Volume ${config.portName} set to ${config.volume}}\n")
        }
        var light: Light? = null
        val output = PrintWriter(writer, true)
        var responded = false
        val attributes = Dynamic(
                "clientType" to "standalone:${AppConfig.version}",
                "clientScreen" to (config.screen != "none")
        )
        val context = BotContext(
                url = if (config.environment != null) {
                    val env = if (listOf("production", "default").contains(config!!.environment))
                        ""
                    else
                        ".${config.environment}"
                    if (config.environment == "local")
                        "http://localhost:8080" else
                        "https://port$env.promethist.com"
                } else
                    config.url,
                key = config.key,
                sender = config.sender,
                voice = config.voice,
                autoStart = config.autoStart,
                locale = Locale(config.language, Locale.getDefault().country),
                attributes = attributes
        )
        val callback = object : DeviceClientCallback(
                output,
                config.distUrl,
                config.autoUpdate,
                config.noCache,
                config.noOutputAudio,
                config.noOutputLogs,
                config.portName,
                logs = config.showLogs
        ) {
            override fun onBotStateChange(client: BotClient, newState: BotClient.State) {
                super.onBotStateChange(client, newState)
                config.signalProcessor?.apply {
                    if (context.sessionId != null) {
                        if (pause())
                            println("{Signal processor paused}")
                    } else if (unpause())
                        println("{Signal processor unpaused}")
                }
                when (newState) {
                    BotClient.State.Listening ->
                        if (light is ColorLight)
                            (light as ColorLight)?.set(Color.GREEN)
                        else
                            light?.high()
                    BotClient.State.Processing ->
                        if (light is ColorLight)
                            (light as ColorLight)?.set(Color.BLUE)
                    BotClient.State.Failed ->
                        if (light is ColorLight)
                            (light as ColorLight)?.set(Color.RED)
                    BotClient.State.Open -> {}
                    else ->
                        light?.low()
                }
            }

            override fun onReady(client: BotClient) {
                super.onReady(client)
                light?.apply {
                    blink(0)
                    low()
                }
            }

            override fun text(client: BotClient, text: String) {
                super.text(client, text)
                responded = true
            }

            override fun onFailure(client: BotClient, t: Throwable) {
                super.onFailure(client, t)
                responded = true
            }
        }
        if (config.introText != null)
            context.introText = config.introText!!
        val micChannel = config.micChannel.split(':').map { it.toInt() }
        val speechDevice = SpeechDeviceFactory.getSpeechDevice(config.speechDevice)
        val client = BotClient(
                context,
                when (config.socketType) {
                    BotSocketType.JWS -> JwsBotClientSocket(context.url, config.exitOnError, config.socketPing)
                    else -> OkHttp3BotClientSocket(context.url, config.exitOnError, config.socketPing)
                },
                if (config.noInputAudio)
                    null
                else
                    Microphone(speechDevice, config.wakeWord, micChannel[0], micChannel[1]),
                callback,
                if (config.noOutputAudio)
                    BotConfig.TtsType.None
                else
                    BotConfig.TtsType.RequiredLinks,
                config.sttMode,
                config.pauseMode,
                if (config.noInputAudio || (config.audioRecordUpload == WavFileAudioRecorder.UploadMode.none))
                    null
                else
                    WavFileAudioRecorder(File("."),
                            if (context.url.startsWith("http://localhost"))
                                ServiceUrlResolver.getEndpointUrl("filestore", ServiceUrlResolver.RunMode.local)
                            else
                                context.url.replace("port", "filestore"),
                            config.audioRecordUpload
                    )
        )
        if (config.screen != "none") {
            Screen.client = client
            Screen.fullScreen = (config.screen == "fullscreen")
            Screen.animations = !config.noAnimations
            thread {
                Screen.launch()
            }
        }
        config.signalProcessor?.apply {
            println("{enabling signal processor}")
            emitter = { signalGroup: SignalGroup, values: PropertyMap ->
                context.attributes.putAll(values)
                when (signalGroup.type) {
                    SignalGroup.Type.Text ->
                        if (client.state == BotClient.State.Sleeping) {
                            println("{Signal text '${signalGroup.name}' values $values}")
                            client.doText(signalGroup.name)
                        }
                    SignalGroup.Type.Touch ->
                        client.touch()
                }
            }
            if (speechDevice is SignalProvider)
                providers.add(speechDevice)
            run()
        }

        println("{context = $context}")
        println("{inputAudioDevice = ${client.inputAudioDevice}}")
        println("{sttMode = ${client.sttMode}}")
        println("{device = ${config.device}}")
        if (listOf("rpi", "model1", "model2", "model3").contains(config.device)) {
            val gpio = GpioFactory.getInstance()
            light = if (config.device == "model2")
                Vk2ColorLed().apply {
                    set(Color.MAGENTA)
                }
            else
                BinLed(gpio).apply {
                    blink(500)
                }
            val button = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_DOWN)
            button.setShutdownOptions(true)
            button.addListener(GpioPinListenerDigital { event ->
                when (event.state) {
                    PinState.LOW -> {
                        light?.high()
                        client.touch()
                    }
                    PinState.HIGH -> {
                        light?.low()
                    }
                }
            })
        }
        client.open()
        if (config.input != "none") {
            InputStreamReader(
                if (config.input == "stdin")
                    System.`in`
                else
                    FileInputStream(config.input)
            ).use {
                val input = BufferedReader(it)
                while (true) {
                    val text = input.readLine()!!.trim()
                    client.outputQueue.clear()
                    when (text) {
                        "" -> {
                            println("[Click when ${client.state}]")
                            client.touch()
                        }
                        "exit", "quit" -> {
                            exitProcess(0)
                        }
                        else -> {
                            if (text.startsWith("audio:"))
                                client.socket.sendAudioData(File(text.substring(6)).readBytes())
                            else if (client.state == BotClient.State.Responding) {
                                responded = false
                                client.doText(text)
                            }
                        }
                    }
                    while (!responded && client.state != BotClient.State.Failed) {
                        Thread.sleep(50)
                    }
                }
            }
        }
    }
}