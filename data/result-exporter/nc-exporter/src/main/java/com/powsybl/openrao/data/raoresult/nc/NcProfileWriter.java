package com.powsybl.openrao.data.raoresult.nc;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
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

    void addProfileContent(Document document, Element rootRdfElement, RaoResult raoResult, CsaProfileCracCreationContext ncCracCreationContext);

    default void addWholeProfile(Document document, Element rootRdfElement, Element header, RaoResult raoResult, CsaProfileCracCreationContext ncCracCreationContext) {
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
