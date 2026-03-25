/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.idcc.core;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.FlowBasedConstraintDocument;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public final class CracUtil {

    private CracUtil() {
        throw new AssertionError("Utility class should not be constructed");
    }

    private static final JAXBContext CONTEXT;
    private static final Unmarshaller UNMARSHALLER;

    static {
        try {
            CONTEXT = JAXBContext.newInstance(FlowBasedConstraintDocument.class);
            UNMARSHALLER = CONTEXT.createUnmarshaller();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(
                "Failed to initialize JAXBContext/Unmarshaller: " + e.getMessage());
        }
    }

    public static FlowBasedConstraintDocument importNativeCrac(File inputStream) {
        try {
            return (FlowBasedConstraintDocument) UNMARSHALLER.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new OpenRaoException("Exception occurred during import of native crac", e);
        }
    }

}
