/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.pstregulation;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record PstRegulationInput(String pstId, String monitoredBranchId, double threshold, TwoSides side) {
    public PstRegulationInput(String pstId, String monitoredBranchId, double threshold, TwoSides side, Network network) {
        this(pstId, monitoredBranchId, threshold, side);
        if (network.getTwoWindingsTransformer(pstId) == null) {
            throw new OpenRaoException("Could not find a two windings transformer with id '%s' in network.".formatted(pstId));
        }
        if (network.getBranch(monitoredBranchId) == null) {
            throw new OpenRaoException("Could not find a branch with id '%s' in network.".formatted(monitoredBranchId));
        }
        configureRegulation(network);
    }

    public void setRegulation(Network network) {
        getPhaseTapChanger(network).setRegulating(true);
    }

    public void unsetRegulation(Network network) {
        getPhaseTapChanger(network).setRegulating(false);
    }

    private void configureRegulation(Network network) {
        getPhaseTapChanger(network).setRegulationValue(threshold)
            .setRegulationTerminal(network.getBranch(monitoredBranchId).getTerminal(side))
            .setRegulationMode(PhaseTapChanger.RegulationMode.CURRENT_LIMITER)
            .setTargetDeadband(Double.MAX_VALUE);
    }

    private PhaseTapChanger getPhaseTapChanger(Network network) {
        return network.getTwoWindingsTransformer(pstId).getPhaseTapChanger();
    }
}
