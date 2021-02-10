/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFactorsProvider;

import java.util.List;
import java.util.Set;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public interface CnecSensitivityProvider extends SensitivityFactorsProvider, ContingenciesProvider {

    Set<BranchCnec> getBranchCnecs();

    default List<SensitivityFactor> getAllFactors(Network network) {
        List<SensitivityFactor> factors = getCommonFactors(network);
        factors.addAll(getAdditionalFactors(network));
        return factors;
    }
}
