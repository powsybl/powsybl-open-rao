/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.contingency.CimContingencyCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.VoltageCnecsCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.VoltageMonitoredContingenciesAndThresholds;
import com.farao_community.farao.data.crac_creation.util.cgmes.CgmesBranchHelper;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates VoltageCnecs after reading VoltageCnecsCreationParameters
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageCnecsCreator {

    private final VoltageCnecsCreationParameters voltageCnecsCreationParameters;
    private final CimCracCreationContext cracCreationContext;
    private final Network network;
    private Map<String, String> networkElementNativeIdPerId = new HashMap<>();
    private Map<String, String> contingencyNativeNamePerId = new HashMap<>();

    public VoltageCnecsCreator(VoltageCnecsCreationParameters voltageCnecsCreationParameters, CimCracCreationContext cracCreationContext, Network network) {
        this.voltageCnecsCreationParameters = voltageCnecsCreationParameters;
        this.cracCreationContext = cracCreationContext;
        this.network = network;
    }

    public void createAndAddCnecs() {
        if (voltageCnecsCreationParameters == null) {
            return;
        }

        Map<String, Double> elementsAndNominalV = filterMonitoredNetworkElementsAndFetchNominalV();
        for (Map.Entry<Instant, VoltageMonitoredContingenciesAndThresholds> entry : voltageCnecsCreationParameters.getMonitoredStatesAndThresholds().entrySet()) {
            Set<String> filteredContingencies = new HashSet<>();
            if (!entry.getKey().equals(Instant.PREVENTIVE)) {
                filteredContingencies = filterContingencies(entry.getValue().getContingencyNames());
            }
            createAndAddCnecs(elementsAndNominalV, entry.getKey(), filteredContingencies, entry.getValue().getThresholdPerNominalV());
        }
    }

    private Map<String, Double> filterMonitoredNetworkElementsAndFetchNominalV() {
        Map<String, Double> elementsAndNominalV = new HashMap<>();
        voltageCnecsCreationParameters.getMonitoredNetworkElements().forEach(neId -> {
                CgmesBranchHelper branchHelper = new CgmesBranchHelper(neId, network);
                if (!branchHelper.isValid()) {
                    cracCreationContext.addVoltageCnecCreationContext(
                        VoltageCnecCreationContext.notImported(neId, null, null, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, branchHelper.getInvalidReason())
                    );
                    return;
                }
                Branch<?> branch = network.getBranch(branchHelper.getIdInNetwork());
                double nominalV1 = branch.getTerminal1().getVoltageLevel().getNominalV();
                double nominalV2 = branch.getTerminal2().getVoltageLevel().getNominalV();
                if (Math.abs(nominalV1 - nominalV2) > 0.1) {
                    cracCreationContext.addVoltageCnecCreationContext(
                        VoltageCnecCreationContext.notImported(neId, null, null, ImportStatus.INCONSISTENCY_IN_DATA, "Different voltage levels on both ends")
                    );
                    return;
                }
                // TODO : other checks ?
                networkElementNativeIdPerId.put(branchHelper.getIdInNetwork(), neId);
                elementsAndNominalV.put(branchHelper.getIdInNetwork(), nominalV1);
            }
        );
        return elementsAndNominalV;
    }

    private Set<String> filterContingencies(Set<String> contingencyNames) {
        if (contingencyNames == null || contingencyNames.isEmpty()) {
            // return all contingencies in the crac
            contingencyNativeNamePerId = cracCreationContext.getContingencyCreationContexts().stream().filter(CimContingencyCreationContext::isImported)
                .collect(Collectors.toMap(CimContingencyCreationContext::getCreatedContingencyId, CimContingencyCreationContext::getNativeName));
            return contingencyNativeNamePerId.keySet();
        }
        Set<String> filteredContingencies = new HashSet<>();
        contingencyNames.forEach(contingencyName -> {
                CimContingencyCreationContext contingencyCreationContext = cracCreationContext.getContingencyCreationContextByName(contingencyName);
                if (contingencyCreationContext == null || !contingencyCreationContext.isImported()) {
                    cracCreationContext.addVoltageCnecCreationContext(
                        VoltageCnecCreationContext.notImported(null, null, contingencyName, ImportStatus.OTHER, "Contingency does not exist in the CRAC or could not be imported")
                    );
                } else {
                    contingencyNativeNamePerId.put(contingencyCreationContext.getCreatedContingencyId(), contingencyName);
                    filteredContingencies.add(contingencyCreationContext.getCreatedContingencyId());
                }
            }
        );
        return filteredContingencies;
    }

    private void createAndAddCnecs(Map<String, Double> elementsAndNominalV, Instant instant, Set<String> filteredContingencies, Map<Double, Threshold> thresholdPerNominalV) {
        if (!instant.equals(Instant.PREVENTIVE) && filteredContingencies.isEmpty()) {
            return;
        }
        elementsAndNominalV.forEach((key, value) -> {
            Threshold threshold = thresholdPerNominalV.get(value);
            if (threshold == null) {
                cracCreationContext.addVoltageCnecCreationContext(
                    VoltageCnecCreationContext.notImported(networkElementNativeIdPerId.get(key), instant, null, ImportStatus.INCOMPLETE_DATA, String.format("the threshold for its nominalV (%.2f) was not defined.", value))
                );
                return;
            }
            if (!filteredContingencies.isEmpty()) {
                filteredContingencies.forEach(coId -> createAndAddVoltageCnecs(key, instant, coId, threshold));
            } else {
                createAndAddVoltageCnecs(key, instant, null, threshold);
            }
        });
    }

    private void createAndAddVoltageCnecs(String networkElementId, Instant instant, String contingencyId, Threshold threshold) {
        VoltageCnecAdder adder = cracCreationContext.getCrac().newVoltageCnec();
        String cnecId;
        if (contingencyId != null) {
            cnecId = String.format("[VC] %s - %s - %s", networkElementId, contingencyId, instant);
            adder.withContingency(contingencyId);
        } else {
            cnecId = String.format("[VC] %s - %s", networkElementId, instant);
        }
        try {
            adder.withId(cnecId)
                .withNetworkElement(networkElementId)
                .withInstant(instant)
                .withMonitored()
                .newThreshold().withUnit(threshold.getUnit()).withMin(threshold.min().orElse(null)).withMax(threshold.max().orElse(null)).add()
                .add();
            cracCreationContext.addVoltageCnecCreationContext(
                VoltageCnecCreationContext.imported(networkElementNativeIdPerId.get(networkElementId), instant, contingencyNativeNamePerId.get(contingencyId), cnecId)
            );
        } catch (FaraoException e) {
            cracCreationContext.addVoltageCnecCreationContext(
                VoltageCnecCreationContext.notImported(networkElementNativeIdPerId.get(networkElementId), instant, contingencyNativeNamePerId.get(contingencyId), ImportStatus.INCONSISTENCY_IN_DATA, e.getMessage())
            );
        }
    }
}
