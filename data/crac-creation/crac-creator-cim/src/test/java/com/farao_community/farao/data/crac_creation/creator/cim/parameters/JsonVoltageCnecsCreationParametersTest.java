package com.farao_community.farao.data.crac_creation.creator.cim.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.JsonCracCreationParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class JsonVoltageCnecsCreationParametersTest {
    public static Stream<Arguments> provideParameters() {
        return Stream.of(
            //Arguments.of("nok1", ""), // Disabled test with a state with an instant id "preventive" and some contingencies. But we don't know this state is a preventive one in the importer.
            Arguments.of("nok2", "A threshold is already defined for instant curative."),
            Arguments.of("nok3", "Multiple thresholds for same nominalV (400.0) defined"),
            Arguments.of("nok4", "Unhandled unit in voltage monitoring: ampere"),
            Arguments.of("nok5", "At least one monitored element and one monitored state with thresholds should be defined."),
            Arguments.of("nok6", "At least one monitored element and one monitored state with thresholds should be defined."),
            Arguments.of("nok7", "At least one threshold should be defined."),
            Arguments.of("nok8", "Field nominalV for thresholds-per-nominal-v should be defined."),
            Arguments.of("nok9", "Could not deserialize monitored-states-and-thresholds")
        );
    }

    @Test
    void testImportVoltageCnecs() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters.json"));

        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);

        VoltageCnecsCreationParameters vParams = cimCracCreationParameters.getVoltageCnecsCreationParameters();
        assertNotNull(vParams);
        assertEquals(Set.of("ne1", "ne2"), vParams.getMonitoredNetworkElements());
        assertEquals(2, vParams.getMonitoredStatesAndThresholds().size());

        assertNotNull(vParams.getMonitoredStatesAndThresholds().get("preventive"));
        assertNull(vParams.getMonitoredStatesAndThresholds().get("preventive").getContingencyNames());
        assertEquals(2, vParams.getMonitoredStatesAndThresholds().get("preventive").getThresholdPerNominalV().size());
        assertEquals(new VoltageThreshold(Unit.KILOVOLT, 180., null), vParams.getMonitoredStatesAndThresholds().get("preventive").getThresholdPerNominalV().get(200.));
        assertEquals(new VoltageThreshold(Unit.KILOVOLT, 395., 430.), vParams.getMonitoredStatesAndThresholds().get("preventive").getThresholdPerNominalV().get(400.));

        assertNotNull(vParams.getMonitoredStatesAndThresholds().get("curative"));
        assertEquals(Set.of("N-1 ONE", "N-1 TWO"), vParams.getMonitoredStatesAndThresholds().get("curative").getContingencyNames());
        assertEquals(2, vParams.getMonitoredStatesAndThresholds().get("curative").getThresholdPerNominalV().size());
        assertEquals(new VoltageThreshold(Unit.KILOVOLT, null, 230.), vParams.getMonitoredStatesAndThresholds().get("curative").getThresholdPerNominalV().get(210.));
        assertEquals(new VoltageThreshold(Unit.KILOVOLT, 380., 430.), vParams.getMonitoredStatesAndThresholds().get("curative").getThresholdPerNominalV().get(400.));
    }

    @Test
    void roundTripOnVoltageMonitoringParameters() throws IOException {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-for-round-trip.json"));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(importedParameters, os);
        String exportedString = os.toString();

        InputStream inputStream = getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-for-round-trip.json");
        assertEquals(new String(Objects.requireNonNull(inputStream).readAllBytes()).replaceAll("\r", ""), exportedString.replaceAll("\r", ""));
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void importNokTest(String source, String message) {
        InputStream inputStream = getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-" + source + ".json");
        FaraoException exception = assertThrows(FaraoException.class, () -> JsonCracCreationParameters.read(inputStream));
        assertEquals(message, exception.getMessage());
    }
}
