/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.factors.BranchFlowPerLinearGlsk;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class PtdfSensitivityProvider extends AbstractSimpleSensitivityProvider {
    private final ZonalData<LinearGlsk> glsk;

    private static final Logger LOGGER = LoggerFactory.getLogger(PtdfSensitivityProvider.class);

    PtdfSensitivityProvider(ZonalData<LinearGlsk> glsk, Set<BranchCnec> cnecs, Set<Unit> units) {
        super(cnecs, units);

        // todo : handle PTDFs in AMPERE
        if (factorsInAmpere || !factorsInMegawatt) {
            LOGGER.warn("PtdfSensitivity provider currently only handle Megawatt unit");
            factorsInMegawatt = true;
            factorsInAmpere = false;
        }
        this.glsk = Objects.requireNonNull(glsk);
    }

    @Override
    public List<SensitivityFactor> getFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();
        Map<String, LinearGlsk> mapCountryLinearGlsk = glsk.getDataPerZone();

        cnecs.stream().map(Cnec::getNetworkElement)
            .distinct()
            .forEach(ne -> mapCountryLinearGlsk.values().stream()
                .map(linearGlsk -> new BranchFlowPerLinearGlsk(new BranchFlow(ne.getId(), ne.getName(), ne.getId()), linearGlsk))
                .forEach(factors::add));

        return factors;
    }

}
