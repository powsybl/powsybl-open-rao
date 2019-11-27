package com.farao_community.farao.commons.data.glsk_file.glsk_quality_check;

import com.farao_community.farao.commons.data.glsk_file.UcteGlskDocument;
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
