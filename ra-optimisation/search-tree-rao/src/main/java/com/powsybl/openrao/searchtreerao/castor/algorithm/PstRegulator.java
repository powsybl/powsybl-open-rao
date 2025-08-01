/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class PstRegulator {
    private PstRegulator() {
    }

    public static Map<PstRangeAction, Integer> regulatePsts(Set<PstRegulationInput> pstRegulationInputs, Network network, LoadFlowParameters loadFlowParameters) {
        pstRegulationInputs.forEach(pstRegulationInput -> setRegulationForPst(network, pstRegulationInput));
        LoadFlow.find("OpenLoadFlow").run(network, loadFlowParameters);
        return pstRegulationInputs.stream().collect(Collectors.toMap(PstRegulationInput::pstRangeAction, pstRegulationInput -> getRegulatedTap(network, pstRegulationInput.pstRangeAction())));
    }

    private static void setRegulationForPst(Network network, PstRegulationInput pstRegulationInput) {
        TwoWindingsTransformer twt = getTwoWindingsTransformer(network, pstRegulationInput.pstRangeAction());
        PhaseTapChanger phaseTapChanger = twt.getPhaseTapChanger();
        phaseTapChanger.setRegulationValue(pstRegulationInput.limitingThreshold());
        phaseTapChanger.setRegulationTerminal(twt.getTerminal(pstRegulationInput.limitingSide()));
        phaseTapChanger.setRegulationMode(PhaseTapChanger.RegulationMode.CURRENT_LIMITER);
        phaseTapChanger.setTargetDeadband(Double.MAX_VALUE);
        phaseTapChanger.setRegulating(true);
    }

    private static TwoWindingsTransformer getTwoWindingsTransformer(Network network, PstRangeAction pstRangeAction) {
        String pstId = pstRangeAction.getNetworkElement().getId();
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(pstId);
        if (twt == null) {
            throw new OpenRaoException("No two-windings transformer with id %s found in network.".formatted(pstId));
        }
        return twt;
    }

    private static int getRegulatedTap(Network network, PstRangeAction pstRangeAction) {
        return getTwoWindingsTransformer(network, pstRangeAction).getPhaseTapChanger().getSolvedTapPosition();
    }
}
