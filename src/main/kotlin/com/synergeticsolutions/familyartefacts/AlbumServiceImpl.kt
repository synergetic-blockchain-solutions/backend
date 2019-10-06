package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

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

        logger.debug("Making $owners the owners of $savedAlbum")
        owners.forEach { it.ownedAlbums.add(savedAlbum) }
        userRepository.saveAll(owners)

        logger.debug("Adding $savedAlbum to $groups")
        groups.forEach { it.albums.add(savedAlbum) }
        groupRepository.saveAll(groups)

        logger.debug("Sharing $savedAlbum with $shares")
        shares.forEach { it.sharedAlbums.add(savedAlbum) }
        userRepository.saveAll(shares)

        logger.debug("Adding $artifacts to $savedAlbum")
        artifacts.forEach { it.albums.add(savedAlbum) }
        artifactRepository.saveAll(artifacts)

        logger.debug("Created album $savedAlbum")
        return savedAlbum
    }
}
