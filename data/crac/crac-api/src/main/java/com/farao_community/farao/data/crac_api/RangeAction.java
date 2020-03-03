/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationResults;


/**
 * Remedial action interface specifying an action of type range. <br/>
 * This means that there is a value to set and this value should be within a range. <br/>
 * The apply method involves a {@link Network} and a double (setpoint's value),
 * that's why there is a need to define this interface besides the {@link NetworkAction} interface
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface RangeAction extends RemedialAction<RangeAction>, Synchronizable {

    double getMinValue(Network network);

    double getMaxValue(Network network);

    double getMaxNegativeVariation(Network network);

    double getMaxPositiveVariation(Network network);

    double getSensitivityValue(SensitivityComputationResults sensitivityComputationResults, Cnec cnec);

    // The setpoint is computed by an optimiser.
    void apply(Network network, double setpoint);
}
