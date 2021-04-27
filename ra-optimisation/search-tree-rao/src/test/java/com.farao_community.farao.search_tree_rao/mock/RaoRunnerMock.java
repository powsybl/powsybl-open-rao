/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.mock;

import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.*;
import com.farao_community.farao.rao_api.parameters.RaoParameters;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class RaoRunnerMock extends Rao.Runner {

    public RaoRunnerMock(RaoProvider provider) {
        super(provider);
    }

    @Override
    public RaoResult run(RaoInput raoInput, RaoParameters raoParameters) {
        String preOpt = "preOpt-".concat(raoInput.getNetworkVariantId());
        String postOpt = "postOpt-".concat(raoInput.getNetworkVariantId());

        ResultVariantManager resultVariantManager = raoInput.getCrac().getExtension(ResultVariantManager.class);
        resultVariantManager.createVariant(preOpt);
        resultVariantManager.createVariant(postOpt);

        raoInput.getCrac().getExtension(CracResultExtension.class).getVariant(preOpt).setFunctionalCost(10);
        raoInput.getCrac().getExtension(CracResultExtension.class).getVariant(postOpt).setFunctionalCost(2);

        RaoResult raoResult = new RaoResult(RaoResult.Status.DEFAULT);
        raoResult.setPreOptimVariantId(preOpt);
        raoResult.setPostOptimVariantId(postOpt);
        return raoResult;
    }
}
