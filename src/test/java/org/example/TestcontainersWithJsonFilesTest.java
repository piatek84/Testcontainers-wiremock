package org.example;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import static io.restassured.RestAssured.when;

@Testcontainers
class TestcontainersWithJsonFilesTest {

    @Container
    WireMockContainer wireMockServer = new WireMockContainer(
            DockerImageName.parse("wiremock/wiremock:3.3.1")
                    .asCompatibleSubstituteFor("wiremock/wiremock"))
            .withCliArg("--global-response-templating")
            .withMappingFromResource("OK.json", TestcontainersWithJsonFilesTest.class, "/OK.json")
            .withMappingFromResource("BadRequest.json", TestcontainersWithJsonFilesTest.class, "/BadRequest.json");

    @Test
    void shouldReturn200AndHelloWorld() {
        when().get(wireMockServer.getUrl("/some/thing"))

                .then()
                .statusCode(200)
                .body("message", CoreMatchers.equalTo("Hello, world!"));
    }

    @Test
    void shouldReturn400() {
        when().get(wireMockServer.getUrl("/some/thing/bad"))

                .then()
                .statusCode(400);
    }
}