/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_xml;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporter;
import com.google.auto.service.AutoService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@AutoService(CracImporter.class)
public class MergedFilteredCBImporter implements CracImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergedFilteredCBImporter.class);
    private static final String XML_EXTENSION = "xml";
    private static final String FBCD_SCHEMA_FILE = "/xsd/validation/flowbasedconstraintdocument-18.xsd";

    public MergedFilteredCBImporter()  {
    }

    @Override
    public Crac importCrac(InputStream inputStream) {
        return null;
    }

    @Override
    public boolean exists(String s, InputStream inputStream) {
        Source xmlFile = new StreamSource(inputStream);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        try {
            Schema schema = schemaFactory.newSchema(new StreamSource(MergedFilteredCBImporter.class.getResourceAsStream(FBCD_SCHEMA_FILE)));
            Validator validator = schema.newValidator();
            validator.validate(xmlFile);
            LOGGER.info("Flowbased constraint document format is valid");
            return FilenameUtils.getExtension(s).equals(XML_EXTENSION);
        } catch (MalformedURLException e) {
            throw new FaraoException("URL error");
        } catch (SAXException e) {
            LOGGER.warn(String.format("Flowbased constraint document format is NOT valid. Reason: %s", e));
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
