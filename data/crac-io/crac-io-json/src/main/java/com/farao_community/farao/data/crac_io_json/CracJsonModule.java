/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_io_json.deserializers.CracDeserializer;
import com.farao_community.farao.data.crac_io_json.deserializers.DeserializedNetworkElement;
import com.farao_community.farao.data.crac_io_json.deserializers.NetworkElementDeserializer;
import com.farao_community.farao.data.crac_io_json.serializers.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracJsonModule extends SimpleModule {

    public CracJsonModule() {
        super();
        this.addSerializer(Crac.class, new CracSerializer());
        this.addSerializer(Contingency.class, new ContingencySerializer());
        this.addSerializer(NetworkElement.class, new NetworkElementSerializer());
        this.addSerializer(FlowCnec.class, new FlowCnecSerializer<>());
        this.addSerializer(BranchThreshold.class, new BranchThresholdSerializer());

        this.addDeserializer(Crac.class, new CracDeserializer());
        this.addDeserializer(DeserializedNetworkElement.class, new NetworkElementDeserializer());
    }
}
