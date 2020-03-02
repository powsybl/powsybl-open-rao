/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Cnec;
import com.powsybl.commons.extensions.AbstractExtensionAdder;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CnecResultAdderImpl extends AbstractExtensionAdder<Cnec, CnecResult> implements CnecResultAdder {

    private double flowInMW = Double.NaN;
    private double flowInA = Double.NaN;

    public CnecResultAdderImpl(Cnec extendable) {
        super(extendable);
    }

    @Override
    public CnecResultAdder withFlowInMW(double flow) {
        this.flowInMW = flow;
        return this;
    }

    @Override
    public CnecResultAdder withFlowInA(double flow) {
        this.flowInA = flow;
        return this;
    }

    @Override
    public CnecResultImpl createExtension(Cnec cnec) {
        return new CnecResultImpl(flowInMW, flowInA);
    }
}
