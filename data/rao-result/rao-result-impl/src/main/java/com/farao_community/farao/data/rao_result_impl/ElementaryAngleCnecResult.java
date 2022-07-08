/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class ElementaryAngleCnecResult {

    private double angle = Double.NaN;
    private double margin = Double.NaN;

    public double getAngle(Unit unit) {
        if (unit.equals(Unit.DEGREE)) {
            return angle;
        } else {
            throw new FaraoException("Angle results are only available in DEGREE");
        }
    }

    public double getMargin(Unit unit) {
        if (unit.equals(Unit.DEGREE)) {
            return margin;
        } else {
            throw new FaraoException("Angle results are only available in DEGREE");
        }
    }

    public void setAngle(double angle, Unit unit) {
        if (unit.equals(Unit.DEGREE)) {
            this.angle = angle;
        } else {
            throw new FaraoException("Angle results are only available in DEGREE");
        }
    }

    public void setMargin(double margin, Unit unit) {
        if (unit.equals(Unit.DEGREE)) {
            this.margin = margin;
        } else {
            throw new FaraoException("Angle results are only available in DEGREE");
        }
    }
}
