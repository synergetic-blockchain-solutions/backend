# Endpoints

This document documents all the API endpoints exposed by this application. Each
endpoint includes an example request that can be run when the application is
started with `./mvnw spring-boot:run` (or `.\mvnw.cmd spring-boot:run` on
Windows).

Endpoints that have ``{token}`` in their headers require the use of the token
returned by the [login endpoint](#POST-/login).

## `POST /registration`

Register a new user with the given name, email and password. If a user with the
given email already exists, the registration will be rejected.

### Example request

``` http request
POST http://localhost:8080/register
Accept: application/json
Content-Type: application/json

{
    "name": "Example Name",
    "email": "example@example/com",
    "password": "password"
}
```

### Response

A successful response will be the created user object (without the password
field). This will include an array of groups with the only element being the
newly created private group for that user.

## `POST /login`
See [login docs](./login.md) for more information.

Exchange user credentials (email and password) for a JWT.

### Example request

``` http request
POST http://localhost:8080/login
Accept: application/json
Content-Type: application/json

{
    "email": "example@example.com",
    "password": "password"
}
```

### Response

A successful response from this endpoint will be a JSON document with one field,
"token". This is the JWT that can be used to authenticate on any other endpoints
that require authentication.

For more information about the JWT, see the [JWT section in the login
docs](./login.md#JWT)

## `POST /artifact`

Create an artifact. The created artifact will always have the creator as an
owner and the creator's private group as one of the groups the artifact is part
of.

### Example Request

``` http request
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
```

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

``` http request
GET http://localhost:8080/artifact
Accept: application/json
Content-Type: application/json
Authorization: Bearer {token}
```

### Response

A successful response will be a JSON document with a list of all the artifacts
the user has access to and potentially filtered by the given query parameters.
The artifact objects returned are the same as that returned by the [create
artifact endpoint](#POST-/artifact).

## `GET /artifact/{id}`

Get artifact with ID. The artifact is only accessible to users that have access
to this artifact.

### Example Request

``` http request
GET http://localhost:8080/artifact/{id}
Accept: application/json
Content-Type: application/json
Authorization: Bearer {token}
```

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

``` http request
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
```

### Response

A successful response will be a JSON representation of the updated artifact. The
artifact object returned is the same as that returned by the [create artifact
endpoint](#POST/-artifact).

## `DELETE /artifact/{id}`

Delete the artifact with ID. This endpoint is only accessible by artifact owners.

### Example Request

``` http request
DELETE http://localhost:8080/artifact
Accept: application/json
Content-Type: application/json
Authorization: Bearer {token}
```

### Response

A successful response will be a JSON representation of the deleted artifact. The
artifact object returned is the same as that returned by the [create artifact
endpoint](#POST/-artifact).

## `POST /group`

Create a group. The created group will always have the creator as an admin and member of the group

### Example Request

    POST http://localhost:8080/group
    Accept: application/json
    Content-Type: application/json
    Authorization: Bearer {token}

    {
        "name": "Group 1",
        "description": "Group description",
        "members": [],
        "admins": []
    }

### Response

A successful response will be a JSON document representing
the created group. It will be the same as the request document with the
following differences:

- "id" field: the group ID
- "admins" field may have the creator's ID in addition to the other IDs if it
  was not explicitly passed in
- "members" field may have the creator's ID in addition to the other IDs if it
  was not explicitly passed in
  
  

## `GET /group`

Get all groups the user has access to by being admin or member.
Groups can also be filtered by:

- `adminID (int)` - Only return groups that are owned by the user with this ID
- `memberID (int)` - Only return groups that are the user with this ID is member

These parameters are optional and can be combined to find group where the user is both admin and member
(which is basically groups where the user is the admin)

### Example Request

  GET http://localhost:8080/group
  Accept: application/json
  Content-Type: application/json
  Authorization: Bearer {token}

### Response

A successful response will be a JSON document with a list of all the groups
the user has access to and potentially filtered by the given query parameters.

## `GET /group/{id}`

Get group with ID. The group is only accessible if the user is either admin or member

### Example Request

    GET http://localhost:8080/group/{id}
    Accept: application/json
    Content-Type: application/json
    Authorization: Bearer {token}

### Response

A successful response will be a JSON document with a representation of the group

## `PUT /group/{id}`

Update the group with ID by specifying the details that the user wants the group to become.
Only the admins of the group can perform this action.

### Example Request

    PUT http://localhost:8080/group
    Accept: application/json
    Content-Type: application/json
    Authorization: Bearer {token}

    {
        "name": "Group 1",
        "description": "Updated description",
        "members": [],
        "admins": []
    }

### Response

A successful response will be a JSON representation of the updated group.

## `DELETE /group/{id}`

Delete the group with ID. Only the admin of the group can perform this action

### Example Request

    DELETE http://localhost:8080/group
    Accept: application/json
    Content-Type: application/json
    Authorization: Bearer {token}

### Response

A successful response will be a JSON representation of the deleted group.


## `POST /artifact/{artifactId}/resource`

Associate a resource with a resource.

This request should be of type `multipart/form-data` and be made up of two named
parts:

- metadata: The metadata associated with the resource, this should be
  `application/json`
- resource: The actual resource, this should be the content type that best fits
  the artifact.

The metadata part **must** contain the following fields:

- name: The name of the resource
- description: Description of the resource

It can also optionally contain

- tags: A collection of tags to associate with the resource

### Example request

``` http request
POST http://localhost:8080/artifact/{artifactId}/resource
Accept: application/json
Content-Type: multipart/form-data; boundary=-----------xxxxxxxxxxxxxxxxxx
Authorization: Bearer {token}

-----------xxxxxxxxxxxxxxxxxx
Content-Disposition: application/json; name="metadata"

{
    "name": "Resource 1",
    "description": "Description"
    "tags": ["tag1", "tag2"]
}

-----------xxxxxxxxxxxxxxxxxx
Content-Disposition: image/png; name="resource"

{image}
```

### Response

A successful response from this endpoint will be a JSON document representing
the resource. It will have the following fields:

- `id`: The resource's ID
- `contentType`: The content type (or mime type) of the artifact, this is to
  help with how to display the resource and the content type when returning it.
- `artifact`: The ID of the artifact the resource is associated with.