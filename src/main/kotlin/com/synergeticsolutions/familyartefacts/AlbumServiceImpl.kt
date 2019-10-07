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

    override fun findAlbumsByOwner(email: String, groupID: Long?, ownerID: Long?, sharedID: Long?): List<Album> {
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
        val creator =
                userRepository.findByEmail(email)
                        ?: throw UsernameNotFoundException("User with email $email does not exist")
        (ownerIDs + sharedWithIDs).forEach {
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }
        groupIDs.forEach {
            if (!groupRepository.existsById(it)) {
                throw GroupNotFoundException("No group with ID $it was found")
            }
        }
        artifactIDs.forEach {
            if (!artifactRepository.existsById(it)) {
                throw ArtifactNotFoundException("No artifact with ID $it was found")
            }
        }
        val owners = userRepository.findAllById(ownerIDs).toMutableList()
        val groups = groupRepository.findAllById(groupIDs).toMutableList()
        val shares = userRepository.findAllById(sharedWithIDs)
        val artifacts = artifactRepository.findAllById(artifactIDs)

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
