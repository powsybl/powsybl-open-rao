/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LoopFlowComputationServiceTest {

    @Test(expected = FaraoException.class)
    public void testRunLoopFlowExtensionInCracNotAvailable() {
        RaoData raoData = Mockito.mock(RaoData.class);
        Crac crac = Mockito.mock(Crac.class);
        Mockito.when(raoData.getCrac()).thenReturn(crac);
        LoopFlowComputationService.checkDataConsistency(raoData);
    }

}
