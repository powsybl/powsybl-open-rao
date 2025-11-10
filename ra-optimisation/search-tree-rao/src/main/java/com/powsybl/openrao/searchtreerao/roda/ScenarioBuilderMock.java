/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda;

import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.searchtreerao.roda.scenariorepository.GeneratorTargetPNetworkVariation;
import com.powsybl.openrao.searchtreerao.roda.scenariorepository.NetworkVariation;
import com.powsybl.openrao.searchtreerao.roda.scenariorepository.ScenarioRepository;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock class to create random simulation scenarios
 * TODO: replace this with new input files
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class ScenarioBuilderMock {
    private ScenarioBuilderMock() {
        // should not be used
    }

    public static ScenarioRepository createScenarios(TemporalData<Network> networks) {
        Map<String, List<NetworkVariation>> networkVariations = new HashMap<>();
        Set<String> networkGenerators = networks.getData(networks.getTimestamps().get(0)).orElseThrow().getGeneratorStream().map(Identifiable::getId).filter(id -> !id.contains("_RA_")).collect(Collectors.toSet());
        for (String genId : networkGenerators) {
            networkVariations.put(genId, createNetworkVariationsForGenerator(genId, networks));
        }
        ScenarioRepository scenarioRepo = new ScenarioRepository(networkVariations.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
        int nScenarios = 10;
        Random rand = new Random();
        // Add reference scenario
        scenarioRepo.addScenario("REFERENCE", Set.of());
        while (scenarioRepo.getNumberOfScenarios() < nScenarios) {
            Set<String> scenarioVariations = new HashSet<>();
            for (String genId : networkGenerators) {
                NetworkVariation randomVariation = networkVariations.get(genId).get(rand.nextInt(networkVariations.get(genId).size()));
                scenarioVariations.add(randomVariation.getId());
            }
            String scenarioId = "scn_" + scenarioRepo.getNumberOfScenarios();
            scenarioRepo.addScenario(scenarioId, scenarioVariations);
        }

        return scenarioRepo;
    }

    private static List<NetworkVariation> createNetworkVariationsForGenerator(String genId, TemporalData<Network> networks) {
        List<NetworkVariation> variations = new ArrayList<>();
        // Reference
        TemporalData<Double> valuesRef = new TemporalDataImpl<>();
        for (OffsetDateTime ts : networks.getTimestamps()) {
            valuesRef.put(ts, networks.getData(ts).orElseThrow().getGenerator(genId).getTargetP());
        }
        variations.add(new GeneratorTargetPNetworkVariation(genId + "_ref", genId, valuesRef));
        // MinP
        TemporalData<Double> valuesMin = new TemporalDataImpl<>();
        for (OffsetDateTime ts : networks.getTimestamps()) {
            valuesMin.put(ts, networks.getData(ts).orElseThrow().getGenerator(genId).getMinP());
        }
        variations.add(new GeneratorTargetPNetworkVariation(genId + "_min_P", genId, valuesMin));
        // MaxP
        TemporalData<Double> valuesMax = new TemporalDataImpl<>();
        for (OffsetDateTime ts : networks.getTimestamps()) {
            valuesMax.put(ts, networks.getData(ts).orElseThrow().getGenerator(genId).getMaxP());
        }
        variations.add(new GeneratorTargetPNetworkVariation(genId + "_max_P", genId, valuesMax));

        return variations;
    }
}
