package org.example;

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
}