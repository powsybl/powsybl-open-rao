/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.contingency;

import com.farao_community.farao.data.crac_api.ContingencyAdder;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.*;
import com.farao_community.farao.data.crac_creation.util.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants.CONTINGENCY_SERIES_BUSINESS_TYPE;

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

        for (TimeSeries cimTimeSerie : cimTimeSeries) {
            for (SeriesPeriod cimPeriodInTimeSerie : cimTimeSerie.getPeriod()) {
                for (Point cimPointInPeriodInTimeSerie : cimPeriodInTimeSerie.getPoint()) {
                    for (Series cimSerie : cimPointInPeriodInTimeSerie.getSeries().stream().filter(this::describesContingencyToImport).collect(Collectors.toList())) {
                        for (ContingencySeries cimContingency : cimSerie.getContingencySeries()) {
                            addContingency(cimContingency);
                        }
                    }
                }
            }
        }
        this.cracCreationContext.setContingencyCreationContexts(cimContingencyCreationContexts);
    }

    private void addContingency(ContingencySeries cimContingency) {
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
        String missingNetworkElements = null;
        for (ContingencyRegisteredResource registeredResource : cimContingency.getRegisteredResource()) {
            String networkElementId = getNetworkElementIdInNetwork(registeredResource.getMRID().getValue());
            if (networkElementId != null) {
                contingencyAdder.withNetworkElement(networkElementId);
                anyRegisteredResourceOk = true;
            } else {
                allRegisteredResourcesOk = false;
                if (missingNetworkElements == null) {
                    missingNetworkElements = registeredResource.getMRID().getValue();
                } else {
                    missingNetworkElements += ", " + registeredResource.getMRID().getValue();
                }
            }
        }
        if (anyRegisteredResourceOk) {
            contingencyAdder.add();
            String message = allRegisteredResourcesOk ? null : String.format("Some network elements were not found in the network: %s", missingNetworkElements);
            cimContingencyCreationContexts.add(CimContingencyCreationContext.imported(createdContingencyId, cimContingency.getName(), createdContingencyId, !allRegisteredResourcesOk, message));
        } else {
            cimContingencyCreationContexts.add(CimContingencyCreationContext.notImported(createdContingencyId, cimContingency.getName(), ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "None of the contingency's registered resources was found in network"));
        }
    }

    private String getNetworkElementIdInNetwork(String networkElementIdInCrac) {
        String networkElementId = null;
        Identifiable<?> networkElement = network.getIdentifiable(networkElementIdInCrac);
        if (networkElement == null) {
            CgmesBranchHelper cgmesBranchHelper = new CgmesBranchHelper(networkElementIdInCrac, network);
            if (cgmesBranchHelper.isValid()) {
                networkElementId = cgmesBranchHelper.getIdInNetwork();
                networkElement = cgmesBranchHelper.getBranch();
            }
        } else {
            networkElementId = networkElement.getId();
        }

        if (networkElement instanceof DanglingLine) {
            Optional<TieLine> optionalTieLine = ((DanglingLine) networkElement).getTieLine();
            if (optionalTieLine.isPresent()) {
                networkElementId = optionalTieLine.get().getId();
            }
        }

        return networkElementId;
    }

    private boolean describesContingencyToImport(Series series) {
        return series.getBusinessType().equals(CONTINGENCY_SERIES_BUSINESS_TYPE);
    }
}
