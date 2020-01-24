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
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class CoreProblemFiller extends AbstractProblemFiller {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreProblemFiller.class);

    public CoreProblemFiller(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        super(linearRaoProblem, linearRaoData);
    }

    @Override
    public void fill() {
        Crac crac = linearRaoData.getCrac();
        Network network = linearRaoData.getNetwork();

        crac.synchronize(network);
        if (crac.getPreventiveState() != null) {
            Set<RangeAction> rangeActions = crac.getRangeActions(network, crac.getPreventiveState(), UsageMethod.AVAILABLE);
            rangeActions.forEach(this::fillRangeAction);
            crac.getCnecs().forEach(cnec -> {
                fillCnec(cnec);
                rangeActions.forEach(rangeAction -> updateCnecConstraintWithRangeAction(cnec, rangeAction));
            });
        }
        linearRaoData.getCrac().desynchronize(); // To be sure it is always synchronized with the good network
    }

    private void fillCnec(Cnec cnec) {
        linearRaoProblem.addCnec(cnec.getId(), linearRaoData.getReferenceFlow(cnec));
    }

    /**
     * We are making two assumptions here. Range actions must be only PST actions to be put in optimization problem
     * and min and max values returned by range action getMinValue and getMaxValue methods must return PST taps.
     * This is a temporary patch waiting for evolutions of crac api.
     *
     * @param rangeAction: range action to be put in optimisation problem.
     */
    private void fillRangeAction(RangeAction rangeAction) {
        double minValue = rangeAction.getMinValue(linearRaoData.getNetwork());
        double maxValue = rangeAction.getMaxValue(linearRaoData.getNetwork());
        rangeAction.getApplicableRangeActions().forEach(applicableRangeAction ->
            applicableRangeAction.getNetworkElements().forEach(networkElement -> {
                Identifiable pNetworkElement = linearRaoData.getNetwork().getIdentifiable(networkElement.getId());
                if (pNetworkElement instanceof TwoWindingsTransformer) {
                    TwoWindingsTransformer transformer = (TwoWindingsTransformer) pNetworkElement;
                    double currentAlpha = transformer.getPhaseTapChanger().getCurrentStep().getAlpha();
                    double minAlpha = transformer.getPhaseTapChanger().getStep((int) minValue).getAlpha();
                    double maxAlpha = transformer.getPhaseTapChanger().getStep((int) maxValue).getAlpha();
                    if (currentAlpha >= minAlpha && currentAlpha <= maxAlpha) {
                        linearRaoProblem.addRangeActionVariable(
                            rangeAction.getId(), networkElement.getId(),
                            Math.abs(minAlpha - currentAlpha), Math.abs(maxAlpha - currentAlpha));
                    } else {
                        LOGGER.warn("Range action {} is not added to optimisation because current value is already out of bound", rangeAction.getName());
                    }
                } else {
                    LOGGER.warn("Range action {} is not added to optimisation because this type of action is not already implemented", rangeAction.getName());
                }
            }));
    }

    private void updateCnecConstraintWithRangeAction(Cnec cnec, RangeAction rangeAction) {
        rangeAction.getApplicableRangeActions().forEach(applicableRangeAction ->
            applicableRangeAction.getNetworkElements().forEach(networkElement ->
                linearRaoProblem.addRangeActionFlowOnBranch(
                    cnec.getId(), rangeAction.getId(), networkElement.getId(),
                    linearRaoData.getSensitivity(cnec, rangeAction)
                )));
    }
}
