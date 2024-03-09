package org.example;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


@Testcontainers
class TestcontainersWithWiremockClientTest {

    @Container
    WireMockContainer wireMockServer = new WireMockContainer(
            DockerImageName.parse("wiremock/wiremock:3.3.1")
                    .asCompatibleSubstituteFor("wiremock/wiremock"))
            .withCliArg("--global-response-templating");

    @Test
    void testUsingWiremockClientAndPathAndQueryParamValues() {
        String path = "Hi";
        String name = "Jan";
        String body = "{\"message\":\"test\"}";

        WireMock wiremockClient = new WireMock(wireMockServer.getHost(), wireMockServer.getPort());
        wiremockClient.register(
                post(urlPathEqualTo("/dynamic/" + path))
                        .withQueryParam("name", equalTo(name))
                        .withRequestBody(equalToJson(body))
                        .willReturn(
                                ok()
                                        .withBody("{ \"message\" : \"{{request.path.[1]}} {{request.query.name}}\" }")
                                        .withHeader("Content-Type","application/json")
                        )
        );

        await().untilAsserted(() -> assertThat(
                given()
                        .header("Content-Type", "application/json")
                        .queryParam("name", name)
                        .body(body)
                .when()
                        .post(wireMockServer.getUrl("/dynamic/"+ path))
                .then()
                        .statusCode(200)
                        .extract().path("message").toString()).isEqualTo(path + " " + name));
    }

    @Test
    void testUsingWiremockClientAndBodyValues() {
        String name = "John";
        String surname = "Smith";
        String requestBody = "{\"name\":\"" + name + "\", \"surname\":\"" + surname + "\"}";

        WireMock wiremockClient = new WireMock(wireMockServer.getHost(), wireMockServer.getPort());
        wiremockClient.register(
                post(urlPathEqualTo("/json/body/transformer"))
                        .withRequestBody(equalToJson(requestBody))
                        .willReturn(
                                ok()
                                        .withBody("{ \"message\" : \"Hello {{jsonPath request.body '$.name'}} {{jsonPath request.body '$.surname'}}!\" }")
                                        .withHeader("Content-Type","application/json")
                                        .withTransformers("response-template")
                        )
        );

        await().untilAsserted(() -> assertThat(
                given()
                        .header("Content-Type", "application/json")
                        .body(requestBody)
                        .when()
                        .post(wireMockServer.getUrl("/json/body/transformer"))
                        .then()
                        .statusCode(200)
                        .extract().path("message").toString()).isEqualTo("Hello " + name + " " + surname + "!"));
    }
}