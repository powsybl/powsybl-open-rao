/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.LoadType;
import com.powsybl.iidm.network.Network;
import org.apache.commons.csv.CSVRecord;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class IcsUtil {

    private IcsUtil() {

    }

    // TODO: move to a configuration file ?
    static final double MAX_GRADIENT = 1000.0;

    public static final String MAXIMUM_POSITIVE_POWER_GRADIENT = "Maximum positive power gradient [MW/h]";
    public static final String MAXIMUM_NEGATIVE_POWER_GRADIENT = "Maximum negative power gradient [MW/h]";
    public static final String RA_RD_ID = "RA RD ID";
    public static final String RDP_UP = "RDP+";
    public static final String RDP_DOWN = "RDP-";
    public static final String P_MIN_RD = "Pmin_RD";
    public static final String P0 = "P0";
    public static final String UCT_NODE_OR_GSK_ID = "UCT Node or GSK ID";
    public static final String PREVENTIVE = "Preventive";
    public static final String TRUE = "TRUE";
    public static final String RD_DESCRIPTION_MODE = "RD description mode";
    public static final String NODE = "NODE";
    public static final String GSK = "GSK";
    public static final String LEAD_TIME = "Lead time [h]";
    public static final String LAG_TIME = "Lag time [h]";
    public static final String STARTUP_ALLOWED = "Startup allowed";
    public static final String SHUTDOWN_ALLOWED = "Shutdown allowed";
    public static final String GSK_ID = "GSK ID";

    public static final String GENERATOR_SUFFIX = "_GENERATOR";
    public static final String LOAD_SUFFIX = "_LOAD";
    public static final int OFFSET = 2;
    public static final double ON_POWER_THRESHOLD = 1.001; // TODO: mutualize with value from linear problem

    public static final String CURATIVE = "Curative";
    public static final String FALSE = "FALSE";
    public static final String GENERATOR_NAME = "Generator Name";
    public static final String RD_SUFFIX = "_RD";

    static Optional<Double> parseValue(Map<String, CSVRecord> seriesPerType, String key, OffsetDateTime timestamp, double shiftKey) {
        if (seriesPerType.containsKey(key)) {
            CSVRecord series = seriesPerType.get(key);
            String value = series.get(timestamp.getHour() + OFFSET);
            if (value != null) {
                return Optional.of(parseDoubleWithPossibleCommas(value) * shiftKey);
            }
        }
        return Optional.empty();
    }

    static double parseDoubleWithPossibleCommas(String string) {
        return Double.parseDouble(string.replaceAll(",", "."));
    }

    // TODO: make this more robust (and less UCTE dependent)
    static Bus findBus(String nodeId, Network network) {
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

    static void processBus(Bus bus, String generatorId, Double p0, double pMinRd) {
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
            .setId(bus.getId() + LOAD_SUFFIX)
            .setP0(p0)
            .setQ0(0)
            .setLoadType(LoadType.FICTITIOUS)
            .add();
    }

    // TODO: Move elsewhere
    // By default, UCTE sets nominal voltage to 220 and 380kV for the voltage levels 6 and 7,
    // whereas default values of Core countries are 225 and 400 kV instead.
    // The preprocessor updates the nominal voltage levels to these values.
    public static void updateNominalVoltage(Network network) {
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
}
