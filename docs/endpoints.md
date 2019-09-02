# Endpoints

This document documents all the API endpoints exposed by this application. Each endpoint includes an example
request that can be run when the application is started with `./mvnw spring-boot:run` (or 
`.\mvnw.cmd spring-boot:run` on Windows).

## `POST /registration`

Register a new user with the given name, email and password. If a user with the given email already
exists, the registration will be rejected.

### Example request

```http request
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

A successful response will be the created user object (without the password field). This will include
an array of groups with the only element being the newly created private group for that user.

## `POST /login`
See [login docs](./login.md) for more information.

Exchange user credentials (email and password) for a JWT.

### Example request

```http request
POST http://locahost:8080/login
Accept: application/json
Content-Type: application/json

{
    "email": "example@example.com",
    "password": "password"
}
```

### Response

A successful response from this endpoint will be a JSON document with one field, "token". This
is the JWT that can be used to authenticate on any other endpoints that require authentication.
For more information about the JWT, see the [JWT section in the login docs](./login.md#JWT)