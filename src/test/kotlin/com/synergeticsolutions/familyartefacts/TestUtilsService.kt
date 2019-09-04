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
    private val artifactRepository: ArtifactRepository
) {

    fun clearDatabase() {
        groupRepository.saveAll(groupRepository.findAll().map {
            it.copy(
                members = mutableListOf(),
                artifacts = mutableListOf()
            )
        })
        userRepository.saveAll(userRepository.findAll().map {
            it.copy(
                groups = mutableListOf(),
                ownedArtifacts = mutableListOf(),
                sharedArtifacts = mutableListOf()
            )
        })
        artifactRepository.saveAll(artifactRepository.findAll().map {
            it.copy(
                groups = mutableListOf(),
                sharedWith = mutableListOf(),
                owners = mutableListOf()
            )
        })
        groupRepository.deleteAll()
        userRepository.deleteAll()
        artifactRepository.deleteAll()
    }
}