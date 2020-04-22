/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.factors.BranchFlowPerInjectionIncrease;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class CracFactorsProviderTest {
    @Test
    public void cracPstWithRange() {
        Crac crac = CommonCracCreation.createWithPstRange();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        CracFactorsProvider provider = new CracFactorsProvider(crac);

        // Common Crac contains 6 CNEC and 1 range action
        List<SensitivityFactor> factorList = provider.getFactors(network);
        assertEquals(6, factorList.size());
        assertTrue(factorList.stream().allMatch(factor -> factor instanceof BranchFlowPerPSTAngle));
    }

    @Test
    public void cracWithoutRangeAction() {
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        CracFactorsProvider provider = new CracFactorsProvider(crac);

        // Common Crac contains 6 CNEC and 1 range action
        List<SensitivityFactor> factorList = provider.getFactors(network);
        assertEquals(6, factorList.size());
        assertTrue(factorList.stream().allMatch(factor -> factor instanceof BranchFlowPerInjectionIncrease));
    }
}
