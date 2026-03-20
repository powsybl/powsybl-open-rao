/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.functionalcostcomputer;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.impl.FlowCnecImpl;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class FunctionalCostComputerTestUtils {
    protected Crac crac;
    protected State autoStateCo1;
    protected State autoStateCo2;
    protected State curativeStateCo1;
    protected State curativeStateCo2;
    protected ObjectiveFunctionResult initialResult;
    protected OptimizationResult secondPreventivePerimeterResult;
    protected Map<State, OptimizationResult> postContingencyResults;

    protected void init() {
        crac = CracFactory.findDefault().create("crac");
        crac.newInstant("preventive", InstantKind.PREVENTIVE);
        crac.newInstant("outage", InstantKind.OUTAGE);
        crac.newInstant("auto", InstantKind.AUTO);
        crac.newInstant("curative", InstantKind.CURATIVE);

        // mock states

        autoStateCo1 = Mockito.mock(State.class);
        Mockito.when(autoStateCo1.getInstant()).thenReturn(crac.getInstant("auto"));

        autoStateCo2 = Mockito.mock(State.class);
        Mockito.when(autoStateCo2.getInstant()).thenReturn(crac.getInstant("auto"));

        curativeStateCo1 = Mockito.mock(State.class);
        Mockito.when(curativeStateCo1.getInstant()).thenReturn(crac.getInstant("curative"));

        curativeStateCo2 = Mockito.mock(State.class);
        Mockito.when(curativeStateCo2.getInstant()).thenReturn(crac.getInstant("curative"));

        // mock flow cnecs

        FlowCnec autoFlowCnec = Mockito.mock(FlowCnecImpl.class);
        FlowCnec curativeFlowCnec = Mockito.mock(FlowCnecImpl.class);

        // mock optimization results

        initialResult = Mockito.mock(ObjectiveFunctionResult.class);
        Mockito.when(initialResult.getFunctionalCost()).thenReturn(50.0);

        secondPreventivePerimeterResult = Mockito.mock(OptimizationResult.class);
        Mockito.when(secondPreventivePerimeterResult.getFunctionalCost()).thenReturn(100.0);

        OptimizationResult autoPerimeterResultCo1 = Mockito.mock(OptimizationResult.class);
        Mockito.when(autoPerimeterResultCo1.getFunctionalCost()).thenReturn(30.0);
        Mockito.when(autoPerimeterResultCo1.getMostLimitingElements(1)).thenReturn(List.of(autoFlowCnec));

        OptimizationResult autoPerimeterResultCo2 = Mockito.mock(OptimizationResult.class);
        Mockito.when(autoPerimeterResultCo2.getFunctionalCost()).thenReturn(17.0);
        Mockito.when(autoPerimeterResultCo2.getMostLimitingElements(1)).thenReturn(List.of(autoFlowCnec));

        OptimizationResult curativePerimeterResultCo1 = Mockito.mock(OptimizationResult.class);
        Mockito.when(curativePerimeterResultCo1.getFunctionalCost()).thenReturn(250.0);
        Mockito.when(curativePerimeterResultCo1.getMostLimitingElements(1)).thenReturn(List.of(curativeFlowCnec));

        OptimizationResult curativePerimeterResultCo2 = Mockito.mock(OptimizationResult.class);
        Mockito.when(curativePerimeterResultCo2.getFunctionalCost()).thenReturn(110.0);
        Mockito.when(curativePerimeterResultCo2.getMostLimitingElements(1)).thenReturn(List.of(curativeFlowCnec));

        // mock post-contingency results

        postContingencyResults = Map.of(
            autoStateCo1, autoPerimeterResultCo1,
            autoStateCo2, autoPerimeterResultCo2,
            curativeStateCo1, curativePerimeterResultCo1,
            curativeStateCo2, curativePerimeterResultCo2
        );
    }
}
