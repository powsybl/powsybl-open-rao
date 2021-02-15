/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.mocks;

import com.powsybl.iidm.network.PhaseTapChangerStep;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PhaseTapChangerStepMock implements PhaseTapChangerStep {
    private final int tapPosition;

    public PhaseTapChangerStepMock(int tapPosition) {
        this.tapPosition = tapPosition;
    }

    @Override
    public double getAlpha() {
        return tapPosition / 100.;
    }

    @Override
    public PhaseTapChangerStep setAlpha(double v) {
        return null;
    }

    @Override
    public double getRho() {
        return 0;
    }

    @Override
    public PhaseTapChangerStep setRho(double v) {
        return null;
    }

    @Override
    public double getR() {
        return 0;
    }

    @Override
    public PhaseTapChangerStep setR(double v) {
        return null;
    }

    @Override
    public double getX() {
        return 0;
    }

    @Override
    public PhaseTapChangerStep setX(double v) {
        return null;
    }

    @Override
    public double getB() {
        return 0;
    }

    @Override
    public PhaseTapChangerStep setB(double v) {
        return null;
    }

    @Override
    public double getG() {
        return 0;
    }

    @Override
    public PhaseTapChangerStep setG(double v) {
        return null;
    }
}
