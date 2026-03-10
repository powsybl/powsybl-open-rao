/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm.pstregulation;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.searchtreerao.reports.CastorReports;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class PstRegulator {
    private PstRegulator() {
    }

    public static Map<PstRangeAction, Integer> regulatePsts(final Set<ElementaryPstRegulationInput> elementaryPstRegulationInputs,
                                                            final Network network,
                                                            final LoadFlowParameters loadFlowParameters,
                                                            final ReportNode reportNode) {
        elementaryPstRegulationInputs.forEach(elementaryPstRegulationInput -> setRegulationForPst(network, elementaryPstRegulationInput, reportNode));
        LoadFlow.find("OpenLoadFlow").run(network, loadFlowParameters);
        return elementaryPstRegulationInputs.stream().collect(Collectors.toMap(ElementaryPstRegulationInput::pstRangeAction, pstRegulationInput -> getRegulatedTap(network, pstRegulationInput.pstRangeAction())));
    }

    private static void setRegulationForPst(Network network, ElementaryPstRegulationInput elementaryPstRegulationInput, ReportNode reportNode) {
        TwoWindingsTransformer twt = getTwoWindingsTransformer(network, elementaryPstRegulationInput.pstRangeAction());
        PhaseTapChanger phaseTapChanger = twt.getPhaseTapChanger();
        phaseTapChanger.setRegulationValue(elementaryPstRegulationInput.limitingThreshold());
        setRegulationTerminal(twt, elementaryPstRegulationInput, reportNode);
        phaseTapChanger.setRegulationMode(PhaseTapChanger.RegulationMode.CURRENT_LIMITER);
        setTargetDeadband(twt, reportNode);
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

    private static void setRegulationTerminal(final TwoWindingsTransformer twt,
                                              final ElementaryPstRegulationInput elementaryPstRegulationInput,
                                              final ReportNode reportNode) {
        PhaseTapChanger phaseTapChanger = twt.getPhaseTapChanger();
        if (phaseTapChanger.getRegulationTerminal() == null) {
            CastorReports.reportNoDefaultRegulationTerminalDefined(reportNode, twt.getId(), elementaryPstRegulationInput.limitingSide());
            phaseTapChanger.setRegulationTerminal(twt.getTerminal(elementaryPstRegulationInput.limitingSide()));
        }
    }

    private static void setTargetDeadband(final TwoWindingsTransformer twt, final ReportNode reportNode) {
        PhaseTapChanger phaseTapChanger = twt.getPhaseTapChanger();
        if (Double.isNaN(phaseTapChanger.getTargetDeadband())) {
            CastorReports.reportNoDefaultTargetDeadbandDefined(reportNode, twt.getId());
            phaseTapChanger.setTargetDeadband(0.0); // value is not used by OpenLoadFlow in CURRENT_LIMITER mode
        }
    }

    private static int getRegulatedTap(Network network, PstRangeAction pstRangeAction) {
        Integer tapPosition = getTwoWindingsTransformer(network, pstRangeAction).getPhaseTapChanger().getSolvedTapPosition();
        if (tapPosition == null) {
            throw new OpenRaoException("Could not retrieve regulated tap position for PST range action %s.".formatted(pstRangeAction.getName()));
        }
        return getTwoWindingsTransformer(network, pstRangeAction).getPhaseTapChanger().getSolvedTapPosition();
    }
}
