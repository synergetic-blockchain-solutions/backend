package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
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
     * @return Collection of artifacts the user has access to and fit the criteria given by the parameters
     */
    @GetMapping
    fun getArtifacts(
        @RequestParam(name = "group", required = false) groupID: Long?,
        @RequestParam(name = "owner", required = false) ownerID: Long?
    ): ResponseEntity<List<Artifact>> {
        val currentUser = SecurityContextHolder.getContext().authentication
        logger.debug("Filtering artifacts for ${currentUser.principal} by group = $groupID and owner = $ownerID")
        val artifacts =
            artifactService.findArtifactsByOwner(
                email = currentUser.principal as String,
                groupID = groupID,
                ownerID = ownerID
            )
        logger.debug("Found ${artifacts.size} artifacts fitting the criteria")

        return ResponseEntity.ok(artifacts)
    }
}
