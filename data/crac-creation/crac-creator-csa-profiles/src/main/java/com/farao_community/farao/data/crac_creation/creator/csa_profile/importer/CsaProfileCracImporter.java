/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.importer;

import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.native_crac_io_api.NativeCracImporter;
import com.google.auto.service.AutoService;
import org.eclipse.rdf4j.rio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
@AutoService(NativeCracImporter.class)
public class CsaProfileCracImporter implements NativeCracImporter<CsaProfileCrac> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsaProfileCracImporter.class);

    @Override
    public String getFormat() {
        return "CsaProfileCrac";
    }

    @Override
    public CsaProfileCrac importNativeCrac(InputStream inputStream) {
        /*CRACDocumentType cracDocumentType;
        try {
            cracDocumentType = JAXBContext.newInstance(CRACDocumentType.class)
                    .createUnmarshaller()
                    .unmarshal(new StreamSource(inputStream), CRACDocumentType.class)
                    .getValue();
        } catch (JAXBException e) {
            throw new FaraoException(e);
        }*/
        return new CsaProfileCrac();
    }

    @Override
    public boolean exists(String fileName, InputStream inputStream) {
        try {
            RDFFormat rdfFormatContingencies = Rio.getParserFormatForFileName(String.valueOf(this.getClass().getResource(CsaProfileConstants.RDF_FORMAT_CSA_PROFILE))).orElse(RDFFormat.RDFXML);
            RDFParser rdfParser = Rio.createParser(rdfFormatContingencies);
            rdfParser.parse(inputStream);
            LOGGER.info("CSA PROFILE CRAC document is valid");
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (RDFParseException e) {
            LOGGER.debug("CSA PROFILE CRAC document is NOT valid. Reason: RDF parsing problem : {}");
            return false;
        } catch (RDFHandlerException e) {
            LOGGER.debug("CSA PROFILE CRAC document is NOT valid. Reason: RDF handler problem :");
            return true;
        }
    }
}
