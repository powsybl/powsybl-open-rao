/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.sensitivity.*;
import org.junit.Test;
import org.mockito.Mockito;


import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class SystematicSensitivityAdapterTest {
    @Test
    public void testLoadflowServiceInitialisation() {
        Network network = Mockito.mock(Network.class);
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        Mockito.when(variantManager.getWorkingVariantId()).thenReturn("");
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);

        assertTrue(SystematicSensitivityAdapter.runSensitivity(network, Mockito.mock(CnecSensitivityProvider.class), Mockito.mock(SensitivityAnalysisParameters.class)).isSuccess());
    }
}
