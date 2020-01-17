/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.linear_rao.AbstractProblemFiller;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.farao_community.farao.linear_rao.AbstractProblemFiller;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class CoreProblemFiller extends AbstractProblemFiller {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreProblemFiller.class);

    @Override
    public void fill(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        Crac crac = linearRaoData.getCrac();
        Network network = linearRaoData.getNetwork();
        crac.synchronize(linearRaoData.getNetwork());
        crac.getCnecs().forEach(cnec -> linearRaoProblem.addFlowVariable(-LinearRaoProblem.infinity(), LinearRaoProblem.infinity(), cnec.getId()));
        if (crac.getPreventiveState() != null) {
            crac.getRangeActions(network, crac.getPreventiveState(), UsageMethod.AVAILABLE).forEach(rangeAction -> {
                double minValue = rangeAction.getMinValue(network);
                double maxValue = rangeAction.getMaxValue(network);
                rangeAction.getApplicableRangeActions().forEach(applicableRangeAction ->
                    applicableRangeAction.getCurrentValues(network).forEach((networkElement, currentValue) -> {
                        if (currentValue >= minValue && currentValue <= maxValue) {
                            linearRaoProblem.addRangeActionVariable(
                                Math.abs(minValue - currentValue),
                                Math.abs(maxValue - currentValue),
                                String.format("%s - %s", rangeAction.getId(), networkElement.getId()));
                        } else {
                            LOGGER.info("Range action {} is not added to optimisation because current value is already out of bound", rangeAction.getName());
                        }
                    }));
            });
        }
        linearRaoData.getCrac().desynchronize(); // To be sure it is always synchronized with the good network
    }
}
