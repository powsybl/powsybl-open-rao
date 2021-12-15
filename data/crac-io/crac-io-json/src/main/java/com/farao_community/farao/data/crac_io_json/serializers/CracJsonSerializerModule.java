/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.*;
import com.farao_community.farao.data.crac_api.range_action.HvdcRange;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.TapRange;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnState;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracJsonSerializerModule extends SimpleModule {

    public CracJsonSerializerModule() {
        super();
        this.addSerializer(Crac.class, new CracSerializer());
        this.addSerializer(Contingency.class, new ContingencySerializer());
        this.addSerializer(FlowCnec.class, new FlowCnecSerializer<>());
        this.addSerializer(BranchThreshold.class, new BranchThresholdSerializer());
        this.addSerializer(PstRangeAction.class, new PstRangeActionSerializer());
        this.addSerializer(HvdcRangeAction.class, new HvdcRangeActionSerializer());
        this.addSerializer(FreeToUse.class, new FreeToUseSerializer());
        this.addSerializer(OnState.class, new OnStateSerializer());
        this.addSerializer(OnFlowConstraint.class, new OnFlowConstraintSerializer());
        this.addSerializer(TapRange.class, new TapRangeSerializer());
        this.addSerializer(HvdcRange.class, new HvdcRangeSerializer());
        this.addSerializer(NetworkAction.class, new NetworkActionSerializer());
        this.addSerializer(TopologicalAction.class, new TopologicalActionSerializer());
        this.addSerializer(PstSetpoint.class, new PstSetpointSerializer());
        this.addSerializer(InjectionSetpoint.class, new InjectionSetpointSerializer());
        this.addSerializer(SwitchPair.class, new SwitchPairSerializer());
    }
}
