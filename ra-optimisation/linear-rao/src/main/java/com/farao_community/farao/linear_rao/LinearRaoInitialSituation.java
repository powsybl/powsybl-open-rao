/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResult;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.powsybl.iidm.network.Network;

/**
 * The LinearRaoInitialSituation is the first AbstractLinearRaoSituation handled
 * by the LinearRao, it is the situation with the RangeActions set-points initially
 * set in the input Network. That is to say the situation before the optimisation of
 * the RangeActions set-points.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class LinearRaoInitialSituation extends AbstractLinearRaoSituation {

    LinearRaoInitialSituation(Crac crac) {
        super(crac);
    }

    @Override
    protected String getVariantPrefix() {
        return "preOptimisationResults-";
    }

    @Override
    protected void addSystematicSensitivityAnalysisResultsToCracVariant(Network network) {
        super.addSystematicSensitivityAnalysisResultsToCracVariant(network);
        // in addition to a standard Situation, add in the Crac the initial RA set-points
        updateRangeActionExtensions(network);
    }

    /**
     * Add in the Crac extension the initial RangeActions set-points
     */
    private void updateRangeActionExtensions(Network network) {
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            double valueInNetwork = rangeAction.getCurrentValue(network);
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            RangeActionResult rangeActionResult = rangeActionResultMap.getVariant(resultVariantId);
            rangeActionResult.setSetPoint(preventiveState, valueInNetwork);
            if (rangeAction instanceof PstRange) {
                ((PstRangeResult) rangeActionResult).setTap(preventiveState, ((PstRange) rangeAction).computeTapPosition(valueInNetwork));
            }
        }
    }

    @Override
    void deleteResultVariant() {
        //We don't want to delete the pre Optim result variant.
    }
}
