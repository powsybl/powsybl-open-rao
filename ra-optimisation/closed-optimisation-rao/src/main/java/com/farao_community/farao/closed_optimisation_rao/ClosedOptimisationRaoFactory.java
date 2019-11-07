/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.ra_optimisation.RaoComputation;
import com.farao_community.farao.ra_optimisation.RaoComputationFactory;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.sensitivity.SensitivityComputationFactory;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class ClosedOptimisationRaoFactory implements RaoComputationFactory {

    @Override
    public RaoComputation create(Network network, CracFile cracFile, ComputationManager computationManager, int priority) {
        LoadFlow.Runner loadFlowRunner = LoadFlow.find();
        SensitivityComputationFactory sensitivityComputationFactory = ComponentDefaultConfig.load().newFactoryImpl(SensitivityComputationFactory.class);
        return new ClosedOptimisationRao(network, cracFile, computationManager, loadFlowRunner, sensitivityComputationFactory);
    }
}
