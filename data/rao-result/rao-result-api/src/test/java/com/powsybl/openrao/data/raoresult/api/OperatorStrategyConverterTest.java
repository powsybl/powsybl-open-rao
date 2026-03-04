/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import com.powsybl.action.Action;
import com.powsybl.action.DanglingLineAction;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.HvdcAction;
import com.powsybl.action.LoadAction;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.action.SwitchAction;
import com.powsybl.action.TerminalsConnectionAction;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPair;
import com.powsybl.security.strategy.ConditionalActions;
import com.powsybl.security.strategy.OperatorStrategy;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class OperatorStrategyConverterTest {
    @Test
    public void testConvertRaoResultToOperatorStrategies() throws IOException {
        Network network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        Crac crac = Crac.read("crac.json", getClass().getResourceAsStream("/crac.json"), network);

        State preventiveState = crac.getPreventiveState();
        State autoState = crac.getState("contingency2Id", crac.getInstant("auto"));
        State curativeState = crac.getState("contingency2Id", crac.getInstant("curative"));

        RaoResult raoResult = Mockito.mock(RaoResult.class);

        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(preventiveState)).thenReturn(Set.of(crac.getNetworkAction("complexNetworkActionId"), crac.getNetworkAction("pstSetpointRaId")));
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(autoState)).thenReturn(Set.of(crac.getNetworkAction("injectionSetpointRaId")));
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(curativeState)).thenReturn(Set.of(crac.getNetworkAction("injectionSetpointRa2Id"), crac.getNetworkAction("injectionSetpointRa3Id"), crac.getNetworkAction("complexNetworkAction2Id"), crac.getNetworkAction("switchPairRaId")));

        Mockito.when(raoResult.getActivatedRangeActionsDuringState(preventiveState)).thenReturn(Set.of(crac.getPstRangeAction("pstRange1Id"), crac.getPstRangeAction("pstRange2Id"), crac.getHvdcRangeAction("hvdcRange1Id"), crac.getHvdcRangeAction("hvdcRange2Id")));
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(autoState)).thenReturn(Set.of());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(curativeState)).thenReturn(Set.of(crac.getPstRangeAction("pstRange3Id"), crac.getPstRangeAction("pstRange4Id"), crac.getInjectionRangeAction("injectionRange1Id")));

        Mockito.when(raoResult.getOptimizedSetPointOnState(preventiveState, crac.getPstRangeAction("pstRange1Id"))).thenReturn(-1.0); // tap position -2
        Mockito.when(raoResult.getOptimizedSetPointOnState(preventiveState, crac.getPstRangeAction("pstRange2Id"))).thenReturn(1.5); // tap position 3
        Mockito.when(raoResult.getOptimizedSetPointOnState(preventiveState, crac.getHvdcRangeAction("hvdcRange1Id"))).thenReturn(750.0);
        Mockito.when(raoResult.getOptimizedSetPointOnState(preventiveState, crac.getHvdcRangeAction("hvdcRange2Id"))).thenReturn(-600.0);

        Mockito.when(raoResult.getOptimizedSetPointOnState(curativeState, crac.getPstRangeAction("pstRange3Id"))).thenReturn(-2.5); // tap position -5
        Mockito.when(raoResult.getOptimizedSetPointOnState(curativeState, crac.getPstRangeAction("pstRange4Id"))).thenReturn(0.0); // tap position -0
        Mockito.when(raoResult.getOptimizedSetPointOnState(curativeState, crac.getInjectionRangeAction("injectionRange1Id"))).thenReturn(250.0);

        Mockito.when(raoResult.getOperatorStrategies(crac, network)).thenCallRealMethod();

        StrategiesAndActions strategiesAndActions = raoResult.getOperatorStrategies(crac, network);

        // check actions
        List<Action> actions = strategiesAndActions.actionList().getActions().stream().sorted(Comparator.comparing(Action::getId)).toList();
        assertEquals(17, actions.size());
        assertBasicActionData(DanglingLineAction.class, "DanglingLineAction_DL1_-120.0", actions.getFirst());
        assertBasicActionData(GeneratorAction.class, "GeneratorAction_injection_260.0", actions.get(1));
        assertBasicActionData(LoadAction.class, "LoadAction_LD1_260.0", actions.get(2));
        assertBasicActionData(PhaseTapChangerTapPositionAction.class, "PhaseTapChangerTapPositionAction_pst_15", actions.get(3));
        assertBasicActionData(PhaseTapChangerTapPositionAction.class, "PhaseTapChangerTapPositionAction_pst_5", actions.get(4));
        assertBasicActionData(ShuntCompensatorPositionAction.class, "ShuntCompensatorPositionAction_SC1_13", actions.get(5));
        assertBasicActionData(SwitchAction.class, "SwitchAction_BR1_OPEN", actions.get(6));
        assertBasicActionData(SwitchPair.class, "SwitchPair_to-open_to-close", actions.get(7));
        assertBasicActionData(TerminalsConnectionAction.class, "TerminalsConnectionAction_ne1Id_CLOSE", actions.get(8));
        assertBasicActionData(HvdcAction.class, "hvdcRange1Id@750.0", actions.get(9));
        assertBasicActionData(HvdcAction.class, "hvdcRange2Id@-600.0", actions.get(10));
        assertBasicActionData(GeneratorAction.class, "injectionRange1Id::generator1Id@250.0", actions.get(11));
        assertBasicActionData(GeneratorAction.class, "injectionRange1Id::generator2Id@-250.0", actions.get(12));
        assertBasicActionData(PhaseTapChangerTapPositionAction.class, "pstRange1Id@-2", actions.get(13));
        assertBasicActionData(PhaseTapChangerTapPositionAction.class, "pstRange2Id@3", actions.get(14));
        assertBasicActionData(PhaseTapChangerTapPositionAction.class, "pstRange3Id@-5", actions.get(15));
        assertBasicActionData(PhaseTapChangerTapPositionAction.class, "pstRange4Id@0", actions.getLast());

        // check operator strategies
        assertEquals(2, strategiesAndActions.operatorStrategyList().getOperatorStrategies().size());

        OperatorStrategy basecaseStrategy = strategiesAndActions.operatorStrategyList().getOperatorStrategies().getFirst();
        assertEquals("preventive", basecaseStrategy.getId());
        assertEquals(ContingencyContext.none(), basecaseStrategy.getContingencyContext());
        assertEquals(1, basecaseStrategy.getConditionalActions().size());

        ConditionalActions preventiveActions = basecaseStrategy.getConditionalActions().getFirst();
        assertEquals("preventive - 202501010000", preventiveActions.getId());
        assertEquals(7, preventiveActions.getActionIds().size());
        List<String> sortedPreventiveActionIds = preventiveActions.getActionIds().stream().sorted().toList();
        assertEquals("PhaseTapChangerTapPositionAction_pst_15", sortedPreventiveActionIds.getFirst());
        assertEquals("PhaseTapChangerTapPositionAction_pst_5", sortedPreventiveActionIds.get(1));
        assertEquals("TerminalsConnectionAction_ne1Id_CLOSE", sortedPreventiveActionIds.get(2));
        assertEquals("hvdcRange1Id@750.0", sortedPreventiveActionIds.get(3));
        assertEquals("hvdcRange2Id@-600.0", sortedPreventiveActionIds.get(4));
        assertEquals("pstRange1Id@-2", sortedPreventiveActionIds.get(5));
        assertEquals("pstRange2Id@3", sortedPreventiveActionIds.getLast());

        OperatorStrategy postContingencyStrategy = strategiesAndActions.operatorStrategyList().getOperatorStrategies().getLast();
        assertEquals("contingency2Id", postContingencyStrategy.getId());
        assertEquals(ContingencyContext.specificContingency("contingency2Id"), postContingencyStrategy.getContingencyContext());
        assertEquals(2, postContingencyStrategy.getConditionalActions().size());

        ConditionalActions autoActions = postContingencyStrategy.getConditionalActions().getFirst();
        assertEquals("contingency2Id - auto - 202501010000", autoActions.getId());
        assertEquals(1, autoActions.getActionIds().size());
        assertEquals("GeneratorAction_injection_260.0", autoActions.getActionIds().getFirst());

        ConditionalActions curativeActions = postContingencyStrategy.getConditionalActions().getLast();
        assertEquals("contingency2Id - curative - 202501010000", curativeActions.getId());
        assertEquals(10, curativeActions.getActionIds().size());
        List<String> sortedCurativeActionIds = curativeActions.getActionIds().stream().sorted().toList();
        assertEquals("DanglingLineAction_DL1_-120.0", sortedCurativeActionIds.getFirst());
        assertEquals("GeneratorAction_injection_260.0", sortedCurativeActionIds.get(1));
        assertEquals("LoadAction_LD1_260.0", sortedCurativeActionIds.get(2));
        assertEquals("ShuntCompensatorPositionAction_SC1_13", sortedCurativeActionIds.get(3));
        assertEquals("SwitchAction_BR1_OPEN", sortedCurativeActionIds.get(4));
        assertEquals("SwitchPair_to-open_to-close", sortedCurativeActionIds.get(5));
        assertEquals("injectionRange1Id::generator1Id@250.0", sortedCurativeActionIds.get(6));
        assertEquals("injectionRange1Id::generator2Id@-250.0", sortedCurativeActionIds.get(7));
        assertEquals("pstRange3Id@-5", sortedCurativeActionIds.get(8));
        assertEquals("pstRange4Id@0", sortedCurativeActionIds.getLast());
    }

    private static void assertBasicActionData(Class<? extends Action> actionType, String actionId, Action action) {
        assertInstanceOf(actionType, action);
        assertEquals(actionId, action.getId());
    }
}
