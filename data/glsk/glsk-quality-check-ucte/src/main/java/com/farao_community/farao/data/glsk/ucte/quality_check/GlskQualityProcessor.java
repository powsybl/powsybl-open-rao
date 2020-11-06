/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.ucte.quality_check;

import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public final class GlskQualityProcessor {

    private GlskQualityProcessor() {
    }

    public static QualityReport process(String cgmName, InputStream cgmIs, InputStream glskIs, Instant localDate) throws IOException, SAXException, ParserConfigurationException {
        return process(UcteGlskDocument.importGlsk(glskIs), Importers.loadNetwork(cgmName, cgmIs), localDate);
    }

    public static QualityReport process(UcteGlskDocument ucteGlskDocument, Network network, Instant instant) {
        GlskQualityCheckInput input = new GlskQualityCheckInput(ucteGlskDocument, network, instant);
        return GlskQualityCheck.gskQualityCheck(input);
    }
}
