/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.*;
import com.powsybl.commons.extensions.AbstractExtension;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("cnec-extension")
@JsonIdentityInfo(scope = CnecExtension.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class CnecExtension extends AbstractExtension<Cnec> {

    private double flowBeforeOptim;
    private double flowAfterOptim;

    @JsonCreator
    public CnecExtension(@JsonProperty("flowBeforeOptim") double flowBeforeOptim, @JsonProperty("flowAfterOptim") double flowAfterOptim) {
        this.flowBeforeOptim = flowBeforeOptim;
        this.flowAfterOptim = flowAfterOptim;
    }

    @Override
    public String getName() {
        return "CnecExtension";
    }

    public double getFlowBeforeOptim() {
        return flowBeforeOptim;
    }

    public double getFlowAfterOptim() {
        return flowAfterOptim;
    }
}
