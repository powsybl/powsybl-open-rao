package com.powsybl.openrao.data.cracio.cim.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.cracapi.parameters.JsonCracCreationParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
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
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String CURATIVE_INSTANT_ID = "curative";

    public static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("nok1", "When monitoring the preventive instant, no contingency can be defined."),
            Arguments.of("nok2", "A threshold is already defined for instant curative."),
            Arguments.of("nok3", String.format(Locale.ENGLISH, "Multiple thresholds for same nominalV (%.1f) defined", 400.)),
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

        assertNotNull(vParams.getMonitoredStatesAndThresholds().get(PREVENTIVE_INSTANT_ID));
        assertNull(vParams.getMonitoredStatesAndThresholds().get(PREVENTIVE_INSTANT_ID).getContingencyNames());
        assertEquals(2, vParams.getMonitoredStatesAndThresholds().get(PREVENTIVE_INSTANT_ID).getThresholdPerNominalV().size());
        assertEquals(new VoltageThreshold(Unit.KILOVOLT, 180., null), vParams.getMonitoredStatesAndThresholds().get(PREVENTIVE_INSTANT_ID).getThresholdPerNominalV().get(200.));
        assertEquals(new VoltageThreshold(Unit.KILOVOLT, 395., 430.), vParams.getMonitoredStatesAndThresholds().get(PREVENTIVE_INSTANT_ID).getThresholdPerNominalV().get(400.));

        assertNotNull(vParams.getMonitoredStatesAndThresholds().get(CURATIVE_INSTANT_ID));
        assertEquals(Set.of("N-1 ONE", "N-1 TWO"), vParams.getMonitoredStatesAndThresholds().get(CURATIVE_INSTANT_ID).getContingencyNames());
        assertEquals(2, vParams.getMonitoredStatesAndThresholds().get(CURATIVE_INSTANT_ID).getThresholdPerNominalV().size());
        assertEquals(new VoltageThreshold(Unit.KILOVOLT, null, 230.), vParams.getMonitoredStatesAndThresholds().get(CURATIVE_INSTANT_ID).getThresholdPerNominalV().get(210.));
        assertEquals(new VoltageThreshold(Unit.KILOVOLT, 380., 430.), vParams.getMonitoredStatesAndThresholds().get(CURATIVE_INSTANT_ID).getThresholdPerNominalV().get(400.));
    }

    @Test
    void roundTripOnVoltageMonitoringParameters() throws IOException {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-for-round-trip.json"));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(importedParameters, os);
        String exportedString = os.toString();

        InputStream inputStream = getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-for-round-trip.json");
        assertEquals(new String(Objects.requireNonNull(inputStream).readAllBytes()), exportedString);
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void importNokTest(String source, String message) {
        InputStream inputStream = getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-" + source + ".json");
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> JsonCracCreationParameters.read(inputStream));
        assertEquals(message, exception.getMessage());
    }
}
