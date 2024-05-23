/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.threshold.BranchThresholdAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class BranchThresholdArrayDeserializer {

    private BranchThresholdArrayDeserializer() {
    }

    public record BranchThreshold(Unit unit, Double min, Double max, Side side) {
    }

    public static Set<BranchThreshold> deserialize(JsonParser jsonParser, FlowCnecAdder ownerAdder, Pair<Double, Double> nominalV, String version) throws IOException {
        Set<BranchThreshold> branchThresholds = new HashSet<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            BranchThresholdAdder branchThresholdAdder = ownerAdder.newThreshold();
            Unit unit = null;
            Double min = null;
            Double max = null;
            Side side = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case UNIT:
                        unit = deserializeUnit(jsonParser.nextTextValue());
                        branchThresholdAdder.withUnit(unit);
                        break;
                    case MIN:
                        jsonParser.nextToken();
                        min = jsonParser.getDoubleValue();
                        branchThresholdAdder.withMin(min);
                        break;
                    case MAX:
                        jsonParser.nextToken();
                        max = jsonParser.getDoubleValue();
                        branchThresholdAdder.withMax(max);
                        break;
                    case RULE:
                        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 5) {
                            throw new OpenRaoException("Branch threshold rule is not handled since CRAC version 1.6");
                        } else {
                            side = convertBranchThresholdRuleToSide(jsonParser.nextTextValue(), nominalV);
                            branchThresholdAdder.withSide(side);
                        }
                        break;
                    case SIDE:
                        side = deserializeSide(jsonParser.nextTextValue());
                        branchThresholdAdder.withSide(side);
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in BranchThreshold: " + jsonParser.getCurrentName());
                }
            }
            branchThresholdAdder.add();
            branchThresholds.add(new BranchThreshold(unit, min, max, side));
        }
        return branchThresholds;
    }
}
