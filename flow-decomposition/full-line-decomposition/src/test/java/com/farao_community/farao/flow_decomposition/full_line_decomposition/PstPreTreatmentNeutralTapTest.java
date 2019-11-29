/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PstPreTreatmentNeutralTapTest {
    private Network testNetwork;

    @Before
    public void setUp() {
        testNetwork = Importers.loadNetwork("testCase.xiidm", NetworkUtilTest.class.getResourceAsStream("/testCase.xiidm"));
    }

    @Test
    public void treatment() {
        assertEquals(5, testNetwork.getTwoWindingsTransformer("BBE1AA1  BBE3AA1  2").getPhaseTapChanger().getTapPosition());
        PstPreTreatmentService service = new PstPreTreatmentNeutralTap();
        service.treatment(testNetwork, Mockito.mock(FullLineDecompositionParameters.class));
        assertEquals(0, testNetwork.getTwoWindingsTransformer("BBE1AA1  BBE3AA1  2").getPhaseTapChanger().getTapPosition());
    }
}
