package com.farao_community.farao.util;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.*;

public class FaraoVariantsPoolTest {

    @Test
    public void variantPoolUsageTest() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));

        String initialVariant = network.getVariantManager().getWorkingVariantId();
        String otherVariant = "otherVariant";
        network.getVariantManager().cloneVariant(initialVariant, otherVariant);

        try (FaraoVariantsPool pool = new FaraoVariantsPool(network, otherVariant, 10)) {
            String variant = pool.getAvailableVariant();

            assertNotNull(variant);
            assertTrue(variant.matches(otherVariant + " variant modified [0-9]+"));

            pool.releaseUsedVariant(variant);
        } catch (InterruptedException e) {
            fail();
            Thread.currentThread().interrupt();
        }
    }
}