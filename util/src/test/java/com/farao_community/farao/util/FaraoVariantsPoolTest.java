/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
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
