/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.api;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface LinearOptimizationResult extends FlowResult, SensitivityResult, RangeActionActivationResult, ObjectiveFunctionResult {

    LinearProblemStatus getStatus();

    RangeActionActivationResult getRangeActionActivationResult();
}
