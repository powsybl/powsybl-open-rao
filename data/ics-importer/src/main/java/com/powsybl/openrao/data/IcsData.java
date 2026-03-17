/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.timecoupledconstraints.GeneratorConstraints;
import org.apache.commons.csv.CSVRecord;

import java.util.*;


/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class IcsData {

    // TODO: put value in common

    private static final double MAX_GRADIENT = 1000.0;

    public static final String MAXIMUM_POSITIVE_POWER_GRADIENT = "Maximum positive power gradient [MW/h]";
    public static final String MAXIMUM_NEGATIVE_POWER_GRADIENT = "Maximum negative power gradient [MW/h]";

    public static final String P0 = "P0";
    public static final String UCT_NODE_OR_GSK_ID = "UCT Node or GSK ID";
    public static final String PREVENTIVE = "Preventive";
    public static final String TRUE = "TRUE";
    public static final String RD_DESCRIPTION_MODE = "RD description mode";
    public static final String NODE = "NODE";
    public static final String LEAD_TIME = "Lead time [h]";
    public static final String LAG_TIME = "Lag time [h]";
    public static final String STARTUP_ALLOWED = "Startup allowed";
    public static final String SHUTDOWN_ALLOWED = "Shutdown allowed";

    public static final String GENERATOR_SUFFIX = "_GENERATOR";


    private static Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType;
    private static Map<String, Map<String, Double>> weightPerNodePerGsk;
    private Map<String, CSVRecord> staticConstraintPerId;

    public IcsData(Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType,
                   Map<String, Map<String, Double>> weightPerNodePerGsk,
                   Map<String, CSVRecord> staticConstraintPerId) {
        this.staticConstraintPerId = staticConstraintPerId;
        this.timeseriesPerIdAndType = timeseriesPerIdAndType;
        this.weightPerNodePerGsk = weightPerNodePerGsk;
    }

        // Define getters

    public Map<String, CSVRecord> getStaticConstraintPerId() {
        return staticConstraintPerId;
    }

    public Map<String, Map<String, CSVRecord>> getTimeseriesPerIdAndType() {
        return timeseriesPerIdAndType;
    }

    public Map<String, Map<String, Double>> getWeightPerNodePerGsk() {
        return weightPerNodePerGsk;
    }

    public String getGeneratorIdFromRaIdAndNodeId(String raId, String nodeId) {
        return raId + "_" + nodeId + GENERATOR_SUFFIX;
    }

    public Set<GeneratorConstraints> getGeneratorConstraints() {
        Set<GeneratorConstraints> generatorConstraintsSet = new HashSet<>();
        staticConstraintPerId.forEach((raId, staticRecord) -> {
            // If the remedial action is defined on a Node.
            if (staticRecord.get(RD_DESCRIPTION_MODE).equalsIgnoreCase(NODE)) {
                // create a generator constraint from staticRecord
                String networkElementId = getGeneratorIdFromRaIdAndNodeId(raId, staticRecord.get(UCT_NODE_OR_GSK_ID));
                GeneratorConstraints generatorConstraints = createGeneratorConstraintFromStaticRecord(networkElementId, staticRecord, 1.0);
                generatorConstraintsSet.add(generatorConstraints);
            } else { // If the remedial action is defined on a GSK
                // For a given GSK, create a generator constraint for each node of the GSK according to the shiftKey
                Map<String, Double> weightPerNode = weightPerNodePerGsk.get(staticRecord.get(UCT_NODE_OR_GSK_ID));
                for (Map.Entry<String, Double> entry : weightPerNode.entrySet()) {
                    String nodeId = entry.getKey();
                    Double shiftKey = entry.getValue();
                    String networkElementId = getGeneratorIdFromRaIdAndNodeId(raId, nodeId);
                    GeneratorConstraints generatorConstraints = createGeneratorConstraintFromStaticRecord(networkElementId, staticRecord, shiftKey);
                    generatorConstraintsSet.add(generatorConstraints);
                }
            }
        });

        return generatorConstraintsSet;
    }


    private static GeneratorConstraints createGeneratorConstraintFromStaticRecord(String networkElementId,
                                                                                  CSVRecord staticRecord,
                                                                                  Double shiftKey) {

        GeneratorConstraints.GeneratorConstraintsBuilder builder = GeneratorConstraints.create().withGeneratorId(networkElementId);
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
        if (staticRecord.get(SHUTDOWN_ALLOWED).isEmpty() ||
            !staticRecord.get(SHUTDOWN_ALLOWED).equalsIgnoreCase(TRUE) && !staticRecord.get(SHUTDOWN_ALLOWED).equalsIgnoreCase(FALSE)) {
            throw new OpenRaoException("Could not parse shutDownAllowed value " + staticRecord.get(SHUTDOWN_ALLOWED) + " for nodeId " + nodeId);
        } else {
            builder.withShutDownAllowed(Boolean.parseBoolean(staticRecord.get(SHUTDOWN_ALLOWED)));
        }
        if (staticRecord.get(STARTUP_ALLOWED).isEmpty() ||
            !staticRecord.get(STARTUP_ALLOWED).equalsIgnoreCase(TRUE) && !staticRecord.get(STARTUP_ALLOWED).equalsIgnoreCase(FALSE)) {
            throw new OpenRaoException("Could not parse startUpAllowed value " + staticRecord.get(STARTUP_ALLOWED) + " for nodeId " + nodeId);
        } else {
            builder.withStartUpAllowed(Boolean.parseBoolean(staticRecord.get(STARTUP_ALLOWED)));
        }
        return builder.build();
    }

    private static double parseDoubleWithPossibleCommas(String string) {
        return Double.parseDouble(string.replaceAll(",", "."));
    }

}