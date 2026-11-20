/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.List;

public class HybridPstProblemFiller implements ProblemFiller {
    private final ContinuousRangeActionGroupFiller continuousFiller;
    private final DiscretePstTapFiller discreteFiller;
    private final DiscretePstGroupFiller discretePstGroupFiller;
    private boolean discretePhaseFilled;
    private FlowResult flowResult;
    private SensitivityResult sensitivityResult;

    public HybridPstProblemFiller(ContinuousRangeActionGroupFiller continuousFiller, DiscretePstTapFiller discreteFiller, DiscretePstGroupFiller discretePstGroupFiller) {
        this.continuousFiller = continuousFiller;
        this.discreteFiller = discreteFiller;
        this.discretePstGroupFiller = discretePstGroupFiller;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        this.flowResult = flowResult;
        this.sensitivityResult = sensitivityResult;
        continuousFiller.fill(linearProblem, flowResult, sensitivityResult, rangeActionActivationResult);
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // after first continuous solve only do discrete solve
        if (!discretePhaseFilled) {
            discreteFiller.fill(linearProblem, flowResult, sensitivityResult, rangeActionActivationResult);
            discretePstGroupFiller.fill(linearProblem, flowResult, sensitivityResult, rangeActionActivationResult);
            discretePhaseFilled = true;
        }
        discreteFiller.updateBetweenMipIteration(linearProblem, rangeActionActivationResult);
        discretePstGroupFiller.updateBetweenMipIteration(linearProblem, rangeActionActivationResult);
    }

}
