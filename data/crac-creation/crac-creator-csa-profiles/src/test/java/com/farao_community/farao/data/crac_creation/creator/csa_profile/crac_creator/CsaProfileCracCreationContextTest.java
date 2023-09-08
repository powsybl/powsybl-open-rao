/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec.CsaProfileCnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.contingency.CsaProfileContingencyCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action.CsaProfileRemedialActionCreationContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CsaProfileCracCreationContextTest {
    @Test
    public void testCopyConstructor() {
        Crac crac = Mockito.mock(Crac.class);
        OffsetDateTime offsetDateTime = Mockito.mock(OffsetDateTime.class);
        CsaProfileCracCreationContext context = new CsaProfileCracCreationContext(crac, offsetDateTime, "network_name");
        context.creationSuccess(crac);

        CsaProfileContingencyCreationContext cocc = Mockito.mock(CsaProfileContingencyCreationContext.class);
        CsaProfileRemedialActionCreationContext racc = Mockito.mock(CsaProfileRemedialActionCreationContext.class);
        CsaProfileCnecCreationContext cncc = Mockito.mock(CsaProfileCnecCreationContext.class);

        context.setContingencyCreationContexts(Set.of(cocc));
        context.setFlowCnecCreationContexts(Set.of(cncc));
        context.setRemedialActionCreationContext(Set.of(racc));

        CsaProfileCracCreationContext copy = new CsaProfileCracCreationContext(context);
        assertEquals(crac, copy.getCrac());
        assertTrue(copy.isCreationSuccessful());
        assertEquals(offsetDateTime, copy.getTimeStamp());
        assertEquals("network_name", copy.getNetworkName());
        assertEquals(Set.of(cocc), copy.getContingencyCreationContexts());
        assertEquals(Set.of(racc), copy.getRemedialActionCreationContext());
        assertEquals(Set.of(cncc), copy.getFlowCnecCreationContexts());
    }
}
