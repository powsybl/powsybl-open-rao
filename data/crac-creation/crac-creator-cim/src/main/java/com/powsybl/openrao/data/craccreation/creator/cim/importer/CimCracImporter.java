/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.cim.importer;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.nativecracioapi.NativeCracImporter;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.data.craccreation.creator.cim.xsd.CRACMarketDocument;
import com.powsybl.openrao.data.craccreation.creator.cim.CimCrac;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.util.Objects;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(NativeCracImporter.class)
public class CimCracImporter implements NativeCracImporter<CimCrac> {
    private static final String CRAC_CIM_SCHEMA_FILE_LOCATION = "/xsd/iec62325-451-n-crac_v2_3.xsd";
    private static final String ETSO_CODES_SCHEMA_FILE_LOCATION = "/xsd/urn-entsoe-eu-wgedi-codelists.xsd";

    @Override
    public String getFormat() {
        return "CimCrac";
    }

    @Override
    public CimCrac importNativeCrac(InputStream inputStream, ReportNode reportNode) {
        CRACMarketDocument cracDocumentType;
        try {
            cracDocumentType = JAXBContext.newInstance(CRACMarketDocument.class)
                    .createUnmarshaller()
                    .unmarshal(new StreamSource(inputStream), CRACMarketDocument.class)
                    .getValue();
        } catch (JAXBException e) {
            throw new OpenRaoException(e);
        }
        return new CimCrac(cracDocumentType);
    }

    @Override
    public boolean exists(String s, InputStream inputStream, ReportNode reportNode) {
        Source xmlFile = new StreamSource(inputStream);
        // The following line triggers sonar issue java:S2755 which prevents us from accessing XSD schema files
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); //NOSONAR

        try {
            Schema schema = schemaFactory.newSchema(new Source[]{
                new StreamSource(Objects.requireNonNull(CimCracImporter.class.getResource(ETSO_CODES_SCHEMA_FILE_LOCATION)).toExternalForm()),
                new StreamSource(Objects.requireNonNull(CimCracImporter.class.getResource(CRAC_CIM_SCHEMA_FILE_LOCATION)).toExternalForm())
            });

            schema.newValidator().validate(xmlFile);
            BUSINESS_LOGS.info("CIM CRAC document is valid");
            return FilenameUtils.getExtension(s).equals("xml");
        } catch (MalformedURLException e) {
            throw new OpenRaoException("URL error");
        } catch (SAXException e) {
            TECHNICAL_LOGS.debug("CIM CRAC document is NOT valid. Reason: {}", e.getMessage());
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
