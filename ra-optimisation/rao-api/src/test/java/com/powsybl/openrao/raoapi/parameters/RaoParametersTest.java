/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

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

    @Test
    void copyOfFullyPopulatedParametersPreservesEveryField() {
        RaoParameters original = new RaoParameters(ReportNode.NO_OP);
        original.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST);
        original.getRangeActionsOptimizationParameters().setPstRAMinImpactThreshold(0.42);
        original.getTopoOptimizationParameters().setRelativeMinImpactThreshold(0.11);
        original.getTopoOptimizationParameters().setAbsoluteMinImpactThreshold(0.22);
        original.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(true);
        MnecParameters originalMnec = new MnecParameters();
        originalMnec.setAcceptableMarginDecrease(0.33);
        original.setMnecParameters(originalMnec);
        original.setRelativeMarginsParameters(new RelativeMarginsParameters());
        LoopFlowParameters originalLoopFlow = new LoopFlowParameters();
        originalLoopFlow.setAcceptableIncrease(1.44);
        original.setLoopFlowParameters(originalLoopFlow);

        RaoParameters copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST, copy.getObjectiveFunctionParameters().getType());
        assertEquals(0.42, copy.getRangeActionsOptimizationParameters().getPstRAMinImpactThreshold());
        assertEquals(0.11, copy.getTopoOptimizationParameters().getRelativeMinImpactThreshold());
        assertEquals(0.22, copy.getTopoOptimizationParameters().getAbsoluteMinImpactThreshold());
        assertTrue(copy.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras());
        assertTrue(copy.getMnecParameters().isPresent());
        assertEquals(0.33, copy.getMnecParameters().get().getAcceptableMarginDecrease());
        assertTrue(copy.getRelativeMarginsParameters().isPresent());
        assertTrue(copy.getLoopFlowParameters().isPresent());
        assertEquals(1.44, copy.getLoopFlowParameters().get().getAcceptableIncrease());
    }

    @Test
    void mutatingOriginalAfterCopyDoesNotAffectCopy() {
        RaoParameters original = new RaoParameters(ReportNode.NO_OP);
        MnecParameters originalMnec = new MnecParameters();
        originalMnec.setAcceptableMarginDecrease(0.1);
        original.setMnecParameters(originalMnec);

        RaoParameters copy = original.copy();
        originalMnec.setAcceptableMarginDecrease(0.9);
        original.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST);

        assertEquals(0.1, copy.getMnecParameters().orElseThrow().getAcceptableMarginDecrease());
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW, copy.getObjectiveFunctionParameters().getType());
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

    @Test
    void copyThrowsWhenTheRoundTripFails() {
        RaoParameters original = new RaoParameters(ReportNode.NO_OP);

        try (MockedStatic<JsonRaoParameters> jsonRaoParameters = mockStatic(JsonRaoParameters.class)) {
            jsonRaoParameters.when(() -> JsonRaoParameters.write(any(), any(OutputStream.class), any(ReportNode.class)))
                .thenAnswer(invocation -> {
                    throw new IOException("simulated serialization failure");
                });

            OpenRaoException e = assertThrows(OpenRaoException.class, original::copy);
            assertEquals("Cannot deep copy RaoParameters: the JSON round trip failed", e.getMessage());
        }
    }
}
