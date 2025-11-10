/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda.scenariorepository;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ScenarioRepository {
    private Map<String, NetworkVariation> networkVariations;
    private Map<String, Set<NetworkVariation>> scenarios;
    private static final String REFERENCE_SCENARIO = "_REFERENCE_";

    public ScenarioRepository(Set<NetworkVariation> networkVariations) {
        this.networkVariations = new HashMap<>();
        for (NetworkVariation networkVariation : networkVariations) {
            this.networkVariations.put(networkVariation.getId(), networkVariation);
        }
        this.scenarios = new HashMap<>();
        // Always create reference scenario without modifications
        this.addScenario(REFERENCE_SCENARIO, Set.of());
    }

    public String getReferenceScenario() {
        return REFERENCE_SCENARIO;
    }

    public void addScenario(String id, Set<String> networkVariationIds) {
        if (scenarios.containsKey(id)) {
            throw new OpenRaoException("Scenario with id " + id + " already exists");
        }
        if (networkVariationIds.stream().anyMatch(networkVariationId -> !networkVariations.containsKey(networkVariationId))) {
            throw new OpenRaoException("At least one network variation was not declared.");
        }
        Set<NetworkVariation> networkVariationsSet = networkVariationIds.stream().map(networkVariations::get).collect(Collectors.toSet());
        if (!scenarios.containsValue(networkVariationsSet)) {
            scenarios.put(id, networkVariationsSet);
        }
    }

    public Set<String> getScenarios() {
        return scenarios.keySet();
    }

    public Map<String, TemporalData<Double>> applyScenario(String scenarioId, TemporalData<Network> networks) {
        Map<String, TemporalData<Double>> shifts = new HashMap<>();
        scenarios.get(scenarioId).forEach(networkVariation -> shifts.put(networkVariation.getNetworkElementId(), networkVariation.apply(networks)));
        return shifts;
    }

    public void applyScenario(String scenarioId, Network network, OffsetDateTime timestamp) {
        scenarios.get(scenarioId).forEach(networkVariation -> networkVariation.apply(network, timestamp));
    }

    public Set<String> getInjectionIds() {
        // TODO make this cleaner, more generic. Also base on scenarios, because networkVariations can contain unused values
        return networkVariations.values().stream().map(NetworkVariation::getNetworkElementId).collect(Collectors.toSet());
    }

    public Map<String, TemporalData<Double>> computeShifts(String scenarioId, TemporalData<Network> networks) {
        Map<String, TemporalData<Double>> shifts = new HashMap<>();
        scenarios.get(scenarioId).forEach(networkVariation -> shifts.put(networkVariation.getNetworkElementId(), networkVariation.computeShifts(networks)));
        return shifts;
    }

    public int getNumberOfScenarios() {
        return scenarios.size();
    }
}
