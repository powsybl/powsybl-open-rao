/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * todo: javadoc
 *
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class PstHelper {
    public enum TapConvention {
        CENTERED_ON_ZERO, // Taps from -x to x
        STARTS_AT_ONE // Taps from 1 to y
    }

    private String pstId;

    private boolean isPstValid = true;
    private String invalidPstReason = "";
    private int lowTapPosition;
    private int highTapPosition;
    private int initialTapPosition;
    private Map<Integer, Double> tapToAngleConversionMap;

    public PstHelper(String pstId, Network network) {
        this.pstId = pstId;
        interpretWithNetwork(network);
    }

    public boolean isPstValid() {
        return isPstValid;
    }

    public String getInvalidPstReason() {
        return invalidPstReason;
    }

    public int getLowTapPosition() {
        return lowTapPosition;
    }

    public int getHighTapPosition() {
        return highTapPosition;
    }

    public int getInitialTap() {
        return initialTapPosition;
    }

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

    public int normalizeTap(int originalTap, TapConvention originalTapConvention) {
        if (originalTapConvention.equals(TapConvention.CENTERED_ON_ZERO)) {
            return originalTap; // TODO : add (min + max) / 2 like before ?
        } else {
            return lowTapPosition + originalTap - 1;
        }
    }
}
