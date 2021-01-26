/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.json;

import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.cnec.FlowCnecImpl;
import com.farao_community.farao.data.crac_impl.json.serializers.ComplexContingencySerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.FlowCnecImplSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.SimpleCracSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.SimpleStateSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.network_action.ComplexNetworkActionSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.network_action.InjectionSetPointSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.network_action.PstSetPointSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.network_action.TopologySerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.range_action.PstWithRangeSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.FreeToUseSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.OnStateSerializer;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.ComplexNetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.PstSetpoint;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUseImpl;
import com.farao_community.farao.data.crac_impl.usage_rule.OnStateImpl;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracImplJsonModule extends SimpleModule {

    public CracImplJsonModule() {
        super();
        this.addSerializer(ComplexContingency.class, new ComplexContingencySerializer());
        this.addSerializer(FreeToUseImpl.class, new FreeToUseSerializer());
        this.addSerializer(OnStateImpl.class, new OnStateSerializer());
        this.addSerializer(ComplexNetworkAction.class, new ComplexNetworkActionSerializer());
        this.addSerializer(PstSetpoint.class, new PstSetPointSerializer());
        this.addSerializer(InjectionSetpoint.class, new InjectionSetPointSerializer());
        this.addSerializer(Topology.class, new TopologySerializer());
        this.addSerializer(PstSetpoint.class, new PstSetPointSerializer());
        this.addSerializer(ComplexNetworkAction.class, new ComplexNetworkActionSerializer());
        this.addSerializer(PstWithRange.class, new PstWithRangeSerializer());
        this.addSerializer(SimpleCrac.class, new SimpleCracSerializer());
        this.addSerializer(FlowCnecImpl.class, new FlowCnecImplSerializer());
        this.addSerializer(SimpleState.class, new SimpleStateSerializer());
    }
}
