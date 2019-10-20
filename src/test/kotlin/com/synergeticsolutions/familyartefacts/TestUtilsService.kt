package com.synergeticsolutions.familyartefacts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TestUtilsService(
    @Autowired
    private val userRepository: UserRepository,
    @Autowired
    private val groupRepository: GroupRepository,
    @Autowired
    private val artifactRepository: ArtifactRepository,
    @Autowired
    private val albumRepository: AlbumRepository
) {

    fun clearDatabase() {
        groupRepository.saveAll(groupRepository.findAll().map {
            it.copy(
                members = mutableListOf(),
                admins = mutableListOf(),
                artifacts = mutableListOf(),
                albums = mutableListOf()
            )
        })
        userRepository.saveAll(userRepository.findAll().map {
            it.copy(
                groups = mutableListOf(),
                ownedArtifacts = mutableListOf(),
                sharedArtifacts = mutableListOf(),
                ownedAlbums = mutableListOf(),
                sharedAlbums = mutableListOf()
            )
        })
        artifactRepository.saveAll(artifactRepository.findAll().map {
            it.copy(
                    groups = mutableListOf(),
                    sharedWith = mutableListOf(),
                    owners = mutableListOf(),
                    albums = mutableListOf()
            )
        })
        albumRepository.saveAll(albumRepository.findAll().map {
            it.copy(
                    artifacts = mutableListOf(),
                    owners = mutableListOf(),
                    sharedWith = mutableListOf(),
                    groups = mutableListOf()
            )
        })

        userRepository.deleteAll()
        groupRepository.deleteAll()
        artifactRepository.deleteAll()
        albumRepository.deleteAll()
    }
}
