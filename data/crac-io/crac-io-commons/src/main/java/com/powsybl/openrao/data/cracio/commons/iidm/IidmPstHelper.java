/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.iidm;

import com.powsybl.openrao.data.cracio.commons.PstHelper;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class to be used in Crac creators.
 * It exposes useful functions to import PST range actions.
 *
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class IidmPstHelper implements PstHelper {

    private final String pstId;

    private boolean isPstValid = true;
    private String invalidPstReason;
    private int lowTapPosition;
    private int highTapPosition;
    private int initialTapPosition;
    private Map<Integer, Double> tapToAngleConversionMap;

    public IidmPstHelper(String pstId, Network network) {
        this.pstId = pstId;
        interpretWithNetwork(network);
    }

    @Override
    public boolean isValid() {
        return isPstValid;
    }

    @Override
    public String getInvalidReason() {
        return invalidPstReason;
    }

    @Override
    public String getIdInNetwork() {
        return pstId;
    }

    @Override
    public boolean isInvertedInNetwork() {
        return true;
    }

    /**
     * Returns the lowest tap position of the PST, as defined in the network. Convention is centered on zero.
     */
    public int getLowTapPosition() {
        return lowTapPosition;
    }

    /**
     * Returns the highest tap position of the PST, as defined in the network. Convention is centered on zero.
     */
    public int getHighTapPosition() {
        return highTapPosition;
    }

    /**
     * Returns the initial tap position of the PST, as defined in the network. Convention is centered on zero.
     */
    public int getInitialTap() {
        return initialTapPosition;
    }

    /**
     * Returns the tap to angle conversion map of the PST, as defined in the network. Convention for taps is centered on zero.
     */
    public Map<Integer, Double> getTapToAngleConversionMap() {
        return tapToAngleConversionMap;
    }

    private void interpretWithNetwork(Network network) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(pstId);
        if (Objects.isNull(transformer)) {
            invalidate(String.format("transformer with id %s was not found in network", pstId));
            return;
        }

        PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
        if (Objects.isNull(phaseTapChanger)) {
            invalidate(String.format("transformer with id %s does not have a phase tap changer", pstId));
            return;
        }

        this.lowTapPosition = phaseTapChanger.getLowTapPosition();
        this.highTapPosition = phaseTapChanger.getHighTapPosition();
        this.initialTapPosition = phaseTapChanger.getTapPosition();

        buildTapToAngleConversionMap(phaseTapChanger);
    }

    private void buildTapToAngleConversionMap(PhaseTapChanger phaseTapChanger) {
        tapToAngleConversionMap = new HashMap<>();
        phaseTapChanger.getAllSteps().forEach((tap, step) -> tapToAngleConversionMap.put(tap, step.getAlpha()));
    }

    private void invalidate(String reason) {
        this.isPstValid = false;
        this.invalidPstReason = reason;
    }
}
