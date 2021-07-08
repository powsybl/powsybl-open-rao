/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.result_api;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface SensitivityResult {

    ComputationStatus getSensitivityStatus();

    double getSensitivityValue(FlowCnec flowCnec, RangeAction rangeAction, Unit unit);

    double getSensitivityValue(FlowCnec flowCnec, LinearGlsk linearGlsk, Unit unit);
}
