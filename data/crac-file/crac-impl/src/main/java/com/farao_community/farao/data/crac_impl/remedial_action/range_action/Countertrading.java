/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.powsybl.iidm.network.Network;

/**
 * Countertrading remedial action.
 *
 * @author Xxx Xxx {@literal <xxx.xxx at rte-france.com>}
 */

public class Countertrading implements ApplicableRangeAction {

    public Countertrading() {
    }

    @Override
    public void apply(Network network, double setpoint) {
        throw new UnsupportedOperationException();
    }
}
