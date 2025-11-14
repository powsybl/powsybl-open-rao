/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.pstregulation;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class PstRegulator {
    private PstRegulator() {
    }

    public static void set(Network network, LoadFlowParameters loadFlowParameters, PstRegulationExtension pstRegulationExtension) {
        pstRegulationExtension.getRegulationInputs().forEach((pstId, regulationInputs) -> regulationInputs.forEach(input -> input.setRegulation(network)));
        loadFlowParameters.setPhaseShifterRegulationOn(true);
        addOpenLoadFlowParameters(loadFlowParameters);
    }

    public static void unset(Network network, LoadFlowParameters loadFlowParameters, PstRegulationExtension pstRegulationExtension) {
        pstRegulationExtension.getRegulationInputs().forEach((pstId, regulationInputs) -> regulationInputs.forEach(input -> input.unsetRegulation(network)));
        loadFlowParameters.setPhaseShifterRegulationOn(false);
    }

    private static void addOpenLoadFlowParameters(LoadFlowParameters loadFlowParameters) {
        if (loadFlowParameters.getExtension(OpenLoadFlowParameters.class) == null) {
            OpenLoadFlowParameters openLoadFlowParameters = new OpenLoadFlowParameters();
            openLoadFlowParameters.setMaxOuterLoopIterations(1000);
            loadFlowParameters.addExtension(OpenLoadFlowParameters.class, openLoadFlowParameters);
        }
    }
}
