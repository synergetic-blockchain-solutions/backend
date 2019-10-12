package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class UserReferenceCollectionSerializer(type: Class<List<User>>? = null) : StdSerializer<List<User>>(type) {
    override fun serialize(value: List<User>?, maybeGen: JsonGenerator?, maybeProvider: SerializerProvider?) {
        val users = checkNotNull(value, { "User parameter should not be null" })
        val gen = checkNotNull(maybeGen, { "Generator parameter should not be null" })
        gen.writeObject(users.map { UserReferenceDto.toDto(it) })
    }
}

data class UserReferenceDto(
    val id: Long = 0,
    val name: String,
    val email: String,
    val groups: List<Long> = mutableListOf(),
    val sharedArtifacts: List<Long> = mutableListOf(),
    val ownedArtifacts: List<Long> = mutableListOf(),
    val ownedGroups: List<Long> = mutableListOf(),
    val privateGroup: Long
) {
    companion object {
        fun toDto(user: User): UserReferenceDto {
            return UserReferenceDto(
                id = user.id,
                name = user.name,
                email = user.email,
                groups = user.groups.map(Group::id),
                sharedArtifacts = user.sharedArtifacts.map(Artifact::id),
                ownedArtifacts = user.ownedArtifacts.map(Artifact::id),
                ownedGroups = user.ownedGroups.map(Group::id),
                privateGroup = user.privateGroup.id
            )
        }
    }
}
