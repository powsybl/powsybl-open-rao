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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class PstRegulator {
    private PstRegulator() {
    }

    public static Map<PstRangeAction, Integer> regulatePsts(Network network, Set<PstRangeAction> pstRangeActions, LoadFlowParameters loadFlowParameters) {
        // TODO: see if always use PATL or provide threshold from monitored FlowCNEC
        pstRangeActions.forEach(pstRangeAction -> setRegulationForPst(network, pstRangeAction));
        runLoadFlowWithRegulation(network, loadFlowParameters);
        return pstRangeActions.stream().collect(Collectors.toMap(Function.identity(), pstRangeAction -> getRegulatedTap(network, pstRangeAction)));
    }

    private static void setRegulationForPst(Network network, PstRangeAction pstRangeAction) {
        TwoWindingsTransformer twt = getTwoWindingsTransformer(network, pstRangeAction);
        PhaseTapChanger phaseTapChanger = twt.getPhaseTapChanger();
        phaseTapChanger.setRegulating(true);
        phaseTapChanger.setRegulationMode(PhaseTapChanger.RegulationMode.CURRENT_LIMITER);
        phaseTapChanger.setRegulationValue(getPermanentLimit(twt));
    }

    private static TwoWindingsTransformer getTwoWindingsTransformer(Network network, PstRangeAction pstRangeAction) {
        String pstId = pstRangeAction.getNetworkElement().getId();
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(pstId);
        if (twt == null) {
            throw new OpenRaoException("No two-windings transformer with id %s found in network.".formatted(pstId));
        }
        return twt;
    }

    private static double getPermanentLimit(TwoWindingsTransformer twt) {
        Set<Double> permanentLimits = new HashSet<>();
        twt.getOperationalLimitsGroups1().forEach(operationalLimitsGroup -> operationalLimitsGroup.getCurrentLimits().ifPresent(currentLimits -> permanentLimits.add(currentLimits.getPermanentLimit())));
        twt.getOperationalLimitsGroups2().forEach(operationalLimitsGroup -> operationalLimitsGroup.getCurrentLimits().ifPresent(currentLimits -> permanentLimits.add(currentLimits.getPermanentLimit())));
        return permanentLimits.stream().min(Double::compareTo).orElse(Double.MAX_VALUE);
    }

    private static void runLoadFlowWithRegulation(Network network, LoadFlowParameters loadFlowParameters) {
        boolean initialPhaseShifterRegulationOnValue = loadFlowParameters.isPhaseShifterRegulationOn();
        loadFlowParameters.setPhaseShifterRegulationOn(true);
        LoadFlow.find("OpenLoadFlow").run(network, loadFlowParameters);
        loadFlowParameters.setPhaseShifterRegulationOn(initialPhaseShifterRegulationOnValue);
    }

    private static int getRegulatedTap(Network network, PstRangeAction pstRangeAction) {
        return getTwoWindingsTransformer(network, pstRangeAction).getPhaseTapChanger().getTapPosition();
    }
}
