package com.synergeticsolutions.familyartefacts

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

    override fun findAlbumsByOwner(
        email: String,
        groupID: Long?,
        ownerID: Long?,
        sharedID: Long?,
        albumName: String?
    ): List<Album> {
        val user =
                userRepository.findByEmail(email) ?: throw UserNotFoundException("No user with email $email was found")

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

        return albums.toSet().toList()
    }

    override fun findAlbumById(email: String, id: Long): Album {
        val user =
                userRepository.findByEmail(email) ?: throw UserNotFoundException("No user with email $email was found")
        val accessibleAlbums =
                user.ownedAlbums.map(Album::id) + user.sharedAlbums.map(Album::id) + user.groups.flatMap {
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
            if (!userRepository.existsById(it)) { throw UserNotFoundException("No user with ID $it was found") }
        }
        groupIDs.forEach {
            if (!groupRepository.existsById(it)) { throw GroupNotFoundException("No group with ID $it was found")
            }
        }
        artifactIDs.forEach {
            if (!artifactRepository.existsById(it)) {
                throw ArtifactNotFoundException("No artifact with ID $it was found")
            }
        }

        val accessibleArtifacts =
                creator.ownedArtifacts.map(Artifact::id) + creator.sharedArtifacts.map(Artifact::id) + creator.groups.flatMap {
                    it.artifacts.map(Artifact::id)
                } + creator.ownedAlbums.flatMap {
                    it.artifacts.map(Artifact::id)
                } + creator.sharedAlbums.flatMap {
                    it.artifacts.map(Artifact::id)
                }

        artifactIDs.forEach {
            if (!accessibleArtifacts.contains(it)) throw ActionNotAllowedException("User does not have access to artifact $it")
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

    override fun updateAlbum(email: String, id: Long, update: AlbumRequest): Album {
        val user =
                userRepository.findByEmail(email) ?: throw UserNotFoundException("User with email $email does not exist")
        val album =
                albumRepository.findByIdOrNull(id)
                        ?: throw AlbumNotFoundException("Could not find album with ID $id")

        val accessibleArtifacts =
                user.ownedArtifacts.map(Artifact::id) + user.sharedArtifacts.map(Artifact::id) + user.groups.flatMap {
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

    override fun deleteAlbum(email: String, id: Long): Album {
        val user =
                userRepository.findByEmail(email) ?: throw UserNotFoundException("User with email $email does not exist")
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
