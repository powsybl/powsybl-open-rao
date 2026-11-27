/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import java.util.List;
import java.util.Objects;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public record InternalHvdc(String eic,
                           String code,
                           List<HvdcPole> poles,
                           MarketArea relatedMa,
                           boolean isMthvdc,
                           boolean isBipolar) {
    public InternalHvdc(final String eic,
                        final String code,
                        final List<HvdcPole> poles,
                        final MarketArea relatedMa,
                        final boolean isMthvdc,
                        final boolean isBipolar) {
        this.code = Objects.requireNonNull(code, "InternalHvdc creation does not allow null attribute 'code'");
        this.eic = Objects.requireNonNull(eic, "InternalHvdc creation does not allow null attribute 'eic'");
        this.poles = Objects.requireNonNull(poles, "InternalHvdc creation does not allow null attribute 'poles'");
        this.relatedMa = relatedMa;
        // TODO Make it mandatory after fixing JsonFbConstraintCracCreationParameters
        // this.relatedMa = Objects.requireNonNull(relatedMa, "InternalHvdc creation does not allow null attribute 'relatedMa'");
        this.isMthvdc = isMthvdc;
        this.isBipolar = isBipolar;
    }
}
