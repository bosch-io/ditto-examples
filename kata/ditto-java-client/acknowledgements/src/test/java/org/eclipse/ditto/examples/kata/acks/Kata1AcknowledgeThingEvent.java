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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.ditto.client.management.AcknowledgementsFailedException;
import org.eclipse.ditto.client.options.Options;
import org.eclipse.ditto.json.JsonPointer;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.model.base.acks.AcknowledgementLabel;
import org.eclipse.ditto.model.base.acks.AcknowledgementRequest;
import org.eclipse.ditto.model.base.acks.DittoAcknowledgementLabel;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.things.Thing;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * While subscribing to attribute changes:
 * <ul>
 * <li>acknowledge each "even" attribute counter value with a successful custom ack,</li>
 * <li>acknowledge each "odd" attribute counter value change with a non-successful custom ack.</li>
 * </ul>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class Kata1AcknowledgeThingEvent extends AbstractAcknowledgementsKata {

    private static final Logger LOGGER = LoggerFactory.getLogger(Kata1AcknowledgeThingEvent.class);

    private static Thing thing;

    @BeforeClass
    public static void setUpClass() throws InterruptedException, TimeoutException, ExecutionException {
        thing = createRandomThingWithAttribute(JsonPointer.of("/counter"), JsonValue.of(-1));
    }

    @Test
    public void part1PublishCustomAcksForAttributeChanges() throws InterruptedException {

        dittoClientSubscriber.twin().registerForAttributeChanges("REG1", "counter", change -> {
            LOGGER.info("Received attribute 'counter' change: {}", change);
            final int counterValue = change.getValue().orElseThrow().asInt();

            // TODO modify consumption of changes by acknowledging a potentially requested acknowledgement "my-custom-ack"
            //  * acknowledge each "even" attribute counter value with a successful status
            //  * acknowledge each "odd" attribute counter value change with a non-successful status
        });

        final DittoHeaders dittoHeaders = DittoHeaders.newBuilder()
                .acknowledgementRequest(
                        AcknowledgementRequest.of(DittoAcknowledgementLabel.PERSISTED),
                        AcknowledgementRequest.of(AcknowledgementLabel.of("my-custom-ack"))
                )
                .timeout("5s")
                .build();

        for (int i=0; i<10; i++) {
            final CountDownLatch latch = new CountDownLatch(1);
            final int loop = i;
            dittoClient.twin().forId(thing.getEntityId().orElseThrow())
                    .putAttribute("counter", i, Options.dittoHeaders(dittoHeaders))
                    .whenComplete((aVoid, throwable) -> {
                        // Verify results
                        if (loop%2 == 0) {
                            softly.assertThat(throwable)
                                    .withFailMessage("A Throwable should not have been raised in it <%s>", loop)
                                    .isNull();
                        } else {
                            softly.assertThat(throwable)
                                    .withFailMessage("A Throwable should have been raised in it <%s>", loop)
                                    .isNotNull();
                            softly.assertThat(throwable)
                                    .hasCauseExactlyInstanceOf(AcknowledgementsFailedException.class);
                        }
                        latch.countDown();
                    });

            softly.assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        }
    }
}
