/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.cim.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class JsonCimCracCreationParametersTest {

    @Test
    void roundTripTest() {
        // prepare parameters to export
        CracCreationParameters exportedParameters = new CracCreationParameters();
        CimCracCreationParameters exportedCimParameters = new CimCracCreationParameters();
        exportedCimParameters.setRangeActionGroupsAsString(List.of("rangeAction3 + rangeAction4", "hvdc1 + hvdc2"));
        exportedCimParameters.setRemedialActionSpeed(Set.of(new RangeActionSpeed("rangeAction1", 1)));
        exportedCimParameters.setTimestamp(OffsetDateTime.parse("2025-01-10T05:00:00Z"));
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
        assertEquals(OffsetDateTime.parse("2025-01-10T05:00:00Z"), cimCracCreationParameters.getTimestamp());
        assertTrue(cimCracCreationParameters.getTimeseriesMrids().isEmpty());
    }

    @Test
    void importOkTest() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-ok.json"));

        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);

        assertEquals(2, cimCracCreationParameters.getRangeActionGroupsAsString().size());
        assertEquals("rangeAction3 + rangeAction4", cimCracCreationParameters.getRangeActionGroupsAsString().get(0));
        assertEquals("hvdc1 + hvdc2", cimCracCreationParameters.getRangeActionGroupsAsString().get(1));
    }

    @Test
    void importOkTest2() {
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
    void importOkTest3() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-ok-aligned.json"));

        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);

        assertEquals(2, cimCracCreationParameters.getRangeActionSpeedSet().size());
        assert cimCracCreationParameters.getRangeActionSpeed("rangeAction1").getSpeed().equals(1);
        assert cimCracCreationParameters.getRangeActionSpeed("rangeAction2").getSpeed().equals(1);
        assertEquals("rangeAction1", cimCracCreationParameters.getRangeActionSpeed("rangeAction1").getRangeActionId());
        assertEquals("rangeAction2", cimCracCreationParameters.getRangeActionSpeed("rangeAction2").getRangeActionId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nok", "nok-same-speed", "nok3", "nok4", "nok5", "nok-aligned"})
    void importNokTest(String source) {
        InputStream inputStream = getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-" + source + ".json");
        assertThrows(OpenRaoException.class, () -> JsonCracCreationParameters.read(inputStream));
    }

    @Test
    void testImportTimeseriesMrid() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-ok-timeseries.json"));

        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);

        assertEquals(Set.of("border1", "border2"), cimCracCreationParameters.getTimeseriesMrids());
    }

    @Test
    void roundTripTestTimeseriesMrid() {
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
