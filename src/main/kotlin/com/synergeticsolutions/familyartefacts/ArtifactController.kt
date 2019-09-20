package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/artifact"])
class ArtifactController(
    @Autowired
    val artifactService: ArtifactService
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * GET /artifact
     *
     * @param groupID ID of the group to restrict artifacts to
     * @param ownerID ID of the owner to restrict artifacts to
     * @param sharedID ID of the user the returned artifacts must be shared to
     * @return Collection of artifacts the user has access to and fit the criteria given by the parameters
     */
    @GetMapping
    fun getArtifacts(
        @RequestParam(name = "group", required = false) groupID: Long?,
        @RequestParam(name = "owner", required = false) ownerID: Long?,
        @RequestParam(name = "shared", required = false) sharedID: Long?
    ): ResponseEntity<List<Artifact>> {
        val currentUser = SecurityContextHolder.getContext().authentication
        logger.debug("Filtering artifacts for ${currentUser.principal} by group=$groupID, owner=$ownerID, shared=$sharedID")
        val artifacts =
            artifactService.findArtifactsByOwner(
                email = currentUser.principal as String,
                groupID = groupID,
                ownerID = ownerID,
                sharedID = sharedID
            )
        logger.debug("Found ${artifacts.size} artifacts fitting the criteria")

        return ResponseEntity.ok(artifacts)
    }

    @GetMapping(path = ["/{id}"])
    fun getArtifactById(@PathVariable id: Long): ResponseEntity<Artifact> {
        val currentUser = SecurityContextHolder.getContext().authentication
        logger.debug("Getting artifact $id for user ${currentUser.principal}")
        val artifact = artifactService.findArtifactById(currentUser.principal as String, id)
        logger.debug("Found $artifact")
        return ResponseEntity.ok(artifact)
    }

    /**
     * POST /artifact
     *
     * Create an artifact based on the [ArtifactRequest] submitted as the body. In addition to the specified
     * [ArtifactRequest.owners], the creating user will be made an owner. In addition to the specified
     * [ArtifactRequest.groups], the creating user's private group will be associated with the artifact.
     *
     * @return [Artifact] representing the created artifact
     */
    @PostMapping
    fun createArtifact(@RequestBody newArtifact: ArtifactRequest): ResponseEntity<Artifact> {
        val currentUser = SecurityContextHolder.getContext().authentication
        val createdArtifact = artifactService.createArtifact(
            email = currentUser.principal as String,
            name = newArtifact.name,
            description = newArtifact.description,
            ownerIDs = newArtifact.owners ?: listOf(),
            groupIDs = newArtifact.groups ?: listOf(),
            sharedWith = newArtifact.sharedWith ?: listOf(),
            resources = newArtifact.resources ?: listOf()
        )
        logger.info("Created artifact $createdArtifact")
        return ResponseEntity.status(HttpStatus.CREATED).body(createdArtifact)
    }

    /**
     * PUT /artifact/{id}
     *
     * Update the artifact with [id]. Generally modification of artifacts is only available to users who are owners of
     * the artifact. However, if the user is the owner a group the artifact is associated with, they can remove that
     * group from the collection of associated groups.
     *
     * @return [Artifact] representing the updated artifact
     */
    @PutMapping(path = ["/{id}"])
    fun updateArtifact(@PathVariable id: Long, @RequestBody artifact: ArtifactRequest): ResponseEntity<Artifact> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val updatedArtifact = artifactService.updateArtifact(currentUser.principal as String, id, artifact)
        logger.info("Updated artifact $updatedArtifact")
        return ResponseEntity.ok(updatedArtifact)
    }

    /**
     * DELETE /artifact/{id}
     *
     * Delete the artifact with [id]. This endpoint is only usable by users who are owners of the artifact.
     *
     * @return [Artifact] representing the deleted artifact
     */
    @DeleteMapping(path = ["/{id}"])
    fun deleteArtifact(@PathVariable id: Long): ResponseEntity<Artifact> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val deletedArtifact = artifactService.deleteArtifact(currentUser.principal as String, id)
        logger.info("Deleted artifact $deletedArtifact")
        return ResponseEntity.ok(deletedArtifact)
    }
}

/**
 * [ArtifactRequest] represents a request to create an artifact.
 *
 * @param [name] Name of the artifact
 * @param [description] Description of the artifact
 * @param [owners] User IDs of the users to be made owners of the artifact
 * @param [groups] Group IDs of the groups to be associated with the artifact
 * @param [sharedWith] User IDs of the users to share the artifact with
 */
data class ArtifactRequest(
    val name: String,
    val description: String,
    val owners: List<Long>?,
    val groups: List<Long>?,
    val sharedWith: List<Long>?,
    val resources: List<Long>? = listOf()
)
