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
public record MarketArea(String code,
                         String eic,
                         boolean isMcParticipant,
                         boolean isAhc,
                         boolean isTechCounterparty,
                         boolean isSdac,
                         SynchronousArea synchronousArea,
                         ControlArea relatedCa,
                         boolean isItcp,
                         boolean isIgnoreECs) {
    public MarketArea(final String code,
                      final String eic,
                      final boolean isMcParticipant,
                      final boolean isAhc,
                      final boolean isTechCounterparty,
                      final boolean isSdac,
                      final SynchronousArea synchronousArea,
                      final ControlArea relatedCa,
                      final boolean isItcp,
                      final boolean isIgnoreECs) {
        this.code = Objects.requireNonNull(code, "MarketArea creation does not allow null attribute 'code'");
        this.eic = Objects.requireNonNull(eic, "MarketArea creation does not allow null attribute 'eic'");
        this.isMcParticipant = isMcParticipant;
        this.isAhc = isAhc;
        this.isTechCounterparty = isTechCounterparty;
        this.isSdac = isSdac;
        this.synchronousArea = synchronousArea;
        this.relatedCa = relatedCa;
        this.isItcp = isItcp;
        this.isIgnoreECs = isIgnoreECs;
    }
}
