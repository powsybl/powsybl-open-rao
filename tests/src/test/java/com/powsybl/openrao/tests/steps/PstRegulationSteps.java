/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.tests.steps;

import com.powsybl.openrao.pstregulation.PstRegulation;
import io.cucumber.java.en.When;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class PstRegulationSteps {
    @When("I launch PST regulation")
    public void launchPSTRegulation() {
        CommonTestData.setRaoResult(PstRegulation.regulatePsts(CommonTestData.getNetwork(), CommonTestData.getCrac(), CommonTestData.getRaoResult(), CommonTestData.getRaoParameters()));
    }
}
