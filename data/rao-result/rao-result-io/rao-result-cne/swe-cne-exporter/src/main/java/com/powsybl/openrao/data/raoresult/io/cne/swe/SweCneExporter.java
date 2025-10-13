/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.swe;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.io.Exporter;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.CriticalNetworkElementMarketDocument;
import com.powsybl.openrao.data.crac.api.Crac;
import org.apache.commons.lang3.NotImplementedException;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.*;
import static com.powsybl.openrao.data.raoresult.io.cne.swe.SweCneUtil.SWE_CNE_EXPORT_PROPERTIES_PREFIX;

/**
 * SWE-CNE Rao Result exporter in XML format.
 * <p/>
 * Optional properties:
 * <ul>
 *     <li>
 *         <i>relative-positive-margins</i>: boolean (default is "false").
 *     </li>
 *     <li>
 *         <i>with-loop-flows</i>: boolean (default is "false").
 *     </li>
 *     <li>
 *         <i>mnec-acceptable-margin-diminution</i>: double (default is "0").
 *     </li>
 * </ul>
 *
 * Required properties:
 * <ul>
 *     <li>
 *         <i>document-id</i>: string
 *     </li>
 *     <li>
 *         <i>revision-number</i>: integer
 *     </li>
 *     <li>
 *         <i>domain-id</i>: string
 *     </li>
 *     <li>
 *         <i>process-type</i>: string
 *     </li>
 *     <li>
 *         <i>sender-id</i>: string
 *     </li>
 *     <li>
 *         <i>sender-role</i>: string
 *     </li>
 *     <li>
 *         <i>receiver-id</i>: string
 *     </li>
 *     <li>
 *         <i>receiver-role</i>: string
 *     </li>
 *     <li>
 *         <i>time-interval</i>: string
 *     </li>
 * </ul>
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(Exporter.class)
public class SweCneExporter implements Exporter {
    @Override
    public String getFormat() {
        return "SWE-CNE";
    }

    @Override
    public Set<String> getRequiredProperties() {
        return CNE_REQUIRED_PROPERTIES.stream().map(propertyName -> SWE_CNE_EXPORT_PROPERTIES_PREFIX + propertyName).collect(Collectors.toSet());
    }

    @Override
    public Class<? extends CracCreationContext> getCracCreationContextClass() {
        return CimCracCreationContext.class;
    }

    @Override
    public void exportData(RaoResult raoResult, CracCreationContext cracCreationContext, Properties properties, OutputStream outputStream) {
        validateDataToExport(cracCreationContext, properties);
        SweCne cne = new SweCne((CimCracCreationContext) cracCreationContext, raoResult, properties);
        cne.generate();
        CriticalNetworkElementMarketDocument marketDocument = cne.getMarketDocument();
        StringWriter stringWriter = new StringWriter();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(CriticalNetworkElementMarketDocument.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // format the XML output
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, CNE_XSD_2_3);

            QName qName = new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, CNE_TAG);
            JAXBElement<CriticalNetworkElementMarketDocument> root = new JAXBElement<>(qName, CriticalNetworkElementMarketDocument.class, marketDocument);

            jaxbMarshaller.marshal(root, stringWriter);

            String result = stringWriter.toString().replace("xsi:" + CNE_TAG, CNE_TAG);

            if (!validateCNESchema(result)) {
                OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("CNE output doesn't fit the xsd.");
            }

            outputStream.write(result.getBytes());

        } catch (JAXBException | IOException e) {
            throw new OpenRaoException("Could not write SWE CNE file.");
        }
    }

    @Override
    public void exportData(RaoResult raoResult, Crac crac, Properties properties, OutputStream outputStream) {
        throw new NotImplementedException("CracCreationContext is required for CNE export.");
    }

    private static String getSchemaFile(String schemaName) {
        return Objects.requireNonNull(SweCneExporter.class.getResource("/xsd/" + schemaName)).toExternalForm();
    }

    public static boolean validateCNESchema(String xmlContent) {

        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");

            Source[] source = {new StreamSource(getSchemaFile(CNE_XSD_2_3)),
                               new StreamSource(getSchemaFile(CODELISTS_XSD)),
                               new StreamSource(getSchemaFile(LOCALTYPES_XSD))};
            Schema schema = factory.newSchema(source);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xmlContent)));
        } catch (IOException | SAXException e) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("Exception: {}", e.getMessage());
            return false;
        }
        return true;
    }
}
