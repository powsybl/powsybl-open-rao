/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.mocks;

import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.PhaseTapChangerStep;
import com.powsybl.iidm.network.Terminal;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PhaseTapChangerMock implements PhaseTapChanger {
    private int lowTapPosition;
    private int highTapPosition;
    private int currentTapPosition;

    public PhaseTapChangerMock(int lowTapPosition, int highTapPosition, int currentTapPosition) {
        this.lowTapPosition = lowTapPosition;
        this.highTapPosition = highTapPosition;
        this.currentTapPosition = currentTapPosition;
    }

    @Override
    public RegulationMode getRegulationMode() {
        return null;
    }

    @Override
    public PhaseTapChanger setRegulationMode(RegulationMode regulationMode) {
        return null;
    }

    @Override
    public double getRegulationValue() {
        return 0;
    }

    @Override
    public PhaseTapChanger setRegulationValue(double v) {
        return null;
    }

    @Override
    public int getLowTapPosition() {
        return lowTapPosition;
    }

    @Override
    public PhaseTapChanger setLowTapPosition(int i) {
        return null;
    }

    @Override
    public int getHighTapPosition() {
        return highTapPosition;
    }

    @Override
    public int getTapPosition() {
        return currentTapPosition;
    }

    @Override
    public PhaseTapChanger setTapPosition(int i) {
        return null;
    }

    @Override
    public int getStepCount() {
        return 0;
    }

    @Override
    public PhaseTapChangerStep getStep(int i) {
        return new PhaseTapChangerStepMock(i);
    }

    @Override
    public PhaseTapChangerStep getCurrentStep() {
        return new PhaseTapChangerStepMock(currentTapPosition);
    }

    @Override
    public boolean isRegulating() {
        return false;
    }

    @Override
    public PhaseTapChanger setRegulating(boolean b) {
        return null;
    }

    @Override
    public Terminal getRegulationTerminal() {
        return null;
    }

    @Override
    public PhaseTapChanger setRegulationTerminal(Terminal terminal) {
        return null;
    }

    @Override
    public void remove() {

    }
}
