package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * Service for performing actions with artifacts.
 */
@Service
class ArtifactServiceImpl(
    @Autowired
    val artifactRepository: ArtifactRepository,
    @Autowired
    val userRepository: UserRepository,
    @Autowired
    val groupRepository: GroupRepository
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
        ownerIDs: List<Long>,
        groupIDs: List<Long>,
        sharedWith: List<Long>
    ): Artifact {
        val creator =
            userRepository.findByEmail(email) ?: throw UserNotFoundException("No user with email $email was found")
        (ownerIDs + sharedWith).forEach {
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }
        groupIDs.forEach {
            if (!groupRepository.existsById(it)) {
                throw GroupNotFoundException("No group with ID $it was found")
            }
        }

        val owners = userRepository.findAllById(ownerIDs).toMutableList()
        val groups = groupRepository.findAllById(groupIDs).toMutableList()
        val shares = userRepository.findAllById(sharedWith)

        if (!owners.contains(creator)) {
            owners.add(creator)
        }

        if (!groups.contains(creator.privateGroup)) {
            groups.add(creator.privateGroup)
        }
        val artifact = Artifact(
            name = name,
            description = description,
            owners = owners,
            groups = groups,
            sharedWith = shares
        )
        val savedArtifact = artifactRepository.save(artifact)

        logger.debug("Making $owners the owners of $savedArtifact")
        owners.forEach { it.ownedArtifacts.add(savedArtifact) }
        userRepository.saveAll(owners)

        logger.debug("Adding $savedArtifact to $groups")
        groups.forEach { it.artifacts.add(savedArtifact) }
        groupRepository.saveAll(groups)

        logger.debug("Created artifact $savedArtifact")
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
    override fun findArtifactsByOwner(email: String, groupID: Long?, ownerID: Long?, sharedID: Long?): List<Artifact> {
        val user =
            userRepository.findByEmail(email) ?: throw UsernameNotFoundException("No user with email $email was found")

        val ownedArtifacts = artifactRepository.findByOwners_Email(email)
        val groupsArtifacts = user.groups.map(Group::id).flatMap(artifactRepository::findByGroups_Id)
        val sharedArtifacts = artifactRepository.findBySharedWith_Email(email)

        var artifacts = ownedArtifacts.union(groupsArtifacts).union(sharedArtifacts).toList()

        if (groupID != null) {
            artifacts = artifacts.filter { it.groups.map(Group::id).contains(groupID) }
        }

        if (ownerID != null) {
            artifacts = artifacts.filter { it.owners.map(User::id).contains(ownerID) }
        }

        if (sharedID != null) {
            artifacts = artifacts.filter { it.sharedWith.map(User::id).contains(sharedID) }
        }

        return artifacts
    }

    /**
     * [updateArtifact] updates the artifact with [id] using [update]. To update an artifact, the user (determined
     * by [email]) must be an owner of the artifact. Except in the case where the user is the owner of a group and
     * removing the artifact from a group, in this case updating the artifact is allowed.
     *
     * @param email Email of the user doing the update
     * @param id ID of the artifact being updated
     * @param update Update of the artifact
     * @return Updated artifact
     */
    override fun updateArtifact(email: String, id: Long, update: ArtifactRequest): Artifact {
        val artifact =
            artifactRepository.findByIdOrNull(id)
                ?: throw ArtifactNotFoundException("Could not find artifact with ID $id")
        val user =
            userRepository.findByEmail(email) ?: throw UserNotFoundException("User with email $email does not exist")
        if (!artifact.owners.contains(user)) {
            throw ActionNotAllowedException("User ${user.id} is not an owner of artifact $id")
        }
        val owners = userRepository.findAllById(update.owners ?: listOf()).toMutableList()
        val groups = groupRepository.findAllById(update.groups ?: listOf()).toMutableList()
        val shares = userRepository.findAllById(update.sharedWith ?: listOf())
        return artifact.copy(
            name = update.name,
            description = update.description,
            owners = owners,
            groups = groups,
            sharedWith = shares
        )
    }

    /**
     * [deleteArtifact] deletes artifact with [id]. This action is only allowable by users who are owners of the
     * artifact.
     *
     * @param email Email of the user doing the deleting
     * @param id ID of the artifact to delete
     * @return delete [Artifact]
     */
    override fun deleteArtifact(email: String, id: Long): Artifact {
        val user =
            userRepository.findByEmail(email) ?: throw UserNotFoundException("User with email $email does not exist")
        val artifact =
            artifactRepository.findByIdOrNull(id)
                ?: throw ArtifactNotFoundException("Could not find artifact with ID $id")
        if (!artifact.owners.contains(user)) {
            throw ActionNotAllowedException("User ${user.id} is not an owner of artifact $id")
        }
        artifact.owners.forEach { it.ownedArtifacts.remove(artifact) }
        artifact.groups.forEach { it.artifacts.remove(artifact) }
        artifact.sharedWith.forEach { it.sharedArtifacts.remove(artifact) }
        artifactRepository.delete(artifact)
        return artifact
    }
}
