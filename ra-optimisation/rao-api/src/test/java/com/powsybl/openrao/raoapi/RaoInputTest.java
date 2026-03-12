/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceExchangeData;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RaoInputTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private static final String INITIAL_VARIANT_ID = "initial-variant-id";
    private static final String VARIANT_ID = "variant-id";

    private Network network;
    private Crac crac;
    private RaoInput.RaoInputBuilder defaultBuilder;

    @BeforeEach
    public void setUp() {
        network = Mockito.mock(Network.class);
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        Mockito.when(variantManager.getWorkingVariantId()).thenReturn(INITIAL_VARIANT_ID);
        crac = Mockito.mock(Crac.class);
        defaultBuilder = RaoInput.build(network, crac).withNetworkVariantId(VARIANT_ID);
    }

    @Test
    void successMinimalBuild() {
        RaoInput raoInput = defaultBuilder.build();
        assertSame(network, raoInput.getNetwork());
        assertSame(crac, raoInput.getCrac());
        assertEquals(VARIANT_ID, raoInput.getNetworkVariantId());
    }

    @Test
    void testBuildWithoutRefProg() {
        RaoInput raoInput = defaultBuilder.build();
        assertNull(raoInput.getReferenceProgram());
    }

    @Test
    void testBuildWithRefProg() {
        EICode areaFrance = new EICode(Country.FR);
        EICode areaBelgium = new EICode(Country.BE);
        EICode areaNetherlands = new EICode(Country.NL);
        EICode areaGermany = new EICode(Country.DE);
        List<ReferenceExchangeData> referenceExchangeDataList = Arrays.asList(
                new ReferenceExchangeData(areaFrance, areaBelgium, 100),
                new ReferenceExchangeData(areaGermany, areaNetherlands, -200));
        RaoInput raoInput = defaultBuilder
                .withRefProg(new ReferenceProgram(referenceExchangeDataList))
                .build();
        assertNotNull(raoInput.getReferenceProgram());
        ReferenceProgram actualRefProg = raoInput.getReferenceProgram();
        assertEquals(2, actualRefProg.getReferenceExchangeDataList().size());
        assertEquals(100, actualRefProg.getExchange(areaFrance, areaBelgium), DOUBLE_TOLERANCE);
        assertEquals(-200, actualRefProg.getExchange(areaGermany, areaNetherlands), DOUBLE_TOLERANCE);
    }

}
