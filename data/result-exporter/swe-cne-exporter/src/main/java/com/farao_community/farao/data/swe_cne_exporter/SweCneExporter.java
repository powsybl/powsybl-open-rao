/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.swe_cne_exporter;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.commons.logs.FaraoLoggerProvider;
import com.powsybl.open_rao.data.cne_exporter_commons.CneExporterParameters;
import com.powsybl.open_rao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.powsybl.open_rao.data.swe_cne_exporter.xsd.CriticalNetworkElementMarketDocument;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.rao_result_api.RaoResult;
import com.powsybl.open_rao.monitoring.angle_monitoring.AngleMonitoringResult;
import com.powsybl.open_rao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
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

import static com.powsybl.open_rao.data.cne_exporter_commons.CneConstants.*;

/**
 * Xml export of the CNE file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SweCneExporter {

    public void exportCne(Crac crac, Network network,
                          CimCracCreationContext cracCreationContext,
                          RaoResult raoResult, AngleMonitoringResult angleMonitoringResult, RaoParameters raoParameters,
                          CneExporterParameters exporterParameters, OutputStream outputStream) {
        SweCne cne = new SweCne(crac, network, cracCreationContext, raoResult, angleMonitoringResult, raoParameters, exporterParameters);
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
                FaraoLoggerProvider.TECHNICAL_LOGS.warn("CNE output doesn't fit the xsd.");
            }

            outputStream.write(result.getBytes());

        } catch (JAXBException | IOException e) {
            throw new FaraoException("Could not write SWE CNE file.");
        }
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
            FaraoLoggerProvider.TECHNICAL_LOGS.warn("Exception: {}", e.getMessage());
            return false;
        }
        return true;
    }
}
