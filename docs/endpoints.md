# Endpoints

This document documents all the API endpoints exposed by this application. Each endpoint includes an example
request that can be run when the application is started with `./mvnw spring-boot:run` (or 
`.\mvnw.cmd spring-boot:run` on Windows).

Endpoints that have ``{token}`` in their headers require the use of the token
returned by the [login endpoint](#POST-/login).

## `POST /registration`

Register a new user with the given name, email and password. If a user with the given email already
exists, the registration will be rejected.

### Example request

    POST http://localhost:8080/register
    Accept: application/json
    Content-Type: application/json

    {
        "name": "Example Name",
        "email": "example@example/com",
        "password": "password"
    }

### Response

A successful response will be the created user object (without the password field). This will include
an array of groups with the only element being the newly created private group for that user.

## `POST /login`
See [login docs](./login.md) for more information.

Exchange user credentials (email and password) for a JWT.

### Example request

    POST http://localhost:8080/login
    Accept: application/json
    Content-Type: application/json

    {
        "email": "example@example.com",
        "password": "password"
    }

### Response

A successful response from this endpoint will be a JSON document with one field, "token". This
is the JWT that can be used to authenticate on any other endpoints that require authentication.
For more information about the JWT, see the [JWT section in the login docs](./login.md#JWT)

## `POST /artifact`

Create an artifact. The created artifact will always have the creator as an
owner and the creator's private group as one of the groups the artifact is part
of.

### Example Request

    POST http://localhost:8080/artifact
    Accept: application/json
    Content-Type: application/json
    Authorization: Bearer {token}

    {
        "name": "Artifact 1",
        "description": "Artifact description",
        "owners": [],
        "groups": [],
        "sharedWith": []
    }

### Response

A successful response from this endpoint will be a JSON document representing
the created artifact. It will be the same as the request document with the
following differences:

- "id" field - ID associated with the ID, this can be used in the [get
  artifact](#POST-/artifact/{id}) endpoint.
- "owners" field may have the creator's ID in addition to the other IDs if it
  was not explicitly passed in
- "groups" field may have the creator's private group's ID in addition to the
  other IDs if it was not explicitly passed in

## `GET /artifact`

Get all artifact a user has access to (owns, group they're part of, shared
with). This endpoint also accepts the following query parameters:

- `group (int)` - Only return artifacts that are associated with the group with ID
- `owner` (int) - Only return artifacts that are owned by the user with this ID
- `shared` (int) - Only return artifacts shared with the user with this ID

None of the query parameters are required and they can be used together to
further filter the returned artifacts.

### Example Request

    GET http://localhost:8080/artifact
    Accept: application/json
    Content-Type: application/json
    Authorization: Bearer {token}

### Response

A successful response will be a JSON document with a list of all the artifacts
the user has access to and potentially filtered by the given query parameters.
The artifact objects returned are the same as that returned by the [create
artifact endpoint](#POST-/artifact).

## `GET /artifact/{id}`

Get artifact with ID. The artifact is only accessible to users that have access
to this artifact.

### Example Request

    GET http://localhost:8080/artifact/{id}
    Accept: application/json
    Content-Type: application/json
    Authorization: Bearer {token}

### Response

A successful response will be a JSON document with a representation of the
retrieved artifact. The artifact object is the same as that returned by the
[create artifact endpoint](#POST/-artifact).

## `PUT /artifact/{id}`

Update the artifact with ID. In general the only users that can use this
endpoint are owners of the artifact. The exception is when removing a group from
the artifact's "groups" field. This can be done by owners of the group being
removed. Any other modifications are only allowed by the owning users.

### Example Request

    PUT http://localhost:8080/artifact
    Accept: application/json
    Content-Type: application/json
    Authorization: Bearer {token}

    {
        "name": "Artifact 1",
        "description": "Artifact description",
        "owners": [],
        "groups": [],
        "sharedWith": []
    }

### Response

A successful response will be a JSON representation of the updated artifact. The
artifact object returned is the same as that returned by the [create artifact
endpoint](#POST/-artifact).

## `DELETE /artifact/{id}`

Delete the artifact with ID. This endpoint is only accessible by artifact owners.

### Example Request

    DELETE http://localhost:8080/artifact
    Accept: application/json
    Content-Type: application/json
    Authorization: Bearer {token}

### Response

A successful response will be a JSON representation of the deleted artifact. The
artifact object returned is the same as that returned by the [create artifact
endpoint](#POST/-artifact).
