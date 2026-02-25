/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.usagerule.*;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;
import java.util.List;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class UsageRulesSerializer {
    private UsageRulesSerializer() {
    }

    public static void serializeUsageRules(RemedialAction<?> remedialAction, JsonGenerator gen) throws IOException {
        serializeUsageRules(remedialAction, OnInstant.class, JsonSerializationConstants.ON_INSTANT_USAGE_RULES, gen);
        serializeUsageRules(remedialAction, OnContingencyState.class, JsonSerializationConstants.ON_CONTINGENCY_STATE_USAGE_RULES, gen);
        serializeUsageRules(remedialAction, OnConstraint.class, JsonSerializationConstants.ON_CONSTRAINT_USAGE_RULES, gen);
        serializeUsageRules(remedialAction, OnFlowConstraintInCountry.class, JsonSerializationConstants.ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES, gen);
    }

    private static void serializeUsageRules(RemedialAction<?> remedialAction, Class<? extends UsageRule> usageRuleType, String arrayName, JsonGenerator gen) throws IOException {
        List<UsageRule> usageRules = remedialAction.getUsageRules().stream()
                .filter(usageRule -> usageRuleType.isAssignableFrom(usageRule.getClass()))
                .sorted(new JsonSerializationConstants.UsageRuleComparator())
                .toList();
        if (!usageRules.isEmpty()) {
            gen.writeArrayFieldStart(arrayName);
            for (UsageRule ea : usageRules) {
                gen.writeObject(ea);
            }
            gen.writeEndArray();
        }
    }
}
