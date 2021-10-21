/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.powsybl.contingency.*;
import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public abstract class AbstractSimpleSensitivityProvider implements CnecSensitivityProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSimpleSensitivityProvider.class);

    protected Set<FlowCnec> cnecs;
    protected boolean factorsInMegawatt = false;
    protected boolean factorsInAmpere = false;
    protected boolean afterContingencyOnly = false;

    AbstractSimpleSensitivityProvider(Set<FlowCnec> cnecs, Set<Unit> requestedUnits) {
        this.cnecs = cnecs;
        this.setRequestedUnits(requestedUnits);
    }

    @Override
    public void setRequestedUnits(Set<Unit> requestedUnits) {
        factorsInMegawatt = false;
        factorsInAmpere = false;
        for (Unit unit : requestedUnits) {
            switch (unit) {
                case MEGAWATT:
                    factorsInMegawatt = true;
                    break;
                case AMPERE:
                    factorsInAmpere = true;
                    break;
                default:
                    LOGGER.warn("Unit {} cannot be handled by the sensitivity provider as it is not a flow unit", unit);
            }
        }

        if (!factorsInAmpere && !factorsInMegawatt) {
            LOGGER.error("The Sensitivity Provider should contain at least Megawatt or Ampere unit");
        }

    }

    public Set<FlowCnec> getFlowCnecs() {
        return cnecs;
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        Set<com.farao_community.farao.data.crac_api.Contingency> cracContingencies =  cnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isPresent())
            .map(cnec -> cnec.getState().getContingency().get())
            .collect(Collectors.toSet());
        return cracContingencies.stream()
            .map(contingency -> SensitivityAnalysisUtil.convertCracContingencyToPowsybl(contingency, network))
            .collect(Collectors.toList());
    }

    @Override
    public void disableFactorsForBaseCaseSituation() {
        this.afterContingencyOnly = true;
    }
}
