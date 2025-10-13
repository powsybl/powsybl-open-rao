/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class NetworkPoolTest {
    private Network network;
    private String initialVariant;
    private String otherVariant = "otherVariant";

    @BeforeEach
    public void setUp() {
        network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        initialVariant = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().cloneVariant(initialVariant, otherVariant);
    }

    @Test
    void testCreate() {
        assertTrue(AbstractNetworkPool.create(network, otherVariant, 10, true) instanceof MultipleNetworkPool);
        assertTrue(AbstractNetworkPool.create(network, otherVariant, 1, true) instanceof SingleNetworkPool);
    }

    @Test
    void networkPoolUsageTest() {
        try (AbstractNetworkPool pool = AbstractNetworkPool.create(network, otherVariant, 10, false)) {

            pool.initClones(4);
            Network networkCopy = pool.getAvailableNetwork();

            assertNotNull(networkCopy);
            assertNotEquals(network, networkCopy);
            assertTrue(networkCopy.getVariantManager().getWorkingVariantId().startsWith("OpenRaoNetworkPool working variant"));

            pool.initClones(1);
            assertNotEquals(network, pool.getAvailableNetwork());

            pool.releaseUsedNetwork(networkCopy);
        } catch (InterruptedException e) {
            fail();
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void singleNetworkPoolUsageTest() throws InterruptedException {
        AbstractNetworkPool pool = AbstractNetworkPool.create(network, otherVariant, 1, true);
        Network networkCopy = pool.getAvailableNetwork();

        assertNotNull(networkCopy);
        assertEquals(network, networkCopy);
        assertEquals(4, network.getVariantManager().getVariantIds().size());
        assertTrue(networkCopy.getVariantManager().getWorkingVariantId().startsWith("OpenRaoNetworkPool working variant"));

        pool.releaseUsedNetwork(networkCopy);

        pool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        assertEquals(2, network.getVariantManager().getVariantIds().size());
    }

    @Test
    void checkMDCIsCopied() throws InterruptedException {
        Logger logger = (Logger) LoggerFactory.getLogger("LOGGER");
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        MDC.put("extrafield", "value from caller");
        AbstractNetworkPool pool = AbstractNetworkPool.create(network, otherVariant, 20, true);
        for (int i = 0; i < 20; i++) {
            pool.submit(() -> {
                LoggerFactory.getLogger("LOGGER").info("Hello from forked thread");
            });
        }
        pool.shutdownAndAwaitTermination(1, TimeUnit.SECONDS);

        List<ILoggingEvent> logsList = listAppender.list;
        for (int i = 0; i < 20; i++) {
            assertTrue(logsList.get(i).getMDCPropertyMap().containsKey("extrafield"));
            assertEquals("value from caller", logsList.get(i).getMDCPropertyMap().get("extrafield"));
        }
    }

    @Test
    void doesNotAddClonesToSingleNetworkPool() {
        AbstractNetworkPool pool = AbstractNetworkPool.create(network, otherVariant, 1, true);
        pool.initClones(6);
        assertEquals(1, pool.getNetworkNumberOfClones());
    }

    @Test
    void addClonesUnderMaxLimit() {
        AbstractNetworkPool pool = AbstractNetworkPool.create(network, otherVariant, 8, false);
        pool.initClones(6);
        assertEquals(6, pool.getNetworkNumberOfClones());
    }

    @Test
    void addClonesOverMaxLimit() {
        AbstractNetworkPool pool = AbstractNetworkPool.create(network, otherVariant, 8, false);
        pool.initClones(14);
        assertEquals(8, pool.getNetworkNumberOfClones());
    }

    @Test
    void initClonesAtConstruction() {
        AbstractNetworkPool pool = AbstractNetworkPool.create(network, otherVariant, 8, true);
        assertEquals(8, pool.getNetworkNumberOfClones());
    }

    // Does not pass so far
    @Test
    void checkSameInitialVariant() throws InterruptedException {
        Set<String> variantsIds = new HashSet<>(network.getVariantManager().getVariantIds());
        AbstractNetworkPool pool = AbstractNetworkPool.create(network, otherVariant, 12, false);

        pool.initClones(1);
        Network newNetwork = pool.getAvailableNetwork();

        pool.releaseUsedNetwork(newNetwork);

        pool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        assertEquals(variantsIds, new HashSet<>(network.getVariantManager().getVariantIds()));
    }
}
