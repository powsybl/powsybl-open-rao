/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_computation;

import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.factors.BranchFlowPerLinearGlsk;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class PtdfSensitivityProvider extends AbstractSimpleSensitivityProvider {
    private final GlskProvider glskProvider;

    PtdfSensitivityProvider(GlskProvider glskProvider) {
        this.glskProvider = Objects.requireNonNull(glskProvider);
        cnecs = new ArrayList<>();
    }

    @Override
    public List<SensitivityFactor> getFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();
        Map<String, LinearGlsk> mapCountryLinearGlsk = glskProvider.getAllGlsk(network);
        cnecs.forEach(cnec -> mapCountryLinearGlsk.values().stream()
            .map(linearGlsk -> new BranchFlowPerLinearGlsk(new BranchFlow(cnec.getId(), cnec.getName(), cnec.getNetworkElement().getId()), linearGlsk))
            .forEach(factors::add));
        return factors;
    }

}
