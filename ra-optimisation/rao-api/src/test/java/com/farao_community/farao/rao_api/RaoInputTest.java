/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.data.refprog.reference_program.ReferenceExchangeData;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.commons.EICode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        defaultBuilder = RaoInput.build(network, crac).withNetworkVariantId(VARIANT_ID);
    }

    @Test
    public void successMinimalBuild() {
        RaoInput raoInput = defaultBuilder.build();
        assertSame(network, raoInput.getNetwork());
        assertSame(crac, raoInput.getCrac());
        assertEquals(VARIANT_ID, raoInput.getNetworkVariantId());
    }

    @Test
    public void testBuildWithoutRefProg() {
        RaoInput raoInput = defaultBuilder.build();
        assertNull(raoInput.getReferenceProgram());
    }

    @Test
    public void testBuildWithRefProg() {
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
        ReferenceProgram actualRefProg =  raoInput.getReferenceProgram();
        assertEquals(2, actualRefProg.getReferenceExchangeDataList().size());
        assertEquals(100, actualRefProg.getExchange(areaFrance, areaBelgium), DOUBLE_TOLERANCE);
        assertEquals(-200, actualRefProg.getExchange(areaGermany, areaNetherlands), DOUBLE_TOLERANCE);
    }

    @Test(expected = FaraoException.class)
    public void testBuildWithBaseCaseVariantAndNoResultVariantManager() {
        Mockito.when(crac.getExtension(ResultVariantManager.class)).thenReturn(null);
        defaultBuilder
            .withBaseCracVariantId("pre-optim-variant")
            .build();
    }

    @Test(expected = FaraoException.class)
    public void testBuildWithBaseCaseVariantAndNonExistingVariant() {
        ResultVariantManager resultVariantManager = Mockito.mock(ResultVariantManager.class);
        Mockito.when(crac.getExtension(ResultVariantManager.class)).thenReturn(resultVariantManager);
        Mockito.when(resultVariantManager.getVariants()).thenReturn(Stream.of("pre-optim-variant", "post-optim-variant").collect(Collectors.toSet()));
        defaultBuilder
            .withBaseCracVariantId("non-existing-variant")
            .build();
    }

    @Test(expected = FaraoException.class)
    public void testBuildWithNoBaseCaseVariantAndExistingVariants() {
        ResultVariantManager resultVariantManager = Mockito.mock(ResultVariantManager.class);
        Mockito.when(crac.getExtension(ResultVariantManager.class)).thenReturn(resultVariantManager);
        Mockito.when(resultVariantManager.getVariants()).thenReturn(Stream.of("pre-optim-variant", "post-optim-variant").collect(Collectors.toSet()));
        defaultBuilder.build();
    }

    @Test
    public void testBuildWithBaseCaseVariantAndExistingVariant() {
        ResultVariantManager resultVariantManager = Mockito.mock(ResultVariantManager.class);
        Mockito.when(crac.getExtension(ResultVariantManager.class)).thenReturn(resultVariantManager);
        Mockito.when(resultVariantManager.getVariants()).thenReturn(Stream.of("pre-optim-variant", "post-optim-variant").collect(Collectors.toSet()));
        RaoInput raoInput = defaultBuilder
            .withBaseCracVariantId("post-optim-variant")
            .build();
        assertEquals("post-optim-variant", raoInput.getBaseCracVariantId());
    }
}
