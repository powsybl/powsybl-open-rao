/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_computation;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class MultipleSensitivityProvider implements SensitivityProvider {
    private List<SensitivityProvider> sensitivityProviders;

    MultipleSensitivityProvider() {
        sensitivityProviders = new ArrayList<>();
    }

    void addProvider(SensitivityProvider sensitivityProvider) {
        sensitivityProviders.add(sensitivityProvider);
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        Set<Contingency> contingencies = new HashSet<>();
        for (SensitivityProvider sensitivityProvider : sensitivityProviders) {
            contingencies.addAll(sensitivityProvider.getContingencies(network));
        }
        return new ArrayList<>(contingencies);

    }

    @Override
    public List<SensitivityFactor> getFactors(Network network) {
        Set<SensitivityFactor> factors = new HashSet<>();
        for (SensitivityProvider sensitivityProvider : sensitivityProviders) {
            factors.addAll(sensitivityProvider.getFactors(network));
        }
        return new ArrayList<>(factors);
    }
}
