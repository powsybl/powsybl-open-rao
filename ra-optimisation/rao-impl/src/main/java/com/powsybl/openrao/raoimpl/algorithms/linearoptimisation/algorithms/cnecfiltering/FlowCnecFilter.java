/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.cnecfiltering;

import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoimpl.result.api.FlowResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionActivationResult;
import com.powsybl.openrao.raoimpl.result.api.SensitivityResult;

import java.util.Set;

/**
 * Selects the flow CNECs that must be modeled in the linear problem.
 *
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 */
@FunctionalInterface
public interface FlowCnecFilter {
    FlowCnecFilter NONE = (cnecs, flowResult, sensitivityResult, linearizationPoint) -> cnecs;

    /**
     * @return {@code cnecs} minus the CNECs that can be omitted from the linear problem built around the given
     * linearization point. CNECs the filter does not know about are always kept.
     */
    Set<FlowCnec> retain(Set<FlowCnec> cnecs, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult linearizationPoint);
}
