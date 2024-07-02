/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.cim.craccreator;

import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.openrao.data.cracapi.ContingencyAdder;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.cim.xsd.*;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.craccreation.util.cgmes.CgmesBranchHelper;

import java.util.*;

import static com.powsybl.openrao.data.craccreation.creator.cim.craccreator.CimConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class CimContingencyCreator {
    private final Crac crac;
    private final Network network;
    private final List<TimeSeries> cimTimeSeries;
    private Set<CimContingencyCreationContext> cimContingencyCreationContexts;
    private CimCracCreationContext cracCreationContext;

    public Set<CimContingencyCreationContext> getContingencyCreationContexts() {
        return new HashSet<>(cimContingencyCreationContexts);
    }

    public CimContingencyCreator(List<TimeSeries> cimTimeSeries, Crac crac, Network network, CimCracCreationContext cracCreationContext) {
        this.cimTimeSeries = cimTimeSeries;
        this.crac = crac;
        this.network = network;
        this.cracCreationContext = cracCreationContext;
    }

    public void createAndAddContingencies() {
        this.cimContingencyCreationContexts = new HashSet<>();
        CimCracUtils.applyActionToEveryPoint(
                cimTimeSeries,
                cracCreationContext.getTimeStamp().toInstant(),
                point -> point.getSeries().stream().filter(this::describesContingencyToImport).forEach(
                        series -> series.getContingencySeries().forEach(this::addContingency)
                )
        );
        this.cracCreationContext.setContingencyCreationContexts(cimContingencyCreationContexts);
    }

    private void addContingency(ContingencySeries cimContingency) {
        if (cimContingencyCreationContexts.stream().anyMatch(ccc -> ccc.getNativeId().equals(cimContingency.getMRID()))) {
            return;
        }

        String createdContingencyId = cimContingency.getMRID();
        ContingencyAdder contingencyAdder = crac.newContingency()
                .withId(createdContingencyId)
                .withName(cimContingency.getName());

        if (cimContingency.getRegisteredResource().isEmpty()) {
            cimContingencyCreationContexts.add(CimContingencyCreationContext.notImported(createdContingencyId, cimContingency.getName(), ImportStatus.INCOMPLETE_DATA, "No registered resources"));
            return;
        }

        boolean anyRegisteredResourceOk = false;
        boolean allRegisteredResourcesOk = true;
        StringBuilder missingNetworkElements = null;
        for (ContingencyRegisteredResource registeredResource : cimContingency.getRegisteredResource()) {
            Identifiable<?> networkElement = getNetworkElementInNetwork(registeredResource.getMRID().getValue());
            if (networkElement != null) {
                String networkElementId = networkElement.getId();
                ContingencyElementType contingencyElementType = ContingencyElement.of(networkElement).getType();
                contingencyAdder.withContingencyElement(networkElementId, contingencyElementType);
                anyRegisteredResourceOk = true;
            } else {
                allRegisteredResourcesOk = false;
                if (missingNetworkElements == null) {
                    missingNetworkElements = new StringBuilder(registeredResource.getMRID().getValue());
                } else {
                    missingNetworkElements.append(", ").append(registeredResource.getMRID().getValue());
                }
            }
        }
        if (anyRegisteredResourceOk) {
            contingencyAdder.add();
            String message = allRegisteredResourcesOk ? null : String.format("Some network elements were not found in the network: %s", missingNetworkElements.toString());
            cimContingencyCreationContexts.add(CimContingencyCreationContext.imported(createdContingencyId, cimContingency.getName(), createdContingencyId, !allRegisteredResourcesOk, message));
        } else {
            cimContingencyCreationContexts.add(CimContingencyCreationContext.notImported(createdContingencyId, cimContingency.getName(), ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "None of the contingency's registered resources was found in network"));
        }
    }

    private Identifiable<?> getNetworkElementInNetwork(String networkElementIdInCrac) {
        Identifiable<?> networkElementToReturn = null;
        Identifiable<?> networkElement = network.getIdentifiable(networkElementIdInCrac);
        if (networkElement == null) {
            CgmesBranchHelper cgmesBranchHelper = new CgmesBranchHelper(networkElementIdInCrac, network);
            if (cgmesBranchHelper.isValid()) {
                networkElementToReturn = cgmesBranchHelper.getBranch();
                networkElement = cgmesBranchHelper.getBranch();
            }
        } else {
            networkElementToReturn = networkElement;
        }

        if (networkElement instanceof DanglingLine danglingLine) {
            Optional<TieLine> optionalTieLine = danglingLine.getTieLine();
            if (optionalTieLine.isPresent()) {
                networkElementToReturn = optionalTieLine.get();
            }
        }
        return networkElementToReturn;
    }

    private boolean describesContingencyToImport(Series series) {
        return series.getBusinessType().equals(CONTINGENCY_SERIES_BUSINESS_TYPE)
            || series.getBusinessType().equals(CNECS_SERIES_BUSINESS_TYPE)
            || series.getBusinessType().equals(REMEDIAL_ACTIONS_SERIES_BUSINESS_TYPE);
    }
}
