/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.api.mock;

import com.farao_community.farao.data.native_crac_api.NativeCrac;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NativeCracMock implements NativeCrac {

    private boolean isOk;

    public NativeCracMock(boolean isOk) {
        this.isOk = isOk;
    }

    @Override
    public String getFormat() {
        return "MockedNativeCracFormat";
    }

    public boolean isOk() {
        return isOk;
    }
}
