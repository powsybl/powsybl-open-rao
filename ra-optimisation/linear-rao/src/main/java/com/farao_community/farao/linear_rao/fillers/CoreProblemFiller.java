/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.linear_rao.AbstractProblemFiller;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;

import java.util.List;
import java.util.Set;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class CoreProblemFiller extends AbstractProblemFiller {

    public CoreProblemFiller(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        super(linearRaoProblem, linearRaoData);
    }

    @Override
    public void fill() {
        Crac crac = linearRaoData.getCrac();
        if (crac.getPreventiveState() != null) {
            Set<RangeAction> rangeActions = crac.getRangeActions(linearRaoData.getNetwork(), crac.getPreventiveState(), UsageMethod.AVAILABLE);
            rangeActions.forEach(this::fillRangeAction);
            crac.getCnecs().forEach(cnec -> {
                fillCnec(cnec);
                rangeActions.forEach(rangeAction -> updateCnecConstraintWithRangeAction(cnec, rangeAction));
            });
        }
    }

    @Override
    public void update(List<String> activatedRangeActionIds) {
        Crac crac = linearRaoData.getCrac();
        if (crac.getPreventiveState() != null) {
            Set<RangeAction> rangeActions = crac.getRangeActions(linearRaoData.getNetwork(), crac.getPreventiveState(), UsageMethod.AVAILABLE);
            activatedRangeActionIds.forEach(this::updateRangeActionBounds);
            crac.getCnecs().forEach(cnec -> {
                linearRaoProblem.updateReferenceFlow(cnec.getId(), linearRaoData.getReferenceFlow(cnec));
                rangeActions.forEach(rangeAction -> updateCnecConstraintWithRangeAction(cnec, rangeAction));
            });
        }
    }

    /**
     * Adds a cnec variable
     *
     * @param cnec: cnec to add to optimisation problem
     */
    private void fillCnec(Cnec cnec) {
        linearRaoProblem.addCnec(cnec.getId(), linearRaoData.getReferenceFlow(cnec));
    }

    /**
     * Adds a range action variable
     *
     * @param rangeAction: range action to add to optimisation problem
     */
    private void fillRangeAction(RangeAction rangeAction) {
        linearRaoProblem.addRangeActionVariable(
            rangeAction.getId(),
            rangeAction.getMaxNegativeVariation(linearRaoData.getNetwork()),
            rangeAction.getMaxPositiveVariation(linearRaoData.getNetwork()));
    }

    /**
     * Updates a cnec constraint with the influence of a range action.
     * Example: flow on a cnec depends on the set point of a PST
     *
     * @param cnec: cnec being influenced by the range action
     * @param rangeAction: range action which influences the cnec flow
     */
    private void updateCnecConstraintWithRangeAction(Cnec cnec, RangeAction rangeAction) {
        linearRaoProblem.updateFlowConstraintsWithRangeAction(
                cnec.getId(),
                rangeAction.getId(),
                rangeAction.getSensitivityValue(linearRaoData.getSensitivityComputationResults(cnec.getState()), cnec));
    }

    /**
     * Updates range action boundaries when it has been activated and so set to a new set point.
     *
     * @param activatedRangeActionId: id of the range action that has been modified
     */
    private void updateRangeActionBounds(String activatedRangeActionId) {
        RangeAction rangeAction = linearRaoData.getCrac().getRangeAction(activatedRangeActionId);
        linearRaoProblem.updateRangeActionBounds(
            rangeAction.getId(),
            rangeAction.getMaxNegativeVariation(linearRaoData.getNetwork()),
            rangeAction.getMaxPositiveVariation(linearRaoData.getNetwork()));
    }
}
