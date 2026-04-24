/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.nc;

import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public interface NcProfileWriter {
    String getKeyword();

    default void addProfileKeywordToHeader(Document document, Element header) {
        Element keywordElement = document.createElement("dcat:keyword");
        keywordElement.setTextContent(getKeyword());
        header.appendChild(keywordElement);
    }

    void addProfileContent(Document document, Element rootRdfElement, RaoResult raoResult, NcCracCreationContext ncCracCreationContext);

    default void addWholeProfile(Document document, Element rootRdfElement, Element header, RaoResult raoResult, NcCracCreationContext ncCracCreationContext) {
        addProfileKeywordToHeader(document, header);
        addProfileContent(document, rootRdfElement, raoResult, ncCracCreationContext);
    }

    static String formatOffsetDateTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    static void setRdfResourceReference(Element element, String reference) {
        element.setAttribute("rdf:resource", reference);
    }

    static String getMRidReference(String mRid) {
        return "#_%s".formatted(mRid);
    }
}
