/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoDataManagerTest {

    @Test
    public void testCalculateLoopFlowConstraintAndUpdateAllCnec() {
        Network network = ExampleGenerator.network();
        Crac crac = ExampleGenerator.crac();
        RaoData raoData = new RaoData(network, crac);
        Map<String, Double> fzeroallmap = new HashMap<>();
        fzeroallmap.put("FR-BE", 0.0);
        fzeroallmap.put("FR-DE", 0.0);
        fzeroallmap.put("BE-NL", 0.0);
        fzeroallmap.put("DE-NL", 0.0);
        Map<Cnec, Double> loopflowShifts = new HashMap<>();
        loopflowShifts.put(crac.getCnec("FR-BE"), 0.0);
        loopflowShifts.put(crac.getCnec("FR-DE"), 0.0);
        loopflowShifts.put(crac.getCnec("BE-NL"), 0.0);
        loopflowShifts.put(crac.getCnec("DE-NL"), 0.0);
        CracLoopFlowExtension cracLoopFlowExtension = new CracLoopFlowExtension();
        crac.addExtension(CracLoopFlowExtension.class, cracLoopFlowExtension);
        raoData.getRaoDataManager().fillCracResultsWithLoopFlowConstraints(fzeroallmap, loopflowShifts);
        crac.getCnecs(crac.getPreventiveState()).forEach(cnec -> {
            assertEquals(100.0, cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint(), 1E-1);
        });
    }
}
