package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class ArtifactReferenceSerializer(type: Class<Artifact>? = null) : StdSerializer<Artifact>(type) {
    override fun serialize(value: Artifact?, maybeGen: JsonGenerator?, provider: SerializerProvider?) {
        val artifact = checkNotNull(value, { "value parameter should not be null" })
        val gen = checkNotNull(maybeGen, { "generator parameter should not be null" })
        gen.writeObject(ArtifactReferenceDto.toDto(artifact))
    }
}
