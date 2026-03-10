/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class ReportUtilsTest {
    @Test
    void testFormatDoubleBasedOnMarginWithPositiveMargin() {
        double margin = 1.2; // margin > 0, formatDoubleBasedOnMargin to default number of decimals = 2;
        assertEquals("10.0", ReportUtils.formatDoubleBasedOnMargin(10., margin));
        assertEquals("-53.63", ReportUtils.formatDoubleBasedOnMargin(-53.634, margin));
        assertEquals("-53.64", ReportUtils.formatDoubleBasedOnMargin(-53.635, margin));
        assertEquals("-infinity", ReportUtils.formatDoubleBasedOnMargin(-Double.MAX_VALUE, margin));
        assertEquals("+infinity", ReportUtils.formatDoubleBasedOnMargin(Double.MAX_VALUE, margin));
        assertEquals("-infinity", ReportUtils.formatDoubleBasedOnMargin(-179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.00, margin));
        assertEquals("+infinity", ReportUtils.formatDoubleBasedOnMargin(179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.00, margin));
    }

    @Test
    void testFormatDoubleBasedOnMarginWithNegativeMargin() {
        double margin = -0.0004; // -1 < margin < 0, formatDoubleBasedOnMargin depending on margin
        assertEquals("10.0", ReportUtils.formatDoubleBasedOnMargin(10., margin));
        assertEquals("-53.634", ReportUtils.formatDoubleBasedOnMargin(-53.634, margin));
        assertEquals("-53.6354", ReportUtils.formatDoubleBasedOnMargin(-53.63535, margin));
        assertEquals("-infinity", ReportUtils.formatDoubleBasedOnMargin(-Double.MAX_VALUE, margin));
        assertEquals("+infinity", ReportUtils.formatDoubleBasedOnMargin(Double.MAX_VALUE, margin));
        assertEquals("-infinity", ReportUtils.formatDoubleBasedOnMargin(-179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.00, margin));
        assertEquals("+infinity", ReportUtils.formatDoubleBasedOnMargin(179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.00, margin));
    }
}
