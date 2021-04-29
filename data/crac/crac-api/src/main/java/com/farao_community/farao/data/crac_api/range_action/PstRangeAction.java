/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.TapConvention;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;

/**
 * A range action interface specifying an action on a PST
 *
 * The bounds of the range within which the tap of the PST should remain
 * are defined with a List of {@link TapRange}.
 *
 * The convention used for the setpoint of the Range Action, and so the
 * 'double' of the super class methods apply(), getMinValue(), getMaxValue()
 * and getCurrentValue() is the angle of the PST, in degrees.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface PstRangeAction extends RangeAction {

    /**
     * Get the PST Network Element on which the remedial action applies
     */
    NetworkElement getNetworkElement();

    /**
     * Get the list of operational TapRange delimiting the bounds of the PST range action
     */
    List<TapRange> getRanges();

    /**
     * Get the initial tap of the PST
     */
    int getInitialTap();

    /**
     * Get the conversion map between the tap and the angle (setpoint) of the PST
     */
    Map<Integer, Double> getTapToAngleConversionMap();

    /**
     * Get the value of the tap of the PST Range Action for a given Network
     */
    int getCurrentTapPosition(Network network, TapConvention tapConvention);

    /**
     * Convert the tap of the PST designated by the Remedial Action in angle
     */
    //todo: network required here
    double convertTapToAngle(int tap);

    /**
     * Convert the angle of the PST designated by the Remedial Action in tap
     */
    //todo: network required here
    int convertAngleToTap(double angle);
}
