package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.Base64Utils
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.Principal
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping(path = ["/artifact/{artifactId}/resource"])
class ArtifactResourceController(
    @Autowired
    val artifactResourceService: ArtifactResourceService
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Retrieve the "metadata" part of a multipart/form-data request.
     *
     * @param[request] HTTP request to retrieve the 'metadata' part from
     * @return [ArtifactResourceMetadata] that was stored in the 'metadata' part
     */
    private fun getMetadataFromRequest(request: HttpServletRequest): ArtifactResourceMetadata {
        val parameters = request.parameterMap
        if (!parameters.containsKey("metadata")) {
            throw MetadataPartRequired()
        }
        val metadataPart = checkNotNull(
            parameters["metadata"]?.firstOrNull(),
            { "Checked that the parameter map contained 'metadata' earlier" }
        )
        return ObjectMapper().registerKotlinModule().readValue(metadataPart)
    }

    /**
     * Retrieve the 'resource' part from a multipart/form-data request.
     *
     * @param[request] HTTP request to retrieve the 'resource' part from
     * @return [Resource] containing the resource and its content type
     */
    private fun getResourceFromRequest(request: HttpServletRequest): Resource {
        val files = (request as StandardMultipartHttpServletRequest).fileMap
        if (!files.containsKey("resource")) {
            throw ResourcePartRequired()
        }

        val resource = checkNotNull(files["resource"]?.bytes, { "Checked that file map contained 'resource' earlier" })
        val contentType =
            checkNotNull(files["resource"], { "Checked that file map contained 'resource' earlier" }).contentType
                ?: throw ResourceContentTypeRequiredException()
        return Resource(contentType, resource)
    }

    /**
     * POST /artifact/{artifactId}/resource
     *
     * Create a new artifact resource associated with [artifactId].
     */
    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createResourceWithMetadata(
        @PathVariable artifactId: Long,
        request: HttpServletRequest,
        principal: Principal
    ): ArtifactResource {
        val resource = getResourceFromRequest(request)
        val metadata = getMetadataFromRequest(request)
        return artifactResourceService.create(
            principal.name, artifactId,
            metadata = metadata,
            contentType = resource.contentType,
            resource = resource.resource
        )
    }

    /**
     * GET /artifact/{artifactId}/resource/{resourceId}/metadata
     *
     * Get the metadata for resource [resourceId] associated with the artifact [artifactId].
     */
    @GetMapping(path = ["/{resourceId}/metadata"], produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getResourceMetadataById(
        @PathVariable artifactId: Long,
        @PathVariable resourceId: Long,
        principal: Principal
    ): ArtifactResourceMetadata {
        return artifactResourceService.findMetadataById(principal.name, artifactId, resourceId)
    }

    /**
     * GET /artifact/artifact{id}/resource/{resourceId}/resource
     *
     * Get the resource for a resource [resourceId] associated with artifact [artifactId]
     */
    @GetMapping(path = ["/{resourceId}/resource"], produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getResourceById(
        @PathVariable artifactId: Long,
        @PathVariable resourceId: Long,
        principal: Principal
    ): ResponseEntity<ByteArrayResource> {
        val resource = artifactResourceService.findResourceById(principal.name, artifactId, resourceId)
        val base64Resource = Base64Utils.encode(resource.resource)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(resource.contentType))
            .body(ByteArrayResource(base64Resource))
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
        request: HttpServletRequest,
        principal: Principal
    ): ArtifactResource {
        logger.info("Updating metadata and resource of resource $resourceId")
        val metadata = getMetadataFromRequest(request)
        val resource = getResourceFromRequest(request)
        return artifactResourceService.update(
            principal.name,
            artifactId,
            resourceId,
            metadata = metadata,
            contentType = resource.contentType,
            resource = resource.resource
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
        return artifactResourceService.update(
            principal.name,
            artifactId,
            resourceId,
            metadata = metadata
        )
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
        @RequestBody resource: ByteArray,
        principal: Principal
    ): ArtifactResource {
        logger.info("Updating resource of resource $resourceId")
        return artifactResourceService.update(
            principal.name,
            artifactId,
            resourceId,
            resource = resource,
            contentType = contentType
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

/**
 * Convert an [InputStream] to a [ByteArray].
 *
 * @receiver [InputStream] to be converted.
 * @return [ByteArray] of the input stream
 */
private fun InputStream.toByteArray(): ByteArray {
    val outputStream = ByteArrayOutputStream()
    this.use { input ->
        outputStream.use { output ->
            input.copyTo(output)
        }
    }
    return outputStream.toByteArray()
}
