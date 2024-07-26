/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi;

import com.powsybl.openrao.data.cracapi.networkaction.*;
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
    private final NetworkAction openSwitchFr = NetworkActionUtils.createSwitchAction(switchFr, ActionType.OPEN);
    private final NetworkAction closeSwitchFr = NetworkActionUtils.createSwitchAction(switchFr, ActionType.CLOSE);
    private final NetworkAction openSwitchBe = NetworkActionUtils.createSwitchAction(switchBe, ActionType.OPEN);
    private final NetworkAction closeSwitchBe = NetworkActionUtils.createSwitchAction(switchBe, ActionType.CLOSE);
    private final NetworkAction openSwitchFrCloseSwitchBe = NetworkActionUtils.createSwitchPair(switchFr, switchBe);
    private final NetworkAction openSwitchBeCloseSwitchFr = NetworkActionUtils.createSwitchPair(switchBe, switchFr);
    private final NetworkAction pstFr0 = NetworkActionUtils.createPhaseTapChangerTapPositionAction(pstFr, 0);
    private final NetworkAction pstFr5 = NetworkActionUtils.createPhaseTapChangerTapPositionAction(pstFr, 5);
    private final NetworkAction pstBe0 = NetworkActionUtils.createPhaseTapChangerTapPositionAction(pstBe, 0);
    private final NetworkAction pstBeMinus5 = NetworkActionUtils.createPhaseTapChangerTapPositionAction(pstBe, -5);
    private final NetworkAction generatorFr0Mw = NetworkActionUtils.createGeneratorActivePowerAction(generatorFr, 0d);
    private final NetworkAction generatorFr100Mw = NetworkActionUtils.createGeneratorActivePowerAction(generatorFr, 100d);
    private final NetworkAction generatorFr0A = NetworkActionUtils.createGeneratorTargetVAction(generatorFr, 0d);
    private final NetworkAction generatorFr1000A = NetworkActionUtils.createGeneratorTargetVAction(generatorFr, 1000d);
    private final NetworkAction generatorBe0Mw = NetworkActionUtils.createGeneratorActivePowerAction(generatorBe, 0d);
    private final NetworkAction generatorBe100Mw = NetworkActionUtils.createGeneratorActivePowerAction(generatorBe, 100d);
    private final NetworkAction generatorBe0A = NetworkActionUtils.createGeneratorTargetVAction(generatorBe, 0d);
    private final NetworkAction generatorBe1000A = NetworkActionUtils.createGeneratorTargetVAction(generatorBe, 1000d);
    private final List<NetworkAction> networkActions = gatherActions();

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

    private List<NetworkAction> gatherActions() {
        List<NetworkAction> actions = new ArrayList<>();
        actions.add(openSwitchFr);
        actions.add(closeSwitchFr);
        actions.add(openSwitchBe);
        actions.add(closeSwitchBe);
        actions.add(openSwitchFrCloseSwitchBe);
        actions.add(openSwitchBeCloseSwitchFr);
        actions.add(pstFr0);
        actions.add(pstFr5);
        actions.add(pstBe0);
        actions.add(pstBeMinus5);
        actions.add(generatorFr0Mw);
        actions.add(generatorFr100Mw);
        actions.add(generatorFr0A);
        actions.add(generatorFr1000A);
        actions.add(generatorBe0Mw);
        actions.add(generatorBe100Mw);
        actions.add(generatorBe0A);
        actions.add(generatorBe1000A);
        return actions;
    }

    private void assertIncompatibility(NetworkAction action, NetworkAction... incompatibleActions) {
        List<NetworkAction> incompatibleActionsList = List.of(incompatibleActions);
        for (NetworkAction na : this.networkActions) {
            if (incompatibleActionsList.contains(na)) {
                assertFalse(action.isCompatibleWith(na));
            } else {
                assertTrue(action.isCompatibleWith(na));
            }
        }
    }
}
