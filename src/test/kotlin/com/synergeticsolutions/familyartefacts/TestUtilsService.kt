package com.synergeticsolutions.familyartefacts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TestUtilsService(
        @Autowired
        private val userRepository: UserRepository,
        @Autowired
        private val groupRepository: GroupRepository
) {

    fun clearDatabase() {
        groupRepository.saveAll(groupRepository.findAll().map {
            it.copy(
                    members = mutableListOf(),
                    admins = mutableListOf()
            )
        })
        userRepository.saveAll(userRepository.findAll().map {
            it.copy(
                    groups = mutableListOf(),
                    ownedGroups = mutableListOf()
            )
        })

        userRepository.deleteAll()
        groupRepository.deleteAll()
    }
}