/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_domain;

import com.powsybl.iidm.network.Network;

/**
 * Absolute validity domain.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class RelativeDynamicRange implements Range {

    private double min;
    private double max;

    @Override
    public double getMin(Network network) {
        return 0;
    }

    @Override
    public double getMax(Network network) {
        return 0;
    }
}
