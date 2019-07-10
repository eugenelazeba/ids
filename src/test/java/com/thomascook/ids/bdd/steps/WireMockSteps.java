package com.thomascook.ids.bdd.steps;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpMethod;
import com.thomascook.ids.bdd.util.JSONAssertCompareIgnoreValues;
import com.thomascook.ids.bdd.util.WireMockProxyClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Slf4j
public class WireMockSteps {
    final static Config config = ConfigFactory.load();

    ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .disable(JsonGenerator.Feature.ESCAPE_NON_ASCII);

    private String msdUrlPattern;
    private WireMockProxyClient wireMockProxyClient;

    @Before
    public void setUp() throws Exception {
        URI msdApiUrl = new URIBuilder(config.getString("msd.apiUrl")).build();
        String msdBaseUrl = msdApiUrl.getScheme() + "://" + msdApiUrl.getHost();
        String proxyBaseUrl = config.getString("wireMock.baseUrl");

        wireMockProxyClient = new WireMockProxyClient(msdBaseUrl, proxyBaseUrl);
        msdUrlPattern = msdApiUrl.getPath();
        wireMockProxyClient.enableProxy(msdBaseUrl + msdUrlPattern, HttpMethod.PUT);
        //   wireMockProxyClient.resetRequests(msdUrlPattern, HttpMethod.PUT);
    }

    @After
    public void tearDown() throws Exception {
        //   wireMockProxyClient.resetRequests(msdUrlPattern, HttpMethod.PUT);
    }

    @Then("^incoming enriched booking with (\\S+) has been sent to MSD system")
    public void compareEnrichedMessages(String bookingNumber) throws Exception {
        JsonNode bookingToMsd = null;
        Config testData = config.getConfig("testData");
        Thread.sleep(45000);

        List<JsonNode> requests = wireMockProxyClient.findRequests(config.getString("msd.urlPath"), HttpMethod.PUT);
        Optional<JsonNode> found = Optional.empty();
        for (JsonNode r : requests) {
            JsonNode b = toJson(r.path("body").asText());
            if (b.path("booking").path("bookingIdentifier").path("bookingNumber").asText().contains(bookingNumber)) {
                found = Optional.of(b);
                bookingToMsd = found.get();
                compareExpectedActualJson(bookingToMsd, bookingNumber, testData);
            }
        }

        Assert.assertTrue("booking was not found in wiremock :", !(bookingToMsd == null));
    }

    @Then("MSD is successfully processed proxy mode (\\S+) message with (\\S+) and returned code is (\\S+)")
    public void responseFromMSDByBookingNumber(String path, Integer bookingNumber, Integer statusCode) throws Exception {
        List<JsonNode> getRequests = wireMockProxyClient.getRequests(path, HttpMethod.GET);

        assertThat(getRequests.stream()
                .filter(r -> {
                    try {
                        return (objectMapper.readTree(r.path("request").path("body").asText()))
                                .path("booking").path("bookingIdentifier").path("bookingNumber").asInt() == bookingNumber;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).map(r -> r.path("response").path("status").intValue()).findFirst().get(), is(statusCode));
    }

    private void compareExpectedActualJson(JsonNode bookingToMsd, String bookingId, Config testData) throws Exception {
        String expectedJsonFile = testData.getString(bookingId);
        JsonNode expectedJson = readTreeFromFile(expectedJsonFile);
        JsonNode actualJson = bookingToMsd;

        log.info("Verifying booking '{}'", bookingId);
        JSONAssert.assertEquals(
                expectedJson.toString(),
                actualJson.toString(),
                new JSONAssertCompareIgnoreValues(JSONCompareMode.LENIENT, "booking.bookingIdentifier.integrationProcessingInitiated",
                        "booking.customer.customerIdentifier.integrationProcessingInitiated"));
        log.info("Booking '{}' is OK", bookingId);
    }


    private JsonNode readTreeFromFile(String fileName) throws Exception {
        URL resource = WireMockSteps.class.getResource(fileName);
        return objectMapper.readTree(resource);
    }

    private JsonNode toJson(String jsonAsString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(jsonAsString);
        } catch (IOException ioe) {
            return null;
        }
    }
}
