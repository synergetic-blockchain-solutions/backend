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

### Testing

To run the tests in docker-compose (using MySQL rather than H2), run:

`docker-compose -f docker-compose.yml -f docker-compose.test.yml up --abort-on-container-exit --exit-code-from backend`

This will overlay the [docker-compose.test.yml](./docker-compose.test.yml) over [docker-compose.yml](./docker-compose.yml)
so the `backend` runs `./mvnw clean test` instead of `./mvnw clean spring-boot:run`. `--abort-on-container-exit` will stop
docker-compose when one of the containers exists (i.e. when the tests finish). `--exit-code-from backend` will make the
exit code that docker-compose returns the same as what `./mnv clean test` returned, so 0 if all is ok and something else
(probably 1) if there is a problem.