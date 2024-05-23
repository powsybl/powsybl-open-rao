/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracioapi;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.google.auto.service.AutoService;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.io.InputStream;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@AutoService(CracImporter.class)
public class CracImporterMock implements CracImporter {

    @Override
    public Crac importCrac(InputStream inputStream, @Nonnull CracFactory cracFactory, Network network, ReportNode reportNode) {
        return Mockito.mock(Crac.class);
    }

    @Override
    public Crac importCrac(InputStream inputStream, Network network, ReportNode reportNode) {
        return Mockito.mock(Crac.class);
    }

    @Override
    public boolean exists(String fileName, InputStream inputStream, ReportNode reportNode) {
        return true;
    }
}
