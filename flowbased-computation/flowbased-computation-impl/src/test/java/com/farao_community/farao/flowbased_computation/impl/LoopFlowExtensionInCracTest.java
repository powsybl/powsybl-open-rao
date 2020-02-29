/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LoopFlowExtensionInCracTest {
    @Test
    public void testRun() {
        GlskProvider glskProvider = MinRamAdjustmentExampleGenerator.glskProviderCore();
        List<String> countries = new ArrayList<>(Arrays.asList("FR", "DE", "BE", "NL"));

        LoopFlowExtensionInCrac loopFlowExtensionInCrac = new LoopFlowExtensionInCrac();
        loopFlowExtensionInCrac.setGlskProvider(glskProvider);
        loopFlowExtensionInCrac.setCountriesForLoopFlow(countries);

        assertFalse(loopFlowExtensionInCrac.getCountriesForLoopFlow().isEmpty());
        assertNotNull(loopFlowExtensionInCrac.getGlskProvider());
    }
}
