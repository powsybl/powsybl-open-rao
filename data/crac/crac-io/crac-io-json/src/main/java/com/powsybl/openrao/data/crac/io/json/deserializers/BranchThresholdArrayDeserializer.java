/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.api.threshold.BranchThresholdAdder;
import com.powsybl.openrao.data.crac.io.commons.CnecElementHelper;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class BranchThresholdArrayDeserializer {

    private BranchThresholdArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, FlowCnecAdder ownerAdder, CnecElementHelper cnecElementHelper, String version) throws IOException {
        //TODO: Refactor this method
        boolean iMaxFetched = false;
        Pair<Double, Double> nominalV = readAndAddNominalV(cnecElementHelper, ownerAdder);

        // Store already deserialized thresholds to validate duplicate bounds
        Map<TwoSides, Set<String>> thresholdSideBounds = new HashMap<>();

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            BranchThresholdAdder branchThresholdAdder = ownerAdder.newThreshold();

            TwoSides side = null; // Stores the side of the current threshold
            boolean hasMin = false; // Track if 'min' is set
            boolean hasMax = false; // Track if 'max' is set

            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case JsonSerializationConstants.UNIT:
                        Unit unit = JsonSerializationConstants.deserializeUnit(jsonParser.nextTextValue());
                        branchThresholdAdder.withUnit(unit);
                        if (Unit.PERCENT_IMAX.equals(unit)) {
                            if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 2 || JsonSerializationConstants.getSubVersionNumber(version) > 12) {
                                throw new OpenRaoException("PERCENT_IMAX thresholds are no longer supported in CRAC JSON format since CRAC version 2.13");
                            }

                            // FlowCNEC's iMax is required only if it has a threshold with a %IMax unit
                            if (!iMaxFetched) {
                                addIMax(cnecElementHelper, ownerAdder);
                                iMaxFetched = true;
                            }
                        }
                        break;
                    case JsonSerializationConstants.MIN:
                        jsonParser.nextToken();
                        branchThresholdAdder.withMin(jsonParser.getDoubleValue());
                        hasMin = true;
                        break;
                    case JsonSerializationConstants.MAX:
                        jsonParser.nextToken();
                        branchThresholdAdder.withMax(jsonParser.getDoubleValue());
                        hasMax = true;
                        break;
                    case JsonSerializationConstants.RULE:
                        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 1 || JsonSerializationConstants.getSubVersionNumber(version) > 5) {
                            throw new OpenRaoException("Branch threshold rule is not handled since CRAC version 1.6");
                        } else {
                            side = JsonSerializationConstants.convertBranchThresholdRuleToSide(jsonParser.nextTextValue(), nominalV);
                            branchThresholdAdder.withSide(side);
                        }
                        break;
                    case JsonSerializationConstants.SIDE:
                        JsonToken sideToken = jsonParser.nextToken();
                        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 2
                            || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) > 3) {
                            if (sideToken == JsonToken.VALUE_NUMBER_INT) {
                                side = JsonSerializationConstants.deserializeSide(jsonParser.getIntValue());
                                branchThresholdAdder.withSide(side);
                            } else if (Objects.equals(jsonParser.getValueAsString(), JsonSerializationConstants.LEFT_SIDE)
                                || Objects.equals(jsonParser.getValueAsString(), JsonSerializationConstants.RIGHT_SIDE)) {
                                throw new OpenRaoException(String.format(
                                    "Side should be %d/%d and not %s/%s since CRAC version 2.4",
                                    JsonSerializationConstants.SIDE_ONE,
                                    JsonSerializationConstants.SIDE_TWO,
                                    JsonSerializationConstants.LEFT_SIDE,
                                    JsonSerializationConstants.RIGHT_SIDE
                                ));
                            } else {
                                throw new OpenRaoException(String.format("Unrecognized side %s", jsonParser.getValueAsString()));
                            }
                        } else {
                            side = JsonSerializationConstants.deserializeSide(jsonParser.getValueAsString());
                            branchThresholdAdder.withSide(side);
                        }
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in BranchThreshold: " + jsonParser.currentName());
                }
            }

            if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 2 || JsonSerializationConstants.getSubVersionNumber(version) > 12) {
                checkForDuplicateThresholds(thresholdSideBounds, side, hasMin, hasMax);
            }

            branchThresholdAdder.add();

        }
    }

    private static void checkForDuplicateThresholds(Map<TwoSides, Set<String>> thresholdSideBounds, TwoSides side, boolean hasMin, boolean hasMax) {
        // Check for duplicate thresholds for the same side and bound
        if (!thresholdSideBounds.containsKey(side)) {
            thresholdSideBounds.put(side, new HashSet<>());
        }
        if (hasMin) {
            if (!thresholdSideBounds.get(side).add("min")) {
                throw new OpenRaoException(String.format("Lower bound can only be defined once for side %s since CRAC v2.13", side));
            }
        }
        if (hasMax) {
            if (!thresholdSideBounds.get(side).add("max")) {
                throw new OpenRaoException(String.format("Upper bound can only be defined once for side %s since CRAC v2.13", side));
            }
        }
    }

    private static void addIMax(CnecElementHelper cnecElementHelper, FlowCnecAdder flowCnecAdder) {
        flowCnecAdder.withIMax(cnecElementHelper.getCurrentLimit(TwoSides.ONE), TwoSides.ONE);
        flowCnecAdder.withIMax(cnecElementHelper.getCurrentLimit(TwoSides.TWO), TwoSides.TWO);
    }

    private static Pair<Double, Double> readAndAddNominalV(CnecElementHelper cnecElementHelper, FlowCnecAdder flowCnecAdder) {
        double nominalVoltage1 = cnecElementHelper.getNominalVoltage(TwoSides.ONE);
        double nominalVoltage2 = cnecElementHelper.getNominalVoltage(TwoSides.TWO);
        flowCnecAdder.withNominalVoltage(nominalVoltage1, TwoSides.ONE);
        flowCnecAdder.withNominalVoltage(nominalVoltage2, TwoSides.TWO);
        return Pair.of(nominalVoltage1, nominalVoltage2);
    }
}
