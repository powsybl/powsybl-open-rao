/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.timecoupledconstraints.PstConstraints;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Limits the tap variation of a preventive PST between two consecutive timestamps.
 * <p>
 *     Requires the PST model to be set to APPROXIMATED_INTEGERS in the RAO parameters.
 * </p>
 *
 * @author Atena Amnache {@literal <atena.amnache at rte-france.com>}
 */
public class PstConstraintsFiller implements ProblemFiller {
    private final List<OffsetDateTime> timestamps;
    private final Set<PstConstraints> pstConstraints;
    private final TemporalData<State> preventiveStates;
    private final TemporalData<Set<PstRangeAction>> pstRangeActionsPerTimestamp;
    private final double timestampDuration;

    public PstConstraintsFiller(TemporalData<State> preventiveStates,
                                TemporalData<Set<PstRangeAction>> pstRangeActionsPerTimestamp,
                                Set<PstConstraints> pstConstraints) {
        this.preventiveStates = preventiveStates;
        this.pstConstraints = pstConstraints;
        this.timestamps = preventiveStates.getTimestamps();
        this.pstRangeActionsPerTimestamp = pstRangeActionsPerTimestamp;
        this.timestampDuration = FillersUtil.computeTimestampDuration(this.timestamps);
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        int numberOfTimestamps = timestamps.size();
        for (PstConstraints individualPstConstraints : pstConstraints) {
            for (int timestampIndex = 0; timestampIndex < numberOfTimestamps - 1; timestampIndex++) {
                addTapGradientConstraint(linearProblem, individualPstConstraints, timestamps.get(timestampIndex), timestamps.get(timestampIndex + 1));
            }
        }
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    // Constraints

    /**
     * Constraint limiting the tap variation of a PST between two consecutive timestamps.
     * <br/>
     * |τ(r,s,t+1) − τ(r,s,t)| <= gradient
     */
    private void addTapGradientConstraint(LinearProblem linearProblem, PstConstraints pstConstraints, OffsetDateTime timestamp, OffsetDateTime nextTimestamp) {
        String pstId = pstConstraints.getPstId();
        Optional<PstRangeAction> pstRangeAction = getPstRangeAction(pstId, timestamp);
        Optional<PstRangeAction> nextPstRangeAction = getPstRangeAction(pstId, nextTimestamp);
        if (pstRangeAction.isPresent() && nextPstRangeAction.isPresent()) {
            State state = preventiveStates.getData(timestamp).orElseThrow();
            State nextState = preventiveStates.getData(nextTimestamp).orElseThrow();

            double downwardTapGradient = pstConstraints.getDownwardTapGradient().map(gradient -> gradient * timestampDuration).orElse(-linearProblem.infinity());
            double upwardTapGradient = pstConstraints.getUpwardTapGradient().map(gradient -> gradient * timestampDuration).orElse(linearProblem.infinity());

            OpenRaoMPConstraint tapGradientConstraint = linearProblem.addPstTapGradientConstraint(pstRangeAction.get(), downwardTapGradient, upwardTapGradient, state, nextState);
            tapGradientConstraint.setCoefficient(linearProblem.getTapVariable(pstRangeAction.get(), state), -1.0);
            tapGradientConstraint.setCoefficient(linearProblem.getTapVariable(nextPstRangeAction.get(), nextState), 1.0);
        }
    }

    private Optional<PstRangeAction> getPstRangeAction(String pstId, OffsetDateTime timestamp) {
        return pstRangeActionsPerTimestamp.getData(timestamp).orElseThrow().stream()
            .filter(pstRangeAction -> pstRangeAction.getId().equals(pstId)).findFirst();
    }
}
