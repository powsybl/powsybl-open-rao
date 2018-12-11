/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.afs;

import com.google.common.collect.ImmutableList;
import com.powsybl.afs.ServiceExtension;
import com.powsybl.afs.ext.base.LocalNetworkCacheServiceExtension;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.flow_decomposition_results.FlowDecompositionResults;
import com.farao_community.farao.flow_decomposition.FlowDecomposition;
import com.farao_community.farao.flow_decomposition.FlowDecompositionFactory;
import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class LocalFlowDecompositionRunningServiceTest extends FlowDecompositionRunnerTest {

    private static class FlowDecompositionFactoryMock implements FlowDecompositionFactory {
        @Override
        public FlowDecomposition create(Network network, ComputationManager computationManager, int priority) {
            return new FlowDecomposition() {
                @Override
                public CompletableFuture<FlowDecompositionResults> run(String workingStateId, FlowDecompositionParameters flowDecompositionParameters, CracFile cracFile) {
                    FlowDecompositionResults result = new FlowDecompositionResults();
                    return CompletableFuture.completedFuture(result);
                }
            };
        }
    }

    @Override
    protected List<ServiceExtension> getServiceExtensions() {
        return ImmutableList.of(new LocalFlowDecompositionRunningServiceExtension(FlowDecompositionFactoryMock::new),
                new LocalNetworkCacheServiceExtension());
    }
}
