/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.action.*;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.*;
import com.powsybl.openrao.data.cracapi.range.StandardRange;
import com.powsybl.openrao.data.cracapi.range.TapRange;
import com.powsybl.openrao.data.cracapi.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.threshold.BranchThreshold;
import com.powsybl.openrao.data.cracapi.threshold.Threshold;
import com.powsybl.openrao.data.cracapi.usagerule.*;
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
        this.addSerializer(CounterTradeRangeAction.class, new CounterTradeRangeActionSerializer());
        this.addSerializer(OnInstant.class, new OnInstantSerializer());
        this.addSerializer(OnContingencyState.class, new OnStateSerializer());
        this.addSerializer(OnConstraint.class, new OnConstraintSerializer());
        this.addSerializer(OnFlowConstraintInCountry.class, new OnFlowConstraintInCountrySerializer());
        this.addSerializer(TapRange.class, new TapRangeSerializer());
        this.addSerializer(StandardRange.class, new StandardRangeSerializer());
        this.addSerializer(NetworkAction.class, new NetworkActionSerializer());
        this.addSerializer(TerminalsConnectionAction.class, new TerminalsConnectionActionSerializer());
        this.addSerializer(SwitchAction.class, new SwitchActionSerializer());
        this.addSerializer(PhaseTapChangerTapPositionAction.class, new PhaseTapChangerTapPositionActionSerializer());
        this.addSerializer(GeneratorAction.class, new GeneratorActionSerializer());
        this.addSerializer(LoadAction.class, new LoadActionSerializer());
        this.addSerializer(DanglingLineAction.class, new DanglingLineActionSerializer());
        this.addSerializer(ShuntCompensatorPositionAction.class, new ShuntCompensatorPositionActionSerializer());
        this.addSerializer(SwitchPair.class, new SwitchPairSerializer());
        this.addSerializer(Instant.class, new InstantSerializer());
    }
}
