package com.synergeticsolutions.familyartefacts.dtos

/**
 * A resource can be any sort of image. Its purpose is to encapsulate the association of a collection of bytes
 * with a content type.
 */
data class Resource(val contentType: String, val resource: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Resource

        if (contentType != other.contentType) return false
        if (!resource.contentEquals(other.resource)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentType.hashCode()
        result = 31 * result + resource.contentHashCode()
        return result
    }
}
