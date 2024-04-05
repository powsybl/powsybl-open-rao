/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class NetworkActionCombinationsUtils {
    public static final String PREVENTIVE_INSTANT_ID = "preventive";

    public static final Crac crac;
    public static final Network network;
    public static final State pState;

    public static final NetworkAction naFr1;
    public static final NetworkAction naBe1;
    public static final PstRangeAction raBe1;

    public static final NetworkActionCombination indFr1;
    public static final NetworkActionCombination indFr2;
    public static final NetworkActionCombination indBe1;
    public static final NetworkActionCombination indBe2;
    public static final NetworkActionCombination indNl1;
    public static final NetworkActionCombination indDe1;
    public static final NetworkActionCombination indFrDe;
    public static final NetworkActionCombination indNlBe;
    public static final NetworkActionCombination indDeNl;

    public static final NetworkActionCombination comb3Fr;
    public static final NetworkActionCombination comb2Fr;
    public static final NetworkActionCombination comb3Be;
    public static final NetworkActionCombination comb2De;
    public static final NetworkActionCombination comb2BeNl;
    public static final NetworkActionCombination comb2FrNl;
    public static final NetworkActionCombination comb2FrDeBe;
    public static final NetworkActionCombination comb3FrNlBe;

    static {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        pState = crac.getPreventiveState();

        crac.newFlowCnec()
            .withId("cnecBe")
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .withInstant(PREVENTIVE_INSTANT_ID).withOptimized(true)
            .withOperator("operator1").newThreshold()
            .withUnit(Unit.MEGAWATT)
            .withSide(Side.LEFT)
            .withMin(-1500.)
            .withMax(1500.)
            .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();

        naFr1 = createNetworkActionWithOperator("FFR1AA1  FFR2AA1  1", "fr");
        naBe1 = createNetworkActionWithOperator("BBE1AA1  BBE2AA1  1", "be");
        raBe1 = createPstRangeActionWithOperator("BBE2AA1  BBE3AA1  1", "be");

        NetworkAction naFr2 = createNetworkActionWithOperator("FFR1AA1  FFR3AA1  1", "fr");
        NetworkAction naFr3 = createNetworkActionWithOperator("FFR2AA1  FFR3AA1  1", "fr");
        NetworkAction naBe2 = createNetworkActionWithOperator("BBE1AA1  BBE3AA1  1", "be");
        NetworkAction naBe3 = createNetworkActionWithOperator("BBE2AA1  BBE3AA1  1", "be");
        NetworkAction naNl1 = createNetworkActionWithOperator("NNL1AA1  NNL2AA1  1", "nl");
        NetworkAction naDe1 = createNetworkActionWithOperator("DDE1AA1  DDE3AA1  1", "de");
        NetworkAction naDe2 = createNetworkActionWithOperator("DDE2AA1  DDE3AA1  1", "de");
        NetworkAction naFrDe = createNetworkActionWithOperator("FFR2AA1  DDE3AA1  1", "fr");
        NetworkAction naNlBe = createNetworkActionWithOperator("NNL2AA1  BBE3AA1  1", "nl");
        NetworkAction naDeNl = createNetworkActionWithOperator("DDE2AA1  NNL3AA1  1", "de");

        indFr1 = new NetworkActionCombination(naFr1);
        indFr2 = new NetworkActionCombination(naFr2);
        indBe1 = new NetworkActionCombination(naBe1);
        indBe2 = new NetworkActionCombination(naBe2);
        indNl1 = new NetworkActionCombination(naNl1);
        indDe1 = new NetworkActionCombination(naDe1);
        indFrDe = new NetworkActionCombination(naFrDe);
        indNlBe = new NetworkActionCombination(naNlBe);
        indDeNl = new NetworkActionCombination(naDeNl);

        comb3Fr = new NetworkActionCombination(Set.of(naFr1, naFr2, naFr3));
        comb2Fr = new NetworkActionCombination(Set.of(naFr2, naFr3));
        comb3Be = new NetworkActionCombination(Set.of(naBe1, naBe2, naBe3));
        comb2De = new NetworkActionCombination(Set.of(naDe1, naDe2));
        comb2BeNl = new NetworkActionCombination(Set.of(naBe1, naNl1));
        comb2FrNl = new NetworkActionCombination(Set.of(naFr2, naNl1));
        comb2FrDeBe = new NetworkActionCombination(Set.of(naFrDe, naBe1));
        comb3FrNlBe = new NetworkActionCombination(Set.of(naFr2, naBe2, naNlBe));
    }

    static NetworkAction createNetworkActionWithOperator(String networkElementId, String operator) {
        return crac.newNetworkAction().withId("na - " + networkElementId).withOperator(operator).newTopologicalAction().withNetworkElement(networkElementId).withActionType(ActionType.OPEN).add().add();
    }

    static PstRangeAction createPstRangeActionWithOperator(String networkElementId, String operator) {
        Map<Integer, Double> conversionMap = new HashMap<>();
        conversionMap.put(0, 0.);
        conversionMap.put(1, 1.);
        return crac.newPstRangeAction().withId("pst - " + networkElementId).withOperator(operator).withNetworkElement(networkElementId).newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add().newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(-16).withMaxTap(16).add().withInitialTap(0).withTapToAngleConversionMap(conversionMap).add();
    }
}
