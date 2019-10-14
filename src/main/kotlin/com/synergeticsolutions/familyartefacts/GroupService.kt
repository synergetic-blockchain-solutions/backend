package com.synergeticsolutions.familyartefacts

interface GroupService {
    fun createGroup(
        email: String,
        groupName: String,
        description: String,
        memberIDs: List<Long>,
        adminIDs: List<Long>
    ): Group
    fun findGroups(email: String, adminID: Long?, memberID: Long?): List<Group>
    fun deleteGroup(email: String, groupID: Long): Group
    fun updateGroup(email: String, groupID: Long, groupRequest: GroupRequest): Group
    fun findGroupById(email: String, groupID: Long): Group
    fun addImage(s: String, contentType: String, id: Long, image: ByteArray): Group
}
