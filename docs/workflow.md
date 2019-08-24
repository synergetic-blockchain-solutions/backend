# Workflow

This document describes the workflow for contributing the the backend of MemoryBook, Synergetic Blockchain Solutions's
IT project.

MemoryBook is written in [kotlin](https://kotlinlang.org) with [Spring Boot](https://spring.io/projects/spring-boot), so
any contributions should be in line with this.

The general workflow is:

1. Be assigned a card (task) on the Trello board and move it to "doing" 
2. Create a feature branch (`feature/<feature>`) based off the `master` branch 
3. Build the feature and write tests for it 
4. Create a pull request against the `master` branch 
6. The test suite will be run on CircleCI, if there are any errors they should be fixed 
7. Move the Trello card the "Awaiting approval" 
8. Someone else will review and approve the pull request then it can be merged

## Trello

All the tasks required to build the application are in Trello, if more arise they will be added to the backlog. For each
spring, cards will be moved from "Backlog" to "Todo". In "Todo", tasks are assigned and moved to "Doing" as work is
begun on them. Once a task is completed, it is moved to "Awaiting approval" whilst the [Pull Request](#pull-request) is
reviewed and approved.

## Feature Branch

Naming the branch `feature/<feature>` (e.g. `feature/user-registration`) is just a convention which makes it clear that
this is a branch for developing a certain feature. As you're developing a feature, the `master` branch may change due to
other features being integrated. In this case it usually a good idea to regularly rebase against the `master` branch
(`git rebase master`) to prevent your branch departing too far from reality. When you rebase against master you will
have to force push to the central repository (`git push -f origin feature/<feature>`). This is okay because you're just
pushing to the feature branch, please don't do this on the `master` branch. Upon completion of the feature, it will be
merged into the `master` branch via a [Pull Request](#pull-request).

## Pull Request

Pull Requests provide a way for features to be developed in a way which is easily overseen by other members of the team,
then when it comes time for features to be merged into the `master` branch, they allow team members to have oversight in
the approvals process.

Pull Requests can be opened whilst a feature is still being developed but should be marked that they are not ready to be
merged (e.g. put "[WIP]" in the title). This works nicely because GitHub Pull Requests provide a nice aggregated view of
activity on that feature. Automated tests are run on each push to the repository on GitHub and will provide a report in
the Pull Request and a notification in the Slack channel.

## Testing

Part of building a feature includes writing unit and integration tests for. The main areas we will be writing code is
`Service`s and `Controller`s. `Service`s should contain most of the logic requiring testing and should be written in a
way that allows for [_constructor injection_](https://www.baeldung.com/constructor-injection-in-spring) meaning we can
run the unit tests without Spring so the tests will run way faster. `Controller`s are a good place to do the integration
testing because we can use
[`WebTestClient`](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/test/web/reactive/server/WebTestClient.html)
to run tests all the way through from sending a request to the API to database actions.

Upon pushing to the remote repository hosted on GitHub, the test suite will be run by [CircleCI](https://circleci.com).
The config is in the [.circleci/config.yml](../.circleci/config.yml). It runs [ktlint](https://ktlint.github.io/) to
check that style is consistent across the codebase, then runs the tests using a MySQL database. Linting and tests are
run in Docker containers which should reduce the effort required to reproduce test failures. If you have Docker and
docker-compose installed, you can copy the commands out of the CircleCI config and run them directly on your machine.