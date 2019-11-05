/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.threshold;

import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.Side;

import static com.farao_community.farao.data.crac_api.Unit.AMPERE;

/**
 * Limits of a flow through an equipment.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class FlowThreshold extends AbstractThreshold {

    private Side side;
    private Direction direction;
    private double maxValue;

    public FlowThreshold(Side side, Direction direction, double maxValue) {
        super(AMPERE);
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
