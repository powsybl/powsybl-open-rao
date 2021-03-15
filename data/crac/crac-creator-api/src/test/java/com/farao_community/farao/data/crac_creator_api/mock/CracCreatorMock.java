/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creator_api.mock;

import com.farao_community.farao.data.crac_creator_api.CracCreator;
import com.farao_community.farao.data.native_crac_api.NativeCrac;
import com.farao_community.farao.data.native_crac_io_api.NativeCracImporter;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import org.mockito.Mockito;

import java.io.InputStream;
import java.time.OffsetDateTime;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(CracCreator.class)
public class CracCreatorMock implements CracCreator<NativeCracMock, CracCreationContextMock> {

    @Override
    public String getNativeCracFormat() {
        return "MockedNativeCracFormat";
    }

    @Override
    public CracCreationContextMock createCrac(NativeCracMock nativeCrac, Network network, OffsetDateTime offsetDateTime) {
        return new CracCreationContextMock(nativeCrac.isOk());
    }
}
