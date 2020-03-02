/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Cnec;
import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(ExtensionAdderProvider.class)
public class CnecResultAdderImplProvider implements ExtensionAdderProvider<Cnec, CnecResult, CnecResultAdderImpl> {

    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public Class<CnecResultAdderImpl> getAdderClass() {
        return CnecResultAdderImpl.class;
    }

    @Override
    public CnecResultAdderImpl newAdder(Cnec extendable) {
        return new CnecResultAdderImpl(extendable);
    }

}
