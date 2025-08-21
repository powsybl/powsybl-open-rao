/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import java.util.Objects;

/**
 * Virtual hub description POJO
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public record VirtualHub(String code, String eic, boolean isMcParticipant, boolean isAhc, String nodeName, MarketArea relatedMa, String oppositeHub) {
    public VirtualHub(String code, String eic, boolean isMcParticipant, boolean isAhc, String nodeName, MarketArea relatedMa, String oppositeHub) {
        this.code = Objects.requireNonNull(code, "VirtualHub creation does not allow null code");
        this.eic = Objects.requireNonNull(eic, "VirtualHub creation does not allow null eic");
        this.isMcParticipant = isMcParticipant;
        this.isAhc = isAhc;
        this.nodeName = Objects.requireNonNull(nodeName, "VirtualHub creation does not allow null nodeName");
        this.relatedMa = Objects.requireNonNull(relatedMa, "VirtualHub creation does not allow null relatedMa");
        this.oppositeHub = oppositeHub;
    }
}
