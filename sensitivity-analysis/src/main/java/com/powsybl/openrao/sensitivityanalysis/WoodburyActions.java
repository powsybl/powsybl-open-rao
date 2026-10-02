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
 * OpenLoadFlow does not support {@link SwitchPair}: it is decomposed into its two switch actions.
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
