package com.powsybl.openrao.data.raoresult.nc;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultapi.io.Exporter;
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

@AutoService(Exporter.class)
public class NcExporter implements Exporter {
    @Override
    public String getFormat() {
        return "NC";
    }

    @Override
    public void exportData(RaoResult raoResult, CracCreationContext cracCreationContext, Properties properties, OutputStream outputStream) {
        if (cracCreationContext instanceof CsaProfileCracCreationContext ncCracCreationContext) {
            Document document = initXmlDocument();
            OffsetDateTime timeStamp = ncCracCreationContext.getTimeStamp();

            Element rootRdfElement = createRootRdfElement(document);
            Element header = writeProfileHeader(document, timeStamp);
            rootRdfElement.appendChild(header);

            new RemedialActionScheduleProfileExporter().addWholeProfile(document, rootRdfElement, header, raoResult, ncCracCreationContext);
            new SecurityAnalysisResultProfileWriter().addWholeProfile(document, rootRdfElement, header, raoResult, ncCracCreationContext);

            writeOutputXmlFile(document, outputStream);
        } else {
            throw new OpenRaoException("CRAC Creation Context is not NC-compliant.");
        }
    }

    @Override
    public void exportData(RaoResult raoResult, Crac crac, Properties properties, OutputStream outputStream) {
        throw new NotImplementedException("CracCreationContext is required for NC export.");
    }

    private static String formatOffsetDateTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
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
        Arrays.stream(Namespace.values()).forEach(namespace -> rootRdfElement.setAttribute("xmlns:%s".formatted(namespace.getKeyword()), namespace.getUri()));
        document.appendChild(rootRdfElement);
        return rootRdfElement;
    }

    private static Element writeProfileHeader(Document document, OffsetDateTime timeStamp) {
        Element header = document.createElement("md:FullModel");

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
