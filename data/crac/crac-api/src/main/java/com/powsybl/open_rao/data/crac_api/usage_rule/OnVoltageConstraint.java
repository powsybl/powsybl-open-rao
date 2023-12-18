/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_api.usage_rule;

import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.cnec.VoltageCnec;

/**
 * The OnVoltageConstraint UsageRule is defined on a given VoltageCnec. For instance, if a RemedialAction
 * has a OnVoltageConstraint UsageRule with State "cnec1" and UsageMethod TO_BE_EVALUATED, this
 * RemedialAction will only be available if "cnec1" is constrained (= has a negative margin).
 *
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
public interface OnVoltageConstraint extends UsageRule {
    /**
     * Get the VoltageCnec that should be constrained
     */
    VoltageCnec getVoltageCnec();

    /**
     * Get the Instant of the free to use
     */
    Instant getInstant();
}
