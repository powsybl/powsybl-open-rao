/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import java.util.List;

/**
 * HVDC remedial action alignment
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public class HvdcAlignLever extends AbstractRemedialActionRange {

    private List<HvdcLever> hvdcLevers;

    public HvdcAlignLever(List<HvdcLever> hvdcLevers) {
        this.hvdcLevers = hvdcLevers;
    }

    public List<HvdcLever> getHvdcLevers() {
        return hvdcLevers;
    }

    public void setHvdcLevers(List<HvdcLever> hvdcLevers) {
        this.hvdcLevers = hvdcLevers;
    }

    public void addHvdcLever(HvdcLever hvdcLever) {
        hvdcLevers.add(hvdcLever);
    }
}
