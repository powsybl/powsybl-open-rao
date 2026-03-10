/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.rangeaction;

import com.powsybl.action.Action;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.range.TapRange;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
public interface PstRangeAction extends RangeAction<PstRangeAction> {

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
     * Get the smallest absolute angle difference between two consecutive taps.
     */
    double getSmallestAngleStep();

    /**
     * Get the value of the tap of the PST Range Action for a given Network
     */
    int getCurrentTapPosition(Network network);

    /**
     * Convert the tap of the PST designated by the Remedial Action in angle
     */
    double convertTapToAngle(int tap);

    /**
     * Convert the angle of the PST designated by the Remedial Action in tap
     */
    int convertAngleToTap(double angle);

    /**
     * Integrity check for angle
     */
    void checkAngle(double angle);

    @Override
    default Set<Action> toActions(double setPoint, Network network) {
        return Set.of(toAction(convertAngleToTap(setPoint)));
    }

    default PhaseTapChangerTapPositionAction toAction(int tapPosition) {
        return new PhaseTapChangerTapPositionAction("%s@%s".formatted(getId(), tapPosition), getNetworkElement().getId(), false, tapPosition);
    }
}
