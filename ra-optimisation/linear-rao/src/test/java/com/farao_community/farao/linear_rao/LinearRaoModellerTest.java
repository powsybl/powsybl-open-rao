/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class LinearRaoModellerTest {
    private LinearRaoModeller linearRaoModeller;
    private LinearRaoProblem linearRaoProblemMock;

    @Before
    public void setUp() {
        linearRaoProblemMock = Mockito.mock(LinearRaoProblem.class);

        Crac cracMock = Mockito.mock(Crac.class);
        Network networkMock = Mockito.mock(Network.class);
        SystematicSensitivityAnalysisResult sensitivityResultMock = Mockito.mock(SystematicSensitivityAnalysisResult.class);

        linearRaoModeller = new LinearRaoModeller(cracMock, networkMock, sensitivityResultMock, linearRaoProblemMock);
    }

    @Test
    public void updateTest() {
        Cnec cnecMock = Mockito.mock(Cnec.class);
        Map<Cnec, Double> cnecMarginMap = new HashMap<>();
        cnecMarginMap.put(cnecMock, 10.0);
        Map<Cnec, Double> cnecThresholdMap = new HashMap<>();
        cnecThresholdMap.put(cnecMock, 500.0);
        SystematicSensitivityAnalysisResult sensitivityAnalysisResultMock = Mockito.mock(SystematicSensitivityAnalysisResult.class);
        Mockito.when(sensitivityAnalysisResultMock.getCnecMarginMap()).thenReturn(cnecMarginMap);
        Mockito.when(sensitivityAnalysisResultMock.getCnecMaxThresholdMap()).thenReturn(cnecThresholdMap);

        linearRaoModeller.updateProblem(sensitivityAnalysisResultMock);
        assertEquals(490.0, linearRaoModeller.getData().getReferenceFlow(cnecMock), 0.1);
    }

}
