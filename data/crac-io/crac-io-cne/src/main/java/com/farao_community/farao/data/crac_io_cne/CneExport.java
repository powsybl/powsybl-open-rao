/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporter;
import com.google.auto.service.AutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@AutoService(CracExporter.class)
public class CneExport implements CracExporter {

    private static final String CNE_FORMAT = "CNE";
    private static final String CNE_XSD_2_4 = "iec62325-451-n-cne_v2_4.xsd";
    private static final String CNE_LOCATION = "src/main/resources/xsd/" + CNE_XSD_2_4;
    private static final String CNE_TAG = "CriticalNetworkElement_MarketDocument";

    private static final Logger LOGGER = LoggerFactory.getLogger(CneExport.class);

    @Override
    public String getFormat() {
        return CNE_FORMAT;
    }

    @Override
    public void exportCrac(Crac crac, OutputStream outputStream) {
        CneFiller.generate(crac);
        CriticalNetworkElementMarketDocument cne = CneFiller.getCne();
        StringWriter stringWriter = new StringWriter();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(CriticalNetworkElementMarketDocument.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // format the XML output
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, CNE_XSD_2_4);

            QName qName = new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, CNE_TAG);
            JAXBElement<CriticalNetworkElementMarketDocument> root = new JAXBElement<>(qName, CriticalNetworkElementMarketDocument.class, cne);

            jaxbMarshaller.marshal(root, stringWriter);

            String result = stringWriter.toString().replace("xsi:" + CNE_TAG, CNE_TAG);

            if (!validateCNESchema(result)) {
                LOGGER.warn("CNE output doesn't fit the xsd.");
            }

            outputStream.write(result.getBytes());

        } catch (JAXBException | IOException e) {
            throw new FaraoException();
        }
    }

    public static boolean validateCNESchema(String xmlContent) {
        return validateXMLSchema(CNE_LOCATION, xmlContent);
    }

    private static boolean validateXMLSchema(String xsdPath, String xmlContent) {

        try {
            SchemaFactory factory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new File(xsdPath));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xmlContent)));
        } catch (IOException | SAXException e) {
            LOGGER.warn(String.format("Exception: %s", e.getMessage()));
            return false;
        }
        return true;
    }
}
