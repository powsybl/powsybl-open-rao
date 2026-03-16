/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.timecoupledconstraints;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface TimeCoupledConstraints {
    /**
     * Check whether the time-coupled constraints are satisfied over several timestamped network situations.
     *
     * @param networks: Timestamped networks to check the constraints upon
     * @return boolean value indicating whether the constraints are respected or not
     */
    boolean validate(TemporalData<Network> networks);
}
