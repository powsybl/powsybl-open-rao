/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_file_impl;


/**
 * Business object for a generic threshold in the CRAC file
 *
 * @author Xxx Xxx {@literal <xxx.xxx at rte-france.com>}
 */
public abstract class AbstractThreshold {
    protected String unit;

    public AbstractThreshold(String unit) {
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
