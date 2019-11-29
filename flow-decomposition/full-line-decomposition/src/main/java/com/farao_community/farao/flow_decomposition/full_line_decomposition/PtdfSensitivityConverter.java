/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.powsybl.sensitivity.factors.BranchFlowPerInjectionIncrease;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.InjectionIncrease;
import com.farao_community.farao.data.crac_file.CracFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sensitivity factors provider for PTDF calculation
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PtdfSensitivityConverter implements SensitivityFactorsProvider {
    private CracFile cracFile;

    public PtdfSensitivityConverter(CracFile cracFile) {
        this.cracFile = Objects.requireNonNull(cracFile);
    }

    private void addFactorsForBranch(Network network, List<SensitivityFactor> appendee, Branch branch) {
        assert network != null;
        assert appendee != null;

        NetworkUtil.getInjectionStream(network)
            .filter(NetworkUtil::isConnectedAndInMainSynchronous)
            .forEach(injection -> appendee.add(new BranchFlowPerInjectionIncrease(
                new BranchFlow(branch.getId(), branch.getName(), branch.getId()),
                new InjectionIncrease(injection.getId(), injection.getName(), injection.getId())))
            );
    }

    @Override
    public List<SensitivityFactor> getFactors(Network network) {
        Objects.requireNonNull(network);

        List<SensitivityFactor> factors = new ArrayList<>();
        cracFile.getPreContingency().getMonitoredBranches().stream()
                .map(monitoredBranch -> network.getBranch(monitoredBranch.getBranchId()))
                .filter(Objects::nonNull)
                .filter(NetworkUtil::isConnectedAndInMainSynchronous)
                .forEach(itBranch -> addFactorsForBranch(network, factors, itBranch));
        return factors;
    }
}
