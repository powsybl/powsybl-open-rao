/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.raoimpl.result.api.FlowResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionActivationResult;
import com.powsybl.openrao.raoimpl.result.api.SensitivityResult;

public class HybridPstProblemFiller implements ProblemFiller {
    private final ContinuousRangeActionGroupFiller continuousRangeActionGroupFiller;
    private final DiscretePstTapFiller discreteFiller;
    private final DiscretePstGroupFiller discretePstGroupFiller;
    private int iteration = 0;

    public HybridPstProblemFiller(ContinuousRangeActionGroupFiller continuousRangeActionGroupFiller, DiscretePstTapFiller discreteFiller, DiscretePstGroupFiller discretePstGroupFiller) {
        this.continuousRangeActionGroupFiller = continuousRangeActionGroupFiller;
        this.discreteFiller = discreteFiller;
        this.discretePstGroupFiller = discretePstGroupFiller;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        this.iteration += 1;
        if (iteration > 1) {
            discreteFiller.fill(linearProblem, flowResult, sensitivityResult, rangeActionActivationResult);
            discretePstGroupFiller.fill(linearProblem, flowResult, sensitivityResult, rangeActionActivationResult);
        } else {
            // fill with continuous pst model only during first iteration
            continuousRangeActionGroupFiller.fill(linearProblem, flowResult, sensitivityResult, rangeActionActivationResult);
        }
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        discreteFiller.updateBetweenMipIteration(linearProblem, rangeActionActivationResult);
        discretePstGroupFiller.updateBetweenMipIteration(linearProblem, rangeActionActivationResult);
    }

}
