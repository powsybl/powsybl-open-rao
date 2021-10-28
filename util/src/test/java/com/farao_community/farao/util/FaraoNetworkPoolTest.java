package com.farao_community.farao.util;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FaraoNetworkPoolTest {
    private Network network;
    private String initialVariant;
    private String otherVariant = "otherVariant";

    @Before
    public void setUp() {
        network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        initialVariant = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().cloneVariant(initialVariant, otherVariant);
    }

    @Test
    public void testCreate() {
        assertFalse(FaraoNetworkPool.create(network, otherVariant, 10) instanceof SingleNetworkPool);
        assertTrue(FaraoNetworkPool.create(network, otherVariant, 1) instanceof SingleNetworkPool);
    }

    @Test
    public void networkPoolUsageTest() {
        try (FaraoNetworkPool pool = FaraoNetworkPool.create(network, otherVariant, 10)) {
            Network networkCopy = pool.getAvailableNetwork();

            assertNotNull(networkCopy);
            assertNotEquals(network, networkCopy);
            assertTrue(networkCopy.getVariantManager().getWorkingVariantId().startsWith("FaraoNetworkPool working variant"));

            pool.releaseUsedNetwork(networkCopy);
        } catch (InterruptedException e) {
            fail();
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void singleNetworkPoolUsageTest() throws InterruptedException {
        FaraoNetworkPool pool = FaraoNetworkPool.create(network, otherVariant, 1);
        Network networkCopy = pool.getAvailableNetwork();

        assertNotNull(networkCopy);
        assertEquals(network, networkCopy);
        assertEquals(4, network.getVariantManager().getVariantIds().size());
        assertTrue(networkCopy.getVariantManager().getWorkingVariantId().startsWith("FaraoNetworkPool working variant"));

        pool.releaseUsedNetwork(networkCopy);

        pool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        assertEquals(2, network.getVariantManager().getVariantIds().size());
    }
}
