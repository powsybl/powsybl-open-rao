/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_computation;

import com.farao_community.farao.data.crac_api.Cnec;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class MultipleSensitivityProvider implements CnecSensitivityProvider {
    private List<CnecSensitivityProvider> cnecSensitivityProviders;

    MultipleSensitivityProvider() {
        cnecSensitivityProviders = new ArrayList<>();
    }

    void addProvider(CnecSensitivityProvider cnecSensitivityProvider) {
        cnecSensitivityProviders.add(cnecSensitivityProvider);
    }

    public Set<Cnec> getCnecs() {
        Set<Cnec> cnecs = new HashSet<>();
        for (CnecSensitivityProvider cnecSensitivityProvider : cnecSensitivityProviders) {
            cnecs.addAll(cnecSensitivityProvider.getCnecs());
        }
        return cnecs;
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        //using a set to avoid duplicates
        Set<Contingency> contingencies = new HashSet<>();
        for (CnecSensitivityProvider cnecSensitivityProvider : cnecSensitivityProviders) {
            contingencies.addAll(cnecSensitivityProvider.getContingencies(network));
        }
        return new ArrayList<>(contingencies);

    }

    @Override
    public List<SensitivityFactor> getFactors(Network network) {
        //using a set to avoid duplicates
        Set<SensitivityFactor> factors = new HashSet<>();
        for (CnecSensitivityProvider cnecSensitivityProvider : cnecSensitivityProviders) {
            factors.addAll(cnecSensitivityProvider.getFactors(network));
        }
        return new ArrayList<>(factors);
    }
}
