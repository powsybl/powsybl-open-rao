/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cim.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.JsonCracCreationParameters;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonCimCracCreationParametersTest {

    @Test
    public void roundTripTest() {
        // prepare parameters to export
        CracCreationParameters exportedParameters = new CracCreationParameters();
        CimCracCreationParameters exportedCimParameters = new CimCracCreationParameters();
        exportedCimParameters.setRangeActionGroupsAsString(List.of("rangeAction3 + rangeAction4", "hvdc1 + hvdc2"));
        exportedCimParameters.setRemedialActionSpeed(Set.of(new RangeActionSpeed("rangeAction1", 1)));

        exportedParameters.addExtension(CimCracCreationParameters.class, exportedCimParameters);

        // roundTrip
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(exportedParameters, os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(is);

        // test re-imported parameters
        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);
        assertEquals(2, cimCracCreationParameters.getRangeActionGroupsAsString().size());
        assertEquals("rangeAction3 + rangeAction4", cimCracCreationParameters.getRangeActionGroupsAsString().get(0));
        assertEquals("hvdc1 + hvdc2", cimCracCreationParameters.getRangeActionGroupsAsString().get(1));
        assertEquals(1, cimCracCreationParameters.getRangeActionSpeed("rangeAction1").getSpeed().intValue());
        assertTrue(cimCracCreationParameters.getTimeseriesMrids().isEmpty());
    }

    @Test
    public void importOkTest() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-ok.json"));

        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);

        assertEquals(2, cimCracCreationParameters.getRangeActionGroupsAsString().size());
        assertEquals("rangeAction3 + rangeAction4", cimCracCreationParameters.getRangeActionGroupsAsString().get(0));
        assertEquals("hvdc1 + hvdc2", cimCracCreationParameters.getRangeActionGroupsAsString().get(1));
    }

    @Test
    public void importOkTest2() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-ok2.json"));

        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);

        assertEquals(2, cimCracCreationParameters.getRangeActionSpeedSet().size());
        assert cimCracCreationParameters.getRangeActionSpeed("rangeAction1").getSpeed().equals(1);
        assert cimCracCreationParameters.getRangeActionSpeed("rangeAction2").getSpeed().equals(2);
        assertEquals("rangeAction1", cimCracCreationParameters.getRangeActionSpeed("rangeAction1").getRangeActionId());
        assertEquals("rangeAction2", cimCracCreationParameters.getRangeActionSpeed("rangeAction2").getRangeActionId());
    }

    @Test
    public void importOkTest3() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-ok-aligned.json"));

        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);

        assertEquals(2, cimCracCreationParameters.getRangeActionSpeedSet().size());
        assert cimCracCreationParameters.getRangeActionSpeed("rangeAction1").getSpeed().equals(1);
        assert cimCracCreationParameters.getRangeActionSpeed("rangeAction2").getSpeed().equals(1);
        assertEquals("rangeAction1", cimCracCreationParameters.getRangeActionSpeed("rangeAction1").getRangeActionId());
        assertEquals("rangeAction2", cimCracCreationParameters.getRangeActionSpeed("rangeAction2").getRangeActionId());
    }

    @Test (expected = FaraoException.class)
    public void importNokTest() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-nok.json"));
    }

    @Test (expected = FaraoException.class)
    public void importNokTest2() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-nok-same-speed.json"));
    }

    @Test (expected = FaraoException.class)
    public void importNokTest3() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-nok3.json"));
    }

    @Test (expected = FaraoException.class)
    public void importNokTest4() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-nok4.json"));
    }

    @Test (expected = FaraoException.class)
    public void importNokTest5() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-nok5.json"));
    }

    @Test (expected = FaraoException.class)
    public void importNokTest6() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-nok-aligned.json"));
    }

    @Test
    public void testImportTimeseriesMrid() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-ok-timeseries.json"));

        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);

        assertEquals(Set.of("border1", "border2"), cimCracCreationParameters.getTimeseriesMrids());
    }

    @Test
    public void roundTripTestTimeseriesMrid() {
        // prepare parameters to export
        CracCreationParameters exportedParameters = new CracCreationParameters();
        CimCracCreationParameters exportedCimParameters = new CimCracCreationParameters();
        exportedCimParameters.setTimeseriesMrids(Set.of("ts1", "ts2"));
        exportedParameters.addExtension(CimCracCreationParameters.class, exportedCimParameters);

        // roundTrip
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(exportedParameters, os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(is);

        // test re-imported parameters
        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);
        assertEquals(Set.of("ts1", "ts2"), cimCracCreationParameters.getTimeseriesMrids());
    }
}
