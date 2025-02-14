/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.fbconstraint;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.io.Importer;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.FlowBasedConstraintDocument;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import static java.lang.Integer.parseInt;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(Importer.class)
public class FbConstraintImporter implements Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FbConstraintImporter.class);
    private static final String XML_EXTENSION = "xml";
    private static final String XML_SCHEMA_VERSION = "flowbasedconstraintdocument-";
    private static final String FLOWBASED_CONSTRAINT_V11_SCHEMA_FILE = "/xsd/validation/flowbasedconstraintdocument-11.xsd";
    private static final String FLOWBASED_CONSTRAINT_V18_SCHEMA_FILE = "/xsd/validation/flowbasedconstraintdocument-18.xsd";
    private static final String FLOWBASED_CONSTRAINT_V23_SCHEMA_FILE = "/xsd/flowbasedconstraintdocument-23.xsd";
    private static final String ETSO_CODE_LIST_SCHEMA_FILE = "/xsd/etso-code-lists.xsd";
    private static final String ETSO_CORE_CMPTS_SCHEMA_FILE = "/xsd/etso-core-cmpts.xsd";

    @Override
    public String getFormat() {
        return "FlowBasedConstraintDocument";
    }

    private FlowBasedConstraintDocument importNativeCrac(InputStream inputStream) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);
            JAXBContext jaxbContext = JAXBContext.newInstance(FlowBasedConstraintDocument.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (FlowBasedConstraintDocument) jaxbUnmarshaller.unmarshal(new ByteArrayInputStream(bytes));
        } catch (JAXBException | IOException e) {
            throw new OpenRaoException(e);
        }
    }

    @Override
    public boolean exists(String filename, InputStream inputStream) {
        if (!FilenameUtils.getExtension(filename).equals(XML_EXTENSION)) {
            return false;
        }
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);
            int flowBasedDocumentVersion = flowBasedDocumentVersion(new ByteArrayInputStream(bytes));
            String schemaFile = schemaVersion(flowBasedDocumentVersion);

            if (schemaFile != null) {
                Source xmlFile = new StreamSource(new ByteArrayInputStream(bytes));
                // The following line triggers sonar issue java:S2755 which prevents us from accessing XSD schema files
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); //NOSONAR

                Schema schema = schemaFactory.newSchema(new Source[]{
                    new StreamSource(Objects.requireNonNull(FbConstraintImporter.class.getResource(schemaFile)).toExternalForm()),
                    new StreamSource(Objects.requireNonNull(FbConstraintImporter.class.getResource(ETSO_CORE_CMPTS_SCHEMA_FILE)).toExternalForm()),
                    new StreamSource(Objects.requireNonNull(FbConstraintImporter.class.getResource(ETSO_CODE_LIST_SCHEMA_FILE)).toExternalForm())
                });

                Validator validator = schema.newValidator();
                validator.validate(xmlFile);
                LOGGER.info("FlowBased Constraint Document format is valid");
                return true;
            } else {
                LOGGER.debug("The schema can't be validated because no xsd file is available. Validity check is skipped.");
                return false;
            }
        } catch (MalformedURLException e) {
            throw new OpenRaoException("URL error");
        } catch (SAXException e) {
            LOGGER.debug("FlowBased Constraint Document format is NOT valid. Reason: {}", e.getMessage());
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public CracCreationContext importData(InputStream inputStream, CracCreationParameters cracCreationParameters, Network network) {
        return new FbConstraintCracCreator().createCrac(importNativeCrac(inputStream), network, cracCreationParameters);
    }

    private int flowBasedDocumentVersion(InputStream inputStream) {
        int schemaVersion = Integer.MIN_VALUE;

        try (InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            Optional<String> xsdLine = br.lines().filter(e -> e.contains(XML_SCHEMA_VERSION)).findFirst();
            if (xsdLine.isPresent()) {
                String[] versionNumber = xsdLine.get().split(XML_SCHEMA_VERSION);
                schemaVersion = parseInt(versionNumber[1].substring(0, 2));
            }
        } catch (IOException e) {
            LOGGER.debug("The schema can't be validated because the xml header is not one of a flow-based constraint document.");
        }
        return schemaVersion;
    }

    private String schemaVersion(int flowBasedDocumentVersion) {
        if (flowBasedDocumentVersion >= 23) {
            return FLOWBASED_CONSTRAINT_V23_SCHEMA_FILE;
        } else if (flowBasedDocumentVersion >= 17) {
            return FLOWBASED_CONSTRAINT_V18_SCHEMA_FILE;
        } else if (flowBasedDocumentVersion == 11) {
            return FLOWBASED_CONSTRAINT_V11_SCHEMA_FILE;
        } else {
            LOGGER.debug("Flow-based constraint document with version {} are not handled by the FbConstraintImporter", flowBasedDocumentVersion);
            return null;
        }
    }

    private static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(inputStream, baos);
        return baos.toByteArray();
    }
}
