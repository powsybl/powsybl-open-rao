/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.action.Action;
import com.powsybl.action.AreaInterchangeTargetAction;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.HvdcAction;
import com.powsybl.action.LoadAction;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.RatioTapChangerTapPositionAction;
import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.action.SwitchAction;
import com.powsybl.action.TerminalsConnectionAction;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;

/**
 * Which powsybl actions OpenLoadFlow can simulate as operator strategy actions in a sensitivity analysis.
 *
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class OperatorStrategySupport {

    private OperatorStrategySupport() {
    }

    public record Mode(boolean dc, boolean distributedSlack) {

        public static Mode of(SensitivityAnalysisParameters parameters) {
            return new Mode(parameters.getLoadFlowParameters().isDc(), parameters.getLoadFlowParameters().isDistributedSlack());
        }

        @Override
        public String toString() {
            return (dc ? "DC" : "AC") + (distributedSlack ? " with distributed slack" : "");
        }
    }

    /**
     * PST tap, connection and switch actions are supported, plus generator and load actions unless in DC with a
     * distributed slack (Woodbury does not rerun the slack distribution after an injection change).
     * <p>Any other action (ratio tap changer, shunt, area interchange, HVDC...) is rejected by the OpenLoadFlow
     * sensitivity analysis whatever the mode: up to 2.4.0 it checks the actions against the Woodbury (fast DC) list even
     * in AC, and an HVDC action only disables the AC emulation without applying the active power setpoint.
     */
    public static boolean isSupported(Action action, Mode mode) {
        if (action instanceof PhaseTapChangerTapPositionAction || action instanceof TerminalsConnectionAction || action instanceof SwitchAction) {
            return true;
        }
        if (action instanceof GeneratorAction || action instanceof LoadAction) {
            return !(mode.dc() && mode.distributedSlack());
        }
        return false;
    }

    /**
     * Whether the element exists as OpenLoadFlow expects it: e.g. the half of a tie line is a dangling line, which a
     * connection action cannot open, whereas {@code NetworkAction.apply} can.
     */
    public static boolean existsInNetwork(Action action, Network network) {
        if (action instanceof TerminalsConnectionAction terminalsConnectionAction) {
            return network.getBranch(terminalsConnectionAction.getElementId()) != null || network.getThreeWindingsTransformer(terminalsConnectionAction.getElementId()) != null;
        }
        if (action instanceof SwitchAction switchAction) {
            return network.getSwitch(switchAction.getSwitchId()) != null;
        }
        if (action instanceof PhaseTapChangerTapPositionAction pstAction) {
            return network.getTwoWindingsTransformer(pstAction.getTransformerId()) != null || network.getThreeWindingsTransformer(pstAction.getTransformerId()) != null;
        }
        if (action instanceof RatioTapChangerTapPositionAction rtcAction) {
            return network.getTwoWindingsTransformer(rtcAction.getTransformerId()) != null || network.getThreeWindingsTransformer(rtcAction.getTransformerId()) != null;
        }
        if (action instanceof GeneratorAction generatorAction) {
            return network.getGenerator(generatorAction.getGeneratorId()) != null;
        }
        if (action instanceof LoadAction loadAction) {
            return network.getLoad(loadAction.getLoadId()) != null;
        }
        if (action instanceof HvdcAction hvdcAction) {
            return network.getHvdcLine(hvdcAction.getHvdcId()) != null;
        }
        if (action instanceof ShuntCompensatorPositionAction shuntAction) {
            return network.getShuntCompensator(shuntAction.getShuntCompensatorId()) != null;
        }
        if (action instanceof AreaInterchangeTargetAction areaAction) {
            return network.getArea(areaAction.getAreaId()) != null;
        }
        return false;
    }

    public static boolean isSupported(Action action, Network network, Mode mode) {
        return isSupported(action, mode) && existsInNetwork(action, network);
    }
}
