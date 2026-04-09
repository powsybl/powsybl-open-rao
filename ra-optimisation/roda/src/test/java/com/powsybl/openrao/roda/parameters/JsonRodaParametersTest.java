/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.roda.parameters;

import com.powsybl.action.*;
import com.powsybl.commons.test.AbstractSerDeTest;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.ThreeSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static com.powsybl.action.PercentChangeLoadAction.QModificationStrategy.CONSTANT_Q;
import static com.powsybl.iidm.network.HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class JsonRodaParametersTest extends AbstractSerDeTest {
    @Test
    void testForcedActionsRoundTrip() throws IOException {
        List<Action> actions = new ArrayList<>();
        actions.add(new SwitchAction("id1", "switchId1", true));
        actions.add(new MultipleActionsAction("id2", Collections.singletonList(new SwitchAction("id3", "switchId2", true))));
        actions.add(new TerminalsConnectionAction("id3", "lineId3", true)); // both sides.
        actions.add(new TerminalsConnectionAction("id4", "lineId4", false)); // both sides.
        actions.add(new PhaseTapChangerTapPositionAction("id5", "transformerId1", true, 5, ThreeSides.TWO));
        actions.add(new PhaseTapChangerTapPositionAction("id6", "transformerId2", false, 12));
        actions.add(new PhaseTapChangerTapPositionAction("id7", "transformerId3", true, -5, ThreeSides.ONE));
        actions.add(new PhaseTapChangerTapPositionAction("id8", "transformerId3", false, 2, ThreeSides.THREE));
        actions.add(new GeneratorActionBuilder().withId("id9").withGeneratorId("generatorId1").withActivePowerRelativeValue(true).withActivePowerValue(100.0).build());
        actions.add(new GeneratorActionBuilder().withId("id10").withGeneratorId("generatorId2").withVoltageRegulatorOn(true).withTargetV(225.0).build());
        actions.add(new GeneratorActionBuilder().withId("id11").withGeneratorId("generatorId2").withVoltageRegulatorOn(false).withTargetQ(400.0).build());
        actions.add(new LoadActionBuilder().withId("id12").withLoadId("loadId1").withRelativeValue(false).withActivePowerValue(50.0).build());
        actions.add(new LoadActionBuilder().withId("id13").withLoadId("loadId1").withRelativeValue(true).withReactivePowerValue(5.0).build());
        actions.add(new PercentChangeLoadActionBuilder().withId("id26").withLoadId("loadId1").withP0PercentChange(5.0).withQModificationStrategy(CONSTANT_Q).build());
        actions.add(new BoundaryLineActionBuilder().withId("id17").withBoundaryLineId("dlId1").withRelativeValue(true).withReactivePowerValue(5.0).build());
        actions.add(new RatioTapChangerTapPositionAction("id14", "transformerId4", false, 2, ThreeSides.THREE));
        actions.add(new RatioTapChangerTapPositionAction("id15", "transformerId5", true, 1));
        actions.add(RatioTapChangerRegulationAction.activateRegulation("id16", "transformerId5", ThreeSides.THREE));
        actions.add(PhaseTapChangerRegulationAction.activateAndChangeRegulationMode("id17", "transformerId5", ThreeSides.ONE,
            PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL, 10.0));
        actions.add(PhaseTapChangerRegulationAction.deactivateRegulation("id18",
            "transformerId6", ThreeSides.ONE));
        actions.add(PhaseTapChangerRegulationAction.activateAndChangeRegulationMode("id19",
            "transformerId6", ThreeSides.ONE,
            PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL, 15.0));
        actions.add(RatioTapChangerRegulationAction.activateRegulationAndChangeTargetV("id20", "transformerId5", 90.0));
        actions.add(RatioTapChangerRegulationAction.deactivateRegulation("id21", "transformerId5", ThreeSides.THREE));
        actions.add(new HvdcActionBuilder()
            .withId("id22")
            .withHvdcId("hvdc1")
            .withAcEmulationEnabled(false)
            .build());
        actions.add(new HvdcActionBuilder()
            .withId("id23")
            .withHvdcId("hvdc2")
            .withAcEmulationEnabled(true)
            .build());
        actions.add(new HvdcActionBuilder()
            .withId("id24")
            .withHvdcId("hvdc2")
            .withAcEmulationEnabled(true)
            .withDroop(121.0)
            .withP0(42.0)
            .withConverterMode(SIDE_1_RECTIFIER_SIDE_2_INVERTER)
            .withRelativeValue(false)
            .build());
        actions.add(new HvdcActionBuilder()
            .withId("id25")
            .withHvdcId("hvdc1")
            .withAcEmulationEnabled(false)
            .withActivePowerSetpoint(12.0)
            .withRelativeValue(true)
            .build());
        actions.add(new ShuntCompensatorPositionActionBuilder().withId("id22").withShuntCompensatorId("shuntId1").withSectionCount(5).build());
        actions.add(new StaticVarCompensatorActionBuilder().withId("id23")
            .withStaticVarCompensatorId("svc").withRegulationMode(StaticVarCompensator.RegulationMode.VOLTAGE)
            .withVoltageSetpoint(56.0).build());
        actions.add(new StaticVarCompensatorActionBuilder().withId("id24")
            .withStaticVarCompensatorId("svc").withRegulationMode(StaticVarCompensator.RegulationMode.REACTIVE_POWER)
            .withReactivePowerSetpoint(120.0).build());
        actions.add(new TerminalsConnectionAction("id4", "transformerId25", ThreeSides.THREE, true)); // only one side.
        actions.add(new AreaInterchangeTargetAction("id99", "AreaA", 101.0));
        actions.add(new AreaInterchangeTargetAction("idDisabledTarget", "AreaA", Double.NaN));

        RaoParameters raoParameters = new RaoParameters();
        RodaParameters forcedActions = new RodaParameters(actions);
        raoParameters.addExtension(RodaParameters.class, forcedActions);

        roundTripTest(raoParameters, JsonRaoParameters::write, JsonRaoParameters::read, "/RaoParameters_with_ForcedActions.json");
    }

    @Test
    void testWrongForcedActions() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParameters_with_wrong_ForcedActions.json")));
        assertEquals("Unexpected token: wrong-key", exception.getMessage());
    }

    @Test
    void testEmptyForcedActions() {
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParameters_with_empty_ForcedActions.json"));
        RodaParameters forcedActions = raoParameters.getExtension(RodaParameters.class);
        assertNotNull(forcedActions);
        assertTrue(forcedActions.getForcedPreventiveActions().isEmpty());
    }
}
