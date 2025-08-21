/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.impl.CracImplFactory;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class AutoOptimizationPerimeterTest {
    @Test
    void nonAutoState() {
        Instant preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.isAuto()).thenReturn(false);
        State preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getInstant()).thenReturn(preventiveInstant);
        Set<FlowCnec> emptyFlowCnecSet = Set.of();
        Set<NetworkAction> emptyNetworkActionSet = Set.of();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new AutoOptimizationPerimeter(preventiveState, emptyFlowCnecSet, emptyFlowCnecSet, emptyNetworkActionSet));
        assertEquals("an AutoOptimizationPerimeter must be based on an auto state", exception.getMessage());
    }

    @Test
    void buildAutoOptimizationPerimeter() {
        Crac crac = initCrac();
        State automatonState = crac.getState("contingency", crac.getInstant("auto"));
        AutoOptimizationPerimeter autoOptimizationPerimeter = AutoOptimizationPerimeter.build(automatonState, crac, null, new RaoParameters(), null);

        // Only available topological actions are considered in the perimeter
        assertEquals(automatonState, autoOptimizationPerimeter.getMainOptimizationState());
        assertEquals(Set.of(automatonState), autoOptimizationPerimeter.getMonitoredStates());
        assertEquals(Set.of(crac.getFlowCnec("FlowCNEC - auto")), autoOptimizationPerimeter.getFlowCnecs());
        assertEquals(Set.of(crac.getNetworkAction("available-topo")), autoOptimizationPerimeter.getNetworkActions());
        assertEquals(Set.of(), autoOptimizationPerimeter.getRangeActions());
    }

    private Crac initCrac() {
        Crac crac = new CracImplFactory().create("crac");

        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative", InstantKind.CURATIVE);

        crac.newContingency()
            .withId("contingency")
            .withContingencyElement("contingency-equipment", ContingencyElementType.LINE)
            .add();

        crac.newFlowCnec()
            .withId("FlowCNEC - preventive")
            .withOptimized(true)
            .withInstant("preventive")
            .withNetworkElement("line")
            .withNominalVoltage(400)
            .newThreshold()
            .withSide(TwoSides.ONE)
            .withMin(-1000d)
            .withMax(1000d)
            .withUnit(Unit.AMPERE)
            .add()
            .add();

        crac.newFlowCnec()
            .withId("FlowCNEC - outage")
            .withOptimized(true)
            .withInstant("outage")
            .withContingency("contingency")
            .withNetworkElement("line")
            .withNominalVoltage(400)
            .newThreshold()
            .withSide(TwoSides.ONE)
            .withMin(-2500d)
            .withMax(2500d)
            .withUnit(Unit.AMPERE)
            .add()
            .add();

        crac.newFlowCnec()
            .withId("FlowCNEC - auto")
            .withOptimized(true)
            .withInstant("auto")
            .withContingency("contingency")
            .withNetworkElement("line")
            .withNominalVoltage(400)
            .newThreshold()
            .withSide(TwoSides.ONE)
            .withMin(-1500d)
            .withMax(1500d)
            .withUnit(Unit.AMPERE)
            .add()
            .add();

        crac.newFlowCnec()
            .withId("FlowCNEC - curative")
            .withOptimized(true)
            .withInstant("curative")
            .withContingency("contingency")
            .withNetworkElement("line")
            .withNominalVoltage(400)
            .newThreshold()
            .withSide(TwoSides.ONE)
            .withMin(-1000d)
            .withMax(1000d)
            .withUnit(Unit.AMPERE)
            .add()
            .add();

        crac.newNetworkAction()
            .withId("forced-topo")
            .withOperator("FR")
            .newSwitchAction()
            .withNetworkElement("switch-1")
            .withActionType(ActionType.OPEN)
            .add()
            .newOnContingencyStateUsageRule()
            .withContingency("contingency")
            .withInstant("auto")
            .withUsageMethod(UsageMethod.FORCED)
            .add()
            .add();

        crac.newNetworkAction()
            .withId("available-topo")
            .withOperator("FR")
            .newSwitchAction()
            .withNetworkElement("switch-2")
            .withActionType(ActionType.OPEN)
            .add()
            .newOnContingencyStateUsageRule()
            .withContingency("contingency")
            .withInstant("auto")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        crac.newPstRangeAction()
            .withId("forced-pst")
            .withOperator("FR")
            .withNetworkElement("pst-1")
            .withSpeed(1)
            .withInitialTap(0)
            .withTapToAngleConversionMap(Map.of(0, 0d, 1, 2.34))
            .newOnContingencyStateUsageRule()
            .withContingency("contingency")
            .withInstant("auto")
            .withUsageMethod(UsageMethod.FORCED)
            .add()
            .add();

        crac.newPstRangeAction()
            .withId("available-pst")
            .withOperator("FR")
            .withNetworkElement("pst-2")
            .withSpeed(2)
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(0, 0d, 1, 2.34))
            .newOnContingencyStateUsageRule()
            .withContingency("contingency")
            .withInstant("auto")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        return crac;
    }
}
