/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json.json;

import com.farao_community.farao.data.crac_impl.*;
import com.farao_community.farao.data.crac_impl.FlowCnecImpl;
import com.farao_community.farao.data.crac_io_json.json.serializers.ContingencyImplSerializer;
import com.farao_community.farao.data.crac_io_json.json.serializers.FlowCnecImplSerializer;
import com.farao_community.farao.data.crac_io_json.json.serializers.SimpleCracSerializer;
import com.farao_community.farao.data.crac_io_json.json.serializers.network_action.InjectionSetPointSerializer;
import com.farao_community.farao.data.crac_io_json.json.serializers.network_action.NetworkActionImplSerializer;
import com.farao_community.farao.data.crac_io_json.json.serializers.network_action.PstSetPointSerializer;
import com.farao_community.farao.data.crac_io_json.json.serializers.network_action.TopologySerializer;
import com.farao_community.farao.data.crac_io_json.json.serializers.range_action.PstRangeActionImplSerializer;
import com.farao_community.farao.data.crac_io_json.json.serializers.usage_rule.FreeToUseSerializer;
import com.farao_community.farao.data.crac_io_json.json.serializers.usage_rule.OnStateSerializer;
import com.farao_community.farao.data.crac_impl.PstRangeActionImpl;
import com.farao_community.farao.data.crac_impl.FreeToUseImpl;
import com.farao_community.farao.data.crac_impl.OnStateImpl;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracImplJsonModule extends SimpleModule {

    public CracImplJsonModule() {
        super();
        this.addSerializer(ContingencyImpl.class, new ContingencyImplSerializer());
        this.addSerializer(FreeToUseImpl.class, new FreeToUseSerializer());
        this.addSerializer(OnStateImpl.class, new OnStateSerializer());
        this.addSerializer(NetworkActionImpl.class, new NetworkActionImplSerializer());
        this.addSerializer(PstSetpointImpl.class, new PstSetPointSerializer());
        this.addSerializer(InjectionSetpointImpl.class, new InjectionSetPointSerializer());
        this.addSerializer(TopologicalActionImpl.class, new TopologySerializer());
        this.addSerializer(PstSetpointImpl.class, new PstSetPointSerializer());
        this.addSerializer(PstRangeActionImpl.class, new PstRangeActionImplSerializer());
        this.addSerializer(SimpleCrac.class, new SimpleCracSerializer());
        this.addSerializer(FlowCnecImpl.class, new FlowCnecImplSerializer());
    }
}
