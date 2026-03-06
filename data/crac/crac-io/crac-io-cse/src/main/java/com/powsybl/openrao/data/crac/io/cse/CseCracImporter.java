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
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.io.Importer;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.cse.xsd.CRACDocumentType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@AutoService(Importer.class)
public class CseCracImporter implements Importer {
    private static final String CRAC_CSE_SCHEMA_FILE_LOCATION = "/com/powsybl/openrao/data/crac/io/cse/xsd/crac-document_4_23.xsd";
    private static final String ETSO_CORE_SCHEMA_FILE_LOCATION = "/com/powsybl/openrao/data/crac/io/cse/xsd/etso-core-cmpts.xsd";
    private static final String ETSO_CODES_SCHEMA_FILE_LOCATION = "/com/powsybl/openrao/data/crac/io/cse/xsd/etso-code-lists.xsd";

    private static final JAXBContext JAXB_CONTEXT;
    private static final Schema SCHEMA;

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(CRACDocumentType.class);
            SCHEMA = initSchema();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Schema initSchema() throws SAXException {
        // The following line triggers sonar issue java:S2755 which prevents us from accessing XSD schema files
        var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); //NOSONAR
        return schemaFactory.newSchema(new Source[]{
            new StreamSource(Objects.requireNonNull(CseCracImporter.class.getResource(ETSO_CODES_SCHEMA_FILE_LOCATION)).toExternalForm()),
            new StreamSource(Objects.requireNonNull(CseCracImporter.class.getResource(ETSO_CORE_SCHEMA_FILE_LOCATION)).toExternalForm()),
            new StreamSource(Objects.requireNonNull(CseCracImporter.class.getResource(CRAC_CSE_SCHEMA_FILE_LOCATION)).toExternalForm())
        });
    }

    @Override
    public String getFormat() {
        return "CseCrac";
    }

    @Override
    public boolean exists(SafeFileReader inputFile) {
        if (!inputFile.hasFileExtension("xml")) {
            return false;
        }
        return inputFile.withReadStream(is -> {
            try {
                Source xmlFile = new StreamSource(is);
                SCHEMA.newValidator().validate(xmlFile);
                OpenRaoLoggerProvider.BUSINESS_LOGS.info("CSE CRAC document is valid");
                return true;
            } catch (SAXException e) {
                OpenRaoLoggerProvider.TECHNICAL_LOGS.debug("CSE CRAC document is NOT valid. Reason: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CracCreationContext importData(SafeFileReader inputFile, CracCreationParameters cracCreationParameters, Network network) {
        var crac = inputFile.withReadStream(this::importNativeCrac);
        return new CseCracCreator().createCrac(crac, network, cracCreationParameters);
    }

    private CRACDocumentType importNativeCrac(InputStream inputStream) {
        try {
            return JAXB_CONTEXT.createUnmarshaller().unmarshal(new StreamSource(inputStream), CRACDocumentType.class).getValue();
        } catch (JAXBException e) {
            throw new OpenRaoException(e);
        }
    }

}
