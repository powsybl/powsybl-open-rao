/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.networkextension;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;
import com.powsybl.iidm.network.Injection;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(ExtensionAdderProvider.class)
public class AssignedVirtualHubAdderImplProvider<T extends Injection<T>> implements ExtensionAdderProvider<T, AssignedVirtualHub<T>, AssignedVirtualHubAdderImpl<T>> {

    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public Class<AssignedVirtualHubAdderImpl> getAdderClass() {
        return AssignedVirtualHubAdderImpl.class;
    }

    @Override
    public AssignedVirtualHubAdderImpl<T> newAdder(T extendable) {
        return new AssignedVirtualHubAdderImpl<>(extendable);
    }

}
