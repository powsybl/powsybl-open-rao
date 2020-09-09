/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.refprog.reference_program.ReferenceExchangeData;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoInputTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private static final String INITIAL_VARIANT_ID = "initial-variant-id";
    private static final String VARIANT_ID = "variant-id";

    private Network network;
    private Crac crac;
    private RaoInput.RaoInputBuilder defaultBuilder;

    @Before
    public void setUp() {
        network = Mockito.mock(Network.class);
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        Mockito.when(variantManager.getWorkingVariantId()).thenReturn(INITIAL_VARIANT_ID);
        crac = Mockito.mock(Crac.class);
        defaultBuilder = RaoInput.builder().withNetwork(network).withCrac(crac).withVariantId(VARIANT_ID);
    }

    @Test(expected = RaoInputException.class)
    public void failWithoutNetwork() {
        RaoInput.builder().withCrac(crac).withVariantId(VARIANT_ID).build();
    }

    @Test(expected = RaoInputException.class)
    public void failWithoutCrac() {
        RaoInput.builder().withNetwork(network).withVariantId(VARIANT_ID).build();
    }

    @Test
    public void failWithoutVariantId() {
        RaoInput raoInput = RaoInput.builder().withNetwork(network).withCrac(crac).build();
        assertEquals(INITIAL_VARIANT_ID, raoInput.getVariantId());
    }

    @Test
    public void successMinimalBuild() {
        RaoInput raoInput = defaultBuilder.build();
        assertSame(network, raoInput.getNetwork());
        assertSame(crac, raoInput.getCrac());
        assertEquals(VARIANT_ID, raoInput.getVariantId());
    }

    @Test
    public void testBuildWithoutRefProg() {
        RaoInput raoInput = defaultBuilder.build();
        assertFalse(raoInput.getReferenceProgram().isPresent());
    }

    @Test
    public void testBuildWithRefProg() {
        List<ReferenceExchangeData> referenceExchangeDataList = Arrays.asList(
                new ReferenceExchangeData(Country.FR, Country.BE, 100),
                new ReferenceExchangeData(Country.DE, Country.NL, -200));
        RaoInput raoInput = defaultBuilder
                .withRefProg(new ReferenceProgram(referenceExchangeDataList))
                .build();
        assertTrue(raoInput.getReferenceProgram().isPresent());
        ReferenceProgram actualRefProg =  raoInput.getReferenceProgram().get();
        assertEquals(2, actualRefProg.getReferenceExchangeDataList().size());
        assertEquals(100, actualRefProg.getExchange(Country.FR, Country.BE), DOUBLE_TOLERANCE);
        assertEquals(-200, actualRefProg.getExchange(Country.DE, Country.NL), DOUBLE_TOLERANCE);
    }
}
