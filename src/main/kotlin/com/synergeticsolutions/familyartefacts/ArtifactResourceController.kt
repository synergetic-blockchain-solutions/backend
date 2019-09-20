package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.security.Principal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping(path = ["/artifact/{artifactId}/resource"])
class ArtifactResourceController(
    @Autowired
    val artifactResourceService: ArtifactResourceService
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * POST /artifact/{artifactId}/resource
     *
     * Create a new artifact resource associated with [artifactId].
     */
    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createResourceWithMetadata(
        @PathVariable artifactId: Long,
        @RequestPart("metadata") metadata: ByteArray,
        @RequestPart("resource") resource: ByteArray,
        principal: Principal
    ): ArtifactResource {
        val resourceMetadata = ObjectMapper().registerKotlinModule().readValue<ArtifactResourceMetadata>(metadata)
        return artifactResourceService.create(
            principal.name, artifactId,
            metadata = resourceMetadata,
            resource = Resource(
                contentType = "",
                resource = resource
            )
        )
    }

    /**
     * GET /artifact/{artifactId}/resource/{resourceId}
     *
     * Get the resource [resourceId] associated with the artifact [artifactId].
     */
    @GetMapping(path = ["/{resourceId}"], produces = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun getResourceById(
        @PathVariable artifactId: Long,
        @PathVariable resourceId: Long,
        @RequestParam(name = "metadata", required = false, defaultValue = "true") includeMetadata: Boolean,
        @RequestParam(name = "resource", required = false, defaultValue = "true") includeResource: Boolean,
        principal: Principal
    ): MultiValueMap<String, Any> {
        if (!(includeMetadata || includeResource)) {
            throw InvalidRequestParamCombinationException("Must include at least one of metadata or resource")
        }

        val response = LinkedMultiValueMap<String, Any>()

        if (includeMetadata) {
            logger.debug("Including metadata in response")
            val metadata = artifactResourceService.findMetadataById(principal.name, artifactId, resourceId)
            val metadataHeaders = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON_UTF8 }
            val metadataHttpEntity = HttpEntity(metadata, metadataHeaders)
            response.add("metadata", metadataHttpEntity)
        }

        if (includeResource) {
            logger.debug("Including resource in response")
            val resource = artifactResourceService.findResourceById(principal.name, artifactId, resourceId)
            val associatedContentType =
                artifactResourceService.findContactTypeById(principal.name, artifactId, resourceId)
            val resourceHeaders = HttpHeaders().apply { contentType = MediaType.parseMediaType(associatedContentType) }
            val resourceHttpEntity = HttpEntity(resource, resourceHeaders)
            response.add("resource", resourceHttpEntity)
        }

        return response
    }

    /**
     * PUT /artifact/{artifactId}/resource/{resourceId}
     *
     * Update both the metadata and resource of an artifact resource.
     */
    @PutMapping(path = ["/{resourceId}"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateResourceAndMetadata(
        @PathVariable artifactId: Long,
        @PathVariable resourceId: Long,
        @RequestParam("metadata") metadata: ByteArray,
        @RequestParam("resource") resource: MultipartFile,
        principal: Principal
    ): ArtifactResource {
        logger.info("Updating metadata and resource of resource $resourceId")
        val resourceMetadata = ObjectMapper().registerKotlinModule().readValue<ArtifactResourceMetadata>(metadata)
        return artifactResourceService.update(
            principal.name,
            artifactId,
            resourceId,
            metadata = resourceMetadata,
            resource = Resource(
                contentType = resource.contentType ?: throw ResourceContentTypeRequiredException(),
                resource = resource.bytes
            )
        )
    }

    /**
     * PUT /artifact/{artifactId}/resource/{resourceId}
     *
     * Update the artifact resource's metadata.
     */
    @PutMapping(path = ["/{resourceId}"], consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun updateResourceMetadata(
        @PathVariable artifactId: Long,
        @PathVariable resourceId: Long,
        @RequestBody metadata: ArtifactResourceMetadata,
        principal: Principal
    ): ArtifactResource {
        logger.info("Updating metadata of resource $resourceId")
        return artifactResourceService.update(principal.name, artifactId, resourceId, metadata = metadata)
    }

    /**
     * PUT /artifact/{artifactId}/resource/{resourceId}
     *
     * Update the artifact resource's resource.
     */
    @PutMapping(path = ["/{resourceId}"])
    fun updateResource(
        @PathVariable artifactId: Long,
        @PathVariable resourceId: Long,
        @RequestHeader(HttpHeaders.CONTENT_TYPE) contentType: String,
        @RequestBody resource: ByteArrayResource,
        principal: Principal
    ): ArtifactResource {
        logger.info("Updating resource of resource $resourceId")
        return artifactResourceService.update(
            principal.name,
            artifactId,
            resourceId,
            resource = Resource(contentType = contentType, resource = resource.byteArray)
        )
    }

    /**
     * DELETE /artifact/{artifactId}/resource/{resourceId}
     *
     * Delete the resource [resourceId] associated with artifact [artifactId]
     */
    @DeleteMapping(path = ["/{resourceId}"])
    fun deleteResourceById(@PathVariable artifactId: Long, @PathVariable resourceId: Long, principal: Principal): ArtifactResourceMetadata =
        artifactResourceService.delete(principal.name, artifactId, resourceId)
}
