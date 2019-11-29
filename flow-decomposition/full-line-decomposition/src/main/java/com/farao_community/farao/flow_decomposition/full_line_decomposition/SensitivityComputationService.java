/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputation;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.farao_community.farao.commons.FaraoException;

import java.util.Objects;

/**
 * sensitivity computation wrapper object
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class SensitivityComputationService {
    private SensitivityComputationFactory sensitivityComputationFactory;
    private ComputationManager computationManager;

    public SensitivityComputationResults compute(SensitivityFactorsProvider provider, Network network, String workingStateId, FullLineDecompositionParameters parameters) {
        SensitivityComputation sensitivityComputation = sensitivityComputationFactory.create(network, computationManager, 1);
        try {
            return sensitivityComputation.run(provider, workingStateId, parameters.getExtendable().getSensitivityComputationParameters()).join();
        } catch (Exception e) {
            throw new FaraoException(e);
        }
    }

    public SensitivityComputationService(SensitivityComputationFactory sensitivityComputationFactory, ComputationManager computationManager) {
        this.sensitivityComputationFactory = Objects.requireNonNull(sensitivityComputationFactory);
        this.computationManager = Objects.requireNonNull(computationManager);
    }
}
