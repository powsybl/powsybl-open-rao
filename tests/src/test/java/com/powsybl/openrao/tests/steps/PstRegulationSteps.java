/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.tests.steps;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.pstregulation.PstRegulation;
import com.powsybl.openrao.tests.utils.RaoUtils;
import io.cucumber.java.en.When;

import java.io.IOException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class PstRegulationSteps {
    @When("I launch PST regulation")
    public void launchPSTRegulation() throws IOException {
        CommonTestData.setRaoResult(
            RaoUtils.roundTripOnRaoResult(
                PstRegulation.regulatePsts(CommonTestData.getNetwork(), CommonTestData.getCrac(), CommonTestData.getRaoResult(), CommonTestData.getRaoParameters(), CommonTestData.getReportNode()),
                CommonTestData.getCrac()
            )
        );
    }
}
