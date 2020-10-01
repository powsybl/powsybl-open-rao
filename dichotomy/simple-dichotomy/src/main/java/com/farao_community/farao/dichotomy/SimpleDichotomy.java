/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.dichotomy;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(DichotomyProvider.class)
public class SimpleDichotomy implements DichotomyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDichotomy.class);

    @Override
    public CompletableFuture<DichotomyResult> run(Network network, Crac crac, GlskProvider glskProvider, Set<Country> region, DichotomyParameters parameters, ComputationManager computationManager) {
        LOGGER.info("For simple dichotomy, computation manager will be ignored");
        return null;
    }

    @Override
    public String getName() {
        return "simple-dichotomy";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
