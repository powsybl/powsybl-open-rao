/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

/**
 * General test file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.data.crac_api.UsageMethod;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class CracTest {

    @Test
    public void testCrac() {

        Instant basecase = new Instant(0);
        Instant curative = new Instant(200);

        NetworkElement networkElementCo1 = new NetworkElement("idElementCo1", "Contingency Element 1");
        NetworkElement networkElementCo2 = new NetworkElement("idElementCo2", "Contingency Element 2");

        List<NetworkElement> contingenciesElements = new ArrayList<>();
        contingenciesElements.add(networkElementCo1);

        Contingency contingency = new Contingency("idContingency", "Contingency", contingenciesElements);

        State stateBasecase = new State(Optional.empty(), basecase);
        State stateCurative = new State(Optional.of(contingency), curative);

        NetworkElement monitoredElement = new NetworkElement("idMR", "Monitored Element");

        FlowViolation threshold1 = new FlowViolation(Unit.AMPERE, Side.LEFT, Direction.IN, 1000);
        SafetyInterval threshold2 = new SafetyInterval(Unit.KILOVOLT, 280, 300);

        Cnec cnec = new Cnec("idCnec", "Cnec", monitoredElement, threshold1, stateCurative);

        List<Cnec> cnecs = new ArrayList<>();
        cnecs.add(cnec);

        FreeToUse usageContext = new FreeToUse();
        UsageRule usageRule = new UsageRule(UsageMethod.FORCED, usageContext);

        NetworkElement networkElementRa = new NetworkElement("idElementRa", "Element RA");

        PstLever pstLever = PstLever.withAbsoluteRange(networkElementRa, 3, 14);
        PstGroupLever pstGroupLever = new PstGroupLever(Arrays.asList(pstLever));

        RemedialAction remedialAction = new RemedialAction("idRA", "Remedial Action", Arrays.asList(pstGroupLever), Arrays.asList(usageRule));

        List<RemedialAction> remedialActions = new ArrayList<>();
        remedialActions.add(remedialAction);

        Crac crac = new Crac("idCrac", "name", cnecs, remedialActions);

        assertEquals(true, crac.getId().equals("idCrac"));
    }

}
