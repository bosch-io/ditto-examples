/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.examples.kata.acks;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.ditto.client.management.AcknowledgementsFailedException;
import org.eclipse.ditto.client.options.Options;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.model.base.acks.AcknowledgementLabel;
import org.eclipse.ditto.model.base.common.HttpStatusCode;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.things.ThingId;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Perform an attribute modification requesting 2 acknowledgements and check why the custom ack failed.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class Kata2RequestAcknowledgement extends AbstractAcknowledgementsKata {

    private static final Logger LOGGER = LoggerFactory.getLogger(Kata2RequestAcknowledgement.class);

    @Test
    public void part1CreateThingRequestingAdditionalCustomAck() throws InterruptedException {
        final ThingId thingId = ThingId.of(configProperties.getNamespace() + ":" + UUID.randomUUID().toString());

        dittoClientSubscriber.twin().registerForThingChanges("REG1", change -> {
            LOGGER.info("Received Thing change: " + change);

            change.handleAcknowledgementRequest(AcknowledgementLabel.of("my-custom-ack"), ackHandle ->
                    ackHandle.acknowledge(HttpStatusCode.NOT_FOUND, JsonObject.newBuilder()
                            .set("error-detail", "Could not be found")
                            .build()));
        });

        // TODO request acknowledgements for the built-in DittoAcknowledgementLabel "twin-persisted"
        //  plus an additional acknowledgement label "my-custom-ack"
        final DittoHeaders dittoHeaders = DittoHeaders.newBuilder()
                .build();

        final CountDownLatch latch = new CountDownLatch(1);
        dittoClient.twin().create(thingId, Options.dittoHeaders(dittoHeaders))
                .whenComplete((thing, throwable) -> {
                    // Verify results
                    softly.assertThat(thing).isNull();
                    softly.assertThat(throwable)
                            .withFailMessage("A Throwable should have been raised")
                            .isNotNull();
                    softly.assertThat(throwable)
                            .hasCauseExactlyInstanceOf(AcknowledgementsFailedException.class);

                    // TODO find out with which status code and error message the "my-custom-ack" acknowledgement failed
                    final HttpStatusCode customAckStatusCode = null;
                    final JsonObject customAckPayload = null;

                    softly.assertThat(customAckStatusCode).isEqualTo(HttpStatusCode.NOT_FOUND);
                    softly.assertThat(customAckPayload.getValue("error-detail"))
                            .contains(JsonValue.of("Could not be found"));
                    latch.countDown();
                });

        softly.assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}
