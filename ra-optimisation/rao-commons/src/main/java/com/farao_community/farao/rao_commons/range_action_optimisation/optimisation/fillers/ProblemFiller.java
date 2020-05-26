/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.range_action_optimisation.optimisation.fillers;

import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.range_action_optimisation.optimisation.LinearRaoProblem;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public interface ProblemFiller {

    void fill(RaoData raoData, LinearRaoProblem linearRaoProblem, FillerParameters fillerParameters);

    void update(RaoData raoData, LinearRaoProblem linearRaoProblem, FillerParameters fillerParameters);
}
