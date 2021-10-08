/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.JsonCracCreationParameters;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonCseCracCreationParametersTest {

    private void checkBusBarChangeSwitchesContent(CseCracCreationParameters parameters, String remedialActionId, List<String> switchesToOpen, List<String> switchesToClose) {
        assertNotNull(parameters.getBusBarChangeSwitches(remedialActionId));
        assertEquals(remedialActionId, parameters.getBusBarChangeSwitches(remedialActionId).getRemedialActionId());
        assertEquals(switchesToOpen, parameters.getBusBarChangeSwitches(remedialActionId).getSwitchesToOpen());
        assertEquals(switchesToClose, parameters.getBusBarChangeSwitches(remedialActionId).getSwitchesToClose());
    }

    @Test
    public void roundTripTest() {
        // prepare parameters to export
        CracCreationParameters exportedParameters = new CracCreationParameters();
        CseCracCreationParameters exportedCseParameters = new CseCracCreationParameters();
        exportedCseParameters.setRangeActionGroupsAsString(List.of("rangeAction3 + rangeAction4", "hvdc1 + hvdc2"));

        exportedCseParameters.setBusBarChangeSwitchesSet(Set.of(
            new BusBarChangeSwitches("ra1", List.of("s1", "s2"), List.of("s3", "s4", "s5")),
            new BusBarChangeSwitches("ra2", List.of(), List.of("s1")),
            new BusBarChangeSwitches("ra3", List.of("s2"), List.of())
        ));

        exportedParameters.addExtension(CseCracCreationParameters.class, exportedCseParameters);

        // roundTrip
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(exportedParameters, os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(is);

        // test re-imported parameters
        CseCracCreationParameters cseCracCreationParameters = importedParameters.getExtension(CseCracCreationParameters.class);
        assertNotNull(cseCracCreationParameters);
        assertEquals(2, cseCracCreationParameters.getRangeActionGroupsAsString().size());
        assertEquals("rangeAction3 + rangeAction4", cseCracCreationParameters.getRangeActionGroupsAsString().get(0));
        assertEquals("hvdc1 + hvdc2", cseCracCreationParameters.getRangeActionGroupsAsString().get(1));

        assertEquals(3, cseCracCreationParameters.getBusBarChangeSwitchesSet().size());
        checkBusBarChangeSwitchesContent(cseCracCreationParameters, "ra1", List.of("s1", "s2"), List.of("s3", "s4", "s5"));
        checkBusBarChangeSwitchesContent(cseCracCreationParameters, "ra2", List.of(), List.of("s1"));
        checkBusBarChangeSwitchesContent(cseCracCreationParameters, "ra3", List.of("s2"), List.of());
    }

    @Test
    public void importOkTest() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cse-crac-creation-parameters-ok.json"));

        CseCracCreationParameters cseCracCreationParameters = importedParameters.getExtension(CseCracCreationParameters.class);
        assertNotNull(cseCracCreationParameters);

        assertEquals(2, cseCracCreationParameters.getRangeActionGroupsAsString().size());
        assertEquals("rangeAction3 + rangeAction4", cseCracCreationParameters.getRangeActionGroupsAsString().get(0));
        assertEquals("hvdc1 + hvdc2", cseCracCreationParameters.getRangeActionGroupsAsString().get(1));

        assertEquals(3, cseCracCreationParameters.getBusBarChangeSwitchesSet().size());
        checkBusBarChangeSwitchesContent(cseCracCreationParameters, "remedialAction1", List.of("switch1", "switch2", "switch3"), List.of("switch4"));
        checkBusBarChangeSwitchesContent(cseCracCreationParameters, "remedialAction2", List.of(), List.of("switch6", "switch2"));
        checkBusBarChangeSwitchesContent(cseCracCreationParameters, "remedialAction3", List.of("switch8", "switch3", "switch1"), List.of());
    }

    @Test (expected = FaraoException.class)
    public void importNokTest() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cse-crac-creation-parameters-nok.json"));
    }

    @Test (expected = FaraoException.class)
    public void importNokTest2() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cse-crac-creation-parameters-nok2.json"));
    }

    @Test (expected = FaraoException.class)
    public void importNokTest3() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cse-crac-creation-parameters-nok3.json"));
    }
}
