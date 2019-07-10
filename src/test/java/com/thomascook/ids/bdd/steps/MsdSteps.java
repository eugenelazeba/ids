package com.thomascook.ids.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import cucumber.api.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;

import java.io.IOException;

@Slf4j
public class MsdSteps {
    @Then("response contains message ([^']*)")
    public void responseMSD(String message) throws IOException {
        JsonNode bookingMessageBodyActual = KibanaSteps.getMessageBody();
        log.info("Received response from Kibana :" + bookingMessageBodyActual);

        if (!bookingMessageBodyActual.isMissingNode() && !bookingMessageBodyActual.isNull()) {
            Assert.assertTrue("Unexpected message :" + bookingMessageBodyActual, bookingMessageBodyActual.asText().contains(message));
        }

        Assert.assertTrue("Kibana isn't working now", !bookingMessageBodyActual.isMissingNode()
                && !bookingMessageBodyActual.isNull());
    }
}
