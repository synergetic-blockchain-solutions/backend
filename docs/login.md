# Login

Login is performed on the REST API backend using a POST request to the `/login` endpoint. It
is used to exchange the user's (`email` and `password`) for a [JSON web token](https://jwt.io) (JWT) that
expires in 24 hours. The JWT is signed using an HS512 (HMAC with SHA-512) secret key, the
secret key is defined using `auth.jwtSecret` in the application properties file so can vary
between the different environments without having th recompile the application.

## JWT

JWT's have the general structure `<header>.<payload>.<signature>`. We use the
[jsonwebtoken.io](https://www.jsonwebtoken.io/) library which abstracts the details of
validating the token using the `header` and `signature` segments. The part of interest
to us is the `payload`. This contains information about the user that we can use when
performing tasks on both the frontend and backend. It is important to note that the **payload
is not encrypted**, only encoded using base-64, so no sensitive information should be placed
in it. The payload in the JWT that we create has the following keys:

- `aud`
- `iss`
- `iat`
- `exp`
- `rol`

### aud

The `aud` key is the _audience_ for the token. This will generally be the value "MemoryBookFrontend" as this
is the intended user for the JWT.

### iss

The `iss` key is the token _issuer_. This will be "MemoryBook" as this is the name of our application.

### sub

The `sub` key is the token _subject_, who the token is issued to. This will be the email of the user who the token
is for as this is used to uniquely identify the user.

### `iat`

The `iat` key unix timestamp for when the JWT was issued.

### `exp`

The `exp` key is the unix timestamp for when the JWT expires.

### `rol`

The `rol` key is the collection of roles that the user has. They can be used by the frontend and backend
to deny the user access to areas they do not have access to without having to check in the database.