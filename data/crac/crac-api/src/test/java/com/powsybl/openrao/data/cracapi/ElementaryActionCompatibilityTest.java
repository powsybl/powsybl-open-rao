/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.ElementaryAction;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.PstSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class ElementaryActionCompatibilityTest {
    private final NetworkElement switchFr = NetworkActionUtils.createNetworkElement("switch-fr");
    private final NetworkElement switchBe = NetworkActionUtils.createNetworkElement("switch-be");
    private final NetworkElement pstFr = NetworkActionUtils.createNetworkElement("pst-fr");
    private final NetworkElement pstBe = NetworkActionUtils.createNetworkElement("pst-be");
    private final NetworkElement generatorFr = NetworkActionUtils.createNetworkElement("generator-fr");
    private final NetworkElement generatorBe = NetworkActionUtils.createNetworkElement("generator-be");
    private final TopologicalAction openSwitchFr = NetworkActionUtils.createTopologyAction(switchFr, ActionType.OPEN);
    private final TopologicalAction closeSwitchFr = NetworkActionUtils.createTopologyAction(switchFr, ActionType.CLOSE);
    private final TopologicalAction openSwitchBe = NetworkActionUtils.createTopologyAction(switchBe, ActionType.OPEN);
    private final TopologicalAction closeSwitchBe = NetworkActionUtils.createTopologyAction(switchBe, ActionType.CLOSE);
    private final SwitchPair openSwitchFrCloseSwitchBe = NetworkActionUtils.createSwitchPair(switchFr, switchBe);
    private final SwitchPair openSwitchBeCloseSwitchFr = NetworkActionUtils.createSwitchPair(switchBe, switchFr);
    private final PstSetpoint pstFr0 = NetworkActionUtils.createPstSetpoint(pstFr, 0);
    private final PstSetpoint pstFr5 = NetworkActionUtils.createPstSetpoint(pstFr, 5);
    private final PstSetpoint pstBe0 = NetworkActionUtils.createPstSetpoint(pstBe, 0);
    private final PstSetpoint pstBeMinus5 = NetworkActionUtils.createPstSetpoint(pstBe, -5);
    private final InjectionSetpoint generatorFr0Mw = NetworkActionUtils.createInjectionSetpoint(generatorFr, 0d, Unit.MEGAWATT);
    private final InjectionSetpoint generatorFr100Mw = NetworkActionUtils.createInjectionSetpoint(generatorFr, 100d, Unit.MEGAWATT);
    private final InjectionSetpoint generatorFr0A = NetworkActionUtils.createInjectionSetpoint(generatorFr, 0d, Unit.AMPERE);
    private final InjectionSetpoint generatorFr1000A = NetworkActionUtils.createInjectionSetpoint(generatorFr, 1000d, Unit.AMPERE);
    private final InjectionSetpoint generatorBe0Mw = NetworkActionUtils.createInjectionSetpoint(generatorBe, 0d, Unit.MEGAWATT);
    private final InjectionSetpoint generatorBe100Mw = NetworkActionUtils.createInjectionSetpoint(generatorBe, 100d, Unit.MEGAWATT);
    private final InjectionSetpoint generatorBe0A = NetworkActionUtils.createInjectionSetpoint(generatorBe, 0d, Unit.AMPERE);
    private final InjectionSetpoint generatorBe1000A = NetworkActionUtils.createInjectionSetpoint(generatorBe, 1000d, Unit.AMPERE);
    private final List<ElementaryAction> elementaryActions = gatherElementaryActions();

    @Test
    void testElementaryActionsCompatibility() {
        assertIncompatibility(openSwitchFr, closeSwitchFr);
        assertIncompatibility(closeSwitchFr, openSwitchFr);
        assertIncompatibility(openSwitchBe, closeSwitchBe);
        assertIncompatibility(closeSwitchBe, openSwitchBe);
        assertIncompatibility(openSwitchFrCloseSwitchBe, openSwitchBeCloseSwitchFr);
        assertIncompatibility(openSwitchBeCloseSwitchFr, openSwitchFrCloseSwitchBe);
        assertIncompatibility(pstFr0, pstFr5);
        assertIncompatibility(pstFr5, pstFr0);
        assertIncompatibility(pstBe0, pstBeMinus5);
        assertIncompatibility(pstBeMinus5, pstBe0);
        assertIncompatibility(generatorFr0Mw, generatorFr100Mw, generatorFr0A, generatorFr1000A);
        assertIncompatibility(generatorFr100Mw, generatorFr0Mw, generatorFr0A, generatorFr1000A);
        assertIncompatibility(generatorFr0A, generatorFr100Mw, generatorFr0Mw, generatorFr1000A);
        assertIncompatibility(generatorFr1000A, generatorFr0Mw, generatorFr100Mw, generatorFr0A);
        assertIncompatibility(generatorBe0Mw, generatorBe100Mw, generatorBe0A, generatorBe1000A);
        assertIncompatibility(generatorBe100Mw, generatorBe0Mw, generatorBe0A, generatorBe1000A);
        assertIncompatibility(generatorBe0A, generatorBe0Mw, generatorBe100Mw, generatorBe1000A);
        assertIncompatibility(generatorBe1000A, generatorBe0Mw, generatorBe100Mw, generatorBe0A);
    }

    private List<ElementaryAction> gatherElementaryActions() {
        List<ElementaryAction> elementaryActions = new ArrayList<>();
        elementaryActions.add(openSwitchFr);
        elementaryActions.add(closeSwitchFr);
        elementaryActions.add(openSwitchBe);
        elementaryActions.add(closeSwitchBe);
        elementaryActions.add(openSwitchFrCloseSwitchBe);
        elementaryActions.add(openSwitchBeCloseSwitchFr);
        elementaryActions.add(pstFr0);
        elementaryActions.add(pstFr5);
        elementaryActions.add(pstBe0);
        elementaryActions.add(pstBeMinus5);
        elementaryActions.add(generatorFr0Mw);
        elementaryActions.add(generatorFr100Mw);
        elementaryActions.add(generatorFr0A);
        elementaryActions.add(generatorFr1000A);
        elementaryActions.add(generatorBe0Mw);
        elementaryActions.add(generatorBe100Mw);
        elementaryActions.add(generatorBe0A);
        elementaryActions.add(generatorBe1000A);
        return elementaryActions;
    }

    private void assertIncompatibility(ElementaryAction elementaryAction, ElementaryAction... incompatibleElementaryActions) {
        List<ElementaryAction> incompatibleElementaryActionsList = List.of(incompatibleElementaryActions);
        for (ElementaryAction ea : elementaryActions) {
            if (incompatibleElementaryActionsList.contains(ea)) {
                assertFalse(elementaryAction.isCompatibleWith(ea));
            } else {
                assertTrue(elementaryAction.isCompatibleWith(ea));
            }
        }
    }
}
