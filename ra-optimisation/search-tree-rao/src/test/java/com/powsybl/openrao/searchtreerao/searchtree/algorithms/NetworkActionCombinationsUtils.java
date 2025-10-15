/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class NetworkActionCombinationsUtils {
    private NetworkActionCombinationsUtils() { }

    public static final String PREVENTIVE_INSTANT_ID = "preventive";

    public static final Crac CRAC = initCrac();
    public static final Network NETWORK = NetworkImportsUtil.import12NodesNetwork();
    public static final State P_STATE = CRAC.getPreventiveState();

    public static final NetworkAction NA_FR_1 = createNetworkActionWithOperator("FFR1AA1  FFR2AA1  1", "fr");
    public static final NetworkAction NA_BE_1 = createNetworkActionWithOperator("BBE1AA1  BBE2AA1  1", "be");
    public static final PstRangeAction RA_BE_1 = createPstRangeActionWithOperator("BBE2AA1  BBE3AA1  1", "be");

    private static final NetworkAction NA_FR_2 = createNetworkActionWithOperator("FFR1AA1  FFR3AA1  1", "fr");
    private static final NetworkAction NA_FR_3 = createNetworkActionWithOperator("FFR2AA1  FFR3AA1  1", "fr");
    private static final NetworkAction NA_BE_2 = createNetworkActionWithOperator("BBE1AA1  BBE3AA1  1", "be");
    private static final NetworkAction NA_BE_3 = createNetworkActionWithOperator("BBE2AA1  BBE3AA1  1", "be");
    private static final NetworkAction NA_NL_1 = createNetworkActionWithOperator("NNL1AA1  NNL2AA1  1", "nl");
    private static final NetworkAction NA_DE_1 = createNetworkActionWithOperator("DDE1AA1  DDE3AA1  1", "de");
    private static final NetworkAction NA_DE_2 = createNetworkActionWithOperator("DDE2AA1  DDE3AA1  1", "de");
    private static final NetworkAction NA_FR_DE = createNetworkActionWithOperator("FFR2AA1  DDE3AA1  1", "fr");
    private static final NetworkAction NA_NL_BE = createNetworkActionWithOperator("NNL2AA1  BBE3AA1  1", "nl");
    private static final NetworkAction NA_DE_NL = createNetworkActionWithOperator("DDE2AA1  NNL3AA1  1", "de");

    public static final NetworkActionCombination IND_FR_1 = new NetworkActionCombination(NA_FR_1);
    public static final NetworkActionCombination IND_FR_2 = new NetworkActionCombination(NA_FR_2);
    public static final NetworkActionCombination IND_BE_1 = new NetworkActionCombination(NA_BE_1);
    public static final NetworkActionCombination IND_BE_2 = new NetworkActionCombination(NA_BE_2);
    public static final NetworkActionCombination IND_NL_1 = new NetworkActionCombination(NA_NL_1);
    public static final NetworkActionCombination IND_DE_1 = new NetworkActionCombination(NA_DE_1);
    public static final NetworkActionCombination IND_FR_DE = new NetworkActionCombination(NA_FR_DE);
    public static final NetworkActionCombination IND_NL_BE = new NetworkActionCombination(NA_NL_BE);
    public static final NetworkActionCombination IND_DE_NL = new NetworkActionCombination(NA_DE_NL);

    public static final NetworkActionCombination COMB_3_FR = new NetworkActionCombination(Set.of(NA_FR_1, NA_FR_2, NA_FR_3));
    public static final NetworkActionCombination COMB_2_FR = new NetworkActionCombination(Set.of(NA_FR_2, NA_FR_3));
    public static final NetworkActionCombination COMB_3_BE = new NetworkActionCombination(Set.of(NA_BE_1, NA_BE_2, NA_BE_3));
    public static final NetworkActionCombination COMB_2_DE = new NetworkActionCombination(Set.of(NA_DE_1, NA_DE_2));
    public static final NetworkActionCombination COMB_2_BE_NL = new NetworkActionCombination(Set.of(NA_BE_1, NA_NL_1));
    public static final NetworkActionCombination COMB_2_FR_NL = new NetworkActionCombination(Set.of(NA_FR_2, NA_NL_1));
    public static final NetworkActionCombination COMB_2_FR_DE_BE = new NetworkActionCombination(Set.of(NA_FR_DE, NA_BE_1));
    public static final NetworkActionCombination COMB_3_FR_NL_BE = new NetworkActionCombination(Set.of(NA_FR_2, NA_BE_2, NA_NL_BE));

    private static Crac initCrac() {
        Crac crac = CommonCracCreation.create();
        crac.newFlowCnec()
            .withId("cnecBe")
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .withInstant(PREVENTIVE_INSTANT_ID).withOptimized(true)
            .withOperator("operator1").newThreshold()
            .withUnit(Unit.MEGAWATT)
            .withSide(TwoSides.ONE)
            .withMin(-1500.)
            .withMax(1500.)
            .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();
        return crac;
    }

    static NetworkAction createNetworkActionWithOperator(String networkElementId, String operator) {
        return CRAC.newNetworkAction().withId("na - " + networkElementId).withOperator(operator).newTerminalsConnectionAction().withNetworkElement(networkElementId).withActionType(ActionType.OPEN).add().add();
    }

    static PstRangeAction createPstRangeActionWithOperator(String networkElementId, String operator) {
        Map<Integer, Double> conversionMap = new HashMap<>();
        conversionMap.put(0, 0.);
        conversionMap.put(1, 1.);
        return CRAC.newPstRangeAction().withId("pst - " + networkElementId).withOperator(operator).withNetworkElement(networkElementId).newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add().newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(-16).withMaxTap(16).add().withInitialTap(0).withTapToAngleConversionMap(conversionMap).add();
    }

    static PstRangeAction addPstRangeActionToCrac() {
        CommonCracCreation.IidmPstHelper iidmPstHelper = new CommonCracCreation.IidmPstHelper("BBE2AA1  BBE3AA1  1", NETWORK);

        Crac crac = CommonCracCreation.create();
        crac.newPstRangeAction()
            .withId("pst-range-action")
            .withName("pst-range-action")
            .withOperator("BE")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(iidmPstHelper.getInitialTap())
            .withTapToAngleConversionMap(iidmPstHelper.getTapToAngleConversionMap())
            .add();

        return crac.getPstRangeAction("pst-range-action");
    }
}
