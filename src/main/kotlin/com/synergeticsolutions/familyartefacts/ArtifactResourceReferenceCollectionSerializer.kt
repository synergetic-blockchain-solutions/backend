package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class ArtifactResourceReferenceCollectionSerializer(resources: Class<List<ArtifactResource>>?) : StdSerializer<List<ArtifactResource>>(resources) {
    override fun serialize(value: List<ArtifactResource>?, maybeGen: JsonGenerator?, provider: SerializerProvider?) {
        val resources = checkNotNull(value, { "value parameter should not be null" })
        val gen = checkNotNull(maybeGen, { "generator parameter should not be null" })
        gen.writeObject(resources.map { ArtifactResourceReferenceDto.toDto(it) })
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
                artifact = resource.artifact.id,
                tags = resource.tags
            )
        }
    }
}
