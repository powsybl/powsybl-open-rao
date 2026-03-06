/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.cim.craccreator;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.io.Importer;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.cim.xsd.CRACMarketDocument;
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
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(Importer.class)
public class CimCracImporter implements Importer {
    private static final String CRAC_CIM_SCHEMA_FILE_LOCATION = "/xsd/iec62325-451-n-crac_v2_3.xsd";
    private static final String ETSO_CODES_SCHEMA_FILE_LOCATION = "/xsd/urn-entsoe-eu-wgedi-codelists.xsd";

    private static final JAXBContext JAXB_CONTEXT;
    private static final Schema SCHEMA;

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(CRACMarketDocument.class);
            SCHEMA = initSchema();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Schema initSchema() throws SAXException {
        // The following line triggers sonar issue java:S2755 which prevents us from accessing XSD schema files
        var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); //NOSONAR
        return schemaFactory.newSchema(new Source[]{
            new StreamSource(Objects.requireNonNull(CimCracImporter.class.getResource(ETSO_CODES_SCHEMA_FILE_LOCATION)).toExternalForm()),
            new StreamSource(Objects.requireNonNull(CimCracImporter.class.getResource(CRAC_CIM_SCHEMA_FILE_LOCATION)).toExternalForm())
        });
    }

    @Override
    public String getFormat() {
        return "CimCrac";
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
                BUSINESS_LOGS.info("CIM CRAC document is valid");
                return true;
            } catch (SAXException e) {
                TECHNICAL_LOGS.debug("CIM CRAC document is NOT valid. Reason: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CracCreationContext importData(SafeFileReader inputFile, CracCreationParameters cracCreationParameters, Network network) {
        var crac = inputFile.withReadStream(this::importNativeCrac);
        return new CimCracCreator().createCrac(crac, network, cracCreationParameters);
    }

    private CRACMarketDocument importNativeCrac(InputStream inputStream) {
        try {
            return JAXB_CONTEXT.createUnmarshaller().unmarshal(new StreamSource(inputStream), CRACMarketDocument.class).getValue();
        } catch (JAXBException e) {
            throw new OpenRaoException(e);
        }
    }

}
