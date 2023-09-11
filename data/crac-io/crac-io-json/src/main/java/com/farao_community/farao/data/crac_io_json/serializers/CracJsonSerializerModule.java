/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.*;
import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range.TapRange;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.farao_community.farao.data.crac_api.usage_rule.*;
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
        this.addSerializer(AngleCnec.class, new AngleCnecSerializer<>());
        this.addSerializer(VoltageCnec.class, new VoltageCnecSerializer<>());
        this.addSerializer(BranchThreshold.class, new BranchThresholdSerializer());
        this.addSerializer(Threshold.class, new ThresholdSerializer());
        this.addSerializer(PstRangeAction.class, new PstRangeActionSerializer());
        this.addSerializer(HvdcRangeAction.class, new HvdcRangeActionSerializer());
        this.addSerializer(InjectionRangeAction.class, new InjectionRangeActionSerializer());
        this.addSerializer(OnInstant.class, new OnInstantSerializer());
        this.addSerializer(OnContingencyState.class, new OnStateSerializer());
        this.addSerializer(OnFlowConstraint.class, new OnFlowConstraintSerializer());
        this.addSerializer(OnAngleConstraint.class, new OnAngleConstraintSerializer());
        this.addSerializer(OnVoltageConstraint.class, new OnVoltageConstraintSerializer());
        this.addSerializer(OnFlowConstraintInCountry.class, new OnFlowConstraintInCountrySerializer());
        this.addSerializer(TapRange.class, new TapRangeSerializer());
        this.addSerializer(StandardRange.class, new StandardRangeSerializer());
        this.addSerializer(NetworkAction.class, new NetworkActionSerializer());
        this.addSerializer(TopologicalAction.class, new TopologicalActionSerializer());
        this.addSerializer(PstSetpoint.class, new PstSetpointSerializer());
        this.addSerializer(InjectionSetpoint.class, new InjectionSetpointSerializer());
        this.addSerializer(SwitchPair.class, new SwitchPairSerializer());
    }
}
