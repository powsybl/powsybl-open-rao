/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.LoadingLimits;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.OperationalLimitsGroup;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import org.apache.commons.lang3.tuple.Pair;

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
        // TODO: use threshold from monitored FlowCNEC instead of PATL?
        pstRangeActions.forEach(pstRangeAction -> setRegulationForPst(network, pstRangeAction));
        runLoadFlowWithRegulation(network, loadFlowParameters);
        return pstRangeActions.stream().collect(Collectors.toMap(Function.identity(), pstRangeAction -> getRegulatedTap(network, pstRangeAction)));
    }

    private static void setRegulationForPst(Network network, PstRangeAction pstRangeAction) {
        TwoWindingsTransformer twt = getTwoWindingsTransformer(network, pstRangeAction);
        Pair<TwoSides, Double> lowestPermanentLimitAndSide = getLowestPermanentLimitAndAssociatedSide(twt);
        PhaseTapChanger phaseTapChanger = twt.getPhaseTapChanger();
        phaseTapChanger.setRegulationValue(lowestPermanentLimitAndSide.getRight());
        phaseTapChanger.setRegulationTerminal(twt.getTerminal(lowestPermanentLimitAndSide.getLeft()));
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

    private static Pair<TwoSides, Double> getLowestPermanentLimitAndAssociatedSide(TwoWindingsTransformer twt) {
        Double permanentLimit1 = twt.getOperationalLimitsGroups1().stream().map(PstRegulator::getPermanentLimit).min(Double::compareTo).orElse(Double.MAX_VALUE);
        Double permanentLimit2 = twt.getOperationalLimitsGroups2().stream().map(PstRegulator::getPermanentLimit).min(Double::compareTo).orElse(Double.MAX_VALUE);
        return permanentLimit1 <= permanentLimit2 ? Pair.of(TwoSides.ONE, permanentLimit1) : Pair.of(TwoSides.TWO, permanentLimit2);
    }

    private static Double getPermanentLimit(OperationalLimitsGroup operationalLimitsGroup) {
        return operationalLimitsGroup.getCurrentLimits().map(LoadingLimits::getPermanentLimit).orElse(Double.MAX_VALUE);
    }

    private static void runLoadFlowWithRegulation(Network network, LoadFlowParameters loadFlowParameters) {
        boolean initialPhaseShifterRegulationOnValue = loadFlowParameters.isPhaseShifterRegulationOn();
        loadFlowParameters.setPhaseShifterRegulationOn(true);
        LoadFlow.find("OpenLoadFlow").run(network, loadFlowParameters);
        loadFlowParameters.setPhaseShifterRegulationOn(initialPhaseShifterRegulationOnValue);
    }

    private static int getRegulatedTap(Network network, PstRangeAction pstRangeAction) {
        return getTwoWindingsTransformer(network, pstRangeAction).getPhaseTapChanger().getSolvedTapPosition();
    }
}
