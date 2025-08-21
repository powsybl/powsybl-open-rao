/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import java.util.Objects;

/**
 * Market area description POJO
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public record MarketArea(String code, String eic, boolean isMcParticipant, boolean isAhc) {
    public MarketArea(String code, String eic, boolean isMcParticipant, boolean isAhc) {
        this.code = Objects.requireNonNull(code, "MarketArea creation does not allow null code");
        this.eic = Objects.requireNonNull(eic, "MarketArea creation does not allow null eic");
        this.isMcParticipant = isMcParticipant;
        this.isAhc = isAhc;
    }
}
