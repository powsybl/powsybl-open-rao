/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.commons;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;

import java.util.Objects;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class FlowCnecAdderUtil {
    private FlowCnecAdderUtil() {
    }

    public static void setCurrentLimits(FlowCnecAdder flowCnecAdder, Network network, String networkElementId) {
        Branch<?> branch = network.getBranch(networkElementId);
        if (branch == null) {
            throw new OpenRaoException("No branch with id %s was found in the network.".formatted(networkElementId));
        }
        Double currentLimitLeft = getCurrentLimit(branch, TwoSides.ONE);
        Double currentLimitRight = getCurrentLimit(branch, TwoSides.TWO);
        if (Objects.nonNull(currentLimitLeft) && Objects.nonNull(currentLimitRight)) {
            flowCnecAdder.withIMax(currentLimitLeft, TwoSides.ONE);
            flowCnecAdder.withIMax(currentLimitRight, TwoSides.TWO);
        } else {
            throw new OpenRaoException(String.format("Unable to get branch current limits from network for branch %s", branch.getId()));
        }
    }

    private static Double getCurrentLimit(Branch<?> branch, TwoSides side) {
        if (hasCurrentLimit(branch, side)) {
            return branch.getCurrentLimits(side).orElseThrow().getPermanentLimit();
        }
        if (side == TwoSides.ONE && hasCurrentLimit(branch, TwoSides.TWO)) {
            return branch.getCurrentLimits(TwoSides.TWO).orElseThrow().getPermanentLimit() * branch.getTerminal1().getVoltageLevel().getNominalV() / branch.getTerminal2().getVoltageLevel().getNominalV();
        }
        if (side == TwoSides.TWO && hasCurrentLimit(branch, TwoSides.ONE)) {
            return branch.getCurrentLimits(TwoSides.ONE).orElseThrow().getPermanentLimit() * branch.getTerminal2().getVoltageLevel().getNominalV() / branch.getTerminal1().getVoltageLevel().getNominalV();
        }
        return null;
    }

    private static boolean hasCurrentLimit(Branch<?> branch, TwoSides side) {
        return branch.getCurrentLimits(side).isPresent();
    }
}
