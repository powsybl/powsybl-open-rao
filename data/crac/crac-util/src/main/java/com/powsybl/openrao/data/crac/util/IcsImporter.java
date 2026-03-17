/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.util;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.LoadType;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.IcsData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.timecoupledconstraints.GeneratorConstraints;
import com.powsybl.openrao.raoapi.RaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInputWithNetworkPaths;
import org.apache.commons.csv.CSVRecord;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class IcsImporter {
    private static final int OFFSET = 2;
    private static final double MAX_GRADIENT = 1000.0;
    private static final double ON_POWER_THRESHOLD = 1.001; // TODO: mutualize with value from linear problem

    // TODO : either parametrize this or set it to true. May have to change the way it works to import for all curative instants instead of only the last one
    public static boolean importCurative = false;
    private static double costUp;
    private static double costDown;

    // Quality checks (or don't import the action at all):
    // TODO in future versions:
    //  - check other implemented constraints
    //  - check consistency between ics files, particularly w.r.t gsk file

    // INFOS
    // Gradient constraints are defined for gsks at the action level and not per group : we translate it to the groups using the shift keys

    public static final String P_MIN_RD = "Pmin_RD";
    public static final String RA_RD_ID = "RA RD ID";
    public static final String RDP_UP = "RDP+";
    public static final String RDP_DOWN = "RDP-";
    public static final String P0 = "P0";
    public static final String UCT_NODE_OR_GSK_ID = "UCT Node or GSK ID";
    public static final String PREVENTIVE = "Preventive";
    public static final String CURATIVE = "Curative";
    public static final String TRUE = "TRUE";
    public static final String FALSE = "FALSE";
    public static final String RD_DESCRIPTION_MODE = "RD description mode";
    public static final String NODE = "NODE";
    public static final String GENERATOR_NAME = "Generator Name";
    public static final String RD_SUFFIX = "_RD";
    public static final String GENERATOR_SUFFIX = "_GENERATOR";

    private IcsImporter() {
        //should only be used statically
    }

    public static void populateInputWithICS(TimeCoupledRaoInputWithNetworkPaths timeCoupledRaoInput,
                                            IcsData icsData,
                                            double icsCostUp,
                                            double icsCostDown) {
        costUp = icsCostUp;
        costDown = icsCostDown;

        TemporalData<Network> modifiedInitialNetworks = new TemporalDataImpl<>();
        timeCoupledRaoInput.getRaoInputs().getDataPerTimestamp().forEach((dateTime, raoInput) -> {
            Network network = Network.read(raoInput.getInitialNetworkPath());
            updateNominalVoltage(network);
            modifiedInitialNetworks.put(dateTime, network);
        });

        // Create redispatching actions in crac, generator in network + create generator constraint

        // For each redispatching action defined in static csv
        icsData.getStaticConstraintPerId().forEach((raId, staticRecord) -> {
            Map<String, CSVRecord> seriesPerType = icsData.getTimeseriesPerIdAndType().get(raId);
            // If the remedial action is defined on a Node.
            if (staticRecord.get(RD_DESCRIPTION_MODE).equalsIgnoreCase(NODE)) {
                importNodeRedispatchingAction(timeCoupledRaoInput, staticRecord, modifiedInitialNetworks, seriesPerType, raId);
            } else { // If the remedial action is defined on a GSK
                Map<String, Double> weightPerNode = icsData.getWeightPerNodePerGsk().get(staticRecord.get("UCT Node or GSK ID"));
                importGskRedispatchingAction(timeCoupledRaoInput, staticRecord, modifiedInitialNetworks, seriesPerType, raId, weightPerNode);
            }
        });

        // Save update networks (with corrected nominal voltage + newly created generator/load)
        modifiedInitialNetworks.getDataPerTimestamp().forEach((dateTime, initialNetwork) ->
            initialNetwork.write("JIIDM", new Properties(), Path.of(timeCoupledRaoInput.getRaoInputs().getData(dateTime).orElseThrow().getPostIcsImportNetworkPath()))
        );

    }


    // By default, UCTE sets nominal voltage to 220 and 380kV for the voltage levels 6 and 7,
    // whereas default values of Core countries are 225 and 400 kV instead.
    // The preprocessor updates the nominal voltage levels to these values.
    private static void updateNominalVoltage(Network network) {
        network.getVoltageLevelStream().forEach(voltageLevel -> {
            if (safeDoubleEquals(voltageLevel.getNominalV(), 380)) {
                voltageLevel.setNominalV(400);
            } else if (safeDoubleEquals(voltageLevel.getNominalV(), 220)) {
                voltageLevel.setNominalV(225);
            }
            // Else, Should not be changed cause is not equal to the default nominal voltage of voltage levels 6 or 7
        });
    }

    private static boolean safeDoubleEquals(double a, double b) {
        return Math.abs(a - b) < 1e-3;
    }

    private static void importGskRedispatchingAction(TimeCoupledRaoInputWithNetworkPaths timeCoupledRaoInput,
                                                     CSVRecord staticRecord,
                                                     TemporalData<Network> initialNetworks,
                                                     Map<String, CSVRecord> seriesPerType,
                                                     String raId,
                                                     Map<String, Double> weightPerNode) {

        // Create generator
        Map<String, String> networkElementPerGskElement = new HashMap<>();
        for (String nodeId : weightPerNode.keySet()) {
            String networkElementId = createGeneratorAndLoadInNetworks(nodeId, initialNetworks, seriesPerType, weightPerNode.get(nodeId));
            if (networkElementId == null) {
                return;
            }
            networkElementPerGskElement.put(nodeId, networkElementId);
        }

        // Create redispatching actions in crac
        timeCoupledRaoInput.getRaoInputs().getDataPerTimestamp().forEach((dateTime, raoInput) -> {
            importGskRedispatchActionForOneTimestamp(staticRecord, seriesPerType, raId, weightPerNode, dateTime, raoInput, networkElementPerGskElement);
        });
    }


    // TODO: put in common ? with importNodeRedispatchingAction
    private static void importGskRedispatchActionForOneTimestamp(CSVRecord staticRecord,
                                  Map<String, CSVRecord> seriesPerType,
                                  String raId,
                                  Map<String, Double> weightPerNode,
                                  OffsetDateTime dateTime,
                                  RaoInputWithNetworkPaths raoInput,
                                  Map<String, String> networkElementPerGskElement) {
        Crac crac = raoInput.getCrac();
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
            injectionRangeActionAdder.withNetworkElementAndKey(shiftKey, networkElementPerGskElement.get(nodeId));
        });

        if (staticRecord.get(PREVENTIVE).equalsIgnoreCase(TRUE)) {
            injectionRangeActionAdder.newOnInstantUsageRule()
                .withInstant(crac.getPreventiveInstant().getId())
                .add();
        }
        if (importCurative && staticRecord.get(CURATIVE).equalsIgnoreCase(TRUE)) {
            injectionRangeActionAdder.newOnInstantUsageRule()
                .withInstant(crac.getLastInstant().getId())
                .add();
        }

        injectionRangeActionAdder.add();
    }

    private static void importNodeRedispatchingAction(TimeCoupledRaoInputWithNetworkPaths timeCoupledRaoInput,
                                                      CSVRecord staticRecord,
                                                      TemporalData<Network> initialNetworks,
                                                      Map<String, CSVRecord> seriesPerType,
                                                      String raId) {

        // Create generator
        String networkElementId = createGeneratorAndLoadInNetworks(staticRecord.get(UCT_NODE_OR_GSK_ID), initialNetworks, seriesPerType, 1.);
        if (networkElementId == null) {
            return;
        }

        // Create injection range action in crac
        timeCoupledRaoInput.getRaoInputs().getDataPerTimestamp().forEach((dateTime, raoInput) -> {

            importGskRedispatchActionForOneTimestamp(staticRecord, seriesPerType, raId, Map.of(networkElementId, 1.0), dateTime, raoInput, Map.of(networkElementId, networkElementId));
        });

    }


    // modify bus, create generator
    private static String createGeneratorAndLoadInNetworks(String nodeId, TemporalData<Network> initialNetworks, Map<String, CSVRecord> seriesPerType, double shiftKey) {
        String generatorId = seriesPerType.get(P0).get(RA_RD_ID) + "_" + nodeId + GENERATOR_SUFFIX;
        for (Map.Entry<OffsetDateTime, Network> entry : initialNetworks.getDataPerTimestamp().entrySet()) {
            Bus bus = findBus(nodeId, entry.getValue());
            if (bus == null) {
                BUSINESS_WARNS.warn("Redispatching action {} cannot be imported because bus {} could not be found", seriesPerType.get(P0).get("RA RD ID"), nodeId);
                return null;
            }
            Double p0 = parseDoubleWithPossibleCommas(seriesPerType.get(P0).get(entry.getKey().getHour() + OFFSET)) * shiftKey;
            Optional<Double> pMinRd = parseValue(seriesPerType, P_MIN_RD, entry.getKey(), shiftKey);
            processBus(bus, generatorId, p0, pMinRd.orElse(ON_POWER_THRESHOLD));
        }
        return generatorId;
    }

    private static Optional<Double> parseValue(Map<String, CSVRecord> seriesPerType, String key, OffsetDateTime timestamp, double shiftKey) {
        if (seriesPerType.containsKey(key)) {
            CSVRecord series = seriesPerType.get(key);
            String value = series.get(timestamp.getHour() + OFFSET);
            if (value != null) {
                return Optional.of(parseDoubleWithPossibleCommas(value) * shiftKey);
            }
        }
        return Optional.empty();
    }

    // TODO: make this more robust (and less UCTE dependent)
    private static Bus findBus(String nodeId, Network network) {
        // First try to get the bus in bus breaker view
        Bus bus = network.getBusBreakerView().getBus(nodeId);
        if (bus != null) {
            return bus;
        }

        // Then, if last char is *, remove it
        String modifiedNodeId = nodeId;
        if (nodeId.endsWith("*")) {
            modifiedNodeId = nodeId.substring(0, nodeId.length() - 1);
        }
        // Try to find the bus using bus view
        return network.getBusBreakerView().getBus(modifiedNodeId + " ");
    }

    private static void processBus(Bus bus, String generatorId, Double p0, double pMinRd) {
        bus.getVoltageLevel().newGenerator()
            .setBus(bus.getId())
            .setEnsureIdUnicity(true)
            .setId(generatorId)
            .setMaxP(999999)
            .setMinP(pMinRd)
            .setTargetP(p0)
            .setTargetQ(0)
            .setTargetV(bus.getVoltageLevel().getNominalV())
            .setVoltageRegulatorOn(false)
            .add()
            .setFictitious(true);

        bus.getVoltageLevel().newLoad()
            .setBus(bus.getId())
            .setEnsureIdUnicity(true)
            .setId(bus.getId() + "_LOAD")
            .setP0(p0)
            .setQ0(0)
            .setLoadType(LoadType.FICTITIOUS)
            .add();
    }


    private static double parseDoubleWithPossibleCommas(String string) {
        return Double.parseDouble(string.replaceAll(",", "."));
    }
}
