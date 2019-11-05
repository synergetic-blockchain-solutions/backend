package com.synergeticsolutions.familyartefacts.dtos

/**
 * Response returned when a user's credentials are successfully verified. [token] is the JWT used to authenticate the
 * user in all other requests.
 */
data class LoginResponse(val token: String)
