/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.Threshold;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CnecResultAdderImplTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testAddCnecResultExtension() {

        Threshold threshold = Mockito.mock(Threshold.class);
        State state = Mockito.mock(State.class);
        NetworkElement networkElement = Mockito.mock(NetworkElement.class);

        Cnec cnec = new SimpleCnec("id", networkElement, threshold, state);

        cnec.newExtension(CnecResultAdderImpl.class).withFlowInMW(-500.0).withFlowInA(750.0).add();

        assertNotNull(cnec.getExtension(CnecResult.class));
        assertEquals(-500.0, cnec.getExtension(CnecResult.class).getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(750.0, cnec.getExtension(CnecResult.class).getFlowInA(), DOUBLE_TOLERANCE);
    }
}
