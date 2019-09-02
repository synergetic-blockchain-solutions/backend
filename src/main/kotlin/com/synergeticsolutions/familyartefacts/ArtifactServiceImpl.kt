package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

/**
 * Service for performing actions with artifacts.
 */
@Service
@Transactional
class ArtifactServiceImpl(
    @Autowired
    val artifactRepository: ArtifactRepository,
    @Autowired
    val userRepository: UserRepository,
    @Autowired
    val groupRepository: GroupRepository,
    @Autowired
    val entityManager: EntityManager
) : ArtifactService {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * createArtifact creates an artifact, encapsulating the necessary book keeping in the artifact, user and group
     * repositories. If there is no user with [email] then a [UsernameNotFoundException] will be thrown.
     *
     * @param email Email of the owner
     * @param name Name of the artifact
     * @param description Description of the artifact
     * @param groupIDs IDs of the groups the artifact is to be shared with
     * @param sharedWith IDs of the users the artifact is to be shared with
     * @return Created artifact
     */
    override fun createArtifact(
        email: String,
        name: String,
        description: String,
        groupIDs: List<Long>,
        sharedWith: List<Long>
    ): Artifact {
        logger.info("Creating artifact '$name' for user with email $email associated with groups $groupIDs and shared with users $sharedWith")
        val owner =
            userRepository.findByEmail(email) ?: throw UsernameNotFoundException("No user with email $email was found")
        val groups = groupRepository.findAllById(groupIDs)
        val shares = userRepository.findAllById(sharedWith)
        val artifact = Artifact(
            name = name,
            description = description,
            owners = listOf(owner),
            groups = groups,
            sharedWith = shares
        )
        val savedArtifact = artifactRepository.save(artifact)
        logger.debug("Created artifact $savedArtifact")
        val updatedOwner = entityManager.merge(owner.copy(ownedArtifacts = owner.ownedArtifacts + savedArtifact))
        logger.debug("Updated artifact owner $updatedOwner")
        val updatedGroups = groups.map { entityManager.merge(it.copy(artifacts = it.artifacts + savedArtifact)) }
        logger.debug("Updated artifact's associated groups $updatedGroups")
        return savedArtifact
    }

    /**
     * findArtifactsByOwner finds all the [Artifact]s a [User] with [email] has access to. The collection can be filtered by
     * [groupID] to only display artifacts from that group, and by [ownerID] to only show artifacts owned by the user
     * with that ID.
     *
     * @param email Email of the user to get the artifacts for
     * @param groupID ID of the group to filter the artifacts to
     * @param ownerID ID of the owning user to filter the artifacts to
     * @return Collection of artifacts the user has access to filtered by the given parameters
     */
    override fun findArtifactsByOwner(email: String, groupID: Long?, ownerID: Long?): List<Artifact> {
        val user =
            userRepository.findByEmail(email) ?: throw UsernameNotFoundException("No user with email $email was found")

        val ownedArtifacts = artifactRepository.findByOwners_Email(email)
        val groupsArtifacts = user.groups.map(Group::id).flatMap(artifactRepository::findByGroups_Id)
        val sharedArtifacts = artifactRepository.findBySharedWith_Email(email)

        var artifacts = ownedArtifacts + groupsArtifacts + sharedArtifacts

        if (groupID != null) {
            artifacts = artifacts.filter { it.groups.map(Group::id).contains(groupID) }
        }

        if (ownerID != null) {
            artifacts = artifacts.filter { it.owners.map(User::id).contains(ownerID) }
        }

        return artifacts
    }
}
