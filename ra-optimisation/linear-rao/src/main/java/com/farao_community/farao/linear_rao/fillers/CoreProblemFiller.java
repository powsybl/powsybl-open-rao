/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.linear_rao.AbstractProblemFiller;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class CoreProblemFiller extends AbstractProblemFiller {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreProblemFiller.class);

    @Override
    public void fill(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        this.linearRaoData = linearRaoData;
        this.linearRaoProblem = linearRaoProblem;
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
        linearRaoProblem.addCnec(cnec.getId(), linearRaoData.getReferenceFlow(cnec), -LinearRaoProblem.infinity(), LinearRaoProblem.infinity());
    }

    private void fillRangeAction(RangeAction rangeAction) {
        double minValue = rangeAction.getMinValue(linearRaoData.getNetwork());
        double maxValue = rangeAction.getMaxValue(linearRaoData.getNetwork());
        rangeAction.getApplicableRangeActions().forEach(applicableRangeAction ->
            applicableRangeAction.getCurrentValues(linearRaoData.getNetwork()).forEach((networkElement, currentValue) -> {
                if (currentValue >= minValue && currentValue <= maxValue) {
                    linearRaoProblem.addRangeActionVariable(
                        rangeAction.getId(), networkElement.getId(),
                        Math.abs(minValue - currentValue), Math.abs(maxValue - currentValue));
                } else {
                    LOGGER.info("Range action {} is not added to optimisation because current value is already out of bound", rangeAction.getName());
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
