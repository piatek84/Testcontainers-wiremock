package org.example;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import org.hamcrest.CoreMatchers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
class TestcontainersWithJsonFilesTest {

    @Container
    WireMockContainer wireMockServer = new WireMockContainer(
            DockerImageName.parse("wiremock/wiremock:3.3.1")
                    .asCompatibleSubstituteFor("wiremock/wiremock"))
            .withCliArg("--global-response-templating")
            .withMappingFromResource("OK.json", TestcontainersWithJsonFilesTest.class, "/OK.json")
            .withMappingFromResource("BadRequest.json", TestcontainersWithJsonFilesTest.class, "/BadRequest.json")
            .withMappingFromResource("JsonBodyResponse.json", TestcontainersWithJsonFilesTest.class, "/JsonBodyResponse.json");

    @Test
    void shouldReturn200AndHelloWorld() {
        when().get(wireMockServer.getUrl("/some/thing"))

                .then()
                .statusCode(200)
                .body("message1", CoreMatchers.equalTo("Hello, world1!"));
    }

    @Test
    void shouldReturn400() {
        when().get(wireMockServer.getUrl("/some/thing/bad"))

                .then()
                .statusCode(400);
    }

    @Test
    void shouldReturn200AndHelloWorldUsingJsonAssert() throws JSONException {
        JSONObject actual =  new JSONObject(when().get(wireMockServer.getUrl("/some/thing")).getBody().asString());
        JSONObject expected = new JSONObject()
                .put("message2", "Hello, world2!")
                .put("message1", "Hello, world1!");
        JSONAssert.assertEquals(expected, actual, false);
    }

    @Test
    void testUsingBodyValues() {
        String name = "John";
        String surname = "Smith";
        String requestBody = "{\"name\":\"" + name + "\", \"surname\":\"" + surname + "\"}";

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