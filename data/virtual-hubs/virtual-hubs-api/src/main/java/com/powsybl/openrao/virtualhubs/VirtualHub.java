/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Virtual hub description POJO
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public record VirtualHub(String code,
                         String eic,
                         boolean isMcParticipant,
                         boolean isAhc,
                         String nodeName,
                         MarketArea relatedMa,
                         String oppositeHub,
                         boolean isSdac,
                         BigDecimal splittingFactor,
                         BigDecimal lf,
                         BigDecimal rlf) {
    public VirtualHub(final String code,
                      final String eic,
                      final boolean isMcParticipant,
                      final boolean isAhc,
                      final String nodeName,
                      final MarketArea relatedMa,
                      final String oppositeHub,
                      final boolean isSdac,
                      final BigDecimal splittingFactor,
                      final BigDecimal lf,
                      final BigDecimal rlf) {
        this.eic = Objects.requireNonNull(eic, "VirtualHub creation does not allow null attribute 'eic'");
        this.code = Objects.requireNonNull(code, "VirtualHub creation does not allow null attribute 'code'");
        this.isMcParticipant = isMcParticipant;
        this.isAhc = isAhc;
        this.nodeName = nodeName;
        this.relatedMa = Objects.requireNonNull(relatedMa, "VirtualHub creation does not allow null attribute 'relatedMa'");
        this.oppositeHub = oppositeHub;
        this.isSdac = isSdac;
        this.splittingFactor = splittingFactor;
        this.lf = lf;
        this.rlf = rlf;
    }
}
