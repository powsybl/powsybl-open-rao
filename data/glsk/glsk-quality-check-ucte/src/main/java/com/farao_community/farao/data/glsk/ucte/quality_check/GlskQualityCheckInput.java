/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.ucte.quality_check;

import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Network;

import java.time.Instant;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
class GlskQualityCheckInput {

    private UcteGlskDocument ucteGlskDocument;

    private Network network;

    private Instant instant;

    public UcteGlskDocument getUcteGlskDocument() {
        return ucteGlskDocument;
    }

    public Network getNetwork() {
        return network;
    }

    public Instant getInstant() {
        return instant;
    }

    public GlskQualityCheckInput(UcteGlskDocument ucteGlskDocument, Network network, Instant instant) {
        this.ucteGlskDocument = ucteGlskDocument;
        this.network = network;
        this.instant = instant;
    }
}
