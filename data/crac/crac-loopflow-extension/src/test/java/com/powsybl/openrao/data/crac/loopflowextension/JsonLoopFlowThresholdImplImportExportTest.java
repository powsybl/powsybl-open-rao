/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.loopflowextension;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.impl.CracImplFactory;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class JsonLoopFlowThresholdImplImportExportTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";

    @Test
    void roundTripTest() {
        Crac crac = new CracImplFactory().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);

        crac.newFlowCnec()
                .withId("cnec1")
                .withNetworkElement("ne1Id")
                .withInstant(PREVENTIVE_INSTANT_ID)
                .newThreshold().withSide(TwoSides.ONE).withUnit(Unit.AMPERE).withMin(-500.).add()
                .withNominalVoltage(380.)
                .add()
                .newExtension(LoopFlowThresholdAdder.class).withValue(100).withUnit(Unit.AMPERE).add();

        crac.newFlowCnec()
                .withId("cnec2")
                .withNetworkElement("ne2Id")
                .withInstant(PREVENTIVE_INSTANT_ID)
                .newThreshold().withSide(TwoSides.ONE).withUnit(Unit.PERCENT_IMAX).withMin(-0.3).add()
                .withNominalVoltage(380.)
                .withIMax(5000.)
                .add()
                .newExtension(LoopFlowThresholdAdder.class).withValue(.3).withUnit(Unit.PERCENT_IMAX).add();

        crac.newFlowCnec()
                .withId("cnec3")
                .withNetworkElement("ne3Id")
                .withInstant(PREVENTIVE_INSTANT_ID)
                .newThreshold().withSide(TwoSides.ONE).withUnit(Unit.MEGAWATT).withMin(-700.).withMax(700.).add()
                .add();

        // export Crac
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        crac.write("JSON", outputStream);

        // create network
        Network network = NetworkImportsUtil.createNetworkForJsonRetrocompatibilityTest(0.0);

        // import Crac
        Crac importedCrac;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            importedCrac = Crac.read("crac.json", inputStream, network);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // test
        assertNotNull(importedCrac.getFlowCnec("cnec1").getExtension(LoopFlowThreshold.class));
        assertEquals(100, importedCrac.getFlowCnec("cnec1").getExtension(LoopFlowThreshold.class).getValue(), 0.1);
        assertEquals(Unit.AMPERE, importedCrac.getFlowCnec("cnec1").getExtension(LoopFlowThreshold.class).getUnit());

        assertNotNull(importedCrac.getFlowCnec("cnec2").getExtension(LoopFlowThreshold.class));
        assertEquals(.3, importedCrac.getFlowCnec("cnec2").getExtension(LoopFlowThreshold.class).getValue(), 0.1);
        assertEquals(Unit.PERCENT_IMAX, importedCrac.getFlowCnec("cnec2").getExtension(LoopFlowThreshold.class).getUnit());

        assertNull(importedCrac.getFlowCnec("cnec3").getExtension(LoopFlowThreshold.class));
    }
}
