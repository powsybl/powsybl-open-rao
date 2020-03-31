/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.mock;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.Rao;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.RaoResult;
import com.powsybl.iidm.network.Network;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class RaoRunnerMock extends Rao.Runner {

    public RaoRunnerMock(RaoProvider provider) {
        super(provider);
    }

    @Override
    public RaoResult run(Network network, Crac crac, String variantId) {
        String preOpt = "preOpt-".concat(variantId);
        String postOpt = "postOpt-".concat(variantId);

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        resultVariantManager.createVariant(preOpt);
        resultVariantManager.createVariant(postOpt);

        crac.getExtension(CracResultExtension.class).getVariant(preOpt).setCost(10);
        crac.getExtension(CracResultExtension.class).getVariant(postOpt).setCost(2);

        RaoResult raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId(preOpt);
        raoResult.setPostOptimVariantId(postOpt);
        return raoResult;
    }
}
