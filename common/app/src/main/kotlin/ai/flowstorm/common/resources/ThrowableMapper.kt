package ai.flowstorm.common.resources


import ai.flowstorm.common.config.ConfigValue
import javax.ws.rs.ServerErrorException
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class ThrowableMapper : ExceptionMapper<Throwable> {

    @ConfigValue("name")
    lateinit var instanceName: String

    override fun toResponse(t: Throwable): Response {
        t.printStackTrace()
        val e = if (t is WebApplicationException) t
        else ServerErrorException(t.message, Response.Status.INTERNAL_SERVER_ERROR, t)

        return Response.fromResponse(e.response)
                .entity("${instanceName}:${e::class.java.simpleName}: ${e.message?:""}")
                .type("text/plain")
                .build()
    }
}