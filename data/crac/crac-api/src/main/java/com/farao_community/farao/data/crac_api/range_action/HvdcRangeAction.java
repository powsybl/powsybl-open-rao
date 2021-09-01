/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;

import java.util.List;

/**
 * A range action interface specifying an action on a HVDC
 *
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface HvdcRangeAction extends RangeAction {

    /**
     * Get the HVDC Network Element on which the remedial action applies
     */
    NetworkElement getNetworkElement();

    /**
     * Get the list of  delimiting the bounds of the PST range action
     */
    List<HvdcRange> getRanges();
}
