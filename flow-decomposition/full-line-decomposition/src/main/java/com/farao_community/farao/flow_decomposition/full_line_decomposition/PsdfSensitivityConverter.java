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
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;
import com.farao_community.farao.data.crac_file.CracFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sensitivity factors provider for PSDF calculation
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PsdfSensitivityConverter implements SensitivityFactorsProvider {
    private CracFile cracFile;

    public PsdfSensitivityConverter(CracFile cracFile) {
        this.cracFile = Objects.requireNonNull(cracFile);
    }

    private void addFactorsForBranch(Network network, List<SensitivityFactor> appendee, Branch branch) {
        assert network != null;
        assert appendee != null;

        network.getTwoWindingsTransformerStream()
                .filter(NetworkUtil::isConnectedAndInMainSynchronous)
                .filter(NetworkUtil::branchIsPst)
                .forEach(twt -> appendee.add(new BranchFlowPerPSTAngle(
                        new BranchFlow(branch.getId(), branch.getName(), branch.getId()),
                        new PhaseTapChangerAngle(twt.getId(), twt.getName(), twt.getId())
                )));
    }

    @Override
    public List<SensitivityFactor> getFactors(Network network) {
        Objects.requireNonNull(network);

        List<SensitivityFactor> factors = new ArrayList<>();
        cracFile.getPreContingency().getMonitoredBranches().stream()
                .map(monitoredBranch -> network.getBranch(monitoredBranch.getBranchId()))
                .filter(Objects::nonNull)
                .filter(branch -> branch.getTerminal1().isConnected() && branch.getTerminal2().isConnected())
                .forEach(itBranch -> addFactorsForBranch(network, factors, itBranch));
        return factors;
    }
}
