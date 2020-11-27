/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.json;

import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.BranchCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.json.serializers.ComplexContingencySerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.SimpleCnecSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.SimpleCracSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.SimpleStateSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.network_action.ComplexNetworkActionSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.network_action.NetworkActionSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.network_action.PstSetPointSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.range_action.AlignedRangeActionSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.FreeToUseSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.OnConstraintSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.OnStateSerializer;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.ComplexNetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.PstSetpoint;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.AlignedRangeAction;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.usage_rule.OnConstraint;
import com.farao_community.farao.data.crac_impl.usage_rule.OnState;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracImplJsonModule extends SimpleModule {

    public CracImplJsonModule() {
        super();
        this.addSerializer(ComplexContingency.class, new ComplexContingencySerializer());
        this.addSerializer(FreeToUse.class, new FreeToUseSerializer());
        this.addSerializer(OnConstraint.class, new OnConstraintSerializer());
        this.addSerializer(OnState.class, new OnStateSerializer());
        this.addSerializer(AlignedRangeAction.class, new AlignedRangeActionSerializer());
        this.addSerializer(ComplexNetworkAction.class, new ComplexNetworkActionSerializer());
        this.addSerializer(PstSetpoint.class, new PstSetPointSerializer());
        this.addSerializer(NetworkAction.class, new NetworkActionSerializer());
        this.addSerializer(SimpleCrac.class, new SimpleCracSerializer());
        this.addSerializer(BranchCnec.class, new SimpleCnecSerializer());
        this.addSerializer(SimpleState.class, new SimpleStateSerializer());
    }
}
