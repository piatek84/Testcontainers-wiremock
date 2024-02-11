package org.example;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

@Testcontainers
class WireMockTest {

    @Container
    WireMockContainer wireMockServer = new WireMockContainer(
            DockerImageName.parse("wiremock/wiremock:3.3.1")
                    .asCompatibleSubstituteFor("wiremock/wiremock"))
            .withCliArg("--global-response-templating")
            .withMappingFromResource("OK.json", WireMockTest.class, "/OK.json")
            .withMappingFromResource("BadRequest.json", WireMockTest.class, "/BadRequest.json");

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

    @Test
    void testUsingWiremockClient() {
        String path = "Hi";
        String name = "Jan";
        WireMock wiremockClient = new WireMock(wireMockServer.getHost(), wireMockServer.getPort());
        wiremockClient.register(
                get(urlPathEqualTo("/dynamic/" + path))
                        .withQueryParam("name", equalTo(name))
                        .willReturn(
                                ok()
                                        .withBody("{ \"message\" : \"{{request.path.[1]}} {{request.query.name}}\" }")
                                        .withHeader("Content-Type","application/json")
                        )
        );

        given()
                .header("Content-Type", "plain/text")
                .queryParam("name", name)

                .when()
                .get(wireMockServer.getUrl("/dynamic/"+ path))

                .then()
                .statusCode(200)
                .body("message", CoreMatchers.equalTo(path + " " + name));
    }
}