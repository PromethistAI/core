package ai.flowstorm.common.resources

import io.swagger.annotations.Api
import ai.flowstorm.common.AppConfig
import ai.flowstorm.common.config.Config
import ai.flowstorm.common.config.ConfigValue
import java.io.InputStream
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(hidden = true)
@Path("/")
class SwaggerResource {

    @ConfigValue("name")
    lateinit var instanceName: String

    @GET
    @Path("/swagger.json")
    fun getJsonFile(): Response {
        return Response.ok(getSwaggerResourceStream("json"), MediaType.APPLICATION_JSON).build()
    }

    @GET
    @Path("/swagger.yaml")
    fun getYamlFile(): Response {
        return Response.ok(getSwaggerResourceStream("yaml"), "application/yaml").build()
    }

    /**
     * Searches for swagger file resource in classpath, in /META_INF/${AppConfig.instance["name"]}-api/swagger.$format first,
     * /swagger.$format aftewards.
     *
     * @param format file format (json or yaml)
     * @throws WebApplicationException 404 if resource file not found
     */
    private fun getSwaggerResourceStream(format: String): InputStream {
        val name = "swagger.${format}"
        val defaultLocation = "/META-INF/${instanceName}-api/"
        for (path in arrayOf(defaultLocation, "/")) {
            val resourceStream = this::class.java.getResourceAsStream("${path}${name}")
            if (resourceStream != null)
                return resourceStream
        }
        throw WebApplicationException("Resource file $name not found (should be placed in $defaultLocation)", Response.Status.NOT_FOUND)
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SwaggerResource().getSwaggerResourceStream("json")
        }
    }
}