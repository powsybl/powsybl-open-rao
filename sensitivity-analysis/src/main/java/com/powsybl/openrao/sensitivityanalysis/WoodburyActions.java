/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.action.Action;
import com.powsybl.action.SwitchActionBuilder;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPair;

import java.util.stream.Stream;

/**
 * Converts the elementary actions of a network action into actions supported by the OLF fast (Woodbury) DC sensitivity.
 *
 * <p>A bus-bar change is modelled as a {@link SwitchPair} (open one switch, close another). The fast DC sensitivity does
 * not handle {@code SwitchPair} directly (see {@code Actions.checkWoodburySupported} in open-loadflow), so it is
 * decomposed here into its two {@link com.powsybl.action.SwitchAction}s, which the fast path does support. All other
 * elementary actions are passed through unchanged.
 *
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
final class WoodburyActions {

    private WoodburyActions() {
    }

    static Stream<Action> toWoodburyActions(Action elementaryAction) {
        if (elementaryAction instanceof SwitchPair switchPair) {
            return Stream.of(
                new SwitchActionBuilder()
                    .withId(switchPair.getId() + "_open")
                    .withNetworkElementId(switchPair.getSwitchToOpen().getId())
                    .withOpen(true)
                    .build(),
                new SwitchActionBuilder()
                    .withId(switchPair.getId() + "_close")
                    .withNetworkElementId(switchPair.getSwitchToClose().getId())
                    .withOpen(false)
                    .build());
        }
        return Stream.of(elementaryAction);
    }
}
