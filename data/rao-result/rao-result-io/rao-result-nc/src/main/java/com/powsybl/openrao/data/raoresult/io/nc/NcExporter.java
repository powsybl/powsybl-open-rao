/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.nc;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.io.Exporter;
import org.apache.commons.lang3.NotImplementedException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;

/**
 * RAO Result exporter in ENTSO-E's standard Network Code profiles.
 * The Remedial Action Schedule (RAS) profile is always exported.
 * Other profiles may be exported if stated in the export properties.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(Exporter.class)
public class NcExporter implements Exporter {
    @Override
    public String getFormat() {
        return "NC";
    }

    @Override
    public Set<String> getRequiredProperties() {
        return Set.of();
    }

    @Override
    public Class<? extends CracCreationContext> getCracCreationContextClass() {
        return null;
    }

    @Override
    public void exportData(RaoResult raoResult, CracCreationContext cracCreationContext, Properties properties, OutputStream outputStream) {
        if (cracCreationContext instanceof NcCracCreationContext ncCracCreationContext) {
            OffsetDateTime timeStamp = ncCracCreationContext.getTimeStamp();
            new RASProfileExporter().fill(timeStamp, raoResult, ncCracCreationContext).write(outputStream);
        } else {
            throw new OpenRaoException("CRAC Creation Context is not NC-compliant.");
        }
    }

    @Override
    public void exportData(RaoResult raoResult, Crac crac, Properties properties, OutputStream outputStream) {
        throw new NotImplementedException("CracCreationContext is required for NC export.");
    }

    private static String formatOffsetDateTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private static Document initXmlDocument() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            return docBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new OpenRaoException("Could not initialize output XML file. Reason: %s".formatted(e.getMessage()));
        }
    }

    private static Element createRootRdfElement(Document document) {
        Element rootRdfElement = document.createElementNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:RDF");
        Arrays.stream(Namespace.values()).forEach(namespace -> rootRdfElement.setAttribute("xmlns:%s".formatted(namespace.getKeyword()), namespace.getURI()));
        document.appendChild(rootRdfElement);
        return rootRdfElement;
    }

    private static Element writeProfileHeader(Document document, OffsetDateTime timeStamp) {
        Element header = document.createElement("md:FullModel");

        OffsetDateTime now = OffsetDateTime.now();

        Element generatedAtTime = document.createElement("prov:generatedAtTime");
        generatedAtTime.setTextContent(formatOffsetDateTime(now));
        header.appendChild(generatedAtTime);

        Element issueDate = document.createElement("dcterms:issued");
        issueDate.setTextContent(formatOffsetDateTime(now));
        header.appendChild(issueDate);

        Element startDate = document.createElement("dcat:startDate");
        startDate.setTextContent(formatOffsetDateTime(timeStamp));
        header.appendChild(startDate);

        Element endDate = document.createElement("dcat:endDate");
        endDate.setTextContent(formatOffsetDateTime(timeStamp.plusHours(1)));
        header.appendChild(endDate);

        return header;
    }

    private static void writeOutputXmlFile(Document document, OutputStream outputStream) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(document), new StreamResult(outputStream));
        } catch (TransformerException e) {
            throw new OpenRaoException("Could not write output XML file. Reason: %s".formatted(e.getMessage()));
        }
    }
}
