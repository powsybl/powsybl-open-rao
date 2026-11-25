/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Maxence Fleury {@literal <maxence.fleury at artelys.com>}
 */
class RaoParametersTest {

    @Test
    void copyProducesDistinctNestedParameters() {
        RaoParameters original = new RaoParameters(ReportNode.NO_OP);
        RaoParameters copy = original.copy();

        assertNotSame(original, copy);
        assertNotSame(original.getObjectiveFunctionParameters(), copy.getObjectiveFunctionParameters());
        assertNotSame(original.getRangeActionsOptimizationParameters(), copy.getRangeActionsOptimizationParameters());
        assertNotSame(original.getTopoOptimizationParameters(), copy.getTopoOptimizationParameters());
        assertNotSame(original.getNotOptimizedCnecsParameters(), copy.getNotOptimizedCnecsParameters());

        assertEquals(original.getObjectiveFunctionParameters().getType(), copy.getObjectiveFunctionParameters().getType());
        assertEquals(original.getTopoOptimizationParameters().getRelativeMinImpactThreshold(), copy.getTopoOptimizationParameters().getRelativeMinImpactThreshold());
    }

    @Test
    void mutatingCopyMutableListDoesNotAffectOriginal() {
        RaoParameters original = new RaoParameters(ReportNode.NO_OP);
        original.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(ReportNode.NO_OP));

        RaoParameters copy = original.copy();
        OpenRaoSearchTreeParameters originalExtension = original.getExtension(OpenRaoSearchTreeParameters.class);
        OpenRaoSearchTreeParameters copyExtension = copy.getExtension(OpenRaoSearchTreeParameters.class);
        copyExtension.getTopoOptimizationParameters().setPredefinedCombinations(List.of(List.of("na1", "na2")));

        assertTrue(originalExtension.getTopoOptimizationParameters().getPredefinedCombinations().isEmpty());
        assertEquals(1, copyExtension.getTopoOptimizationParameters().getPredefinedCombinations().size());
    }

    @Test
    void copyOfOpenRaoSearchTreeParametersExtensionIsIndependent() {
        RaoParameters original = new RaoParameters(ReportNode.NO_OP);
        OpenRaoSearchTreeParameters originalExtension = new OpenRaoSearchTreeParameters(ReportNode.NO_OP);
        originalExtension.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().setDc(false);
        original.addExtension(OpenRaoSearchTreeParameters.class, originalExtension);

        RaoParameters copy = original.copy();
        OpenRaoSearchTreeParameters copyExtension = copy.getExtension(OpenRaoSearchTreeParameters.class);

        assertNotSame(originalExtension, copyExtension);
        assertNotSame(originalExtension.getLoadFlowAndSensitivityParameters(), copyExtension.getLoadFlowAndSensitivityParameters());
        assertNotSame(
            originalExtension.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters(),
            copyExtension.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters());

        copyExtension.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().setDc(true);

        assertFalse(originalExtension.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().isDc());
        assertTrue(copyExtension.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().isDc());
    }

    private static final class UnsupportedExtension extends AbstractExtension<RaoParameters> {
        @Override
        public String getName() {
            return "unsupported-extension";
        }
    }

    @Test
    void copyDropsExtensionTypeWithoutJsonSerializer() {
        RaoParameters original = new RaoParameters(ReportNode.NO_OP);
        original.addExtension(UnsupportedExtension.class, new UnsupportedExtension());

        RaoParameters copy = original.copy();
        assertNull(copy.getExtension(UnsupportedExtension.class));
    }
}
