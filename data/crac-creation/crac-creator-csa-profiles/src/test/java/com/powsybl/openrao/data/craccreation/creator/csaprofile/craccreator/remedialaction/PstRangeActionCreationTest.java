/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.cracimpl.OnContingencyStateImpl;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertRaNotImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PstRangeActionCreationTest {

    // TODO: create tests

    @Test
    void checkRaWithSeveralTapPositionIsNotAccepted() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/RaWithMultipleTapPositions.zip");
        assertRaNotImported(cracCreationContext, "pst-range-action-with-multiple-psts", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 'pst-range-action-with-multiple-psts' will not be imported because several TapPositionActions were defined for the same PST Range Action when only one is expected");
        assertRaNotImported(cracCreationContext, "pst-range-action-with-disabled-pst", ImportStatus.NOT_FOR_RAO, "Remedial action 'pst-range-action-with-disabled-pst' will not be imported because the field 'normalEnabled' in TapPositionAction is set to false");
        assertRaNotImported(cracCreationContext, "pst-range-action-with-multiple-max-taps", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action pst-range-action-with-multiple-max-taps will not be imported because StaticPropertyRange has more than ONE direction RelativeDirectionKind.up");
        assertRaNotImported(cracCreationContext, "pst-range-action-with-multiple-min-taps", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action pst-range-action-with-multiple-min-taps will not be imported because StaticPropertyRange has more than ONE direction RelativeDirectionKind.down");
    }

}
