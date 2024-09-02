/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class MultipleSensitivityProviderTest {

    @Test
    void testCommonFactors() {

        // mock network
        Network network = Mockito.mock(Network.class);

        // mock providers
        CnecSensitivityProvider provider1 = Mockito.mock(CnecSensitivityProvider.class);
        CnecSensitivityProvider provider2 = Mockito.mock(CnecSensitivityProvider.class);

        // mock factors
        SensitivityFactor factor1 = Mockito.mock(SensitivityFactor.class);
        SensitivityFactor factor2 = Mockito.mock(SensitivityFactor.class);
        SensitivityFactor factor3 = Mockito.mock(SensitivityFactor.class);

        Mockito.when(provider1.getBasecaseFactors(any())).thenReturn(Collections.singletonList(factor1));
        Mockito.when(provider2.getBasecaseFactors(any())).thenReturn(Arrays.asList(factor2, factor3));

        MultipleSensitivityProvider multipleSensitivityProvider = new MultipleSensitivityProvider();

        // with one provider
        multipleSensitivityProvider.addProvider(provider1);
        assertEquals(1, multipleSensitivityProvider.getBasecaseFactors(network).size());

        // with two provider
        multipleSensitivityProvider.addProvider(provider2);
        assertEquals(3, multipleSensitivityProvider.getBasecaseFactors(network).size());
    }

    @Test
    void testAdditionalFactorsContingency() {

        // mock network
        Network network = Mockito.mock(Network.class);

        // mock providers
        CnecSensitivityProvider provider1 = Mockito.mock(CnecSensitivityProvider.class);
        CnecSensitivityProvider provider2 = Mockito.mock(CnecSensitivityProvider.class);

        // mock factors
        SensitivityFactor factor1 = Mockito.mock(SensitivityFactor.class);
        SensitivityFactor factor2 = Mockito.mock(SensitivityFactor.class);
        SensitivityFactor factor3 = Mockito.mock(SensitivityFactor.class);

        Mockito.when(provider1.getContingencyFactors(any(), any())).thenReturn(Collections.singletonList(factor1));
        Mockito.when(provider2.getContingencyFactors(any(), any())).thenReturn(Arrays.asList(factor2, factor3));

        MultipleSensitivityProvider multipleSensitivityProvider = new MultipleSensitivityProvider();

        // with one provider
        multipleSensitivityProvider.addProvider(provider1);
        assertEquals(1, multipleSensitivityProvider.getContingencyFactors(network, List.of(new Contingency("co", new ArrayList<>()))).size());

        // with two provider
        multipleSensitivityProvider.addProvider(provider2);
        assertEquals(3, multipleSensitivityProvider.getContingencyFactors(network, List.of(new Contingency("co", new ArrayList<>()))).size());
    }
}
