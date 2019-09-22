package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/group"])
class GroupController(
    @Autowired
    val groupService: GroupService
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * GET /group
     *
     * @param adminID ID of the user, to get only groups that have the user as admin
     * @param memberID ID of the member, to get only groups that have the user as member
     * @return List of groups the user has access to and fit the criteria given by the parameters
    */
    @GetMapping
    fun getGroups(
        @RequestParam(name = "owner", required = false) adminID: Long?,
        @RequestParam(name = "member", required = false) memberID: Long?
    ): ResponseEntity<List<Group>> {
        val currentUser = SecurityContextHolder.getContext().authentication
        val groups = groupService.findGroups(
                email = currentUser.principal as String,
                adminID = adminID,
                memberID = memberID
        )
        return ResponseEntity.ok(groups)
    }

    /**
     * GET /group/{id}
     *
     * @param id ID of the group to get
     * @return The group with the requested ID, if the user has access to that group
     */
    @GetMapping(path = ["/{id}"])
    fun getGroupById(@PathVariable id: Long): ResponseEntity<Group> {
        val currentUser = SecurityContextHolder.getContext().authentication
        val artifact = groupService.findGroupById(currentUser.principal as String, id)
        return ResponseEntity.ok(artifact)
    }

    /**
     * POST /artifact
     *
     * @param groupRequest Details of the group to be created
     * @return [Group] representing the created group
     */
    @PostMapping
    fun createGroup(
        @RequestBody groupRequest: GroupRequest
    ): ResponseEntity<Group> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val newGroup =
                groupService.createGroup(
                        email = currentUser.principal as String,
                        groupName = groupRequest.name,
                        description = groupRequest.description,
                        memberIDs = groupRequest.members ?: listOf()
                )
        return ResponseEntity.status(HttpStatus.CREATED).body(newGroup)
    }

    /**
     * PUT /artifact/{id}
     *
     * @param id ID of the group to be updated
     * @param groupRequest New details of the group that the user wants to update
     * The group can only be updated if the user is the admin of the group
     * @return [Group] representing the updated group
     */
    @PutMapping(path = ["/{id}"])
    fun updateArtifact(@PathVariable id: Long, @RequestBody groupRequest: GroupRequest): ResponseEntity<Group> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val updatedGroup = groupService.updateGroup(currentUser.principal as String, id, groupRequest)
        return ResponseEntity.ok(updatedGroup)
    }

    /**
     * DELETE /artifact/{id}
     *
     * @param id ID of the group that the user wants to delete.
     * The group can only be deleted if the user is the admin of the group
     * @return [Group] representing the deleted group
     */
    @DeleteMapping(path = ["/{id}"])
    fun deleteGroup(@PathVariable id: Long): ResponseEntity<Group> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val deletedArtifact = groupService.deleteGroup(currentUser.principal as String, id)
        return ResponseEntity.ok(deletedArtifact)
    }
}

/**
 * [GroupRequest] represents a request to create or update a group.
 *
 * @param [name] Group name
 * @param [description] Description of the group
 * @param [members] User IDs of the users to be members of the group
 * @param [admins] User IDs of the users to be admins of the group
 */
data class GroupRequest(
    val name: String,
    val description: String,
    val members: List<Long>?,
    val admins: List<Long>?
)
