package com.synergeticsolutions.familyartefacts.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.synergeticsolutions.familyartefacts.entities.Artifact
import com.synergeticsolutions.familyartefacts.entities.ArtifactResource
import com.synergeticsolutions.familyartefacts.entities.Group
import com.synergeticsolutions.familyartefacts.entities.User

/**
 * Serialise collections of [Artifact] entities.
 *
 * This serializer is intended to be used as the custom serialiser for [Artifact] collections that are properties of
 * another entity. The idea is to provide a good depth of serialisation with having huge JSON documents or recursive
 * structures.
 */
class ArtifactReferenceCollectionSerializer(type: Class<List<Artifact>>? = null) : StdSerializer<List<Artifact>>(type) {
    override fun serialize(value: List<Artifact>?, maybeGen: JsonGenerator?, provider: SerializerProvider?) {
        val artifacts = checkNotNull(value, { "value parameter should not be null" })
        val gen = checkNotNull(maybeGen, { "generator parameter should not be null" })
        gen.writeObject(artifacts.map {
            ArtifactReferenceDto.toDto(
                it
            )
        })
    }
}

data class ArtifactReferenceDto(
    val id: Long,
    val name: String,
    val description: String,
    val owners: List<Long>,
    val groups: List<Long>,
    val sharedWith: List<Long> = listOf(),
    val resources: List<Long> = listOf(),
    val tags: List<String> = listOf()
) {
    companion object {
        fun toDto(artifact: Artifact): ArtifactReferenceDto {
            return ArtifactReferenceDto(
                id = artifact.id,
                name = artifact.name,
                description = artifact.description,
                owners = artifact.owners.map(User::id),
                groups = artifact.groups.map(Group::id),
                sharedWith = artifact.sharedWith.map(User::id),
                resources = artifact.resources.map(ArtifactResource::id),
                tags = artifact.tags
            )
        }
    }
}
