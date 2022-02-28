/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.fb_constraint;

import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.FlowBasedConstraintDocument;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class FbConstraintTest {

    @Test
    public void testConstructor() {
        FlowBasedConstraintDocument flowBasedConstraintDocument = Mockito.mock(FlowBasedConstraintDocument.class);
        FbConstraint fbConstraint = new FbConstraint(flowBasedConstraintDocument, 20);

        assertEquals("FlowBasedConstraintDocument", fbConstraint.getFormat());
        assertEquals(20, fbConstraint.getFlowBasedDocumentVersion());
        assertNotNull(fbConstraint.getDocument());
    }
}
