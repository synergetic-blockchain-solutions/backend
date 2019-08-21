package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    val mapper = ObjectMapper().registerKotlinModule()

    @Test
    fun `it should not allow blank names`() {
        val registrationRequest = RegistrationRequest("", "example@example.com", "secret", "secret")
        mockMvc.perform(post("/register")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(mapper.writeValueAsString(registrationRequest)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `it should not allow invalid emails`() {
        val registrationRequest = RegistrationRequest("name", "example", "secret", "secret")
        mockMvc.perform(post("/register")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(mapper.writeValueAsString(registrationRequest)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `it should not allow blank password or confirm passwords`() {
        val registrationRequest = RegistrationRequest("", "example@example.com", "", "")
        mockMvc.perform(post("/register")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(mapper.writeValueAsString(registrationRequest)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `it should not allow different password and confirm passwords`() {
        val registrationRequest = RegistrationRequest("", "example@example.com", "secret1", "secret2")
        mockMvc.perform(post("/register")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(mapper.writeValueAsString(registrationRequest)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `it should return the created user without the password`() {
        val registrationRequest = RegistrationRequest("name", "example@example.com", "secret", "secret")
        val resp = mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(mapper.writeValueAsString(registrationRequest))
        )
            .andReturn()

        val returnedUser = mapper.readValue<Map<String, String>>(resp.response.contentAsString)
        assertTrue(returnedUser.containsKey("name"))
        assertEquals(registrationRequest.name, returnedUser["name"])
        assertTrue(returnedUser.containsKey("email"))
        assertEquals(registrationRequest.email, returnedUser["email"])
        assertFalse(returnedUser.containsKey("password"))
    }
}
