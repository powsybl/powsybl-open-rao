/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_api.threshold;

import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.Synchronizable;

import java.util.Optional;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public interface Threshold extends Synchronizable {

    NetworkElement getNetworkElement();

    void setNetworkElement(NetworkElement networkElement);

    Unit getUnit();

    PhysicalParameter getPhysicalParameter();

    double getMaxValue();

    Optional<Double> getMinThreshold(Unit requestedUnit);

    Optional<Double> getMaxThreshold(Unit requestedUnit);

    Threshold copy();

    void setMargin(double margin, Unit unit);
}
