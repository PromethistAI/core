package com.promethistai.port.bot

import com.promethistai.common.AppConfig
import com.promethistai.common.RestClient
import com.promethistai.port.DataService
import com.promethistai.port.model.Message
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

class IllusionistService : BotService {

    data class Response(var answer: String = "", var confidence: Double = 1.0)

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var dataService: DataService

    private var logger = LoggerFactory.getLogger(IllusionistService::class.java)

    override fun message(key: String, message: Message): Message? {
        try {
            val contract = dataService.getContract(key)
            val botKey = contract.botKey?:key
            val model = contract.model?:"GlobalRepeat1"
            val remoteEndpoint = """https://illusionist.${appConfig["namespace"]}.promethist.ai/query"""
            val url = URL("""${remoteEndpoint}/${model}?key=${botKey}&query=${URLEncoder.encode(message.text, "utf-8")}""")
            if (logger.isInfoEnabled)
                logger.info("remoteEndpoint = $remoteEndpoint, botKey = $botKey, model = $model")
            val responses = RestClient.call(url, Array<Response>::class.java, "POST")
            if (responses.isNotEmpty())
                return message.response(responses[0].answer, responses[0].confidence)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

}