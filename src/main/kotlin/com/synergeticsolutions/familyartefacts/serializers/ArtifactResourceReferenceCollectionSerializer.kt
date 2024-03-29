package com.synergeticsolutions.familyartefacts.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.synergeticsolutions.familyartefacts.entities.ArtifactResource
/**
 * Serialise collections of [ArtifactResource] entities.
 *
 * This serializer is intended to be used as the custom serialiser for [ArtifactResource] collections that are properties of
 * another entity. The idea is to provide a good depth of serialisation with having huge JSON documents or recursive
 * structures.
 */
class ArtifactResourceReferenceCollectionSerializer(resources: Class<List<ArtifactResource>>? = null) : StdSerializer<List<ArtifactResource>>(resources) {
    override fun serialize(value: List<ArtifactResource>?, maybeGen: JsonGenerator?, provider: SerializerProvider?) {
        val resources = checkNotNull(value, { "value parameter should not be null" })
        val gen = checkNotNull(maybeGen, { "generator parameter should not be null" })
        gen.writeObject(resources.map {
            ArtifactResourceReferenceDto.toDto(
                it
            )
        })
    }
}

data class ArtifactResourceReferenceDto(
    val id: Long = 0,
    val name: String,
    val description: String,
    val contentType: String,
    val artifact: Long,
    val tags: MutableList<String> = mutableListOf()
) {
    companion object {
        fun toDto(resource: ArtifactResource): ArtifactResourceReferenceDto {
            return ArtifactResourceReferenceDto(
                id = resource.id,
                name = resource.name,
                description = resource.description,
                contentType = resource.contentType,
                artifact = resource.artifact.id
            )
        }
    }
}
