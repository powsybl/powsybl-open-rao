/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.dichotomy;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.powsybl.commons.Versionable;
import com.powsybl.commons.config.PlatformConfigNamedProvider;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface DichotomyProvider extends Versionable, PlatformConfigNamedProvider {

    CompletableFuture<DichotomyResult> run(Network network,
                                           Crac crac,
                                           GlskProvider glskProvider,
                                           Set<Country> region,
                                           DichotomyParameters parameters,
                                           ComputationManager computationManager);
}
