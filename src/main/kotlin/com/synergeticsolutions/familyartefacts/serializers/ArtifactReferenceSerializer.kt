package com.synergeticsolutions.familyartefacts.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.synergeticsolutions.familyartefacts.entities.Artifact

/**
 * Serialiser of [Artifact] entities.
 *
 * This serializer is intended to be used as the custom serialiser for [Artifacts] properties of
 * another entity. The idea is to provide a good depth of serialisation with having huge JSON documents or recursive
 * structures.
 */
class ArtifactReferenceSerializer(type: Class<Artifact>? = null) : StdSerializer<Artifact>(type) {
    override fun serialize(value: Artifact?, maybeGen: JsonGenerator?, provider: SerializerProvider?) {
        val artifact = checkNotNull(value, { "value parameter should not be null" })
        val gen = checkNotNull(maybeGen, { "generator parameter should not be null" })
        gen.writeObject(
            ArtifactReferenceDto.toDto(
                artifact
            )
        )
    }
}
