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

    private void fillRangeAction(RangeAction rangeAction) {
        linearRaoProblem.addRangeActionVariable(
            rangeAction.getId(),
            rangeAction.getMinValue(linearRaoData.getNetwork()),
            rangeAction.getMaxValue(linearRaoData.getNetwork()));
    }

    private void updateCnecConstraintWithRangeAction(Cnec cnec, RangeAction rangeAction) {
        State preventiveState = linearRaoData.getCrac().getPreventiveState();
        if (preventiveState != null) {
            linearRaoProblem.addRangeActionFlowOnBranch(
                cnec.getId(),
                rangeAction.getId(),
                rangeAction.getSensitivityValue(linearRaoData.getSensitivityComputationResults(preventiveState), cnec));
        }
    }
}
