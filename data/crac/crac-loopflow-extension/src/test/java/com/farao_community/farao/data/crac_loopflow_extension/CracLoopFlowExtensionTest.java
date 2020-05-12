/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.powsybl.iidm.network.Country;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class CracLoopFlowExtensionTest {
    @Test
    public void testCracLoopFlowExtension() {
        GlskProvider glskProvider = Mockito.mock(GlskProvider.class);
        List<String> countriesAsString = new ArrayList<>(Arrays.asList("FR", "DE", "BE", "NL"));
        List<Country> countries = countriesAsString.stream().map(Country::valueOf).collect(Collectors.toList());

        CracLoopFlowExtension cracLoopFlowExtension = new CracLoopFlowExtension();
        cracLoopFlowExtension.setGlskProvider(glskProvider);
        cracLoopFlowExtension.setCountriesForLoopFlow(countries);

        Assert.assertFalse(cracLoopFlowExtension.getCountriesForLoopFlow().isEmpty());
        Assert.assertNotNull(cracLoopFlowExtension.getGlskProvider());
        Assert.assertEquals("CracLoopFlowExtension", cracLoopFlowExtension.getName());
    }

    @Test
    public void testCnecLoopFlowExtension() {
        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension();
        assertEquals(0.0, cnecLoopFlowExtension.getInputLoopFlow(), 0.1);
        CnecLoopFlowExtension cnecLoopFlowExtensionBeta = new CnecLoopFlowExtension(100.0);
        assertEquals(100, cnecLoopFlowExtensionBeta.getInputLoopFlow(), 0.1);
    }
}
