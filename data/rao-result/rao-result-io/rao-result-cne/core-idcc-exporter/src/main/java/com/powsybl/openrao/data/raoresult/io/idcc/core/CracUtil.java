/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.io.idcc.core;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.FlowBasedConstraintDocument;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public final class CracUtil {
    private CracUtil() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static FlowBasedConstraintDocument importNativeCrac(InputStream inputStream) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);
            JAXBContext jaxbContext = JAXBContext.newInstance(FlowBasedConstraintDocument.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (FlowBasedConstraintDocument) jaxbUnmarshaller.unmarshal(new ByteArrayInputStream(bytes));
        } catch (JAXBException | IOException e) {
            throw new OpenRaoException("Exception occurred during import of native crac", e);
        }
    }

    public static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, baos);
        return baos.toByteArray();
    }
}
