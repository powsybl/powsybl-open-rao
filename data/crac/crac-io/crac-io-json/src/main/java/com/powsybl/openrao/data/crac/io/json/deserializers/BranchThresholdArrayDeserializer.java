/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.io.commons.CnecElementHelper;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.api.threshold.BranchThresholdAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class BranchThresholdArrayDeserializer {

    private BranchThresholdArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, FlowCnecAdder ownerAdder, CnecElementHelper cnecElementHelper, String version) throws IOException {
        boolean iMaxFetched = false;
        Pair<Double, Double> nominalV = readAndAddNominalV(cnecElementHelper, ownerAdder);
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            BranchThresholdAdder branchThresholdAdder = ownerAdder.newThreshold();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case JsonSerializationConstants.UNIT:
                        Unit unit = JsonSerializationConstants.deserializeUnit(jsonParser.nextTextValue());
                        branchThresholdAdder.withUnit(unit);
                        // FlowCNEC's iMax is required only if it ahs a threshold with a %IMax unit
                        if (Unit.PERCENT_IMAX.equals(unit) && !iMaxFetched) {
                            addIMax(cnecElementHelper, ownerAdder);
                            iMaxFetched = true;
                        }
                        break;
                    case JsonSerializationConstants.MIN:
                        jsonParser.nextToken();
                        branchThresholdAdder.withMin(jsonParser.getDoubleValue());
                        break;
                    case JsonSerializationConstants.MAX:
                        jsonParser.nextToken();
                        branchThresholdAdder.withMax(jsonParser.getDoubleValue());
                        break;
                    case JsonSerializationConstants.RULE:
                        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 1 || JsonSerializationConstants.getSubVersionNumber(version) > 5) {
                            throw new OpenRaoException("Branch threshold rule is not handled since CRAC version 1.6");
                        } else {
                            branchThresholdAdder.withSide(JsonSerializationConstants.convertBranchThresholdRuleToSide(jsonParser.nextTextValue(), nominalV));
                        }
                        break;
                    case JsonSerializationConstants.SIDE:
                        JsonToken side = jsonParser.nextToken();
                        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 2 || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) > 3) {
                            if (side == JsonToken.VALUE_NUMBER_INT) {
                                branchThresholdAdder.withSide(JsonSerializationConstants.deserializeSide(jsonParser.getIntValue()));
                            } else if (Objects.equals(jsonParser.getValueAsString(), JsonSerializationConstants.LEFT_SIDE) || Objects.equals(jsonParser.getValueAsString(), JsonSerializationConstants.RIGHT_SIDE)) {
                                throw new OpenRaoException(String.format("Side should be %d/%d and not %s/%s since CRAC version 2.4", JsonSerializationConstants.SIDE_ONE, JsonSerializationConstants.SIDE_TWO, JsonSerializationConstants.LEFT_SIDE, JsonSerializationConstants.RIGHT_SIDE));
                            } else {
                                throw new OpenRaoException(String.format("Unrecognized side %s", jsonParser.getValueAsString()));
                            }
                        } else {
                            branchThresholdAdder.withSide(JsonSerializationConstants.deserializeSide(jsonParser.getValueAsString()));
                        }
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in BranchThreshold: " + jsonParser.getCurrentName());
                }
            }
            branchThresholdAdder.add();
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
