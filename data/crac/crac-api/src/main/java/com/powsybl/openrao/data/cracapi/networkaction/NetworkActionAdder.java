/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracapi.networkaction;

import com.powsybl.openrao.data.cracapi.RemedialActionAdder;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface NetworkActionAdder extends RemedialActionAdder<NetworkActionAdder> {

    TerminalsConnectionActionAdder newTerminalsConnectionAction();

    SwitchActionAdder newSwitchAction();

    PhaseTapChangerTapPositionActionAdder newPhaseTapChangerTapPositionAction();

    GeneratorActionAdder newGeneratorAction();

    LoadActionAdder newLoadAction();

    DanglingLineActionAdder newDanglingLineAction();

    ShuntCompensatorPositionActionAdder newShuntCompensatorPositionAction();

    SwitchPairAdder newSwitchPair();

    NetworkAction add();
}
