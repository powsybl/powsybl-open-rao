/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.pstregulation;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.extensions.PstRegulation;
import com.powsybl.openrao.data.crac.api.extensions.PstRegulationInput;

import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class PstRegulator {
    private PstRegulator() {
    }

    public static void set(Network network, LoadFlowParameters loadFlowParameters, PstRegulation pstRegulation) {
        pstRegulation.getRegulationInputs().forEach(input -> setRegulation(input, network));
        loadFlowParameters.setPhaseShifterRegulationOn(true);
        addOpenLoadFlowParameters(loadFlowParameters);
    }

    public static void unset(Network network, LoadFlowParameters loadFlowParameters, PstRegulation pstRegulation) {
        pstRegulation.getRegulationInputs().forEach(input -> unsetRegulation(input, network));
        loadFlowParameters.setPhaseShifterRegulationOn(false);
    }

    private static void addOpenLoadFlowParameters(LoadFlowParameters loadFlowParameters) {
        if (loadFlowParameters.getExtension(OpenLoadFlowParameters.class) == null) {
            OpenLoadFlowParameters openLoadFlowParameters = new OpenLoadFlowParameters();
            openLoadFlowParameters.setMaxOuterLoopIterations(1000);
            loadFlowParameters.addExtension(OpenLoadFlowParameters.class, openLoadFlowParameters);
        }
    }

    private static void setRegulation(PstRegulationInput pstRegulationInput, Network network) {
        configureRegulation(pstRegulationInput, network);
        getPhaseTapChanger(pstRegulationInput, network).setRegulating(true);
    }

    private static void unsetRegulation(PstRegulationInput pstRegulationInput, Network network) {
        getPhaseTapChanger(pstRegulationInput, network).setRegulating(false);
    }

    private static void configureRegulation(PstRegulationInput pstRegulationInput, Network network) {
        TwoWindingsTransformer twoWindingsTransformer = getTwoWindingsTransformer(pstRegulationInput, network);
        if (twoWindingsTransformer == null) {
            throw new OpenRaoException("Could not find a two windings transformer with id '%s' in network.".formatted(pstRegulationInput.pstId()));
        }
        Branch<?> monitoredBranch = getMonitoredBranch(pstRegulationInput, network);
        if (monitoredBranch == null) {
            throw new OpenRaoException("Could not find a branch with id '%s' in network.".formatted(pstRegulationInput.monitoredBranchId()));
        }
        Optional<Terminal> commonTerminal = getCommonTerminal(monitoredBranch, twoWindingsTransformer);
        if (commonTerminal.isEmpty()) {
            throw new OpenRaoException("Branch '%s' and two-windings transformer '%s' do not share a common terminal.".formatted(pstRegulationInput.monitoredBranchId(), pstRegulationInput.pstId()));
        }

        twoWindingsTransformer.getPhaseTapChanger().setRegulationValue(pstRegulationInput.threshold())
            .setRegulationTerminal(commonTerminal.get())
            .setRegulationMode(PhaseTapChanger.RegulationMode.CURRENT_LIMITER)
            .setTargetDeadband(Double.MAX_VALUE);
    }

    private static TwoWindingsTransformer getTwoWindingsTransformer(PstRegulationInput pstRegulationInput, Network network) {
        return network.getTwoWindingsTransformer(pstRegulationInput.pstId());
    }

    private static PhaseTapChanger getPhaseTapChanger(PstRegulationInput pstRegulationInput, Network network) {
        return getTwoWindingsTransformer(pstRegulationInput, network).getPhaseTapChanger();
    }

    private static Branch<?> getMonitoredBranch(PstRegulationInput pstRegulationInput, Network network) {
        return network.getBranch(pstRegulationInput.monitoredBranchId());
    }

    private static Optional<Terminal> getCommonTerminal(Branch<?> monitoredElement, TwoWindingsTransformer twoWindingsTransformer) {
        Terminal twtTerminal1 = twoWindingsTransformer.getTerminal(TwoSides.ONE);
        Terminal twtTerminal2 = twoWindingsTransformer.getTerminal(TwoSides.TWO);
        if (monitoredElement.getTerminal(TwoSides.ONE) == twtTerminal1) {
            return Optional.ofNullable(twtTerminal1);
        }
        if (monitoredElement.getTerminal(TwoSides.ONE) == twtTerminal2) {
            return Optional.ofNullable(twtTerminal2);
        }
        if (monitoredElement.getTerminal(TwoSides.TWO) == twtTerminal1) {
            return Optional.ofNullable(twtTerminal1);
        }
        if (monitoredElement.getTerminal(TwoSides.TWO) == twtTerminal2) {
            return Optional.ofNullable(twtTerminal2);
        }
        return Optional.empty();
    }
}
