/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class CracLoopFlowExtensionTest {
    @Test
    public void testRun() {
        GlskProvider glskProvider = Mockito.mock(GlskProvider.class);
        List<String> countries = new ArrayList<>(Arrays.asList("FR", "DE", "BE", "NL"));

        CracLoopFlowExtension cracLoopFlowExtension = new CracLoopFlowExtension();
        cracLoopFlowExtension.setGlskProvider(glskProvider);
        cracLoopFlowExtension.setCountriesForLoopFlow(countries);

        Assert.assertFalse(cracLoopFlowExtension.getCountriesForLoopFlow().isEmpty());
        Assert.assertNotNull(cracLoopFlowExtension.getGlskProvider());
    }
}
