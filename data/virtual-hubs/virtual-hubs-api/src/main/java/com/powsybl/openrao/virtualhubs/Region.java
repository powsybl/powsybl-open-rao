/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import java.util.Objects;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public record Region(String code, String eic) {
    public Region(final String code, final String eic) {
        this.code = Objects.requireNonNull(code, "Region creation does not allow null attribute 'code'");
        this.eic = Objects.requireNonNull(eic, "Region creation does not allow null attribute 'eic'");
    }
}
