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
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoDataManagerTest {

    static final double DOUBLE_TOLERANCE = 0.1;

    private Crac crac;
    private RaoData raoData;

    @Before
    public void setUp() {
        Network network = ExampleGenerator.network();
        crac = ExampleGenerator.crac();
        raoData = new RaoData(network, crac);
    }

    @Test
    public void testCalculateLoopFlowConstraintAndUpdateAllCnec() {
        //CnecLoopFlowExtension
        crac.getCnecs().forEach(cnec -> {
            CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension(100.0);
            cnec.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);
        });
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
            assertEquals(100.0, cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint(), DOUBLE_TOLERANCE);
        });
    }

    @Test
    public void testLoopflowRelated() {
        CnecLoopFlowExtension cnec1LoopFlowExtension = new CnecLoopFlowExtension(0.0);
        cnec1LoopFlowExtension.setLoopFlowConstraint(0.0);
        crac.getCnec("FR-BE").addExtension(CnecLoopFlowExtension.class, cnec1LoopFlowExtension);
        Map<String, Double> loopflows = new HashMap<>();
        loopflows.put("FR-BE", 1.0);
        raoData.getRaoDataManager().fillCracResultsWithLoopFlows(loopflows, 1.0);
        assertEquals(1.0, raoData.getCracResult().getVirtualCost(), DOUBLE_TOLERANCE);
        raoData.getRaoDataManager().fillCracResultsWithLoopFlows(loopflows, 0.0);
        assertEquals(1000000.0, raoData.getCracResult().getVirtualCost(), DOUBLE_TOLERANCE);
    }
}
