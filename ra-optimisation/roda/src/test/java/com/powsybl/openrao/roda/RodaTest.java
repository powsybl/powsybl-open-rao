/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.roda;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.action.Action;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.TerminalsConnectionAction;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.logs.RaoBusinessWarns;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.roda.parameters.RodaParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RodaTest {
    Network network;
    TemporalData<RaoInput> raoInput;

    @BeforeEach
    void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        String variantId = network.getVariantManager().getWorkingVariantId();
        raoInput = new TemporalDataImpl<>(
            Map.of(OffsetDateTime.now(), RaoInput.buildWithPreventiveState(network, crac)
                .withNetworkVariantId(variantId)
                .build()));
    }

    @Test
    void testApplyForcedActions() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RaoBusinessWarns.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        List<ILoggingEvent> logsList = listAppender.list;

        Action action1 = new PhaseTapChangerTapPositionAction("action1", "BBE2AA1  BBE3AA1  1", false, -8);
        Action action2 = new TerminalsConnectionAction("action2", "FFR1AA1  FFR2AA1  1", true);
        Action action3 = new TerminalsConnectionAction("wrong_action", "wrong_id", true);
        RodaParameters rodaParameters = new RodaParameters(List.of(action1, action2, action3));
        Roda.applyForcedActions(raoInput, rodaParameters);
        assertEquals(-8, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
        assertFalse(network.getLine("FFR1AA1  FFR2AA1  1").getTerminal1().isConnected());
        assertFalse(network.getLine("FFR1AA1  FFR2AA1  1").getTerminal2().isConnected());
        assertTrue(logsList.stream().anyMatch(e -> e.getMessage().contains("Action 'wrong_action' could not be applied.")));
    }

    @Test
    void testApplyForcedActionsNullOrEmpty() {
        assertDoesNotThrow(() -> Roda.applyForcedActions(raoInput, null));
        assertDoesNotThrow(() -> Roda.applyForcedActions(raoInput, new RodaParameters(List.of())));
    }
}
