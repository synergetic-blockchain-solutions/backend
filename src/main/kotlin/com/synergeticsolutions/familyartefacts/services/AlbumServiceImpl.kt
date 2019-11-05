package com.synergeticsolutions.familyartefacts.services

import com.synergeticsolutions.familyartefacts.controllers.AlbumRequest
import com.synergeticsolutions.familyartefacts.entities.Album
import com.synergeticsolutions.familyartefacts.entities.Artifact
import com.synergeticsolutions.familyartefacts.entities.Group
import com.synergeticsolutions.familyartefacts.entities.User
import com.synergeticsolutions.familyartefacts.exceptions.ActionNotAllowedException
import com.synergeticsolutions.familyartefacts.exceptions.ArtifactNotFoundException
import com.synergeticsolutions.familyartefacts.exceptions.GroupNotFoundException
import com.synergeticsolutions.familyartefacts.exceptions.UserNotFoundException
import com.synergeticsolutions.familyartefacts.repositories.AlbumRepository
import com.synergeticsolutions.familyartefacts.repositories.ArtifactRepository
import com.synergeticsolutions.familyartefacts.repositories.GroupRepository
import com.synergeticsolutions.familyartefacts.repositories.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class AlbumNotFoundException(msg: String) : RuntimeException(msg)

/**
 * Service for performing actions on albums.
 */
@Service
class AlbumServiceImpl(
    @Autowired
    val userRepository: UserRepository,
    @Autowired
    val groupRepository: GroupRepository,
    @Autowired
    val artifactRepository: ArtifactRepository,
    @Autowired
    val albumRepository: AlbumRepository
) : AlbumService {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * [findAlbumsByOwner] finds all the [Album]s a [User] with [email] has access to, with the given criteria
     *
     * @param email Email of the user
     * @param groupID ID of the group that the album is in
     * @param ownerID ID of the user that owns the album
     * @param sharedID ID of the users that the album shares with
     * @param albumName name of the album
     * @return Collection of albums the user has access to filtered by the given parameters
     * @throws UsernameNotFoundException when a user with [email] does not exist
     */
    override fun findAlbums(
        email: String,
        groupID: Long?,
        ownerID: Long?,
        sharedID: Long?,
        albumName: String?
    ): List<Album> {
        val user =
                userRepository.findByEmail(email) ?: throw UserNotFoundException(
                    "No user with email $email was found"
                )

        val ownedAlbums = albumRepository.findByOwners_Email(email)
        val groupsAlbums = user.groups.map(Group::id).flatMap(albumRepository::findByGroups_Id)
        val sharedAlbums = albumRepository.findBySharedWith_Email(email)

        var albums = ownedAlbums.union(groupsAlbums).union(sharedAlbums).toList()

        if (groupID != null) {
            albums = albums.filter { it.groups.map(Group::id).contains(groupID) }
        }

        if (ownerID != null) {
            albums = albums.filter { it.owners.map(User::id).contains(ownerID) }
        }

        if (sharedID != null) {
            albums = albums.filter { it.sharedWith.map(User::id).contains(sharedID) }
        }

        if (albumName != null) {
            albums = albums.filter { it.name.startsWith(albumName, ignoreCase = true) }
        }

        return albums.toList()
    }

    /**
     * findAlbumById finds album [id] in the context of user with [email]. If the user has access to the album
     * then it is returned. If they do not have access [ActionNotAllowedException] is thrown.
     *
     * @param email Email of the user performing user
     * @param id ID of the album to get
     * @return [Album] with ID [id]
     * @throws UserNotFoundException when there is no user with [email]
     * @throws ActionNotAllowedException when the user does not have access to album with [id].
     * @throws AlbumNotFoundException when the album does not exist
     */
    override fun findAlbumById(email: String, id: Long): Album {
        val user =
                userRepository.findByEmail(email) ?: throw UserNotFoundException(
                    "No user with email $email was found"
                )
        val accessibleAlbums =
                user.ownedAlbums.map(Album::id) + user.sharedAlbums.map(
                    Album::id) + user.groups.flatMap {
                    it.albums.map(Album::id)
                }
        if (!accessibleAlbums.contains(id)) {
            if (!albumRepository.existsById(id)) {
                logger.info("Album $id does not exist")
            }
            throw ActionNotAllowedException("User ${user.id} does not have access to album $id")
        }
        return albumRepository.findByIdOrNull(id)
                ?: throw AlbumNotFoundException("No album with ID $id was found")
    }

    /**
     * [createAlbum] creates an album with given details
     *
     * @param email Email of the owner
     * @param name Name of the album
     * @param description Description of the album
     * @param groupIDs IDs of the groups the album is to be shared with
     * @param sharedWithIDs IDs of the users the album is to be shared with
     * @param artifactIDs IDs of the artifacts to be added to the album
     * @return Created [Album]
     * @throws UserNotFoundException when no user with [email] or one of the IDs in [ownerIDs] or [sharedWithIDs] does not correspond to a [User.id]
     * @throws GroupNotFoundException when one of the IDs in [groupIDs] does not correspond to a [Group.id]()
     */
    override fun createAlbum(
        email: String,
        name: String,
        description: String,
        ownerIDs: List<Long>,
        groupIDs: List<Long>,
        sharedWithIDs: List<Long>,
        artifactIDs: List<Long>
    ): Album {
        val creator = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("User with email $email does not exist")
        (ownerIDs + sharedWithIDs).forEach {
            if (!userRepository.existsById(it)) { throw UserNotFoundException(
                "No user with ID $it was found"
            )
            }
        }
        groupIDs.forEach {
            if (!groupRepository.existsById(it)) { throw GroupNotFoundException(
                "No group with ID $it was found"
            )
            }
        }
        artifactIDs.forEach {
            if (!artifactRepository.existsById(it)) {
                throw ArtifactNotFoundException("No artifact with ID $it was found")
            }
        }

        val accessibleArtifacts =
                creator.ownedArtifacts.map(Artifact::id) + creator.sharedArtifacts.map(
                    Artifact::id) + creator.groups.flatMap {
                    it.artifacts.map(Artifact::id)
                } + creator.ownedAlbums.flatMap {
                    it.artifacts.map(Artifact::id)
                } + creator.sharedAlbums.flatMap {
                    it.artifacts.map(Artifact::id)
                }

        artifactIDs.forEach {
            if (!accessibleArtifacts.contains(it)) throw ActionNotAllowedException(
                "User does not have access to artifact $it"
            )
        }

        val artifacts = artifactRepository.findAllById(artifactIDs)
        val owners = userRepository.findAllById(ownerIDs).toMutableList()
        val groups = groupRepository.findAllById(groupIDs).toMutableList()
        val shares = userRepository.findAllById(sharedWithIDs)

        if (!owners.contains(creator)) {
            owners.add(creator)
        }

        if (!groups.contains(creator.privateGroup)) {
            groups.add(creator.privateGroup)
        }

        val album = Album(
            name = name,
            description = description,
            owners = owners,
            groups = groups,
            sharedWith = shares,
            artifacts = artifacts
        )

        val savedAlbum = albumRepository.save(album)

        logger.debug("Making $owners the owners of $album")
        owners.forEach { it.ownedAlbums.add(album) }
        userRepository.saveAll(owners)

        logger.debug("Adding $album to $groups")
        groups.forEach { it.albums.add(album) }
        groupRepository.saveAll(groups)

        logger.debug("Sharing $album with $shares")
        shares.forEach { it.sharedAlbums.add(album) }
        userRepository.saveAll(shares)

        logger.debug("Adding $artifacts to $album")
        artifacts.forEach { it.albums.add(album) }
        artifactRepository.saveAll(artifacts)

        logger.debug("Created album $album")

        return savedAlbum
    }

    /**
     * [addArtifact] adds the artifact with [artifactID] to album with [albumID]
     *
     * @param email email of the performing user
     * @param albumID ID of the album to be updated
     * @param artifactID ID of the artifact to be added to the album
     * Only the owner of the album can perform the action
     * @return updated [Album]
     * @throws UserNotFoundException if there is no user with [email]
     * @throws AlbumNotFoundException if there is no album with [albumID]
     * @throws ArtifactNotFoundException if there is no artifact with [artifactID]
     * @throws ActionNotAllowedException if user neither owns the album nor have access to the artifact
     */
    override fun addArtifact(email: String, albumID: Long, artifactID: Long): Album {
        val user =
                userRepository.findByEmail(email) ?: throw UserNotFoundException(
                    "User with email $email does not exist"
                )
        val album =
                albumRepository.findByIdOrNull(albumID)
                        ?: throw AlbumNotFoundException("Could not find album with ID $albumID")
        val artifact =
                artifactRepository.findByIdOrNull(artifactID)
                        ?: throw ArtifactNotFoundException("Could not find artifact with ID $artifactID")
        if (!user.ownedAlbums.contains(album)) {
            throw ActionNotAllowedException("User is not owner of album $albumID")
        }
        val accessibleArtifacts =
                user.ownedArtifacts.map(Artifact::id) + user.sharedArtifacts.map(
                    Artifact::id) + user.groups.flatMap {
                    it.artifacts.map(Artifact::id)
                } + user.ownedAlbums.flatMap {
                    it.artifacts.map(Artifact::id)
                } + user.sharedAlbums.flatMap {
                    it.artifacts.map(Artifact::id)
                }
        if (!accessibleArtifacts.contains(artifactID)) {
            throw ActionNotAllowedException("User does not have access to artifact $artifactID")
        }
        album.artifacts.add(artifact)
        artifact.albums.add(album)
        artifactRepository.save(artifact)
        return albumRepository.save(album)
    }

    /**
     * [updateAlbum] updates the album with [id]. To update an album, the user must be an owner of the album.
     * Except in the case where the user is the owner of a group an removing the album from a group.
     *
     * @param email Email of the performing user
     * @param id ID of the album being updated
     * @param update Update details of the album
     * @return Updated [Album]
     * @throws UserNotFoundException when there exists no user with [email]
     * @throws AlbumNotFoundException when the album does not exist
     * @throws GroupNotFoundException when a group specified in [update] does not exist
     * @throws ArtifactNotFoundException when an artifact specifies in [update] does not exist
     * @throws ActionNotAllowedException when the user is not authorised to perform the action they're attempting
     */
    override fun updateAlbum(email: String, id: Long, update: AlbumRequest): Album {
        val user =
                userRepository.findByEmail(email) ?: throw UserNotFoundException(
                    "User with email $email does not exist"
                )
        val album =
                albumRepository.findByIdOrNull(id)
                        ?: throw AlbumNotFoundException("Could not find album with ID $id")

        val accessibleArtifacts =
                user.ownedArtifacts.map(Artifact::id) + user.sharedArtifacts.map(
                    Artifact::id) + user.groups.flatMap {
                    it.artifacts.map(Artifact::id)
                } + user.ownedAlbums.flatMap {
                    it.artifacts.map(Artifact::id)
                } + user.sharedAlbums.flatMap {
                    it.artifacts.map(Artifact::id)
                }

        assertCanUpdate(user, album, update)
        // Past this point we can perform any actions and be comfortable the user is authorised to perform them

        ((update.owners ?: listOf()) + (update.sharedWith ?: listOf())).forEach {
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("Could not find user with ID $it")
            }
        }

        (update.groups ?: listOf()).forEach {
            if (!groupRepository.existsById(it)) {
                throw GroupNotFoundException("Could not find group with ID $it")
            }
        }

        // User can only add/remove artifacts that they have access to to/from the album
        (update.artifacts ?: listOf()).forEach {
            if (!artifactRepository.existsById(it)) {
                throw ArtifactNotFoundException("Could not find artifact with ID $it")
            }
            if (!accessibleArtifacts.contains(it)) {
                throw ActionNotAllowedException("User does not have access to artifact $it")
            }
        }
        val updatedArtifacts = artifactRepository.findAllById(update.artifacts ?: listOf())

        album.artifacts.subtract(updatedArtifacts).forEach { it.albums.remove(album) }
        updatedArtifacts.subtract(album.artifacts).forEach { it.albums.add(album) }
        artifactRepository.saveAll(album.artifacts)
        artifactRepository.saveAll(updatedArtifacts)

        // Fix up new users and owners who are being removed
        val updatedOwners = userRepository.findAllById(update.owners ?: listOf())
        album.owners.subtract(updatedOwners).forEach { it.ownedAlbums.remove(album) }
        updatedOwners.subtract(album.owners).forEach { it.ownedAlbums.add(album) }
        userRepository.saveAll(album.owners)
        userRepository.saveAll(updatedOwners)

        // Fix up new groups and groups that are being removed
        val updatedGroups = groupRepository.findAllById(update.groups ?: listOf())
        album.groups.subtract(updatedGroups).forEach { it.albums.remove(album) }
        updatedGroups.subtract(album.groups).forEach { it.albums.add(album) }
        groupRepository.saveAll(album.groups)
        groupRepository.saveAll(updatedGroups)

        // Fix up new sharees and sharees who are being removed
        val updatedShares = userRepository.findAllById(update.sharedWith ?: listOf())
        album.sharedWith.subtract(updatedShares).forEach { it.sharedAlbums.remove(album) }
        updatedShares.subtract(album.sharedWith).forEach { it.sharedAlbums.add(album) }
        userRepository.saveAll(album.sharedWith)
        userRepository.saveAll(updatedShares)

        val updatedAlbum = album.copy(
                name = update.name,
                description = update.description,
                owners = updatedOwners,
                groups = updatedGroups,
                sharedWith = updatedShares,
                artifacts = updatedArtifacts
        )
        return albumRepository.save(updatedAlbum)
    }

    /**
     * Check a user is allowed to update the album. If not thrown an [ActionNotAllowedException].
     */
    private fun assertCanUpdate(user: User, album: Album, update: AlbumRequest) {
        // Owners of an album can make any changes they want
        if (album.owners.contains(user)) {
            return
        }

        // Other than owners, the only users that can make any sort of modifications are group owners of groups the
        // album is in. In this case, they're limited to being able to remove an album from a group they're an
        // owner of.
        if (update.groups != album.groups.map(Group::id)) {
            // The set of groups a user removes must be a subset of the set groups in which they are an owner
            val removedGroups = album.groups.map(Group::id).subtract(update.groups!!)
            if (!user.ownedGroups.map(Group::id).containsAll(removedGroups.toList())) {
                throw ActionNotAllowedException("User ${user.id} is not an admin of all the groups they attempted to remove")
            }
        } else {
            throw ActionNotAllowedException("User ${user.id} is not an owner of album ${album.id}")
        }
    }

    /**
     * [deleteAlbum] deletes album with [id]. This action is only allowable by users who are owners of the album.
     *
     * @param email Email of the user doing the deleting
     * @param id ID of the artifact to delete
     * @return deleted [Album]
     * @throws UserNotFoundException when there exists no user with [email]
     * @throws AlbumNotFoundException when no album with [id] exists
     * @throws ActionNotAllowedException when the user is not authorised to delete the album
     */
    override fun deleteAlbum(email: String, id: Long): Album {
        val user =
                userRepository.findByEmail(email) ?: throw UserNotFoundException(
                    "User with email $email does not exist"
                )
        val album =
                albumRepository.findByIdOrNull(id)
                        ?: throw AlbumNotFoundException("Could not find album with ID $id")
        if (!album.owners.contains(user)) {
            throw ActionNotAllowedException("User ${user.id} is not an owner of album $id")
        }
        album.owners.forEach { it.ownedAlbums.remove(album) }
        userRepository.saveAll(album.owners)
        album.groups.forEach { it.albums.remove(album) }
        groupRepository.saveAll(album.groups)
        album.sharedWith.forEach { it.sharedAlbums.remove(album) }
        userRepository.saveAll(album.sharedWith)
        album.artifacts.forEach { it.albums.remove(album) }
        artifactRepository.saveAll(album.artifacts)
        albumRepository.delete(album)
        return album
    }
}
