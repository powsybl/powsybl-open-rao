/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.powsybl.contingency.*;
import com.powsybl.iidm.network.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public abstract class AbstractSimpleSensitivityProvider implements CnecSensitivityProvider {
    protected Set<Cnec> cnecs;

    AbstractSimpleSensitivityProvider() {
        cnecs = new HashSet<>();
    }

    public void addCnecs(Set<Cnec> cnecs) {
        this.cnecs.addAll(cnecs);
    }

    public Set<Cnec> getCnecs() {
        return cnecs;
    }

    private Contingency convertCracContingencyToPowsybl(com.farao_community.farao.data.crac_api.Contingency cracContingency, Network network) {
        String id = cracContingency.getId();
        List<ContingencyElement> contingencyElements = cracContingency.getNetworkElements().stream()
            .map(element -> convertCracContingencyElementToPowsybl(element, network))
            .collect(Collectors.toList());
        return new Contingency(id, contingencyElements);
    }

    private ContingencyElement convertCracContingencyElementToPowsybl(NetworkElement cracContingencyElement, Network network) {
        String elementId = cracContingencyElement.getId();
        Identifiable<?> networkIdentifiable = network.getIdentifiable(elementId);
        if (networkIdentifiable instanceof Branch) {
            return new BranchContingency(elementId);
        } else if (networkIdentifiable instanceof Generator) {
            return new GeneratorContingency(elementId);
        } else if (networkIdentifiable instanceof HvdcLine) {
            return new HvdcLineContingency(elementId);
        } else if (networkIdentifiable instanceof BusbarSection) {
            return new BusbarSectionContingency(elementId);
        } else {
            throw new SensitivityAnalysisException("Unable to apply contingency element " + elementId);
        }
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        Set<com.farao_community.farao.data.crac_api.Contingency> cracContingencies =  cnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isPresent())
            .map(cnec -> cnec.getState().getContingency().get())
            .collect(Collectors.toSet());
        return cracContingencies.stream()
            .map(contingency -> convertCracContingencyToPowsybl(contingency, network))
            .collect(Collectors.toList());
    }
}
