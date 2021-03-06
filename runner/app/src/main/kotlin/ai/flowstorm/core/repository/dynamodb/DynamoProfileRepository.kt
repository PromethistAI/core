@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package ai.flowstorm.core.repository.dynamodb

import com.amazonaws.services.dynamodbv2.document.KeyAttribute
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.document.utils.NameMap
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.litote.kmongo.*
import ai.flowstorm.common.query.DynamoDbFiltersFactory
import ai.flowstorm.common.query.Query
import ai.flowstorm.common.repository.DynamoAbstractEntityRepository
import ai.flowstorm.core.model.*
import ai.flowstorm.core.repository.ProfileRepository

class DynamoProfileRepository : DynamoAbstractEntityRepository<Profile>(), ProfileRepository {

    override val tableName = "profile"

    override fun findBy(userId: Id<User>, spaceId: Id<Space>): Profile? {
        val spec = QuerySpec()
            .withKeyConditionExpression("#userid = :value")
            .withFilterExpression("#spaceid = :value2")
            .withNameMap(NameMap()
                .with("#userid", "user_id")
                .with("#spaceid", "space_id"))
            .withValueMap(ValueMap()
                .withString(":value", userId.toString())
                .withString(":value2", spaceId.toString())
            )
        return table.getIndex("user_id").query(spec).toEntityList<Profile>().singleOrNull()
    }

    override fun find(id: Id<Profile>): Profile? {
        return table.getItem(KeyAttribute("_id", id.toString()))?.toEntity()
    }

    override fun find(query: Query): List<Profile> {
        val spec = QuerySpec()
        val (filterExpression, keywordExpression, nameMap, valueMap) = DynamoDbFiltersFactory.createFilters(query, indexValues=mutableListOf("space_id"))

        filterExpression.ifNotEmpty { spec.withFilterExpression(this.joinToString(separator = " and ")) }
        keywordExpression.ifNotEmpty { spec.withKeyConditionExpression(this.joinToString(separator = " and ")) }
        spec.withNameMap(nameMap)
        spec.withValueMap(valueMap)
        spec.withMaxResultSize(query.limit)
        spec.withScanIndexForward(false)

        return table.getIndex("space_id").query(spec).toEntityList()
    }

    override fun all(): List<Profile> = table.scan().toEntityList()

}