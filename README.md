# Family Artefact Registry

## Maven commands

### Run

Linux/Mac OS: `./mvnw spring-boot:run`

Windows: `.\mvnw.cmd spring-boot:run`

### Tests

Linux/Mac OS: `./mvnw test`

Windows: `.\mvnw.cmd test`

### Lint

Linux/Mac OS: `./mvnw antrun:run@ktlint`

Windows: `.\mvnw.cmd antrun:run@ktlint`

## Docker

The [docker-compose.yml](./docker-compose.yml) file allows us to run the backend
spring boot application with a MySQL database without having to install MySQL. To
run the application in docker:

`docker-compose up`

This will start the application running on http://localhost:8080