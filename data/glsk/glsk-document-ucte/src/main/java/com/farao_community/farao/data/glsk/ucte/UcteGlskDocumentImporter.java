/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.ucte;

import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.io.AbstractGlskDocumentImporter;
import com.farao_community.farao.data.glsk.api.io.GlskDocumentImporter;
import com.google.auto.service.AutoService;

import java.io.InputStream;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(GlskDocumentImporter.class)
public class UcteGlskDocumentImporter extends AbstractGlskDocumentImporter implements GlskDocumentImporter {
    @Override
    public GlskDocument importGlsk(InputStream inputStream) {
        if (document != null) {
            return UcteGlskDocument.importGlsk(document);
        }
        return UcteGlskDocument.importGlsk(inputStream);
    }

    @Override
    public boolean canImport(InputStream inputStream) {
        if (!setDocument(inputStream)) {
            return false;
        }

        if ("GSKDocument".equals(document.getDocumentElement().getTagName()) &&
                document.getDocumentElement().getElementsByTagName("GSKSeries").getLength() > 0) {
            FaraoLoggerProvider.TECHNICAL_LOGS.info("UCTE GLSK importer could import this document.");
            return true;
        } else {
            FaraoLoggerProvider.TECHNICAL_LOGS.info("UCTE GLSK importer could not import this document.");
            document = null; // As document is not recognized ensure document is null, in case import method is called afterwards
            return false;
        }
    }
}
