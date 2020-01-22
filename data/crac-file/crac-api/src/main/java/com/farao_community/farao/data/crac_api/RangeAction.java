/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.Set;

/**
 * Remedial action interface specifying an action of type range
 * This means that there is a value to set and this value is not directly defined
 * but define by a range.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public interface RangeAction extends ApplicableRangeAction, RemedialAction {
    Set<ApplicableRangeAction> getApplicableRangeActions();

    double getMinValue(Network network);

    double getMaxValue(Network network);

    // The setpoint is computed by an optimiser.
    void apply(Network network, double setpoint);
}
