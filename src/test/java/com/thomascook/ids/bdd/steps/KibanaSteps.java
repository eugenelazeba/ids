package com.thomascook.ids.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cucumber.api.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.tika.io.IOUtils;
import org.junit.Assert;

import java.io.IOException;

@Slf4j
public class KibanaSteps {
    static final Config config = ConfigFactory.load();
    static JsonNode messageBody;
    private ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode getKibanaMessageBody(String file, int retries, long pollIntervalMs, String bookingNumber) throws IOException, InterruptedException, UnirestException {
        String requestBody = IOUtils.toString(getClass().getResourceAsStream(file), "UTF-8");
        JsonNode requestBodyNode = objectMapper.readTree(requestBody);

        ((ObjectNode) requestBodyNode.path("query").path("bool").path("must").path(4).path("match_phrase_prefix")).put("message", bookingNumber);
        String publishMessage = String.valueOf(requestBodyNode);
        log.info("Executing request in Kibana : " + publishMessage);

        messageBody = null;
        for (int i = 0; i < retries; i++) {
            HttpResponse<String> response = Unirest.post(config.getString("kibana.url") + ":" +
                    config.getString("kibana.port") +
                    config.getString("kibana.action"))
                    .header(HttpHeaders.AUTHORIZATION, config.getString("kibana.authorization"))
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(publishMessage).asString();

            Assert.assertEquals("Unexpected response: " + response.getStatus(), HttpStatus.SC_OK, response.getStatus());

            JsonNode responseBodyKibana = objectMapper.readTree(response.getBody());
            messageBody = responseBodyKibana.path("hits").path("hits").findPath("message");
            if (!messageBody.isMissingNode() && !messageBody.isNull())
                return messageBody;
            Thread.sleep(pollIntervalMs);
        }
        return messageBody;
    }

    @Then("wait (\\w+) milliseconds before booking processed")
    public void sleep(int milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }

    @Then("execute (\\S+) for (\\S+)")
    public JsonNode messageKibana(String requestBodyKibana, String bookingNumber) throws Exception {
        return getKibanaMessageBody(requestBodyKibana, 6, 30000, bookingNumber);
    }

    public static JsonNode getMessageBody() {
        return messageBody;
    }

}




