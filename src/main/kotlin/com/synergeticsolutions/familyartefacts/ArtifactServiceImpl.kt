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
    val groupRepository: GroupRepository,
    @Autowired
    val artifactResourceRepository: ArtifactResourceRepository
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
     * @throws UserNotFoundException when no user with [email] or one of the IDs in [ownerIDs] or [sharedWith] does not correspond to a [User.id]
     * @throws GroupNotFoundException when one of the IDs in [groupIDs] does not correspond to a [Group.id]()
     */
    override fun createArtifact(
        email: String,
        name: String,
        description: String,
        ownerIDs: List<Long>,
        groupIDs: List<Long>,
        sharedWith: List<Long>,
        resourceIDs: List<Long>,
        tags: List<String>
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
        resourceIDs.forEach {
            if (!artifactResourceRepository.existsById(it)) {
                throw ArtifactResourceNotFoundException("No artifact resource with ID $it was found")
            }
        }

        val owners = userRepository.findAllById(ownerIDs).toMutableList()
        val groups = groupRepository.findAllById(groupIDs).toMutableList()
        val shares = userRepository.findAllById(sharedWith)
        val resources = artifactResourceRepository.findAllById(resourceIDs)

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
            sharedWith = shares,
            resources = resources,
            tags = tags.toMutableList()
        )
        val savedArtifact = artifactRepository.save(artifact)

        logger.debug("Making $owners the owners of $savedArtifact")
        owners.forEach { it.ownedArtifacts.add(savedArtifact) }
        userRepository.saveAll(owners)

        logger.debug("Adding $savedArtifact to $groups")
        groups.forEach { it.artifacts.add(savedArtifact) }
        groupRepository.saveAll(groups)

        logger.debug("Sharing $savedArtifact with $shares")
        shares.forEach { it.sharedArtifacts.add(savedArtifact) }
        userRepository.saveAll(shares)

        logger.debug("Adding $savedArtifact to $resources")
        artifactResourceRepository.saveAll(resources.map { it.copy(artifact = savedArtifact) })

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
     * @throws UserNotFoundException when a user with [email] does not exist
     */
    override fun findArtifactsByOwner(email: String, groupID: Long?, ownerID: Long?, sharedID: Long?, tag: String?): List<Artifact> {
        val user =
            userRepository.findByEmail(email) ?: throw UserNotFoundException("No user with email $email was found")

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

        if (tag != null) {
            artifacts = artifacts.filter { it.tags.contains(tag) }
        }

        return artifacts
    }

    /**
     * findArtifactById finds artifact [id] in the context of user with [email]. If the user has access to the artifact
     * then it is returned. If they do not have access [ActionNotAllowedException] is thrown.
     *
     * @param email Email of the user to get the artifact for
     * @param id ID of the artifact to get
     * @return [ArtifactRepository] with ID [id]
     * @throws UserNotFoundException when there is no user with [email]
     * @throws ActionNotAllowedException when the user does not have access to artifact with [id].
     * @throws ArtifactNotFoundException when the artifact does not exist
     */
    override fun findArtifactById(email: String, id: Long): Artifact {
        val user =
            userRepository.findByEmail(email) ?: throw UserNotFoundException("No user with email $email was found")
        // A user can access an artifact if they're an owner, it has been shared with them or they're part of a group
        // that has access to the artifact.
        val accessibleArtifacts =
            user.ownedArtifacts.map(Artifact::id) + user.sharedArtifacts.map(Artifact::id) + user.groups.flatMap {
                it.artifacts.map(Artifact::id)
            }
        if (!accessibleArtifacts.contains(id)) {
            if (!artifactRepository.existsById(id)) {
                logger.info("Artifact $id does not exist")
            }
            throw ActionNotAllowedException("User ${user.id} does not have access to artifact $id")
        }
        return artifactRepository.findByIdOrNull(id)
            ?: throw ArtifactNotFoundException("No artifact with ID $id was found")
    }

    /**
     * [updateArtifact] updates the artifact with [id] using [update]. To update an artifact, the user (determined
     * by [email]) must be an owner of the artifact. Except in the case where the user is the owner of a group and
     * removing the artifact from a group, in this case updating the artifact is allowed.
     *
     * @param email Email of the user doing the update
     * @param id ID of the artifact being updated
     * @param update Update of the artifact
     * @return Updated [Artifact]
     * @throws UserNotFoundException when there exists no user with [email]
     * @throws ArtifactNotFoundException when the artifact does not exist
     * @throws ActionNotAllowedException when the user is not authorised to perform the action they're attempting
     */
    override fun updateArtifact(email: String, id: Long, update: ArtifactRequest): Artifact {
        val user =
            userRepository.findByEmail(email) ?: throw UserNotFoundException("User with email $email does not exist")
        val artifact =
            artifactRepository.findByIdOrNull(id)
                ?: throw ArtifactNotFoundException("Could not find artifact with ID $id")

        if (!(update.resources ?: listOf()).containsAll(artifact.resources.map(ArtifactResource::id))) {
            throw BadHttpRequestException("Cannot remove resources in artifact update")
        }

        assertCanUpdate(user, artifact, update)
        // Past this point we can perform any actions and be comfortable the user is authorised to perform them

        ((update.owners ?: listOf()) + (update.sharedWith ?: listOf())).forEach {
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("Could not find user with ID $id")
            }
        }

        (update.groups ?: listOf()).forEach {
            if (!groupRepository.existsById(it)) {
                throw GroupNotFoundException("Could not find group with ID $id")
            }
        }

        (update.resources ?: listOf()).forEach {
            if (!artifactResourceRepository.existsById(it)) {
                throw ArtifactResourceNotFoundException("Could not find artifact resource with ID $id")
            }
        }

        // Fix up new users and owners who are being removed
        val updatedOwners = userRepository.findAllById(update.owners ?: listOf())
        artifact.owners.subtract(updatedOwners).forEach { it.ownedArtifacts.remove(artifact) }
        updatedOwners.subtract(artifact.owners).forEach { it.ownedArtifacts.add(artifact) }
        userRepository.saveAll(artifact.owners)
        userRepository.saveAll(updatedOwners)

        // Fix up new groups and groups that are being removed
        val updatedGroups = groupRepository.findAllById(update.groups ?: listOf())
        artifact.groups.subtract(updatedGroups).forEach { it.artifacts.remove(artifact) }
        updatedGroups.subtract(artifact.groups).forEach { it.artifacts.add(artifact) }
        groupRepository.saveAll(artifact.groups)
        groupRepository.saveAll(updatedGroups)

        // Fix up new sharees and sharees who are being removed
        val updatedShares = userRepository.findAllById(update.sharedWith ?: listOf())
        artifact.sharedWith.subtract(updatedShares).forEach { it.sharedArtifacts.remove(artifact) }
        updatedShares.subtract(artifact.sharedWith).forEach { it.sharedArtifacts.add(artifact) }
        userRepository.saveAll(artifact.sharedWith)
        userRepository.saveAll(updatedShares)

        val updatedArtifact = artifact.copy(
            name = update.name,
            description = update.description,
            owners = updatedOwners,
            groups = updatedGroups,
            sharedWith = updatedShares,
            tags = update.tags?.toMutableList() ?: mutableListOf()
        )
        return artifactRepository.save(updatedArtifact)
    }

    private fun assertCanUpdate(user: User, artifact: Artifact, update: ArtifactRequest) {
        // Owners of an artifact can make any changes they want
        if (artifact.owners.contains(user)) {
            return
        }

        // Other than owners, the only users that can make any sort of modifications are group owners of groups the
        // artifact is in. In this case, they're limited to being able to remove an artifact from a group they're an
        // owner of.
        if (update.groups != artifact.groups.map(Group::id)) {
            // The set of groups a user removes must be a subset of the set groups in which they are an owner
            val removedGroups = artifact.groups.map(Group::id).subtract(update.groups!!)
            if (!user.ownedGroups.map(Group::id).containsAll(removedGroups.toList())) {
                throw ActionNotAllowedException("User ${user.id} is not an admin of all the groups they attempted to remove")
            }
        } else {
            throw ActionNotAllowedException("User ${user.id} is not an owner of artifact ${artifact.id}")
        }
    }

    /**
     * [deleteArtifact] deletes artifact with [id]. This action is only allowable by users who are owners of the
     * artifact.
     *
     * @param email Email of the user doing the deleting
     * @param id ID of the artifact to delete
     * @return delete [Artifact]
     * @throws UserNotFoundException when there exists no user with [email]
     * @throws ArtifactNotFoundException when no artifact with [id] exists
     * @throws ActionNotAllowedException when the user is not authorised to delete the artifact
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
        userRepository.saveAll(artifact.owners)
        artifact.groups.forEach { it.artifacts.remove(artifact) }
        groupRepository.saveAll(artifact.groups)
        artifact.sharedWith.forEach { it.sharedArtifacts.remove(artifact) }
        userRepository.saveAll(artifact.sharedWith)
        artifactRepository.delete(artifact)
        return artifact
    }
}
