/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitr at rte-france.com>}
 */
public class AbsRemedialActionTest {
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Network network;
    private Crac crac;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.createWithPreventivePstRange();
    }

    @Test
    public void testGetFlowCnecsConstrainingForOneUsageRule() {
        RemedialAction<?> na1 = crac.newNetworkAction().withId("na1")
            .newTerminalsConnectionAction().withNetworkElement("ne1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(CURATIVE_INSTANT_ID).withCountry(Country.BE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        assertEquals(Set.of(crac.getCnec("cnec1stateCurativeContingency1"), crac.getCnec("cnec1stateCurativeContingency2")),
            na1.getFlowCnecsConstrainingForOneUsageRule(na1.getUsageRules().iterator().next(), crac.getFlowCnecs(), network));

        RemedialAction<?> na2 = crac.newNetworkAction().withId("na2")
            .newTerminalsConnectionAction().withNetworkElement("ne1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(CURATIVE_INSTANT_ID).withContingency("Contingency FR1 FR3").withCountry(Country.FR).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        assertEquals(Set.of(crac.getCnec("cnec1stateCurativeContingency1"), crac.getCnec("cnec2stateCurativeContingency1")),
            na2.getFlowCnecsConstrainingForOneUsageRule(na2.getUsageRules().iterator().next(), crac.getFlowCnecs(), network));
    }
}
