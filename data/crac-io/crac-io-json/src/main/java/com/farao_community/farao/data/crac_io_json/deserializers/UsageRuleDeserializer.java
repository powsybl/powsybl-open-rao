/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.usage_rule.OnConstraint;
import com.farao_community.farao.data.crac_impl.usage_rule.OnContingency;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.data.crac_io_json.deserializers.DeserializerNames.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class UsageRuleDeserializer {

    private UsageRuleDeserializer() {
    }

    static List<UsageRule> deserialize(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard UsageRule deserializer as it requires the simpleCrac to compare
        // the contingencies of the OnConstraints UsageRules with the contingencies in the Crac
        List<UsageRule> usageRules = new ArrayList<>();

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {

            String type = null;
            UsageMethod usageMethod = null;
            State state = null;
            Cnec cnec = null;
            Contingency contingency = null;

            while (!jsonParser.nextToken().isStructEnd()) {
                {
                    switch (jsonParser.getCurrentName()) {

                        case TYPE:
                            type = jsonParser.nextTextValue();
                            break;

                        case USAGE_METHOD:
                            jsonParser.nextToken();
                            usageMethod = jsonParser.readValueAs(UsageMethod.class);
                            break;

                        case STATE:
                            String stateId = jsonParser.nextTextValue();
                            state = simpleCrac.getState(stateId);
                            if (state == null) {
                                throw new FaraoException(String.format("The state [%s] mentioned in the usage rule is not defined", stateId));
                            }
                            break;

                        case CNEC:
                            String cnecId = jsonParser.nextTextValue();
                            cnec = simpleCrac.getCnec(cnecId);
                            if (cnec == null) {
                                throw new FaraoException(String.format("The cnec [%s] mentioned in the on-constraint usage rule is not defined", cnecId));
                            }
                            break;

                        case CONTINGENCY:
                            String contingencyId = jsonParser.nextTextValue();
                            contingency = simpleCrac.getContingency(contingencyId);
                            if (contingency == null) {
                                throw new FaraoException(String.format("The contingency [%s] mentioned in the on-contingency usage rule is not defined", contingencyId));
                            }
                            break;

                        default:
                            throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
                    }
                }
            }

            if (type == null) {
                throw new FaraoException("Type of usage rule not defined");
            }

            switch (type) {
                case FREE_TO_USE_TYPE:
                    usageRules.add(new FreeToUse(usageMethod, state));
                    break;

                case ON_CONSTRAINT_TYPE:
                    usageRules.add(new OnConstraint(usageMethod, state, cnec));
                    break;

                case ON_CONTINGENCY_TYPE:
                    usageRules.add(new OnContingency(usageMethod, state, contingency));
                    break;

                default:
                    throw new FaraoException(String.format("Type of usage rule [%s] not handled by SimpleCrac deserializer.", type));
            }
        }
        return usageRules;
    }
}
