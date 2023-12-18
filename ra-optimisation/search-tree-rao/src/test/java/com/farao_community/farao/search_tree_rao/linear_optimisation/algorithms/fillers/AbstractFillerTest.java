/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.range.RangeType;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeAction;
import com.powsybl.open_rao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.open_rao.data.crac_io_api.CracImporters;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPSolver;
import com.powsybl.open_rao.search_tree_rao.result.api.FlowResult;
import com.powsybl.open_rao.search_tree_rao.result.api.SensitivityResult;
import com.powsybl.iidm.network.Network;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
abstract class AbstractFillerTest {
    static final double DOUBLE_TOLERANCE = 1e-4;

    static final String PREVENTIVE_INSTANT_ID = "preventive";

    // data related to the two Cnecs
    static final double MIN_FLOW_1 = -750.0;
    static final double MAX_FLOW_1 = 750.0;

    static final double REF_FLOW_CNEC1_IT1 = 500.0;
    static final double REF_FLOW_CNEC2_IT1 = 300.0;
    static final double REF_FLOW_CNEC1_IT2 = 400.0;
    static final double REF_FLOW_CNEC2_IT2 = 350.0;

    static final double SENSI_CNEC1_IT1 = 2.0;
    static final double SENSI_CNEC2_IT1 = 5.0;
    static final double SENSI_CNEC1_IT2 = 3.0;
    static final double SENSI_CNEC2_IT2 = -7.0;

    // data related to the Range Action
    static final int TAP_INITIAL = 5;
    static final int TAP_IT2 = -7;

    static final String CNEC_1_ID = "Tieline BE FR - N - preventive"; // monitored on left side
    static final String CNEC_2_ID = "Tieline BE FR - Defaut - N-1 NL1-NL3"; // monitored on right side
    static final String RANGE_ACTION_ID = "PRA_PST_BE";
    static final String RANGE_ACTION_ELEMENT_ID = "BBE2AA1  BBE3AA1  1";

    FaraoMPSolver mpSolver;
    FlowCnec cnec1;
    FlowCnec cnec2;
    PstRangeAction pstRangeAction;
    FlowResult flowResult;
    SensitivityResult sensitivityResult;
    Crac crac;
    Network network;

    void init() {
        // arrange some data for all fillers test
        // crac and network
        crac = CracImporters.importCrac("crac/small-crac.json", getClass().getResourceAsStream("/crac/small-crac.json"));
        network = NetworkImportsUtil.import12NodesNetwork();

        // get cnec and rangeAction
        cnec1 = crac.getFlowCnec(CNEC_1_ID);
        cnec2 = crac.getFlowCnec(CNEC_2_ID);
        pstRangeAction = crac.getPstRangeAction(RANGE_ACTION_ID);

        // MPSolver and linearRaoProblem
        mpSolver = new FaraoMPSolver();

        flowResult = Mockito.mock(FlowResult.class);
        when(flowResult.getFlow(cnec1, Side.LEFT, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC1_IT1);
        when(flowResult.getFlow(cnec2, Side.RIGHT, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC2_IT1);

        sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityValue(cnec1, Side.LEFT, pstRangeAction, Unit.MEGAWATT)).thenReturn(SENSI_CNEC1_IT1);
        when(sensitivityResult.getSensitivityValue(cnec2, Side.RIGHT, pstRangeAction, Unit.MEGAWATT)).thenReturn(SENSI_CNEC2_IT1);
    }

    protected void addPstGroupInCrac() {
        Map<Integer, Double> tapToAngle = pstRangeAction.getTapToAngleConversionMap();
        crac.removePstRangeAction(RANGE_ACTION_ID);

        crac.newPstRangeAction()
                .withId("pst1-group1")
                .withGroupId("group1")
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .withInitialTap(0)
                .withTapToAngleConversionMap(tapToAngle)
                .newTapRange()
                .withRangeType(RangeType.ABSOLUTE)
                .withMinTap(-2)
                .withMaxTap(5)
                .add()
                .withOperator("RTE")
                .add();
        crac.newPstRangeAction()
                .withId("pst2-group1")
                .withGroupId("group1")
                .withNetworkElement("BBE1AA1  BBE3AA1  1")
                .withInitialTap(0)
                .withTapToAngleConversionMap(tapToAngle)
                .newTapRange()
                .withRangeType(RangeType.ABSOLUTE)
                .withMinTap(-5)
                .withMaxTap(10)
                .add()
                .withOperator("RTE")
                .add();
    }

    protected void useNetworkWithTwoPsts() {
        network = NetworkImportsUtil.import12NodesWith2PstsNetwork();
    }
}
