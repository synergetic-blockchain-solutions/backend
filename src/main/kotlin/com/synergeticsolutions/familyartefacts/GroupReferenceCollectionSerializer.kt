package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class GroupReferenceCollectionSerializer(type: Class<List<Group>>?) : StdSerializer<List<Group>>(type) {
    override fun serialize(value: List<Group>?, maybeGen: JsonGenerator?, provider: SerializerProvider?) {
        val groups = checkNotNull(value, { "value parameter should not be null" })
        val gen = checkNotNull(maybeGen, { "generator parameter should not be null" })
        gen.writeObject(groups.map { GroupReferenceDto.toDto(it) })
    }
}

data class GroupReferenceDto(
    val id: Long = 0,
    val name: String,
    val description: String,
    val members: List<Long> = listOf(),
    val admins: List<Long> = listOf(),
    val artifacts: List<Long> = listOf()
) {
    companion object {
        fun toDto(group: Group): GroupReferenceDto {
            return GroupReferenceDto(
                id = group.id,
                name = group.name,
                description = group.description,
                members = group.members.map(User::id),
                admins = group.admins.map(User::id),
                artifacts = group.artifacts.map(Artifact::id)
            )
        }
    }
}
