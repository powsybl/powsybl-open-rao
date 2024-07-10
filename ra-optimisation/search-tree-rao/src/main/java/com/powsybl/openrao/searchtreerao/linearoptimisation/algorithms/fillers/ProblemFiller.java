/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.impl.MultiStateRemedialActionResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PerimeterResultWithCnecs;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public interface ProblemFiller {

    void fill(LinearProblem linearProblem, PerimeterResultWithCnecs flowAndSensiResult);

    void updateBetweenSensiIteration(LinearProblem linearProblem, PerimeterResultWithCnecs flowAndSensiResult, MultiStateRemedialActionResultImpl rangeActionResult);

    void updateBetweenMipIteration(LinearProblem linearProblem, MultiStateRemedialActionResultImpl rangeActionResult);
}
