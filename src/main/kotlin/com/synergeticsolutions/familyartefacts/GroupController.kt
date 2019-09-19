package com.synergeticsolutions.familyartefacts

import javax.naming.AuthenticationException
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

class NoAuthenticationException() : AuthenticationException()

@RestController
@RequestMapping(path = ["/group"])
class GroupController(
    @Autowired
    val groupService: GroupService
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping
    fun getGroups(
        @RequestParam(name = "owner", required = false) ownerID: Long?,
        @RequestParam(name = "member", required = false) memberID: Long?
    ): ResponseEntity<List<Group>> {
        val currentUser = SecurityContextHolder.getContext().authentication
        val groups = groupService.findGroups(
                email = currentUser.principal as String,
                adminID = ownerID,
                memberID = memberID
        )
        return ResponseEntity.ok(groups)
    }

    @GetMapping(path = ["/{id}"])
    fun getGroupById(@PathVariable id: Long): ResponseEntity<Group> {
        val currentUser = SecurityContextHolder.getContext().authentication
        val artifact = groupService.findGroupById(currentUser.principal as String, id)
        return ResponseEntity.ok(artifact)
    }

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

    @PutMapping(path = ["/{id}"])
    fun updateArtifact(@PathVariable id: Long, @RequestBody groupRequest: GroupRequest): ResponseEntity<Group> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val updatedGroup = groupService.updateGroup(currentUser.principal as String, id, groupRequest)
        return ResponseEntity.ok(updatedGroup)
    }

    @DeleteMapping(path = ["/{id}"])
    fun deleteGroup(@PathVariable id: Long): ResponseEntity<Group> {
        val currentUser = SecurityContextHolder.getContext().authentication ?: throw NoAuthenticationException()
        val deletedArtifact = groupService.deleteGroup(currentUser.principal as String, id)
        return ResponseEntity.ok(deletedArtifact)
    }
}

data class GroupRequest(
    val name: String,
    val description: String,
    val members: List<Long>?,
    val admins: List<Long>?
)
