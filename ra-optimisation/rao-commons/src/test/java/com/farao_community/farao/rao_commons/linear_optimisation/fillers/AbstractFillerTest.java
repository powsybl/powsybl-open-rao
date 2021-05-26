/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.OnStateImpl;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.rao_api.results.FlowResult;
import com.farao_community.farao.rao_api.results.SensitivityResult;
import com.farao_community.farao.rao_commons.linear_optimisation.mocks.MPSolverMock;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.Network;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.mockito.Mockito.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@PrepareForTest(MPSolver.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
abstract class AbstractFillerTest {
    static final double DOUBLE_TOLERANCE = 0.01;

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

    static final String CNEC_1_ID = "Tieline BE FR - N - preventive";
    static final String CNEC_2_ID = "Tieline BE FR - Defaut - N-1 NL1-NL3";
    static final String RANGE_ACTION_ID = "PRA_PST_BE";
    static final String RANGE_ACTION_ELEMENT_ID = "BBE2AA1  BBE3AA1  1";

    MPSolver mpSolver;
    FlowCnec cnec1;
    FlowCnec cnec2;
    RangeAction rangeAction;
    FlowResult flowResult;
    SensitivityResult sensitivityResult;
    Crac crac;
    Network network;

    void init() {
        // arrange some data for all fillers test
        // crac and network
        crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));
        network = NetworkImportsUtil.import12NodesNetwork();

        // get cnec and rangeAction
        cnec1 = crac.getFlowCnecs().stream().filter(c -> c.getId().equals(CNEC_1_ID)).findFirst().orElseThrow(FaraoException::new);
        cnec2 = crac.getFlowCnecs().stream().filter(c -> c.getId().equals(CNEC_2_ID)).findFirst().orElseThrow(FaraoException::new);
        rangeAction = crac.getRangeAction(RANGE_ACTION_ID);
        rangeAction.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        rangeAction.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getState("N-1 NL1-NL3", Instant.OUTAGE)));

        // MPSolver and linearRaoProblem
        mpSolver = new MPSolverMock();
        PowerMockito.mockStatic(MPSolver.class);
        when(MPSolver.infinity()).thenAnswer((Answer<Double>) invocation -> Double.POSITIVE_INFINITY);

        flowResult = Mockito.mock(FlowResult.class);
        when(flowResult.getFlow(cnec1, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC1_IT1);
        when(flowResult.getFlow(cnec2, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC2_IT1);

        sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityValue(cnec1, rangeAction, Unit.MEGAWATT)).thenReturn(SENSI_CNEC1_IT1);
        when(sensitivityResult.getSensitivityValue(cnec2, rangeAction, Unit.MEGAWATT)).thenReturn(SENSI_CNEC2_IT1);
    }
}
