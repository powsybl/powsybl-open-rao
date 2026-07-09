/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.fbconstraint.parameters;

import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.virtualhubs.HvdcConverter;
import com.powsybl.openrao.virtualhubs.HvdcLine;
import com.powsybl.openrao.virtualhubs.HvdcPole;
import com.powsybl.openrao.virtualhubs.InternalHvdc;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class JsonFbConstraintCracCreationParametersTest {
    private static final String POLE_ID = "EIC_OF_POLE";
    private static final String HVDC_EIC = "EIC_OF_HVDC";

    private static FbConstraintCracCreationParameters getFbConstraintCracCreationParameters() {
        final FbConstraintCracCreationParameters exportedFbConstraintParameters = new FbConstraintCracCreationParameters();
        exportedFbConstraintParameters.setTimestamp(OffsetDateTime.parse("2025-01-10T05:00:00Z"));
        exportedFbConstraintParameters.setIcsCostUp(30.0);
        exportedFbConstraintParameters.setIcsCostDown(15.0);

        final HvdcPole pole1 = new HvdcPole(POLE_ID, List.of(new HvdcConverter("node 1A", "station A"),
                                                             new HvdcConverter("node 1B", "station B")),
                                            List.of(new HvdcLine("node 1A", "node 1B")));
        final HvdcPole pole2 = new HvdcPole(POLE_ID, List.of(new HvdcConverter("node 2A", "station A"),
                                                             new HvdcConverter("node 2B", "station B")),
                                            List.of(new HvdcLine("node 2A", "node 2B")));

        exportedFbConstraintParameters.setInternalHvdcs(List.of(new InternalHvdc(HVDC_EIC, List.of(pole1, pole2))));
        return exportedFbConstraintParameters;
    }

    @Test
    void roundTripTest() {
        // prepare parameters to export
        final CracCreationParameters exportedParameters = new CracCreationParameters();
        final FbConstraintCracCreationParameters exportedFbConstraintParameters = getFbConstraintCracCreationParameters();
        exportedParameters.addExtension(FbConstraintCracCreationParameters.class, exportedFbConstraintParameters);

        // roundTrip
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(exportedParameters, os);
        final ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        final CracCreationParameters importedParameters = JsonCracCreationParameters.read(is);

        // test re-imported parameters
        final FbConstraintCracCreationParameters fbConstraintCracCreationParameters = importedParameters.getExtension(FbConstraintCracCreationParameters.class);
        Assertions.assertThat(fbConstraintCracCreationParameters).isNotNull();
        Assertions.assertThat(fbConstraintCracCreationParameters.getTimestamp()).isEqualTo(OffsetDateTime.parse("2025-01-10T05:00:00Z"));
        Assertions.assertThat(fbConstraintCracCreationParameters.getIcsCostUp()).isEqualTo(30.0);
        Assertions.assertThat(fbConstraintCracCreationParameters.getIcsCostDown()).isEqualTo(15.0);

        Assertions.assertThat(fbConstraintCracCreationParameters.getInternalHvdcs()).hasSize(1);

        final List<HvdcPole> poles = fbConstraintCracCreationParameters.getInternalHvdcs().getFirst().poles();
        Assertions.assertThat(poles).hasSize(2);

        final HvdcPole pole1 = poles.getFirst();
        final HvdcPole pole2 = poles.getLast();

        Assertions.assertThat(pole1.converters()).hasSize(2);
        Assertions.assertThat(pole1.converters().get(0))
            .hasFieldOrPropertyWithValue("node", "node 1A")
            .hasFieldOrPropertyWithValue("station", "station A");
        Assertions.assertThat(pole1.converters().get(1))
            .hasFieldOrPropertyWithValue("node", "node 1B")
            .hasFieldOrPropertyWithValue("station", "station B");
        Assertions.assertThat(pole1.lines()).hasSize(1);
        Assertions.assertThat(pole1.lines().getFirst())
            .hasFieldOrPropertyWithValue("from", "node 1A")
            .hasFieldOrPropertyWithValue("to", "node 1B");

        Assertions.assertThat(pole2.converters()).hasSize(2);
        Assertions.assertThat(pole2.converters().get(0))
            .hasFieldOrPropertyWithValue("node", "node 2A")
            .hasFieldOrPropertyWithValue("station", "station A");
        Assertions.assertThat(pole2.converters().get(1))
            .hasFieldOrPropertyWithValue("node", "node 2B")
            .hasFieldOrPropertyWithValue("station", "station B");
        Assertions.assertThat(pole2.lines()).hasSize(1);
        Assertions.assertThat(pole2.lines().getFirst())
            .hasFieldOrPropertyWithValue("from", "node 2A")
            .hasFieldOrPropertyWithValue("to", "node 2B");

    }

    @Test
    void exportTest() {
        final CracCreationParameters exportedParameters = new CracCreationParameters();
        final FbConstraintCracCreationParameters exportedFbConstraintParameters = getFbConstraintCracCreationParameters();
        exportedParameters.addExtension(FbConstraintCracCreationParameters.class, exportedFbConstraintParameters);

        // roundTrip
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(exportedParameters, os);

        Assertions.assertThat(os.toString().replace("\r\n", "\n")).isEqualTo("""
                                                                                    {
                                                                                      "crac-factory" : "CracImplFactory",
                                                                                      "default-monitored-line-side" : "monitor-lines-on-both-sides",
                                                                                      "ra-usage-limits-per-instant" : [ ],
                                                                                      "extensions" : {
                                                                                        "FbConstraintCracCreatorParameters" : {
                                                                                          "timestamp" : "2025-01-10T05:00:00Z",
                                                                                          "ics-cost-up" : 30.0,
                                                                                          "ics-cost-down" : 15.0,
                                                                                          "internal-hvdcs" : [ {
                                                                                            "eic" : "EIC_OF_HVDC",
                                                                                            "poles" : [ {
                                                                                              "id" : "EIC_OF_POLE",
                                                                                              "converters" : [ {
                                                                                                "node" : "node 1A",
                                                                                                "station" : "station A"
                                                                                              }, {
                                                                                                "node" : "node 1B",
                                                                                                "station" : "station B"
                                                                                              } ],
                                                                                              "lines" : [ {
                                                                                                "from" : "node 1A",
                                                                                                "to" : "node 1B"
                                                                                              } ]
                                                                                            }, {
                                                                                              "id" : "EIC_OF_POLE",
                                                                                              "converters" : [ {
                                                                                                "node" : "node 2A",
                                                                                                "station" : "station A"
                                                                                              }, {
                                                                                                "node" : "node 2B",
                                                                                                "station" : "station B"
                                                                                              } ],
                                                                                              "lines" : [ {
                                                                                                "from" : "node 2A",
                                                                                                "to" : "node 2B"
                                                                                              } ]
                                                                                            } ]
                                                                                          } ]
                                                                                        }
                                                                                      }
                                                                                    }""");
    }

    @Test
    void importOkTest() {
        final CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/fbconstraint-crac-creation-parameters_ok.json"));

        final FbConstraintCracCreationParameters fbConstraintCracCreationParameters = importedParameters.getExtension(FbConstraintCracCreationParameters.class);
        assertNotNull(fbConstraintCracCreationParameters);
        assertEquals(OffsetDateTime.parse("2025-01-10T05:00:00Z"), fbConstraintCracCreationParameters.getTimestamp());
        assertEquals(50.0, fbConstraintCracCreationParameters.getIcsCostUp());
        assertEquals(20.0, fbConstraintCracCreationParameters.getIcsCostDown());
    }

    @Test
    void importNokTest() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/parameters/fbconstraint-crac-creation-parameters_nok.json")) {
            assertThrows(DateTimeParseException.class, () -> JsonCracCreationParameters.read(inputStream));
        }
    }
}
