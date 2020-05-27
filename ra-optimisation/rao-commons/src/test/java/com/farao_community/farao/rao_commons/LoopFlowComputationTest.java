/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import org.junit.Test;
import org.mockito.Mockito;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LoopFlowComputationTest {

    @Test
    public void testCalculateLoopFlowConstraintAndUpdateAllCnec() {
        Crac crac = ExampleGenerator.crac();
        Map<String, Double> fzeroallmap = new HashMap<>();
        fzeroallmap.put("FR-BE", 0.0);
        fzeroallmap.put("FR-DE", 0.0);
        fzeroallmap.put("BE-NL", 0.0);
        fzeroallmap.put("DE-NL", 0.0);
        CracLoopFlowExtension cracLoopFlowExtension = new CracLoopFlowExtension();
        crac.addExtension(CracLoopFlowExtension.class, cracLoopFlowExtension);
        LoopFlowComputation.updateCnecsLoopFlowConstraint(crac, fzeroallmap);
        crac.getCnecs(crac.getPreventiveState()).forEach(cnec -> {
            assertEquals(100.0, cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint(), 1E-1);
        });
    }

    @Test(expected = FaraoException.class)
    public void testRunLoopFlowExtensionInCracNotAvailable() {
        RaoData raoData = Mockito.mock(RaoData.class);
        Crac crac = Mockito.mock(Crac.class);
        Mockito.when(crac.getExtension(CracLoopFlowExtension.class)).thenReturn(null);
        Mockito.when(raoData.getCrac()).thenReturn(crac);
        LoopFlowComputation.run(raoData);
    }

}
