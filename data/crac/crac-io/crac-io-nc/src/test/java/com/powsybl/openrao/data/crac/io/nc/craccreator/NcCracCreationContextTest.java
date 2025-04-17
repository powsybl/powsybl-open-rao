/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class NcCracCreationContextTest {
    @Test
    void testCopyConstructor() {
        Crac crac = Mockito.mock(Crac.class);
        OffsetDateTime offsetDateTime = Mockito.mock(OffsetDateTime.class);
        NcCracCreationContext context = new NcCracCreationContext(crac, offsetDateTime, "network_name");
        context.creationSuccess(crac);

        ElementaryCreationContext cocc = Mockito.mock(ElementaryCreationContext.class);
        ElementaryCreationContext racc = Mockito.mock(ElementaryCreationContext.class);
        ElementaryCreationContext cncc = Mockito.mock(ElementaryCreationContext.class);

        context.setContingencyCreationContexts(Set.of(cocc));
        context.setCnecCreationContexts(Set.of(cncc));
        context.setRemedialActionCreationContexts(Set.of(racc));

        NcCracCreationContext copy = new NcCracCreationContext(context);
        assertEquals(crac, copy.getCrac());
        assertTrue(copy.isCreationSuccessful());
        assertEquals(offsetDateTime, copy.getTimeStamp());
        assertEquals("network_name", copy.getNetworkName());
        assertEquals(Set.of(cocc), copy.getContingencyCreationContexts());
        assertEquals(Set.of(racc), copy.getRemedialActionCreationContexts());
        assertEquals(Set.of(cncc), copy.getCnecCreationContexts());
    }
}
