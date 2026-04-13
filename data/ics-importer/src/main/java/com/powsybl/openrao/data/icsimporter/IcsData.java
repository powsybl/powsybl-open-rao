/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.icsimporter;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.timecoupledconstraints.GeneratorConstraints;
import com.powsybl.openrao.raoapi.LazyNetwork;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import org.apache.commons.csv.CSVRecord;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class IcsData {

    private static Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType;
    private static Map<String, Map<String, Double>> weightPerNodePerGsk;
    private static Map<String, CSVRecord> staticConstraintPerId;
    private static Set<String> redispatchingActions;

    // TODO : either parametrize this or set it to true. May have to change the way it works to import for all curative instants instead of only the last one
    public static boolean importCurative = false;

    public IcsData(Set<String> consistentRedispatchingActions,
                   Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType,
                   Map<String, Map<String, Double>> weightPerNodePerGsk,
                   Map<String, CSVRecord> staticConstraintPerId) {
        this.redispatchingActions = consistentRedispatchingActions;
        this.staticConstraintPerId = staticConstraintPerId;
        this.timeseriesPerIdAndType = timeseriesPerIdAndType;
        this.weightPerNodePerGsk = weightPerNodePerGsk;
    }

    public Map<String, CSVRecord> getStaticConstraintPerId() {
        return staticConstraintPerId;
    }

    public static Map<String, Map<String, CSVRecord>> getTimeseriesPerIdAndType() {
        return timeseriesPerIdAndType;
    }

    public Map<String, Map<String, Double>> getWeightPerNodePerGsk() {
        return weightPerNodePerGsk;
    }

    public Set<String> getRedispatchingActions() {
        return redispatchingActions;
    }

    public static String getGeneratorIdFromRaIdAndNodeId(String raId, String nodeId) {
        return raId + "_" + nodeId + GENERATOR_SUFFIX;
    }

    public static boolean isRaDefinedOnANode(String raId) {
        return staticConstraintPerId.get(raId).get(RD_DESCRIPTION_MODE).equalsIgnoreCase(NODE);
    }

    public static String getNodeIdOrGskIdFromRaId(String raId) {
        return staticConstraintPerId.get(raId).get(UCT_NODE_OR_GSK_ID);
    }

    public static Map<String, Double> getWeightPerNode(String raId) {
        if (isRaDefinedOnANode(raId)) {
            return Map.of(getNodeIdOrGskIdFromRaId(raId), 1.0);
        } else {
            return weightPerNodePerGsk.get(getNodeIdOrGskIdFromRaId(raId));
        }
    }

    public static Map<String, String> getDefaultGeneratorIdPerNode(String raId) {
        Map<String, String> defaultGeneratorIdPerNode = new HashMap<>();
        Map<String, Double> weightPerNode = getWeightPerNode(raId);
        for (Map.Entry<String, Double> entry : weightPerNode.entrySet()) {
            defaultGeneratorIdPerNode.put(entry.getKey(), getGeneratorIdFromRaIdAndNodeId(raId, entry.getKey()));
        }
        return defaultGeneratorIdPerNode;
    }

    /**
     * Generates a set of generator constraints based on the provided remedial action ID.
     *
     * @param raId The identifier of the remedial action for which the generator constraints are being created.
     * @param networkElementIdPerNodeId A map linking nodeId to their respective network elements id.
     * @return A set of {@code GeneratorConstraints} generated for the specified parameters.
     * @throws OpenRaoException if data related to shutdown or startup allowances cannot be parsed.
     */
    public static Set<GeneratorConstraints> createGeneratorConstraints(String raId, Map<String, String> networkElementIdPerNodeId) {
        Set<GeneratorConstraints> generatorConstraintsSet = new HashSet<>();
        Map<String, Double> weightPerNode = getWeightPerNode(raId);
        for (Map.Entry<String, Double> entry : weightPerNode.entrySet()) {
            String nodeId = entry.getKey();
            Double shiftKey = entry.getValue();
            CSVRecord staticRecord = staticConstraintPerId.get(raId);
            GeneratorConstraints.GeneratorConstraintsBuilder builder = GeneratorConstraints.create().withGeneratorId(networkElementIdPerNodeId.get(nodeId));

            // Shutdown allowed and startup allowed are mandatory fields
            builder.withShutDownAllowed(Boolean.parseBoolean(staticRecord.get(SHUTDOWN_ALLOWED)));
            builder.withStartUpAllowed(Boolean.parseBoolean(staticRecord.get(STARTUP_ALLOWED)));

            if (!staticRecord.get(MAXIMUM_POSITIVE_POWER_GRADIENT).isEmpty()) {
                builder.withUpwardPowerGradient(shiftKey * parseDoubleWithPossibleCommas(staticRecord.get(MAXIMUM_POSITIVE_POWER_GRADIENT)));
            } else {
                builder.withUpwardPowerGradient(shiftKey * MAX_GRADIENT);
            }
            if (!staticRecord.get(MAXIMUM_NEGATIVE_POWER_GRADIENT).isEmpty()) {
                builder.withDownwardPowerGradient(-shiftKey * parseDoubleWithPossibleCommas(staticRecord.get(MAXIMUM_NEGATIVE_POWER_GRADIENT)));
            } else {
                builder.withDownwardPowerGradient(-shiftKey * MAX_GRADIENT);
            }
            if (!staticRecord.get(LEAD_TIME).isEmpty()) {
                builder.withLeadTime(parseDoubleWithPossibleCommas(staticRecord.get(LEAD_TIME)));
            }
            if (!staticRecord.get(LAG_TIME).isEmpty()) {
                builder.withLagTime(parseDoubleWithPossibleCommas(staticRecord.get(LAG_TIME)));
            }

            GeneratorConstraints generatorConstraints = builder.build();
            generatorConstraintsSet.add(generatorConstraints);
        }
        return generatorConstraintsSet;
    }

    /**
     * Creates generator/load elements and updates provided networks given the remedial action's ID and its shift key mapping
     *
     * @param initialNetworksToModify Temporal data representing the networks that will be modified.
     *                                Contains network configurations per timestamp.
     * @param raId The identifier of the remedial action for which generators and network modifications are being applied.
     * @return A map associating each node identifier to its corresponding generator identifier.
     *         Returns an empty map if the process is aborted due to missing network components.
     */
    public static Map<String, String> createGeneratorAndLoadAndUpdateNetworks(TemporalData<LazyNetwork> initialNetworksToModify,
                                                                              String raId) {

        Map<String, String> networkElementPerGskElement = new HashMap<>();
        Map<String, CSVRecord> seriesPerType = timeseriesPerIdAndType.get(raId);
        Map<String, Double> weightPerNode = getWeightPerNode(raId);

        for (Map.Entry<String, Double> entry : weightPerNode.entrySet()) {

            String nodeId = entry.getKey();
            Double shiftKey = entry.getValue();
            String generatorId = getGeneratorIdFromRaIdAndNodeId(raId, nodeId);

            for (Map.Entry<OffsetDateTime, LazyNetwork> networkEntry : initialNetworksToModify.getDataPerTimestamp().entrySet()) {
                OffsetDateTime dateTime = networkEntry.getKey();
                Network network = networkEntry.getValue();

                Bus bus = findBus(nodeId, network);
                if (bus == null) {
                    BUSINESS_WARNS.warn("Redispatching action {} cannot be imported because bus {} could not be found", raId, nodeId);
                    return Map.of();
                }

                int index = dateTime.getHour() + OFFSET;
                // p0 values checked during IcsData import
                Double p0 = parseDoubleWithPossibleCommas(seriesPerType.get(P0).get(index)) * shiftKey;
                // pMin can be undefined
                Optional<Double> pMinRd = IcsUtil.parseValue(seriesPerType, P_MIN_RD, dateTime, shiftKey);
                processBus(bus, generatorId, p0, pMinRd.orElse(ON_POWER_THRESHOLD));
            }

            networkElementPerGskElement.put(nodeId, generatorId);
        }

        return networkElementPerGskElement;
    }

    /**
     * Creates injection range actions and updates CRACs for all timestamps.
     *
     * @param cracToModify Temporal data containing CRACs to be modified and timestamps to consider.
     * @param raId The identifier of the remedial action for which injection range actions are created.
     * @param networkElementPerNode A map linking each node identifier to its corresponding network element/generator id.
     * @param costUp The cost associated with increasing the generation (VariationDirection.UP).
     * @param costDown The cost associated with decreasing the generation (VariationDirection.DOWN).
     */
    public static void createInjectionRangeActionsAndUpdateCracs(TemporalData<Crac> cracToModify,
                                                                 String raId,
                                                                 Map<String, String> networkElementPerNode,
                                                                 double costUp,
                                                                 double costDown) {

        CSVRecord staticRecord = staticConstraintPerId.get(raId);
        Map<String, CSVRecord> seriesPerType = timeseriesPerIdAndType.get(raId);
        Map<String, Double> weightPerNode = getWeightPerNode(raId);

        cracToModify.getDataPerTimestamp().forEach((dateTime, crac) -> {
            double p0 = parseDoubleWithPossibleCommas(seriesPerType.get(P0).get(dateTime.getHour() + OFFSET));
            InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
                .withId(raId + RD_SUFFIX)
                .withName(staticRecord.get(GENERATOR_NAME))
                .withInitialSetpoint(p0)
                .withVariationCost(costUp, VariationDirection.UP)
                .withVariationCost(costDown, VariationDirection.DOWN)
                .newRange()
                .withMin(p0 - parseDoubleWithPossibleCommas(seriesPerType.get(RDP_DOWN).get(dateTime.getHour() + OFFSET)))
                .withMax(p0 + parseDoubleWithPossibleCommas(seriesPerType.get(RDP_UP).get(dateTime.getHour() + OFFSET)))
                .add();

            weightPerNode.forEach((nodeId, shiftKey) -> {
                injectionRangeActionAdder.withNetworkElementAndKey(shiftKey, networkElementPerNode.get(nodeId));
            });

            injectionRangeActionAdder.newOnInstantUsageRule()
                .withInstant(crac.getPreventiveInstant().getId())
                .add();

            if (importCurative && staticRecord.get(CURATIVE).equalsIgnoreCase(TRUE)) {
                injectionRangeActionAdder.newOnInstantUsageRule()
                    .withInstant(crac.getLastInstant().getId())
                    .add();
            }

            injectionRangeActionAdder.add();
        });
    }

    /**
     * Processes all redispatching actions for the provided time-coupled input, updates networks
     * and CRACs, and generates the required constraints for the specified costs.
     *
     * @param timeCoupledRaoInput The input data containing network and CRAC information for
     *                            each timestamp. Includes all RAO-specific input required for
     *                            redispatching action processing.
     * @param costUp The cost associated with increasing the generation (VariationDirection.UP).
     * @param costDown The cost associated with decreasing the generation (VariationDirection.DOWN).
     * @param exportDirectory The directory where the exported networks will be saved.
     * @return The updated time-coupled RAO input with processed redispatching actions and constraints.
     */
    public TimeCoupledRaoInput processAllRedispatchingActions(TimeCoupledRaoInput timeCoupledRaoInput,
                                                              double costUp,
                                                              double costDown,
                                                              String exportDirectory) {

        // Update nominal voltage in network
        // TODO: More of a IDCC focused special processing ? Move elsewhere ?
        TemporalData<LazyNetwork> modifiedInitialNetworks = new TemporalDataImpl<>();
        timeCoupledRaoInput.getRaoInputs().getDataPerTimestamp().forEach((dateTime, raoInput) -> {
            Network network = raoInput.getNetwork();
            updateNominalVoltage(network);
            modifiedInitialNetworks.put(dateTime, new LazyNetwork(network));
            if (network instanceof LazyNetwork lazyNetwork) {
                try {
                    lazyNetwork.close();
                } catch (Exception e) {
                    throw new OpenRaoException(e);
                }
            }
        });

        TemporalData<Crac> cracToModify = new TemporalDataImpl<>();
        timeCoupledRaoInput.getRaoInputs().getDataPerTimestamp().forEach((dateTime, raoInput) -> {
            cracToModify.put(dateTime, raoInput.getCrac());
        });

        // For each redispatching actions defined in static csv update networks and update cracs
        redispatchingActions.forEach(raId -> {

            // Create generator and load in networks
            Map<String, String> generatorIdPerNode = createGeneratorAndLoadAndUpdateNetworks(modifiedInitialNetworks, raId);
            // One of the node could not be find no need to create injection range actions and generator constraint.
            if (generatorIdPerNode.isEmpty()) {
                return;
            }

            // Create Injection Range Actions in CRACs
            createInjectionRangeActionsAndUpdateCracs(cracToModify, raId, generatorIdPerNode, costUp, costDown);

            // Create generator constraints and them to time coupled rao input
            Set<GeneratorConstraints> generatorConstraintsSet = createGeneratorConstraints(raId, generatorIdPerNode);
            generatorConstraintsSet.forEach(generatorConstraints -> timeCoupledRaoInput.getTimeCoupledConstraints().addGeneratorConstraints(generatorConstraints));
        });

        TemporalData<RaoInput> postIcsRaoInputs = new TemporalDataImpl<>();

        modifiedInitialNetworks.getDataPerTimestamp().forEach((dateTime, initialNetwork) -> {
            String exportedNetworkPath = exportDirectory + dateTime.format(DateTimeFormatter.ofPattern("%y%m%d_%H%M%S")) + ".jiidm";
            initialNetwork.write("JIIDM", new Properties(), Path.of(exportedNetworkPath));
            try (LazyNetwork postIcsNetwork = new LazyNetwork(exportedNetworkPath)) {
                postIcsRaoInputs.put(dateTime, RaoInput.build(postIcsNetwork, timeCoupledRaoInput.getRaoInputs().getData(dateTime).orElseThrow().getCrac()).build());
            } catch (Exception e) {
                throw new OpenRaoException(e);
            }
            try {
                initialNetwork.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return new TimeCoupledRaoInput(postIcsRaoInputs, timeCoupledRaoInput.getTimestampsToRun(), timeCoupledRaoInput.getTimeCoupledConstraints());
    }

}
