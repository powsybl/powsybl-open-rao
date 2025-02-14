/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.cse;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.io.cse.xsd.CRACDocumentType;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.io.Importer;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.util.Objects;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@AutoService(Importer.class)
public class CseCracImporter implements Importer {
    private static final String CRAC_CSE_SCHEMA_FILE_LOCATION = "/com/powsybl/openrao/data/crac/io/cse/xsd/crac-document_4_23.xsd";
    private static final String ETSO_CORE_SCHEMA_FILE_LOCATION = "/com/powsybl/openrao/data/crac/io/cse/xsd/etso-core-cmpts.xsd";
    private static final String ETSO_CODES_SCHEMA_FILE_LOCATION = "/com/powsybl/openrao/data/crac/io/cse/xsd/etso-code-lists.xsd";

    @Override
    public String getFormat() {
        return "CseCrac";
    }

    private CRACDocumentType importNativeCrac(InputStream inputStream) {
        CRACDocumentType cracDocumentType;
        try {
            cracDocumentType = JAXBContext.newInstance(CRACDocumentType.class)
                .createUnmarshaller()
                .unmarshal(new StreamSource(inputStream), CRACDocumentType.class)
                .getValue();
        } catch (JAXBException e) {
            throw new OpenRaoException(e);
        }
        return cracDocumentType;
    }

    @Override
    public boolean exists(String filename, InputStream inputStream) {
        if (!FilenameUtils.getExtension(filename).equals("xml")) {
            return false;
        }
        Source xmlFile = new StreamSource(inputStream);
        // The following line triggers sonar issue java:S2755 which prevents us from accessing XSD schema files
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); //NOSONAR

        try {
            Schema schema = schemaFactory.newSchema(new Source[]{
                new StreamSource(Objects.requireNonNull(CseCracImporter.class.getResource(ETSO_CODES_SCHEMA_FILE_LOCATION)).toExternalForm()),
                new StreamSource(Objects.requireNonNull(CseCracImporter.class.getResource(ETSO_CORE_SCHEMA_FILE_LOCATION)).toExternalForm()),
                new StreamSource(Objects.requireNonNull(CseCracImporter.class.getResource(CRAC_CSE_SCHEMA_FILE_LOCATION)).toExternalForm())
            });

            schema.newValidator().validate(xmlFile);
            OpenRaoLoggerProvider.BUSINESS_LOGS.info("CSE CRAC document is valid");
            return true;
        } catch (MalformedURLException e) {
            throw new OpenRaoException("URL error");
        } catch (SAXException e) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.debug("CSE CRAC document is NOT valid. Reason: {}", e.getMessage());
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public CracCreationContext importData(InputStream inputStream, CracCreationParameters cracCreationParameters, Network network) {
        return new CseCracCreator().createCrac(importNativeCrac(inputStream), network, cracCreationParameters);
    }
}
