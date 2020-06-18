/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.core.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.CoreProblemFiller;
import com.farao_community.farao.rao_commons.linear_optimisation.mocks.MPSolverMock;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.*;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.mockito.Mockito.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@PrepareForTest(MPSolver.class)
abstract class AbstractFillerTest {

    static final double DOUBLE_TOLERANCE = 0.1;

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
    static final int MIN_TAP = -16;
    static final int MAX_TAP = 16;
    static final int TAP_INITIAL = 5;
    static final int TAP_IT2 = -7;

    static final String CNEC_1_ID = "Tieline BE FR - N - preventive";
    static final String CNEC_2_ID = "Tieline BE FR - Defaut - N-1 NL1-NL3";
    static final String RANGE_ACTION_ID = "PRA_PST_BE";
    static final String RANGE_ACTION_ELEMENT_ID = "BBE2AA1  BBE3AA1  1";

    Cnec cnec1;
    Cnec cnec2;
    RangeAction rangeAction;

    CoreProblemFiller coreProblemFiller;
    LinearProblem linearProblem;
    SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult;
    RaoData raoData;
    LinearProblemParameters linearProblemParameters;
    Crac crac;
    Network network;

    void init() {

        // arrange some data for all fillers test
        // crac and network
        crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));
        network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        raoData = new RaoData(network, crac);

        // get cnec and rangeAction
        cnec1 = crac.getCnecs().stream().filter(c -> c.getId().equals(CNEC_1_ID)).findFirst().orElseThrow(FaraoException::new);
        cnec2 = crac.getCnecs().stream().filter(c -> c.getId().equals(CNEC_2_ID)).findFirst().orElseThrow(FaraoException::new);
        rangeAction = crac.getRangeAction(RANGE_ACTION_ID);

        // MPSolver and linearRaoProblem
        MPSolverMock solver = new MPSolverMock();
        PowerMockito.mockStatic(MPSolver.class);
        when(MPSolver.infinity()).thenReturn(Double.POSITIVE_INFINITY);
        linearProblem = new LinearProblem(solver);

        systematicSensitivityAnalysisResult = Mockito.mock(SystematicSensitivityAnalysisResult.class);
        when(systematicSensitivityAnalysisResult.getReferenceFlow(cnec1)).thenReturn(REF_FLOW_CNEC1_IT1);
        when(systematicSensitivityAnalysisResult.getReferenceFlow(cnec2)).thenReturn(REF_FLOW_CNEC2_IT1);
        when(systematicSensitivityAnalysisResult.getSensitivityOnFlow(rangeAction, cnec1)).thenReturn(SENSI_CNEC1_IT1);
        when(systematicSensitivityAnalysisResult.getSensitivityOnFlow(rangeAction, cnec2)).thenReturn(SENSI_CNEC2_IT1);
        raoData.setSystematicSensitivityAnalysisResult(systematicSensitivityAnalysisResult);

        // parameters
        linearProblemParameters = new LinearProblemParameters();
    }
}
