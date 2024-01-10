/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.fbconstraint;

import com.powsybl.openrao.data.craccreation.creator.fbconstraint.xsd.FlowBasedConstraintDocument;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
class FbConstraintTest {

    @Test
    void testConstructor() {
        FlowBasedConstraintDocument flowBasedConstraintDocument = Mockito.mock(FlowBasedConstraintDocument.class);
        FbConstraint fbConstraint = new FbConstraint(flowBasedConstraintDocument, 20);

        assertEquals("FlowBasedConstraintDocument", fbConstraint.getFormat());
        assertEquals(20, fbConstraint.getFlowBasedDocumentVersion());
        assertNotNull(fbConstraint.getDocument());
    }
}
