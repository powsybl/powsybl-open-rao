/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.mock;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.RaoInput;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.RaoResult;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.concurrent.CompletableFuture;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class LinearRaoMock implements RaoProvider {

    public static final String CRAC_NAME_RAO_THROWS_EXCEPTION = "Exception";
    public static final String CRAC_NAME_RAO_RETURNS_FAILURE = "StatusFailure";

    private RaoResult createLittleResult(Crac crac) {
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager == null) {
            resultVariantManager = new ResultVariantManager();
            crac.addExtension(ResultVariantManager.class, resultVariantManager);
        }
        resultVariantManager.createVariant("preOptimVariant");
        resultVariantManager.createVariant("postOptimVariant");

        RaoResult raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId("preOptimVariant");
        raoResult.setPreOptimVariantId("postOptimVariant");
        return raoResult;
    }

    @Override
    public CompletableFuture<RaoResult> run(Network network, Crac crac, String variantId, ComputationManager computationManager, RaoParameters parameters) {
        if (crac.getName().equals(CRAC_NAME_RAO_THROWS_EXCEPTION)) {
            throw new FaraoException("Mocked error while running LinearRaoMock");
        }
        if (crac.getName().equals(CRAC_NAME_RAO_RETURNS_FAILURE)) {
            return CompletableFuture.completedFuture(new RaoResult(RaoResult.Status.FAILURE));
        }
        return CompletableFuture.completedFuture(createLittleResult(crac));
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, ComputationManager computationManager, RaoParameters parameters) {
        if (raoInput.getCrac().getName().equals(CRAC_NAME_RAO_THROWS_EXCEPTION)) {
            throw new FaraoException("Mocked error while running LinearRaoMock");
        }
        if (raoInput.getCrac().getName().equals(CRAC_NAME_RAO_RETURNS_FAILURE)) {
            return CompletableFuture.completedFuture(new RaoResult(RaoResult.Status.FAILURE));
        }
        return CompletableFuture.completedFuture(createLittleResult(raoInput.getCrac()));
    }

    @Override
    public String getName() {
        return "Linear Range Action Rao Mock";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
