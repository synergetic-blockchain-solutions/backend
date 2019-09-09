package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
                ownerID = ownerID,
                memberID = memberID
        )
        return ResponseEntity.ok(groups)
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


}



data class GroupRequest(
            val name: String,
            val description: String,
            val members: List<Long>?
    )
