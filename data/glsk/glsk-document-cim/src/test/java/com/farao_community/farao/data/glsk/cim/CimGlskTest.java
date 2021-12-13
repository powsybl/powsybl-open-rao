/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.cim;

import com.farao_community.farao.commons.ZonalData;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Map;

/**
 * FlowBased Glsk Values Provider Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class CimGlskTest {

    private Network testNetwork;
    private Instant instant;

    @Before
    public void setUp() {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        instant = Instant.parse("2018-08-28T22:00:00Z");
    }

    @Test
    public void run() {
        ZonalData<SensitivityVariableSet> zonalGlsks = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskCountry.xml"))
            .getZonalGlsks(testNetwork, instant);
        Map<String, SensitivityVariableSet> map = zonalGlsks.getDataPerZone();
        Assert.assertFalse(map.isEmpty());

        SensitivityVariableSet linearGlsk = zonalGlsks.getData("10YBE----------2");
        Assert.assertFalse(linearGlsk.getVariables().isEmpty());
    }

    @Test
    public void runWithInvalidCountry() {
        ZonalData<SensitivityVariableSet> zonalGlsks = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskCountry.xml"))
            .getZonalGlsks(testNetwork, instant);
        Assert.assertNull(zonalGlsks.getData("fake-area"));
    }
}
