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
class UserIsNotMemberException(msg: String) : RuntimeException(msg)
@ResponseStatus(HttpStatus.BAD_REQUEST)
class UserIsNotAdminException(msg: String) : RuntimeException(msg)
@ResponseStatus(HttpStatus.BAD_REQUEST)
class MemberAlreadyInGroupException(msg: String) : RuntimeException(msg)
@ResponseStatus(HttpStatus.BAD_REQUEST)
class MemberIsAlreadyAdminException(msg: String) : RuntimeException(msg)

/**
 * Service for performing actions with groups
 */
@Service
class GroupServiceImpl(
    @Autowired
    val userRepository: UserRepository,
    @Autowired
    val groupRepository: GroupRepository

) : GroupService {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * [findGroups] finds all the [Group]s a [User] with [email] has access to. The collection can be filtered by
     * [adminID] to only get groups owned by the user, and by [memberID] to only get groups where the user is member
     *
     * @param email Email of the user
     * @param adminID ID of the user that is the admin of the group
     * @param ownerID ID of the user that is the member of the group
     * @return Collection of groups the user has access to filtered by the given parameters
     * @throws UsernameNotFoundException when a user with [email] does not exist
     */
    override fun findGroups(email: String, adminID: Long?, memberID: Long?, name: String?): List<Group> {
        val user = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("No user with email $email was found")
        val ownedGroups = groupRepository.findByAdmins_Email(email)
        val memberGroup = groupRepository.findByMembers_Email(email)
        var groups = ownedGroups.union(memberGroup).toList()

        if (adminID != null) {
            groups = groups.filter { it.admins.map(User::id).contains(adminID) }
        }

        if (memberID != null) {
            groups = groups.filter { it.members.map(User::id).contains(memberID) }
        }

        if (name != null) {
            groups = groups.filter { it.name.startsWith(name) }
        }

        return groups.toList()
    }

    /**
     * []findGroupById] finds group with [groupID]. If the user with email [email] has access to the group
     * then it is returned, else [ActionNotAllowedException] is thrown.
     *
     * @param email Email of the user
     * @param groupID ID of the group to get
     * @return The [Group] found
     * @throws UserNotFoundException when there is no user with [email]
     * @throws ActionNotAllowedException when the user does not have access to the group with [groupID].
     * @throws GroupNotFoundException when the group does not exist
     */
    override fun findGroupById(email: String, groupID: Long): Group {
        val user =
                userRepository.findByEmail(email) ?: throw UserNotFoundException("No user with email $email was found")

        val accessibleGroups = user.ownedGroups.map(Group::id) + user.groups.map(Group::id)
        val group = groupRepository.findByIdOrNull(groupID)
                ?: throw GroupNotFoundException("No group with ID $groupID was found")
        if (!accessibleGroups.contains(groupID)) {
            throw ActionNotAllowedException("User ${user.id} does not have access to group $groupID")
        }
        return group
    }

    /**
     * [createGroup] creates a group
     * If there is no user with [email] then a [UserNotFoundException] will be thrown.
     *
     * @param email Email of the owner
     * @param groupName Name of the group
     * @param description Description of the group
     * @param memberIDs IDs of the members
     * @param adminIDs IDs of the admins (must be subset of [memberIDs])
     * @return Created [Group]
     * @throws UserNotFoundException when no user with [email] or one of the IDs in [memberIDs] does not correspond to a [User.id]
     * @throws UserIsNotMemberException when one of the IDs in [adminIDs] is not in [memberIDs]
     */
    override fun createGroup(
        email: String,
        groupName: String,
        description: String,
        memberIDs: List<Long>,
        adminIDs: List<Long>
    ): Group {
        val owner = userRepository.findByEmail(email)
                ?: throw UsernameNotFoundException("No user with email $email was found")
        val group = Group(name = groupName, description = description, members = mutableListOf(), admins = mutableListOf(), albums = mutableListOf())
        memberIDs.forEach {
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }
        adminIDs.forEach {
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }

        val members = userRepository.findAllById(memberIDs).toMutableList()
        if (!members.contains(owner)) {
            members.add(owner)
        }
        val admins = userRepository.findAllById(adminIDs).toMutableList()
        if (!admins.contains(owner)) {
            admins.add(owner)
        }
        admins.forEach {
            if (!members.contains(it)) {
                members.add(it)
            }
        }
        members.forEach {
            it.groups.add(group)
            group.members.add(it)
        }
        admins.forEach {
            it.ownedGroups.add(group)
            group.admins.add(it)
        }
        val savedGroup = groupRepository.save(group)
        userRepository.saveAll(members)
        userRepository.saveAll(admins)
        return savedGroup
    }

    /**
     * [updateGroup] updates the group from the details given by:
     * @param email Email of the user performing the action
     * @param groupID ID of the group to be updated
     * @param groupRequest New details of the group to be updated
     * @throws UsernameNotFoundException when there exists no user with [email]
     * @throws GroupNotFoundException when there exists no group with [groupID]
     * @throws ActionNotAllowedException when the user is not the admin of the group
     * @throws UserNotFoundException when one of the memberIDs or adminIDs do not correspond with any User.id in the database
     */
    override fun updateGroup(email: String, groupID: Long, groupRequest: GroupRequest): Group {
        val user =
                userRepository.findByEmail(email)
                        ?: throw UsernameNotFoundException("User with email $email does not exist")
        var group =
                groupRepository.findByIdOrNull(groupID)
                        ?: throw GroupNotFoundException("Could not find group with ID $groupID")
        if (!group.admins.contains(user)) {
            throw ActionNotAllowedException("User with email $email is not allowed to update the group")
        }
        groupRequest.members?.forEach {
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }
        groupRequest.admins?.forEach {
            if (!userRepository.existsById(it)) {
                throw UserNotFoundException("No user with ID $it was found")
            }
        }

        val updatedAdmins = userRepository.findAllById(groupRequest.admins ?: listOf())
        group.admins.subtract(updatedAdmins).forEach { it.ownedGroups.remove(group) }
        updatedAdmins.subtract(group.admins).forEach { it.ownedGroups.add(group) }
        userRepository.saveAll(group.admins)
        userRepository.saveAll(updatedAdmins)

        val updatedMembers = userRepository.findAllById(groupRequest.members ?: listOf())
        group.members.subtract(updatedMembers).forEach { it.groups.remove(group) }
        updatedMembers.subtract(group.members).forEach { it.groups.add(group) }
        userRepository.saveAll(group.members)
        userRepository.saveAll(updatedMembers)
        val updatedGroup = group.copy(
                name = groupRequest.name,
                description = groupRequest.description,
                admins = updatedAdmins,
                members = updatedMembers
        )
        return groupRepository.save(updatedGroup)
    }

    /**
     * [addImage] adds the profile image to the group
     * @param email Email of the user performing the action
     * @param contentType Type of the image to be added
     * @param id ID of the group to be updated
     * @param image The image represented in ByteArray
     * @throws UsernameNotFoundException when there exists no user with [email]
     * @throws GroupNotFoundException when there exists no group with [id]
     * @throws ActionNotAllowedException when the user is not the admin of the group
     * @throws UserNotFoundException when one of the memberIDs or adminIDs do not correspond with any User.id in the database
     */
    override fun addImage(email: String, contentType: String, id: Long, image: ByteArray): Group {
        logger.debug("Retrieving user with email $email")
        val user = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("User with email $email does not exist")
        logger.debug("Retrieving group with id $id")
        var group =
                groupRepository.findByIdOrNull(id)
                        ?: throw GroupNotFoundException("Could not find group with ID $id")
        logger.debug("Checking if user $email is an admin of Group $id")
        if (!group.admins.map(User::id).contains(user.id)) {
            throw ActionNotAllowedException("User with email $email is not allowed to update the group")
        }
        logger.debug("Updating image in Group $id")
        group = group.copy(contentType = contentType, image = image)
        return groupRepository.save(group)
    }

    /**
     * [deleteGroup] deletes group with [groupID]. This action is only allowable by admins of the group
     *
     * @param email Email of the user doing the deleting
     * @param groupID ID of the group to delete
     * @return deleted [Group]
     * @throws GroupNotFoundException when there exists no group with [groupID]
     * @throws UsernameNotFoundException when there exists no user with [email]
     * @throws ActionNotAllowedException when the user is not authorised to delete the group
     */
    override fun deleteGroup(email: String, groupID: Long): Group {
        val group = groupRepository.findByIdOrNull(groupID) ?: throw GroupNotFoundException("No group with id $groupID was found")
        val owner = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("No user with email $email was found")
        if (!group.admins.contains(owner)) {
            throw ActionNotAllowedException("User with email $email is not allowed to delete the group")
        }
        group.members.forEach { it.groups.remove(group) }
        group.admins.forEach { it.ownedGroups.remove(group) }
        groupRepository.delete(group)
        return group
    }
}
