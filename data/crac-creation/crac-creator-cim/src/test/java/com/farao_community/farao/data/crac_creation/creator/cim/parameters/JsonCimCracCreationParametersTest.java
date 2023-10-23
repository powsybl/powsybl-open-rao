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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class JsonCimCracCreationParametersTest {

    public static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("nok", "Unexpected field: unknown-field"),
            Arguments.of("nok-same-speed", "Range action rangeAction1 has a speed 1 already defined"),
            Arguments.of("nok3", "Range action rangeAction1 has two or more associated speed"),
            Arguments.of("nok4", "Missing range action ID in range-action-speeds"),
            Arguments.of("nok5", "Missing speed in range-action-speeds"),
            Arguments.of("nok-aligned", "Range actions rangeAction1 and rangeAction2 are aligned but have different speeds (1 and 2)")
        );
    }

    @Test
    void roundTripTest() {
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
    @MethodSource("provideParameters")
    void importNokTest(String source, String message) {
        InputStream inputStream = getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-" + source + ".json");
        FaraoException exception = assertThrows(FaraoException.class, () -> JsonCracCreationParameters.read(inputStream));
        assertEquals(message, exception.getMessage());
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
