package ai.flowstorm.common.repository

import com.amazonaws.services.dynamodbv2.document.*
import org.litote.kmongo.Id
import ai.flowstorm.common.AppConfig
import ai.flowstorm.common.ObjectUtil
import ai.flowstorm.common.config.ConfigValue
import ai.flowstorm.common.model.Entity
import javax.inject.Inject

abstract class DynamoAbstractEntityRepository<E: Entity<E>> : EntityRepository<E> {

    @Inject
    lateinit var database: DynamoDB

    @ConfigValue("name")
    lateinit var instanceName: String

    abstract val tableName: String
    open val table by lazy { database.getTable(tableName(tableName)) }

    protected fun tableName(name: String) = instanceName + "." + name

    override fun create(entity: E): E {
        table.putItem(Item.fromJSON(ObjectUtil.defaultMapper.writeValueAsString(entity)))
        return entity
    }

    override fun update(entity: E, upsert: Boolean): E {
        return create(entity) // it will be updated if the id is the same
    }

    override fun remove(id: Id<E>) {
        table.deleteItem(KeyAttribute("_id", id.toString()))
    }

    inline fun <reified T: E> Item.toEntity(): T = ObjectUtil.defaultMapper.readValue(this.toJSON(), T::class.java)

    inline fun <reified T: E> ItemCollection<*>.toEntityList(): List<T> = map { ObjectUtil.defaultMapper.readValue(it.toJSON(), T::class.java) }
}