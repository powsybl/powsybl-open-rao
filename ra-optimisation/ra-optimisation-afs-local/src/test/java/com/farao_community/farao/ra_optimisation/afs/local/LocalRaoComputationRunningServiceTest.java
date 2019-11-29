/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.afs.local;

import com.google.common.collect.ImmutableList;
import com.powsybl.afs.ServiceExtension;
import com.powsybl.afs.ext.base.LocalNetworkCacheServiceExtension;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.ra_optimisation.afs.RaoComputationRunnerTest;
import com.farao_community.farao.ra_optimisation.RaoComputation;
import com.farao_community.farao.ra_optimisation.RaoComputationFactory;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class LocalRaoComputationRunningServiceTest extends RaoComputationRunnerTest {

    private static class RaoComputationFactoryMock implements RaoComputationFactory {
        @Override
        public RaoComputation create(Network network, CracFile cracFile, ComputationManager computationManager, int priority) {
            return (workingStateId, parameters) -> CompletableFuture.completedFuture(new RaoComputationResult(RaoComputationResult.Status.SUCCESS));
        }
    }

    @Override
    protected List<ServiceExtension> getServiceExtensions() {
        return ImmutableList.of(new LocalRaoComputationServiceExtension(RaoComputationFactoryMock::new),
                new LocalNetworkCacheServiceExtension());
    }

}

