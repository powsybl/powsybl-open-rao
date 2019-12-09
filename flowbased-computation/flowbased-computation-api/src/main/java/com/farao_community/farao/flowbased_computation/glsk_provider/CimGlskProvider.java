/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.glsk_provider;

import com.farao_community.farao.data.glsk.import_.actors.GlskDocumentLinearGlskConverter;
import com.powsybl.iidm.network.Network;

import java.io.InputStream;
import java.time.Instant;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class CimGlskProvider extends ChronologyGlskProvider {

    public CimGlskProvider(InputStream cimGlskInputStream, Network network, Instant instant) {
        super(GlskDocumentLinearGlskConverter.convert(cimGlskInputStream, network), instant);
    }

    public CimGlskProvider(InputStream cimGlskInputStream, Network network) {
        this(cimGlskInputStream, network, Instant.now());
    }
}
