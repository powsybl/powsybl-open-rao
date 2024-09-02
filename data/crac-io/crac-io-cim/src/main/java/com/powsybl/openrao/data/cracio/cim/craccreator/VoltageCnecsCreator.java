/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnecAdder;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.cim.parameters.VoltageCnecsCreationParameters;
import com.powsybl.openrao.data.cracio.cim.parameters.VoltageMonitoredContingenciesAndThresholds;
import com.powsybl.openrao.data.cracio.cim.parameters.VoltageThreshold;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

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
        Map<String, Double> elementsAndNominalV = filterMonitoredNetworkElementsAndFetchNominalV();
        for (Map.Entry<String, VoltageMonitoredContingenciesAndThresholds> entry : voltageCnecsCreationParameters.getMonitoredStatesAndThresholds().entrySet()) {
            Set<String> filteredContingencies = new HashSet<>();
            if (!cracCreationContext.getCrac().getInstant(entry.getKey()).isPreventive()) {
                filteredContingencies = filterContingencies(entry.getValue().getContingencyNames());
            }
            createAndAddCnecs(elementsAndNominalV, entry.getKey(), filteredContingencies, entry.getValue().getThresholdPerNominalV());
        }
    }

    private Map<String, Double> filterMonitoredNetworkElementsAndFetchNominalV() {
        Map<String, Double> elementsAndNominalV = new HashMap<>();
        voltageCnecsCreationParameters.getMonitoredNetworkElements().forEach(neId -> {
                Identifiable<?> identifiable = network.getIdentifiable(neId);
                if (identifiable == null) {
                    cracCreationContext.addVoltageCnecCreationContext(
                        VoltageCnecCreationContext.notImported(neId, null, null, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("Element %s was not found in the network", neId))
                    );
                    return;
                } else if (!(identifiable instanceof VoltageLevel)) {
                    cracCreationContext.addVoltageCnecCreationContext(
                        VoltageCnecCreationContext.notImported(neId, null, null, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Element %s is not a voltage level", neId))
                    );
                    return;
                }
                networkElementNativeIdPerId.put(identifiable.getId(), neId);
                elementsAndNominalV.put(identifiable.getId(), ((VoltageLevel) identifiable).getNominalV());
            }
        );
        return elementsAndNominalV;
    }

    private Set<String> filterContingencies(Set<String> contingencyNames) {
        if (contingencyNames == null || contingencyNames.isEmpty()) {
            // return all contingencies in the crac
            contingencyNativeNamePerId = cracCreationContext.getContingencyCreationContexts().stream().filter(ElementaryCreationContext::isImported)
                .collect(Collectors.toMap(ElementaryCreationContext::getCreatedObjectId, ElementaryCreationContext::getNativeObjectName));
            return contingencyNativeNamePerId.keySet();
        }
        Set<String> filteredContingencies = new HashSet<>();
        contingencyNames.forEach(contingencyName -> {
                ElementaryCreationContext contingencyCreationContext = cracCreationContext.getContingencyCreationContextByName(contingencyName);
                if (contingencyCreationContext == null || !contingencyCreationContext.isImported()) {
                    cracCreationContext.addVoltageCnecCreationContext(
                        VoltageCnecCreationContext.notImported(null, null, contingencyName, ImportStatus.OTHER, "Contingency does not exist in the CRAC or could not be imported")
                    );
                } else {
                    contingencyNativeNamePerId.put(contingencyCreationContext.getCreatedObjectId(), contingencyName);
                    filteredContingencies.add(contingencyCreationContext.getCreatedObjectId());
                }
            }
        );
        return filteredContingencies;
    }

    private void createAndAddCnecs(Map<String, Double> elementsAndNominalV, String instantId, Set<String> filteredContingencies, Map<Double, VoltageThreshold> thresholdPerNominalV) {
        if (!cracCreationContext.getCrac().getInstant(instantId).isPreventive() && filteredContingencies.isEmpty()) {
            return;
        }
        elementsAndNominalV.forEach((key, value) -> {
            VoltageThreshold threshold = thresholdPerNominalV.get(value);
            if (threshold == null) {
                cracCreationContext.addVoltageCnecCreationContext(
                    VoltageCnecCreationContext.notImported(networkElementNativeIdPerId.get(key), instantId, null, ImportStatus.INCOMPLETE_DATA, String.format("the threshold for its nominalV (%.2f) was not defined.", value))
                );
                return;
            }
            if (!filteredContingencies.isEmpty()) {
                filteredContingencies.forEach(coId -> createAndAddVoltageCnecs(key, instantId, coId, threshold));
            } else {
                createAndAddVoltageCnecs(key, instantId, null, threshold);
            }
        });
    }

    private void createAndAddVoltageCnecs(String networkElementId, String instantId, String contingencyId, VoltageThreshold threshold) {
        VoltageCnecAdder adder = cracCreationContext.getCrac().newVoltageCnec();
        String cnecId;
        if (contingencyId != null) {
            cnecId = String.format("[VC] %s - %s - %s", networkElementId, contingencyId, instantId);
            adder.withContingency(contingencyId);
        } else {
            cnecId = String.format("[VC] %s - %s", networkElementId, instantId);
        }
        try {
            adder.withId(cnecId)
                .withNetworkElement(networkElementId)
                .withInstant(instantId)
                .withMonitored()
                .newThreshold().withUnit(threshold.getUnit()).withMin(threshold.getMin()).withMax(threshold.getMax()).add()
                .add();
            cracCreationContext.addVoltageCnecCreationContext(
                VoltageCnecCreationContext.imported(networkElementNativeIdPerId.get(networkElementId), instantId, contingencyNativeNamePerId.get(contingencyId), cnecId)
            );
        } catch (OpenRaoException e) {
            cracCreationContext.addVoltageCnecCreationContext(
                VoltageCnecCreationContext.notImported(networkElementNativeIdPerId.get(networkElementId), instantId, contingencyNativeNamePerId.get(contingencyId), ImportStatus.INCONSISTENCY_IN_DATA, e.getMessage())
            );
        }
    }
}
