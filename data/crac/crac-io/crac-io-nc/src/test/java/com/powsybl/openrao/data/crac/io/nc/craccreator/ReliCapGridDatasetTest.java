/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class ReliCapGridDatasetTest {
    private static final String NETWORK_PATH = "/networks/ReliCapGrid.zip";
    private static final String NC_PATH = "/profiles/ReliCapGrid_CGMES_NC.zip";
    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.parse("2022-06-15T22:30:00Z");

    private final CracCreationParameters cracCreationParameters = new CracCreationParameters();
    private final NcCracCreationParameters ncCracCreationParameters = new NcCracCreationParameters();

    {
        ncCracCreationParameters.setCreateAutoInstant(false);
        ncCracCreationParameters.setCurativeInstants(Map.of("curative", 60));
        ncCracCreationParameters.setCapacityCalculationRegion(null);
        cracCreationParameters.addExtension(NcCracCreationParameters.class, ncCracCreationParameters);
    }

    // TODO: add test to check network content

    @Test
    void testImportReliCapGridDatasetTest() {
        Network network = NcCracCreationTestUtil.getNetworkFromResource(NETWORK_PATH);
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext(NC_PATH, network, TIMESTAMP, cracCreationParameters);
        Crac crac = cracCreationContext.getCrac();
        assertNotNull(crac);

        // check base CRAC data
        List<Instant> instants = crac.getSortedInstants();
        assertEquals(3, instants.size());
        assertInstant(instants.getFirst(), InstantKind.PREVENTIVE, "preventive");
        assertInstant(instants.get(1), InstantKind.OUTAGE, "outage");
        assertInstant(instants.getLast(), InstantKind.CURATIVE, "curative");

        assertEquals(16, crac.getContingencies().size()); // as expected
        assertEquals(12, crac.getPstRangeActions().size()); // as expected
        // TODO: implement and check -> assertEquals(32, crac.getInjectionRangeActions().size());
        // TODO: allow curative CT + FIXME why 0? -> assertEquals(8, crac.getCounterTradeRangeActions().size());
    }

    private static void assertInstant(Instant instant, InstantKind expectedInstantKind, String expectedId) {
        assertEquals(expectedInstantKind, instant.getKind());
        assertEquals(expectedId, instant.getId());
    }
}
