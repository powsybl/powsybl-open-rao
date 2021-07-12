/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public interface ProblemFiller {

    void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult);

    void update(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult);
}
