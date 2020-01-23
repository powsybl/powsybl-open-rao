/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.range_domain.AbsoluteFixedRange;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.ComplexRangeAction;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstRange;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.linear_rao.fillers.CoreProblemFiller;
import com.farao_community.farao.linear_rao.mocks.MPSolverMock;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@Ignore
public class LinearRaoSkeletonTest {
    private LinearRaoProblem linearRaoProblem;

    @Before
    public void setUp() {
        MPSolverMock solver = new MPSolverMock();
        linearRaoProblem = new LinearRaoProblem(solver);
    }

    @Test
    public void test() {
        AbstractProblemFiller coreFiller = new CoreProblemFiller();
        Network network = Importers.loadNetwork("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));
        Crac crac = new SimpleCrac("crac-test");
        State preventiveState = new SimpleState(Optional.empty(), new Instant("N", 0));
        crac.addState(preventiveState);
        crac.addCnec(new SimpleCnec(
            "cnec-test",
            new NetworkElement("network-element-test"),
            new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.BOTH, 500),
            preventiveState
        ));
        UsageRule usageRule = new FreeToUse(UsageMethod.AVAILABLE, crac.getPreventiveState());
        Range range = new AbsoluteFixedRange(-16, 16, RangeDefinition.CENTERED_ON_ZERO);
        ApplicableRangeAction applicableRangeAction = new PstRange(new NetworkElement("BBE2AA1  BBE3AA1  1"));
        RangeAction rangeAction = new ComplexRangeAction(
            "pst-range-test",
            "RTE",
            Collections.singletonList(usageRule),
            Collections.singletonList(range),
            Collections.singleton(applicableRangeAction)
        );
        crac.addRangeAction(rangeAction);
        LinearRaoData linearRaoData = new LinearRaoData(crac, network, null);
        LinearRaoModeller linearRaoModeller = new LinearRaoModeller(linearRaoProblem, linearRaoData, Collections.singletonList(coreFiller), null, null);

        linearRaoModeller.buildProblem();
        linearRaoModeller.updateProblem(null);
        linearRaoModeller.solve();

        //LinearRaoData linearRaoData = new LinearRaoData(null, null, null);
        linearRaoData.getCrac();
        linearRaoData.getNetwork();
        assertEquals(0.0, linearRaoData.getReferenceFlow(null), 1e-10);
        assertEquals(0.0, linearRaoData.getSensitivity(null, null), 1e-10);
        assertEquals(0.0, linearRaoData.getReferenceFlow(null), 1e-10);
    }
}
