package com.farao_community.farao.data.crac_creation.creator.cim.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.JsonCracCreationParameters;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class JsonVoltageCnecsCreationParametersTest {
    @Test
    public void testImportVoltageCnecs() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters.json"));

        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);

        VoltageCnecsCreationParameters vParams = cimCracCreationParameters.getVoltageCnecsCreationParameters();
        assertNotNull(vParams);
        assertEquals(Set.of("ne1", "ne2"), vParams.getMonitoredNetworkElements());
        assertEquals(2, vParams.getMonitoredStatesAndThresholds().size());

        assertNotNull(vParams.getMonitoredStatesAndThresholds().get(Instant.PREVENTIVE));
        assertNull(vParams.getMonitoredStatesAndThresholds().get(Instant.PREVENTIVE).getContingencyNames());
        assertEquals(2, vParams.getMonitoredStatesAndThresholds().get(Instant.PREVENTIVE).getThresholdPerNominalV().size());
        assertEquals(new ThresholdImpl(Unit.KILOVOLT, 180., null), vParams.getMonitoredStatesAndThresholds().get(Instant.PREVENTIVE).getThresholdPerNominalV().get(200.));
        assertEquals(new ThresholdImpl(Unit.KILOVOLT, 395., 430.), vParams.getMonitoredStatesAndThresholds().get(Instant.PREVENTIVE).getThresholdPerNominalV().get(400.));

        assertNotNull(vParams.getMonitoredStatesAndThresholds().get(Instant.CURATIVE));
        assertEquals(Set.of("N-1 ONE", "N-1 TWO"), vParams.getMonitoredStatesAndThresholds().get(Instant.CURATIVE).getContingencyNames());
        assertEquals(2, vParams.getMonitoredStatesAndThresholds().get(Instant.CURATIVE).getThresholdPerNominalV().size());
        assertEquals(new ThresholdImpl(Unit.KILOVOLT, null, 230.), vParams.getMonitoredStatesAndThresholds().get(Instant.CURATIVE).getThresholdPerNominalV().get(210.));
        assertEquals(new ThresholdImpl(Unit.KILOVOLT, 380., 430.), vParams.getMonitoredStatesAndThresholds().get(Instant.CURATIVE).getThresholdPerNominalV().get(400.));
    }

    @Test
    public void roundTripOnVoltageMonitoringParameters() throws IOException {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-for-round-trip.json"));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(importedParameters, os);
        String exportedString = os.toString();

        InputStream inputStream = getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-for-round-trip.json");
        assertEquals(new String(inputStream.readAllBytes()), exportedString);
    }

    @Test (expected = FaraoException.class)
    public void testFailIfPreventiveWithContingencies() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-nok1.json"));
    }

    @Test (expected = FaraoException.class)
    public void testFailIfInstantDefinedMultipleTimes() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-nok2.json"));
    }

    @Test (expected = FaraoException.class)
    public void testFailIfNominalVDefinedMultipleTimes() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-nok3.json"));
    }

    @Test (expected = FaraoException.class)
    public void testFailIfUnitNotKv() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-nok4.json"));
    }

    @Test (expected = FaraoException.class)
    public void testFailIfNoMonitoredElements() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-nok5.json"));
    }

    @Test (expected = FaraoException.class)
    public void testFailIfNoMonitoredStates() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-nok6.json"));
    }

    @Test (expected = FaraoException.class)
    public void testFailIfNoThresholds() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/voltage-cnecs-creation-parameters-nok7.json"));
    }
}
