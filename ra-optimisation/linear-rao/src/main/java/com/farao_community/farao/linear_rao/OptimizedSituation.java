/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.rao_api.RaoResult;
import com.powsybl.iidm.network.Network;

/**
 * The OptimizedSituation is an AbstractSituation in which the RangeAction
 * set-points are optimized. The LinearRao might go through several
 * OptimizedSituations as its algorithm iterates over several network situations.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class OptimizedSituation extends AbstractSituation {

    /**
     * Computation status of the optimisation problem resolution
     */
    ComputationStatus lpStatus;

    /**
     * Constructor
     */
    OptimizedSituation(Crac crac, Network network) {
        super(crac, network);
        this.lpStatus = ComputationStatus.NOT_RUN;
    }

    ComputationStatus getLpStatus() {
        return lpStatus;
    }

    @Override
    protected String getVariantPrefix() {
        return "postOptimisationResults-";
    }

    /**
     * Solve the LinearRaoProblem associated to this network situation. Results are
     * set in the Crac result variant with id resultVariantId.
     */
    void solveLp(LinearOptimisationEngine linearOptimisationEngine) {
        RaoResult lpRaoResult = linearOptimisationEngine.solve(resultVariantId);
        if (lpRaoResult.getStatus() == RaoResult.Status.FAILURE) {
            lpStatus = ComputationStatus.RUN_NOK;
        } else {
            lpStatus = ComputationStatus.RUN_OK;
        }
    }

    /**
     * Apply the optimised RangeAction on a Network
     */
    void applyRAs() {
        if (lpStatus != ComputationStatus.RUN_OK) {
            throw new FaraoException("RangeAction have not been optimized yet and therefore cannot be applied");
        }
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            rangeAction.apply(network, rangeActionResultMap.getVariant(resultVariantId).getSetPoint(preventiveState));
        }
    }

}
