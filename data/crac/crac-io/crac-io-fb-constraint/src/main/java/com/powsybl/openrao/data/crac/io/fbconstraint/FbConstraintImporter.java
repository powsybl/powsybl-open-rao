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
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.FlowBasedConstraintDocument;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(Importer.class)
public class FbConstraintImporter implements Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FbConstraintImporter.class);
    private static final String XML_EXTENSION = "xml";
    private static final String XML_SCHEMA_VERSION = "flowbasedconstraintdocument-";
    private static final String FLOWBASED_CONSTRAINT_V23_SCHEMA_FILE = "/xsd/flowbasedconstraintdocument-23.xsd";
    private static final String ETSO_CODE_LIST_SCHEMA_FILE = "/xsd/etso-code-lists.xsd";
    private static final String ETSO_CORE_CMPTS_SCHEMA_FILE = "/xsd/etso-core-cmpts.xsd";

    private static final JAXBContext JAXB_CONTEXT;
    private static final Map<String, Schema> SCHEMA_CACHE = new ConcurrentHashMap<>();

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(FlowBasedConstraintDocument.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to initialize: " + e.getMessage());
        }
    }

    private static Schema buildSchema(String xsd) {
        // The following line triggers sonar issue java:S2755 which prevents us from accessing XSD schema files
        var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); //NOSONAR
        return SCHEMA_CACHE.computeIfAbsent(xsd, key -> {
            List<Source> sources = new ArrayList<>();
            sources.add(buildSource(xsd));
            sources.add(buildSource(ETSO_CORE_CMPTS_SCHEMA_FILE));
            sources.add(buildSource(ETSO_CODE_LIST_SCHEMA_FILE));
            try {
                return schemaFactory.newSchema(sources.toArray(new Source[0]));
            } catch (SAXException e) {
                throw new RuntimeException("Failed to build schema for: " + xsd, e);
            }
        });
    }

    private static StreamSource buildSource(String xsd) {
        return new StreamSource(Objects.requireNonNull(FbConstraintImporter.class.getResource(xsd)).toExternalForm());
    }

    @Override
    public String getFormat() {
        return "FlowBasedConstraintDocument";
    }

    @Override
    public boolean exists(SafeFileReader inputFile) {
        if (!inputFile.hasFileExtension(XML_EXTENSION)) {
            return false;
        }

        var flowBasedDocumentVersion = inputFile.withReadStream(this::readFlowBasedDocumentVersion);
        if (flowBasedDocumentVersion.isEmpty()) {
            LOGGER.debug("The schema can't be validated because no version can be read. Validity check is skipped.");
            return false;
        }

        var schemaFile = getSchemaVersion(flowBasedDocumentVersion.get());
        if (schemaFile.isEmpty()) {
            LOGGER.debug(
                "The schema can't be validated because no xsd file is available. Validity check is skipped.");
            return false;
        }

        return inputFile.withReadStream(is -> {
            try {
                Schema schema = buildSchema(schemaFile.get());
                Validator validator = schema.newValidator();

                var xmlFile = new StreamSource(is);
                validator.validate(xmlFile);

                LOGGER.info("FlowBased Constraint Document format is valid");
                return true;
            } catch (SAXException e) {
                LOGGER.debug("FlowBased Constraint Document format is NOT valid. Reason: {}", e.getMessage());
                return false;
            }
        });

    }

    @Override
    public CracCreationContext importData(SafeFileReader inputFile,
        CracCreationParameters cracCreationParameters, Network network) {
        var crac = inputFile.withReadStream(this::importNativeCrac);
        return new FbConstraintCracCreator().createCrac(crac, network, cracCreationParameters);
    }

    private FlowBasedConstraintDocument importNativeCrac(InputStream inputStream) {
        try {
            return (FlowBasedConstraintDocument) JAXB_CONTEXT.createUnmarshaller().unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new OpenRaoException(e);
        }
    }

    private Optional<Integer> readFlowBasedDocumentVersion(InputStream is) {
        final int maxLines = 30;
        try (var isr = new InputStreamReader(is, StandardCharsets.UTF_8); var br = new BufferedReader(isr)) {
            String line;
            int linesRead = 0;
            while ((line = br.readLine()) != null && linesRead < maxLines) {
                linesRead++;
                if (line.contains(XML_SCHEMA_VERSION)) {
                    String[] versionNumber = line.split(XML_SCHEMA_VERSION);
                    return Optional.of(Integer.parseInt(versionNumber[1].substring(0, 2)));
                }
            }
        } catch (IOException e) {
            LOGGER.debug("The schema can't be validated because the xml header is not a flow-based constraint document.");
        }
        return Optional.empty();
    }

    private Optional<String> getSchemaVersion(int flowBasedDocumentVersion) {
        if (flowBasedDocumentVersion >= 17) {
            return Optional.of(FLOWBASED_CONSTRAINT_V23_SCHEMA_FILE);
        }
        LOGGER.debug("Flow-based constraint document with version {} are not handled by the FbConstraintImporter", flowBasedDocumentVersion);
        return Optional.empty();
    }

}
