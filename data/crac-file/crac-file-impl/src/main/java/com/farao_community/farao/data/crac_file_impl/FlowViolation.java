/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_file_impl;

import com.farao_community.farao.data.crac_file_new.Direction;
import com.farao_community.farao.data.crac_file_new.Side;

/**
 * Limits of a flow through an equipment.
 *
 * @author Xxx Xxx {@literal <xxx.xxx at rte-france.com>}
 */
public class FlowViolation extends AbstractThreshold {

    private Side side;
    private Direction direction;
    private double maxValue;

    public FlowViolation(String unit, Side side, Direction direction, double maxValue) {
        super(unit);
        this.side = side;
        this.direction = direction;
        this.maxValue = maxValue;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }
}
