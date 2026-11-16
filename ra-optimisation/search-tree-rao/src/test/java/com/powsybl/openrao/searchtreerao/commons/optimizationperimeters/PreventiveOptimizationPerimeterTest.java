/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.impl.extensions.HvdcAngleDroopActivePowerControlImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;
import java.util.function.Consumer;

import static com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil.addHvdcLine;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class PreventiveOptimizationPerimeterTest extends AbstractOptimizationPerimeterTest {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void fullPreventivePerimeter1Test() {
        Mockito.when(prePerimeterResult.getSetpoint(pRA)).thenReturn(500.);
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        Perimeter preventivePerimeter = new Perimeter(pState, Set.of(oState1, oState2, cState2));
        OptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(preventivePerimeter, crac, network, raoParameters, prePerimeterResult, ReportNode.NO_OP);

        assertEquals(pState, optPerimeter.getMainOptimizationState());
        assertEquals(Set.of(pState), optPerimeter.getRangeActionOptimizationStates());
        assertEquals(Set.of(pState, oState1, oState2, cState2), optPerimeter.getMonitoredStates());

        assertEquals(Set.of(pCnec, oCnec1, oCnec2, cCnec2), optPerimeter.getFlowCnecs());
        assertEquals(Set.of(pCnec, oCnec2, cCnec2), optPerimeter.getOptimizedFlowCnecs());
        assertEquals(Set.of(oCnec1, oCnec2), optPerimeter.getMonitoredFlowCnecs());
        assertTrue(optPerimeter.getLoopFlowCnecs().isEmpty()); // loop-flow not monitored according to raoParameters

        assertEquals(Set.of(pNA), optPerimeter.getNetworkActions());

        assertEquals(1, optPerimeter.getRangeActionsPerState().size());
        assertTrue(optPerimeter.getRangeActionsPerState().containsKey(pState));
        assertEquals(1, optPerimeter.getRangeActionsPerState().get(pState).size());
        assertTrue(optPerimeter.getRangeActionsPerState().get(pState).contains(pRA));
    }

    @Test
    void fullPreventivePerimeter2Test() {
        raoParameters.setLoopFlowParameters(new LoopFlowParameters());
        Mockito.when(prePerimeterResult.getSetpoint(pRA)).thenReturn(10000.);
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        Perimeter preventivePerimeter = new Perimeter(pState, Set.of(oState1, oState2, cState2));
        OptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(preventivePerimeter, crac, network, raoParameters, prePerimeterResult, ReportNode.NO_OP);

        assertEquals(Set.of(oCnec1, cCnec2), optPerimeter.getLoopFlowCnecs());
        assertTrue(optPerimeter.getRangeActions().isEmpty());
        assertTrue(optPerimeter.getRangeActionOptimizationStates().isEmpty());
    }

    @Test
    void fullPreventivePerimeter3Test() {
        LoopFlowParameters loopFlowParameters = new LoopFlowParameters();
        loopFlowParameters.setCountries(Set.of(Country.BE));
        raoParameters.setLoopFlowParameters(loopFlowParameters);
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        Perimeter preventivePerimeter = new Perimeter(pState, Set.of(oState1, oState2, cState2));
        OptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(preventivePerimeter, crac, network, raoParameters, prePerimeterResult, ReportNode.NO_OP);

        assertEquals(Set.of(cCnec2), optPerimeter.getLoopFlowCnecs()); // the other loop-flow CNEC is not considered as outside of the loopFlow countries scope
    }

    @Test
    void fullWithPreventiveCnecOnlyTest() {
        Mockito.when(prePerimeterResult.getSetpoint(pRA)).thenReturn(500.);
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        OptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildWithPreventiveCnecsOnly(crac, network, raoParameters, prePerimeterResult, ReportNode.NO_OP);

        assertEquals(pState, optPerimeter.getMainOptimizationState());
        assertEquals(Set.of(pState), optPerimeter.getRangeActionOptimizationStates());
        assertEquals(Set.of(pState), optPerimeter.getMonitoredStates());

        assertEquals(Set.of(pCnec), optPerimeter.getFlowCnecs());
        assertEquals(Set.of(pCnec), optPerimeter.getOptimizedFlowCnecs());
        assertTrue(optPerimeter.getMonitoredFlowCnecs().isEmpty());
        assertTrue(optPerimeter.getLoopFlowCnecs().isEmpty());

        assertEquals(Set.of(pNA), optPerimeter.getNetworkActions());

        assertEquals(1, optPerimeter.getRangeActionsPerState().size());
        assertTrue(optPerimeter.getRangeActionsPerState().containsKey(pState));
        assertEquals(1, optPerimeter.getRangeActionsPerState().get(pState).size());
        assertTrue(optPerimeter.getRangeActionsPerState().get(pState).contains(pRA));
    }

    private FlowCnec addAutoCnec(String id, String networkElement) {
        return crac.newFlowCnec()
            .withId(id)
            .withNetworkElement(networkElement)
            .withInstant("auto")
            .withContingency("outage-1")
            .withOptimized(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withMin(-500.).withSide(TwoSides.ONE).add()
            .add();
    }

    private void addAutoNetworkAction(Consumer<NetworkActionAdder> usageRuleAdder) {
        NetworkActionAdder adder = crac.newNetworkAction().withId("auto-na")
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add();
        usageRuleAdder.accept(adder);
        adder.add();
    }

    private Set<FlowCnec> buildPerimeterFlowCnecs() {
        Mockito.when(prePerimeterResult.getSetpoint(pRA)).thenReturn(500.);
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        Perimeter preventivePerimeter = new Perimeter(pState, Set.of());
        return PreventiveOptimizationPerimeter.buildFromBasecaseScenario(preventivePerimeter, crac, network, raoParameters, prePerimeterResult, ReportNode.NO_OP).getFlowCnecs();
    }

    @Test
    void autoCnecNotIncludedWithoutAutoRemedialAction() {
        addAutoCnec("auto-cnec", "FFR2AA1  FFR3AA1  1");
        assertEquals(Set.of(pCnec), buildPerimeterFlowCnecs());
    }

    @Test
    void autoCnecNotIncludedWithOnInstantAutoRemedialAction() {
        addAutoCnec("auto-cnec", "FFR2AA1  FFR3AA1  1");
        addAutoNetworkAction(adder -> adder.newOnInstantUsageRule().withInstant("auto").add());
        assertEquals(Set.of(pCnec), buildPerimeterFlowCnecs());
    }

    @Test
    void autoCnecNotIncludedWithOnContingencyStateAutoRemedialAction() {
        addAutoCnec("auto-cnec", "FFR2AA1  FFR3AA1  1");
        addAutoNetworkAction(adder -> adder.newOnContingencyStateUsageRule().withInstant("auto").withContingency("outage-1").add());
        assertEquals(Set.of(pCnec), buildPerimeterFlowCnecs());
    }

    @Test
    void autoCnecNotIncludedWithOnContingencyStateAutoRemedialActionForOtherContingency() {
        addAutoCnec("auto-cnec", "FFR2AA1  FFR3AA1  1");
        addAutoNetworkAction(adder -> adder.newOnContingencyStateUsageRule().withInstant("auto").withContingency("outage-2").add());
        // the remedial action is only available for outage-2: auto state of outage-1 has no remedial action
        assertEquals(Set.of(pCnec), buildPerimeterFlowCnecs());
    }

    @Test
    void autoCnecsIncludedOrNotWithOnConstraintAutoRemedialAction() {
        FlowCnec protectedCnec = addAutoCnec("auto-cnec-protected", "FFR2AA1  FFR3AA1  1");
        FlowCnec unprotectedCnec = addAutoCnec("auto-cnec-unprotected", "BBE2AA1  FFR3AA1  1");
        addAutoNetworkAction(adder -> adder.newOnConstraintUsageRule().withInstant("auto").withCnec(protectedCnec.getId()).add());
        Set<FlowCnec> flowCnecs = buildPerimeterFlowCnecs();
        assertEquals(Set.of(pCnec, unprotectedCnec), flowCnecs);
        assertFalse(flowCnecs.contains(protectedCnec));
    }

    @Test
    void autoCnecNotIncludedWithOnFlowConstraintInCountryAutoRemedialActionInSameCountry() {
        addAutoCnec("auto-cnec", "FFR2AA1  FFR3AA1  1");
        addAutoNetworkAction(adder -> adder.newOnFlowConstraintInCountryUsageRule().withInstant("auto").withCountry(Country.FR).add());
        assertEquals(Set.of(pCnec), buildPerimeterFlowCnecs());
    }

    @Test
    void autoCnecIncludedWithOnFlowConstraintInCountryAutoRemedialActionInOtherCountry() {
        FlowCnec autoCnec = addAutoCnec("auto-cnec", "FFR2AA1  FFR3AA1  1");
        addAutoNetworkAction(adder -> adder.newOnFlowConstraintInCountryUsageRule().withInstant("auto").withCountry(Country.NL).add());
        assertEquals(Set.of(pCnec, autoCnec), buildPerimeterFlowCnecs());
    }

    @Test
    void autoCnecIncludedWithOnFlowConstraintInCountryAutoRemedialActionInOtherCountryWithContingency() {
        FlowCnec autoCnec = addAutoCnec("auto-cnec", "FFR2AA1  FFR3AA1  1");
        addAutoNetworkAction(adder -> adder.newOnFlowConstraintInCountryUsageRule().withInstant("auto").withContingency("outage-1").withCountry(Country.NL).add());
        assertEquals(Set.of(pCnec, autoCnec), buildPerimeterFlowCnecs());
    }

    @Test
    void testCopyWithoutHvdcRangeActionAcEmulation() {
        // set up a network with HVDC  line in ac emulation
        addHvdcLine(network);
        // add ac emulation
        network.getHvdcLine("hvdc").addExtension(HvdcAngleDroopActivePowerControl.class, new HvdcAngleDroopActivePowerControlImpl(network.getHvdcLine("hvdc"), 10, 10, true));
        // add hvdc range action to crac
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction()
            .withId("hvdc-range-action-id")
            .withName("hvdc-range-action-name")
            .withNetworkElement("hvdc")
            .withOperator("operator")
            .newOnInstantUsageRule().withInstant("preventive").add()
            .newRange().withMin(-5).withMax(10).add()
            .add();
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        OptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildWithPreventiveCnecsOnly(crac, network, raoParameters, prePerimeterResult, ReportNode.NO_OP);
        assertTrue(optPerimeter.getRangeActions().contains(hvdcRangeAction));
        // test copy the hvdc range action is filtered from the perimeter
        PreventiveOptimizationPerimeter copyPerimeter = (PreventiveOptimizationPerimeter) optPerimeter.copyWithFilteredAvailableHvdcRangeAction(network);
        assertFalse(copyPerimeter.getRangeActions().contains(hvdcRangeAction));
    }
}
