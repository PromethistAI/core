package com.promethist.core.resources

import com.promethist.core.model.Application
import com.promethist.core.model.User
import com.promethist.core.type.MutablePropertyMap
import io.swagger.annotations.Api
import io.swagger.annotations.Authorization
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["Content Distribution"], authorizations = [Authorization("Authorization")])
@Path("/contentDistribution")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ContentDistributionResource {

    @POST
    fun resolve(
            contentRequest: ContentRequest
    ): ContentResponse

    data class ContentRequest(
            val sender: String,
            val token: String?,
            val appKey: String,
            val language: String?
    )

    data class ContentResponse(
            val application: Application,
            val user: User,
            val test: Boolean,
            val sessionProperties: MutablePropertyMap
    )
}