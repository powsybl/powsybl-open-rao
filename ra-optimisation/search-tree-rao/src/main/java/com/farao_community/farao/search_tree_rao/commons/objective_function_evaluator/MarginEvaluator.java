/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.commons.objective_function_evaluator;

import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.search_tree_rao.result.api.FlowResult;
import com.powsybl.open_rao.search_tree_rao.result.api.RangeActionActivationResult;
import com.powsybl.open_rao.search_tree_rao.result.api.SensitivityResult;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface MarginEvaluator {

    double getMargin(FlowResult flowResult, FlowCnec flowCnec, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, Unit unit);

    double getMargin(FlowResult flowResult, FlowCnec flowCnec, Side side, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, Unit unit);
}
