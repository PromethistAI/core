package com.promethist.core.resources

import com.promethist.core.model.FileObject
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import java.io.InputStream
import java.io.OutputStream
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(description = "File resource")
@Produces(MediaType.APPLICATION_JSON)
interface FileResource {

    @GET
    @Path("{path: .*}")
    fun readFile(
            @ApiParam(required = true) @PathParam("path") path: String
    ): Response

    // local reading
    fun readFile(path: String, output: OutputStream)

    @GET
    @Path("{path: .*}/_object")
    @Produces(MediaType.APPLICATION_JSON)
    fun getFile(
            @ApiParam(required = true) @PathParam("path") path: String): FileObject

    @POST
    @Path("{path: .*}")
    fun writeFile(
            @ApiParam(required = true) @PathParam("path") path: String,
            @ApiParam(required = true) @HeaderParam("Content-Type") contentType: String,
            @MatrixParam("m") meta: List<String>,
            input: InputStream
    )

    @DELETE
    @Path("{path: .*}")
    fun deleteFile(
            @ApiParam(required = true) @PathParam("path") path: String
    ): Boolean

    @GET
    @Path("provider")
    fun provider(): String
}