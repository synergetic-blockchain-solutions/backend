package com.synergeticsolutions.familyartefacts.controllers

import com.synergeticsolutions.familyartefacts.entities.Album
import com.synergeticsolutions.familyartefacts.exceptions.NoAuthenticationException
import com.synergeticsolutions.familyartefacts.services.AlbumService
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

/**
 * Controller for managing requests related to the [Album] entity.
 */
@RestController
@RequestMapping(path = ["/album"])
class AlbumController(
    @Autowired
    val albumService: AlbumService
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * GET /album
     *
     * Retrieves all the albums a user has access to. The results can optionally be filtered by [albumName], [groupID],
     * [ownerID], and [sharedID]. [albumName] only needs to match the first part of the actual album's name.
     * @param albumName name of the album
     * @param groupID ID of the group the album is in
     * @param ownerID ID of the album owner
     * @param sharedID ID of the people the album is shared with
     * @return List of albums the user has access to and fit the criteria given by the parameters
     */
    @GetMapping
    fun getAlbums(
        @RequestParam(name = "name", required = false) albumName: String?,
        @RequestParam(name = "group", required = false) groupID: Long?,
        @RequestParam(name = "owner", required = false) ownerID: Long?,
        @RequestParam(name = "shared", required = false) sharedID: Long?
    ): ResponseEntity<List<Album>> {
        val currentUser = SecurityContextHolder.getContext().authentication
        logger.debug("Filtering albums for ${currentUser.principal} by group=$groupID, owner=$ownerID, shared=$sharedID")
        val albums =
                albumService.findAlbums(
                        email = currentUser.principal as String,
                        albumName = albumName,
                        groupID = groupID,
                        ownerID = ownerID,
                        sharedID = sharedID
                )
        logger.debug("Found ${albums.size} albums fitting the criteria")

        return ResponseEntity.ok(albums)
    }

    /**
     * GET /album/{id}
     *
     * @param id ID of the album to get
     * @return The album with the requested ID, if the user has access to that album
     */
    @GetMapping(path = ["/{id}"])
    fun getAlbumById(@PathVariable id: Long): ResponseEntity<Album> {
        val currentUser = SecurityContextHolder.getContext().authentication
        logger.debug("Getting album $id for user ${currentUser.principal}")
        val album = albumService.findAlbumById(currentUser.principal as String, id)
        logger.debug("Found $album")
        return ResponseEntity.ok(album)
    }

    /**
     * POST /album
     *
     * @param newAlbum Details of the album to be created
     * @return [Album] representing the created album
     */
    @PostMapping
    fun createAlbum(@RequestBody newAlbum: AlbumRequest): ResponseEntity<Album> {
        val currentUser = SecurityContextHolder.getContext().authentication
        val createdAlbum = albumService.createAlbum(
                email = currentUser.principal as String,
                name = newAlbum.name,
                description = newAlbum.description,
                ownerIDs = newAlbum.owners ?: listOf(),
                groupIDs = newAlbum.groups ?: listOf(),
                sharedWithIDs = newAlbum.sharedWith ?: listOf(),
                artifactIDs = newAlbum.artifacts ?: listOf()
        )
        logger.info("Created album $createdAlbum")
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAlbum)
    }

    /**
     * PUT /album/{id}
     *
     * @param id ID of the album to be updated
     * @param album New details of the album that the user wants to update
     * The album can only be updated if the user is the owner of the album
     * @return [Album] representing the updated album
     */
    @PutMapping(path = ["/{id}"])
    fun updateAlbum(@PathVariable id: Long, @RequestBody album: AlbumRequest): ResponseEntity<Album> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val updatedAlbum = albumService.updateAlbum(currentUser.principal as String, id, album)
        logger.info("Updated album $updatedAlbum")
        return ResponseEntity.ok(updatedAlbum)
    }

    /**
     * PUT /album/{albumID}/artifact/{artifactID}
     *
     * @param albumID ID of the album to add the artifact to
     * @param artifactID ID of the artifact to add to the album
     * Can only add artifact to album if the user has access to the artifact and owns the album
     * @return [Album] the newly updated album with the added artifact in there
     */
    @PutMapping(path = ["/{albumID}/artifact/{artifactID}"])
    fun addArtifact(@PathVariable albumID: Long, @PathVariable artifactID: Long): ResponseEntity<Album> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val updatedAlbum = albumService.addArtifact(currentUser.principal as String, albumID, artifactID)
        logger.info("Added artifact $artifactID to album $albumID")
        return ResponseEntity.ok(updatedAlbum)
    }

    /**
     * DELETE /album/{id}
     *
     * @param id ID of the album that the user wants to delete.
     * Only the owner of the album can delete it
     * @return [Album] representing the deleted album
     */
    @DeleteMapping(path = ["/{id}"])
    fun deleteAlbum(@PathVariable id: Long): ResponseEntity<Album> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val deletedAlbum = albumService.deleteAlbum(currentUser.principal as String, id)
        logger.info("Deleted album $deletedAlbum")
        return ResponseEntity.ok(deletedAlbum)
    }
}

/**
 * Request to create an album
 *
 * @param name Name of the album to create
 * @param description Description of the album
 * @param owners List of the user IDs to own the album
 * @param groups List of the group IDs to associate the album with
 * @param sharedWith List of the user IDs to share the album with
 * @param artifacts List of the artifact IDs to associate with the album
 */
data class AlbumRequest(
    val name: String,
    val description: String,
    val owners: List<Long>,
    val groups: List<Long>,
    val sharedWith: List<Long>,
    val artifacts: List<Long>
)
