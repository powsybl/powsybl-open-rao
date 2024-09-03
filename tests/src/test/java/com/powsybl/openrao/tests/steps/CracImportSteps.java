/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.steps;

import com.powsybl.action.*;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.*;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.range.StandardRange;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.threshold.BranchThreshold;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracapi.usagerule.*;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.UcteCracCreationContext;
import com.powsybl.openrao.data.cracio.cse.CseCracCreationContext;
import com.powsybl.openrao.data.cracio.fbconstraint.FbConstraintCreationContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Marjorie Cosson {@literal <marjorie.cosson at rte-france.com>}
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CracImportSteps {

    private static final double DOUBLE_TOLERANCE = 1e-1;
    private static final String NOT_IMPLEMENTED_JSON = "This step is not implemented for farao-native crac import";
    private static final String NOT_IMPLEMENTED_FB = "This step is not implemented for FbConstraintCreationContext";
    private static final String TYPE_NOT_HANDLED = "%s type is not handled";
    private Crac crac;
    private CracCreationContext cracCreationContext;

    @When("I import crac")
    public void iImportCrac() throws IOException {
        importData(null);
    }

    @When("I import crac at {string}")
    public void iImportCrac(String timestamp) throws IOException {
        importData(timestamp);
    }

    private void importData(String timestamp) throws IOException {
        CommonTestData.loadData(timestamp);
        this.crac = CommonTestData.getCrac();
        this.cracCreationContext = CommonTestData.getCracCreationContext();
    }

    @Then("its name should be {string}")
    public void itsNameShouldBe(String expectedName) {
        assertEquals(expectedName, crac.getName());
    }

    @Then("its id should be {string}")
    public void itsIdShouldBe(String expectedId) {
        assertEquals(expectedId, crac.getId());
    }

    @Then("all the contingencies should be imported successfully")
    public void allContingenciesMustBeImported() {
        if (cracCreationContext == null) {
            throw new NotImplementedException(NOT_IMPLEMENTED_JSON);
        } else if (cracCreationContext instanceof FbConstraintCreationContext) {
            throw new NotImplementedException(NOT_IMPLEMENTED_FB);
        } else if (cracCreationContext instanceof CseCracCreationContext cseCracCreationContext) {
            assertTrue(cseCracCreationContext.getOutageCreationContexts().stream().allMatch(ElementaryCreationContext::isImported));
        } else {
            throw new NotImplementedException(String.format(TYPE_NOT_HANDLED, cracCreationContext.getClass().getName()));
        }
    }

    @Then("the native contingency {string} should create the contingency {string}")
    public void mapContingency(String nativeContingencyId, String createdContingencyId) {
        if (cracCreationContext == null) {
            throw new NotImplementedException(NOT_IMPLEMENTED_JSON);
        } else if (cracCreationContext instanceof FbConstraintCreationContext) {
            throw new NotImplementedException(NOT_IMPLEMENTED_FB);
        } else if (cracCreationContext instanceof CseCracCreationContext cseCracCreationContext) {
            ElementaryCreationContext creationContext = cseCracCreationContext.getOutageCreationContext(nativeContingencyId);
            assertNotNull(creationContext);
            assertTrue(creationContext.isImported());
            assertEquals(createdContingencyId, creationContext.getNativeObjectName());
        } else {
            throw new NotImplementedException(String.format(TYPE_NOT_HANDLED, cracCreationContext.getClass().getName()));
        }
    }

    @Then("the native contingency {string} should not be imported")
    public void contingencyShouldNotBeImported(String nativeContingencyId) {
        if (cracCreationContext == null) {
            throw new NotImplementedException(NOT_IMPLEMENTED_JSON);
        } else if (cracCreationContext instanceof FbConstraintCreationContext) {
            throw new NotImplementedException(NOT_IMPLEMENTED_FB);
        } else if (cracCreationContext instanceof CseCracCreationContext cseCracCreationContext) {
            ElementaryCreationContext creationContext = cseCracCreationContext.getOutageCreationContext(nativeContingencyId);
            assertNotNull(creationContext);
            assertFalse(creationContext.isImported());
            assertNull(creationContext.getCreatedObjectId());
        } else {
            throw new NotImplementedException(String.format(TYPE_NOT_HANDLED, cracCreationContext.getClass().getName()));
        }
    }

    @Then("it should have {int} contingencies")
    public void itShouldHaveContingencies(int expectedContingencyNumber) {
        assertEquals(expectedContingencyNumber, crac.getContingencies().size());
    }

    @Then("it should have the following contingencies:")
    public void itShouldHaveTheFollowingContingencies(DataTable arg1) {
        List<Map<String, String>> expectedContingencies = arg1.asMaps(String.class, String.class);
        int number = expectedContingencies.stream().map(stringStringMap -> stringStringMap.get("ContingencyId")).collect(Collectors.toSet()).size();
        assertEquals(number, crac.getContingencies().size());
        for (Map<String, String> expectedContingency : expectedContingencies) {
            Contingency contingency = crac.getContingency(expectedContingency.get("ContingencyId"));
            assertNotNull(contingency);
            assertEquals(Optional.of(expectedContingency.get("ContingencyName")), contingency.getName());
            assertEquals(Integer.parseInt(expectedContingency.get("NetworkElements")), contingency.getElements().size());
            assertTrue(contingency.getElements().stream().anyMatch(ne -> ne.getId().equals(expectedContingency.get("NetworkElementId"))));
        }
    }

    @Then("all the CNECs should be imported successfully")
    public void allCnecsMustBeImported() {
        if (cracCreationContext == null) {
            throw new NotImplementedException(NOT_IMPLEMENTED_JSON);
        } else if (cracCreationContext instanceof UcteCracCreationContext ucteCracCreationContext) {
            assertTrue(ucteCracCreationContext.getBranchCnecCreationContexts().stream().allMatch(BranchCnecCreationContext::isImported));
        } else {
            throw new NotImplementedException(String.format(TYPE_NOT_HANDLED, cracCreationContext.getClass().getName()));
        }
    }

    @Then("the native CNEC {string} from {string} to {string} with suffix {string} should create the CNEC {string} at instant {string}")
    public void mapNativeCnec(String nativeCnecId, String nativeFrom, String nativeTo, String nativeSuffix, String createdCnecId, String instantId) {
        if (cracCreationContext == null) {
            throw new NotImplementedException(NOT_IMPLEMENTED_JSON);
        } else if (cracCreationContext instanceof UcteCracCreationContext ucteCracCreationContext) {
            BranchCnecCreationContext branchCnecCreationContext = ucteCracCreationContext.getBranchCnecCreationContext(nativeCnecId);
            assertNotNull(branchCnecCreationContext);
            assertTrue(branchCnecCreationContext.isImported());
            assertEquals(nativeFrom, branchCnecCreationContext.getNativeBranch().getFrom());
            assertEquals(nativeTo, branchCnecCreationContext.getNativeBranch().getTo());
            assertEquals(nativeSuffix, branchCnecCreationContext.getNativeBranch().getSuffix());
            assertEquals(createdCnecId, branchCnecCreationContext.getCreatedCnecsIds().get(instantId.toLowerCase()));
        } else {
            throw new NotImplementedException(String.format("%s type is not handled by this step", cracCreationContext.getClass().getName()));
        }
    }

    @Then("the native CNEC {string} should create the CNEC {string} at instant {string}")
    public void mapNativeCnec(String nativeCnecId, String createdCnecId, String instantId) {
        if (cracCreationContext == null) {
            throw new NotImplementedException(NOT_IMPLEMENTED_JSON);
        } else if (cracCreationContext instanceof UcteCracCreationContext ucteCracCreationContext) {
            BranchCnecCreationContext branchCnecCreationContext = ucteCracCreationContext.getBranchCnecCreationContext(nativeCnecId);
            assertNotNull(branchCnecCreationContext);
            assertTrue(branchCnecCreationContext.isImported());
            assertEquals(createdCnecId, branchCnecCreationContext.getCreatedCnecsIds().get(instantId.toLowerCase()));
        } else {
            throw new NotImplementedException(String.format(TYPE_NOT_HANDLED, cracCreationContext.getClass().getName()));
        }
    }

    @Then("the native CNEC {string} should not be imported")
    public void cnecShouldNotBeImported(String nativeCnecId) {
        if (cracCreationContext == null) {
            throw new NotImplementedException(NOT_IMPLEMENTED_JSON);
        } else if (cracCreationContext instanceof UcteCracCreationContext ucteCracCreationContext) {
            BranchCnecCreationContext branchCnecCreationContext = ucteCracCreationContext.getBranchCnecCreationContext(nativeCnecId);
            assertNotNull(branchCnecCreationContext);
            assertFalse(branchCnecCreationContext.isImported());
            assertTrue(branchCnecCreationContext.getCreatedCnecsIds().isEmpty());
        } else {
            throw new NotImplementedException(String.format(TYPE_NOT_HANDLED, cracCreationContext.getClass().getName()));
        }
    }

    @Then("it should have the following flow CNECs:")
    public void itShouldHaveTheFollowingCNECs(DataTable arg1) throws Exception {
        List<Map<String, String>> expectedCnecs = arg1.asMaps(String.class, String.class);
        assertEquals(expectedCnecs.size(), crac.getFlowCnecs().size());
        for (Map<String, String> expectedCnec : expectedCnecs) {
            String cnecName = expectedCnec.get("Name");
            String networkElementId = expectedCnec.get("NetworkElementId");
            String contingency = expectedCnec.get("Contingency");
            Instant instant = crac.getInstant(expectedCnec.get("Instant").toLowerCase());
            boolean optimized = expectedCnec.get("Optimized").equalsIgnoreCase("yes");
            boolean monitored = expectedCnec.get("Monitored").equalsIgnoreCase("yes");

            Set<FlowCnec> cnecs;
            if (instant.isPreventive()) {
                cnecs = crac.getFlowCnecs(crac.getPreventiveState());
            } else {
                cnecs = crac.getFlowCnecs(crac.getState(contingency, instant));
            }
            FlowCnec flowCnec = cnecs.stream()
                .filter(cnec -> cnec.getName()
                    .equals(cnecName))
                .filter(cnec -> cnec.getNetworkElement().getId()
                    .equals(networkElementId))
                .filter(cnec -> cnec.isOptimized() == optimized)
                .filter(cnec -> cnec.isMonitored() == monitored)
                .findFirst()
                .orElseThrow(Exception::new);

            if (expectedCnec.get("ImaxLeft") != null) {
                assertEquals(Double.parseDouble(expectedCnec.get("ImaxLeft")), flowCnec.getIMax(TwoSides.ONE), DOUBLE_TOLERANCE);
            } else {
                assertNull(flowCnec.getIMax(TwoSides.ONE));
            }
            if (expectedCnec.get("ImaxRight") != null) {
                assertEquals(Double.parseDouble(expectedCnec.get("ImaxRight")), flowCnec.getIMax(TwoSides.TWO), DOUBLE_TOLERANCE);
            } else {
                assertNull(flowCnec.getIMax(TwoSides.TWO));
            }
            if (expectedCnec.get("NominalVoltageLeft") != null) {
                assertEquals(Double.parseDouble(expectedCnec.get("NominalVoltageLeft")), flowCnec.getNominalVoltage(TwoSides.ONE), DOUBLE_TOLERANCE);
            } else {
                assertNull(flowCnec.getNominalVoltage(TwoSides.ONE));
            }
            if (expectedCnec.get("NominalVoltageRight") != null) {
                assertEquals(Double.parseDouble(expectedCnec.get("NominalVoltageRight")), flowCnec.getNominalVoltage(TwoSides.TWO), DOUBLE_TOLERANCE);
            } else {
                assertNull(flowCnec.getNominalVoltage(TwoSides.TWO));
            }
        }
    }

    @Then("the flow cnecs should have the following thresholds:")
    public void theFlowCnecsShouldHaveTheFollowingThresholds(DataTable arg1) {
        List<Map<String, String>> expectedThresholds = arg1.asMaps(String.class, String.class);
        for (Map<String, String> expectedThreshold : expectedThresholds) {
            BranchCnec<?> branchCnec = crac.getFlowCnec(expectedThreshold.get("CnecId"));
            assertNotNull(branchCnec);
            Unit expectedUnit = Unit.valueOf(expectedThreshold.get("Unit"));
            TwoSides side = TwoSides.valueOf(expectedThreshold.get("Side"));
            Optional<Double> min = Optional.ofNullable(expectedThreshold.get("Min").equals("None") ? null : Double.parseDouble(expectedThreshold.get("Min")));
            Optional<Double> max = Optional.ofNullable(expectedThreshold.get("Max").equals("None") ? null : Double.parseDouble(expectedThreshold.get("Max")));
            assertTrue(branchCnec.getThresholds().stream().anyMatch(threshold -> matchThreshold(threshold, expectedUnit, side, min, max)));
        }
    }

    private boolean matchThreshold(BranchThreshold threshold, Unit unit, TwoSides side, Optional<Double> min, Optional<Double> max) {
        if (!unit.equals(threshold.getUnit())) {
            return false;
        }
        if (!side.equals(threshold.getSide())) {
            return false;
        }
        if (min.isPresent()) {
            if (threshold.min().isEmpty() || Math.abs(min.get() - threshold.min().get()) > DOUBLE_TOLERANCE) {
                return false;
            }
        } else {
            if (threshold.min().isPresent()) {
                return false;
            }
        }
        if (max.isPresent()) {
            return threshold.max().isPresent() && Math.abs(max.get() - threshold.max().get()) < DOUBLE_TOLERANCE;
        } else {
            return threshold.max().isEmpty();
        }
    }

    @Then("it should have the following angle CNECs:")
    public void itShouldHaveTheFollowingAngleCNECs(DataTable arg1) throws Exception {
        List<Map<String, String>> expectedCnecs = arg1.asMaps(String.class, String.class);
        assertEquals(expectedCnecs.size(), crac.getAngleCnecs().size());
        for (Map<String, String> expectedCnec : expectedCnecs) {
            String cnecId = expectedCnec.get("AngleCnecId");
            String cnecName = expectedCnec.get("Name");
            String importingElementId = expectedCnec.get("ImportingElementId");
            String exportingElementId = expectedCnec.get("ExportingElementId");
            String contingency = expectedCnec.get("Contingency");
            String instant = expectedCnec.get("Instant");
            boolean optimized = expectedCnec.get("Optimized").equalsIgnoreCase("yes");
            boolean monitored = expectedCnec.get("Monitored").equalsIgnoreCase("yes");

            Set<AngleCnec> cnecs = crac.getAngleCnecs(crac.getPreventiveState());
            cnecs.addAll(crac.getAngleCnecs(crac.getState(contingency, crac.getInstant(InstantKind.CURATIVE))));

            AngleCnec angleCnec = cnecs.stream()
                .filter(cnec -> cnec.getId()
                    .equals(cnecId))
                .filter(cnec -> cnec.getName()
                    .equals(cnecName))
                .filter(cnec -> cnec.getImportingNetworkElement().getId()
                    .equals(importingElementId))
                .filter(cnec -> cnec.getExportingNetworkElement().getId()
                    .equals(exportingElementId))
                .filter(cnec -> cnec.isOptimized() == optimized)
                .filter(cnec -> cnec.isMonitored() == monitored)
                .filter(cnec -> cnec.getState().getInstant().toString().equalsIgnoreCase(instant))
                .findFirst()
                .orElseThrow(Exception::new);

            Set<String> angleCnecsNetworkElementIds = angleCnec.getNetworkElements().stream().map(Identifiable::getId).collect(Collectors.toSet());
            assert angleCnecsNetworkElementIds.containsAll(Set.of(importingElementId, exportingElementId))
                && Set.of(importingElementId, exportingElementId).containsAll(angleCnecsNetworkElementIds);

            if (expectedCnec.get("LowerBound") != null) {
                Optional<Double> lowerBound = angleCnec.getLowerBound(Unit.DEGREE);
                if (lowerBound.isPresent()) {
                    assertEquals(Double.parseDouble(expectedCnec.get("LowerBound")), lowerBound.get(), DOUBLE_TOLERANCE);
                } else {
                    assertEquals("null", expectedCnec.get("LowerBound"));
                }
            }
            if (expectedCnec.get("UpperBound") != null) {
                Optional<Double> upperBound = angleCnec.getUpperBound(Unit.DEGREE);
                if (upperBound.isPresent()) {
                    assertEquals(Double.parseDouble(expectedCnec.get("UpperBound")), upperBound.get(), DOUBLE_TOLERANCE);
                } else {
                    assertEquals("null", expectedCnec.get("UpperBound"));
                }
            }
        }
    }

    @Then("it should have the following voltage CNECs:")
    public void itShouldHaveTheFollowingVoltageCNECs(DataTable arg1) {
        List<Map<String, String>> expectedCnecs = arg1.asMaps(String.class, String.class);
        assertEquals(expectedCnecs.size(), crac.getVoltageCnecs().size());
        for (Map<String, String> expectedCnec : expectedCnecs) {
            String cnecId = expectedCnec.get("VoltageCnecId");
            String networkElementId = expectedCnec.get("NetworkElementId");
            Instant instant = crac.getInstant(expectedCnec.get("Instant").toLowerCase());
            String contingency = expectedCnec.get("Contingency");
            String contingencyName = expectedCnec.get("ContingencyName");
            Double min = expectedCnec.get("Min").equals("null") ? null : Double.valueOf(expectedCnec.get("Min"));
            Double max = expectedCnec.get("Max").equals("null") ? null : Double.valueOf(expectedCnec.get("Max"));

            VoltageCnec voltageCnec = crac.getVoltageCnec(cnecId);
            assertEquals(networkElementId, voltageCnec.getNetworkElement().getId());
            if (instant.isPreventive()) {
                assertEquals(crac.getPreventiveState(), voltageCnec.getState());
                assertNull(contingencyName);
            } else {
                assertNotNull(contingencyName);
                assertEquals(crac.getState(contingency, instant), voltageCnec.getState());
                if (voltageCnec.getState().getContingency().isPresent()) {
                    assertEquals(Optional.of(contingencyName), voltageCnec.getState().getContingency().get().getName());
                } else {
                    throw new OpenRaoException("Contingency should be defined");
                }
            }
            assertEquals(1, voltageCnec.getThresholds().size());
            assertEquals(Optional.ofNullable(min), voltageCnec.getThresholds().iterator().next().min());
            assertEquals(Optional.ofNullable(max), voltageCnec.getThresholds().iterator().next().max());
        }
    }

    @Then("all the remedial actions should be imported successfully")
    public void allRasMustBeImported() {
        if (cracCreationContext == null) {
            throw new NotImplementedException(NOT_IMPLEMENTED_JSON);
        } else if (cracCreationContext instanceof UcteCracCreationContext ucteCracCreationContext) {
            assertTrue(ucteCracCreationContext.getRemedialActionCreationContexts().stream().allMatch(ElementaryCreationContext::isImported));
        } else {
            throw new NotImplementedException(String.format(TYPE_NOT_HANDLED, cracCreationContext.getClass().getName()));
        }
    }

    @Then("the native remedial action {string} should create the remedial action {string}")
    public void mapRemedialAction(String nativeRaId, String createdRaId) {
        if (cracCreationContext == null) {
            throw new NotImplementedException(NOT_IMPLEMENTED_JSON);
        } else if (cracCreationContext instanceof UcteCracCreationContext ucteCracCreationContext) {
            ElementaryCreationContext remedialActionCreationContext = ucteCracCreationContext.getRemedialActionCreationContext(nativeRaId);
            assertNotNull(remedialActionCreationContext);
            assertTrue(remedialActionCreationContext.isImported());
            assertEquals(createdRaId, remedialActionCreationContext.getCreatedObjectId());
        } else {
            throw new NotImplementedException(String.format(TYPE_NOT_HANDLED, cracCreationContext.getClass().getName()));
        }
    }

    @Then("the native remedial action {string} should not be imported because of {string}")
    public void raShouldNotBeImported(String nativeRaId, String importStatus) {
        if (cracCreationContext == null) {
            throw new NotImplementedException(NOT_IMPLEMENTED_JSON);
        } else if (cracCreationContext instanceof UcteCracCreationContext ucteCracCreationContext) {
            ElementaryCreationContext remedialActionCreationContext = ucteCracCreationContext.getRemedialActionCreationContext(nativeRaId);
            assertNotNull(remedialActionCreationContext);
            assertFalse(remedialActionCreationContext.isImported());
            assertNull(remedialActionCreationContext.getCreatedObjectId());
            assertEquals(ImportStatus.valueOf(importStatus), remedialActionCreationContext.getImportStatus());
        } else {
            throw new NotImplementedException(String.format(TYPE_NOT_HANDLED, cracCreationContext.getClass().getName()));
        }
    }

    @Then("it should have the following PST range actions:")
    public void itShouldHaveTheFollowingPstRangeActions(DataTable arg1) {
        List<Map<String, String>> expectedPsts = arg1.asMaps(String.class, String.class);

        int number = expectedPsts.stream().map(stringStringMap -> stringStringMap.get("PstRangeActionId")).collect(Collectors.toSet()).size();
        assertEquals(number, crac.getPstRangeActions().size());

        for (Map<String, String> expectedPst : expectedPsts) {
            String id = expectedPst.get("PstRangeActionId");
            PstRangeAction pstRangeAction = crac.getPstRangeAction(id);
            assertNotNull(pstRangeAction);
            assertEquals(expectedPst.get("PstRangeActionName"), pstRangeAction.getName());
            assertEquals(expectedPst.get("NetworkElementId"), pstRangeAction.getNetworkElement().getId());
            assertEquals(Integer.parseInt(expectedPst.get("InitialTap")), pstRangeAction.getInitialTap());
            int minTap = Collections.min(pstRangeAction.getTapToAngleConversionMap().keySet());
            int maxTap = Collections.max(pstRangeAction.getTapToAngleConversionMap().keySet());
            assertEquals(Integer.parseInt(expectedPst.get("MinTap")), minTap);
            assertEquals(Integer.parseInt(expectedPst.get("MaxTap")), maxTap);
            assertEquals(Double.parseDouble(expectedPst.get("MinTapAngle")), pstRangeAction.convertTapToAngle(minTap), DOUBLE_TOLERANCE);
            assertEquals(Double.parseDouble(expectedPst.get("MaxTapAngle")), pstRangeAction.convertTapToAngle(maxTap), DOUBLE_TOLERANCE);
        }
    }

    @Then("it should have the following injection range actions:")
    public void itShouldHaveTheFollowingInjectionRangeActions(DataTable arg1) {
        List<Map<String, String>> expectedRAs = arg1.asMaps(String.class, String.class);

        int number = expectedRAs.stream().map(stringStringMap -> stringStringMap.get("RangeActionId")).collect(Collectors.toSet()).size();
        assertEquals(number, crac.getInjectionRangeActions().size());

        for (Map<String, String> expectedRA : expectedRAs) {
            String id = expectedRA.get("RangeActionId");
            InjectionRangeAction rangeAction = crac.getInjectionRangeAction(id);
            assertNotNull(rangeAction);
            assertEquals(expectedRA.get("RangeActionName"), rangeAction.getName());
            String minRange = rangeAction.getRanges().stream().mapToDouble(StandardRange::getMin).max().isPresent() ? String.valueOf(rangeAction.getRanges().stream().mapToDouble(StandardRange::getMin).max().getAsDouble()) : "null";
            String maxRange = rangeAction.getRanges().stream().mapToDouble(StandardRange::getMax).min().isPresent() ? String.valueOf(rangeAction.getRanges().stream().mapToDouble(StandardRange::getMax).min().getAsDouble()) : "null";
            assertEquals(expectedRA.get("MaxRange"), maxRange);
            assertEquals(expectedRA.get("MinRange"), minRange);
            String groupId = rangeAction.getGroupId().isPresent() ? rangeAction.getGroupId().get() : "null";
            assertEquals(expectedRA.get("GroupId"), groupId);
        }
    }

    @Then("it should have the following HVDC range actions:")
    public void itShouldHaveTheFollowingHvdcRangeActions(DataTable arg1) {
        List<Map<String, String>> expectedRAs = arg1.asMaps(String.class, String.class);

        int number = expectedRAs.stream().map(stringStringMap -> stringStringMap.get("HvdcRangeActionId")).collect(Collectors.toSet()).size();
        assertEquals(number, crac.getHvdcRangeActions().size());

        for (Map<String, String> expectedRA : expectedRAs) {
            String id = expectedRA.get("HvdcRangeActionId");
            HvdcRangeAction rangeAction = crac.getHvdcRangeAction(id);
            assertNotNull(rangeAction);
            assertEquals(expectedRA.get("HvdcRangeActionName"), rangeAction.getName());
            assertEquals(expectedRA.get("NetworkElementId"), rangeAction.getNetworkElement().getId());
            String groupId = rangeAction.getGroupId().isPresent() ? rangeAction.getGroupId().get() : "null";
            assertEquals(expectedRA.get("GroupId"), groupId);
            assertEquals(Double.parseDouble(expectedRA.get("InitialSetpoint")), rangeAction.getInitialSetpoint(), DOUBLE_TOLERANCE);
        }
    }

    @Then("the PST range actions should have the following ranges:")
    public void thePstsShouldHaveRanges(DataTable arg1) {
        List<Map<String, String>> expectedPsts = arg1.asMaps(String.class, String.class);

        int number = expectedPsts.stream().map(stringStringMap -> stringStringMap.get("PstRangeActionId")).collect(Collectors.toSet()).size();
        assertEquals(number, crac.getPstRangeActions().size());

        for (Map<String, String> expectedPst : expectedPsts) {
            String id = expectedPst.get("PstRangeActionId");
            PstRangeAction pstRangeAction = crac.getPstRangeAction(id);
            assertNotNull(pstRangeAction);
            assertEquals(Integer.parseInt(expectedPst.get("Ranges")), pstRangeAction.getRanges().size());
            if (!pstRangeAction.getRanges().isEmpty()) {
                RangeType rangeType = RangeType.valueOf(expectedPst.get("RangeType"));
                assertTrue(pstRangeAction.getRanges().stream().anyMatch(range ->
                    range.getUnit().equals(Unit.TAP)
                        && range.getRangeType().equals(rangeType)
                        && range.getMinTap() == Integer.parseInt(expectedPst.get("MinTap"))
                        && range.getMaxTap() == Integer.parseInt(expectedPst.get("MaxTap"))
                ));
            }
        }
    }

    @Then("the PST {string} should have groupId {string}")
    public void thePstShouldHaveGroupId(String pstRangeActionId, String expectedGroupId) {
        RangeAction<?> rangeAction = CommonTestData.getCrac().getRangeAction(pstRangeActionId);
        assertNotNull(rangeAction);
        assertTrue(rangeAction.getGroupId().isPresent());
        assertEquals(expectedGroupId, rangeAction.getGroupId().get());
    }

    @Then("the HVDC range actions should have the following ranges:")
    public void theHvdcsShouldHaveRanges(DataTable arg1) {
        List<Map<String, String>> expectedHvdcs = arg1.asMaps(String.class, String.class);

        int number = expectedHvdcs.stream().map(stringStringMap -> stringStringMap.get("HvdcRangeActionId")).collect(Collectors.toSet()).size();
        assertEquals(number, crac.getHvdcRangeActions().size());

        for (Map<String, String> expectedHvdc : expectedHvdcs) {
            String id = expectedHvdc.get("HvdcRangeActionId");
            HvdcRangeAction hvdcRangeAction = crac.getHvdcRangeAction(id);
            assertNotNull(hvdcRangeAction);
            assertEquals(Integer.parseInt(expectedHvdc.get("Ranges")), hvdcRangeAction.getRanges().size());
            RangeType rangeType = RangeType.valueOf(expectedHvdc.get("RangeType"));
            assertTrue(hvdcRangeAction.getRanges().stream().anyMatch(range ->
                range.getUnit().equals(Unit.MEGAWATT)
                    && range.getRangeType().equals(rangeType)
                    && range.getMin() == Double.parseDouble(expectedHvdc.get("Min"))
                    && range.getMax() == Double.parseDouble(expectedHvdc.get("Max"))
            ));
        }
    }

    @Then("the injection range action {string} should have the following injection distribution keys:")
    public void itShouldHaveTheFollowingInjectionDistributionKeys(String injectionRangeActionId, DataTable arg2) {

        InjectionRangeAction ira = crac.getInjectionRangeAction(injectionRangeActionId);
        assertNotNull(ira);

        List<Map<String, String>> expectedInjectionAndKeys = arg2.asMaps(String.class, String.class);
        assertEquals(expectedInjectionAndKeys.size(), ira.getInjectionDistributionKeys().size());

        for (Map<String, String> expectedInjectionAndKey : expectedInjectionAndKeys) {
            String expectedInjectionId = expectedInjectionAndKey.get("InjectionId");
            String expectedKey = expectedInjectionAndKey.get("Key");

            Map.Entry<NetworkElement, Double> correspondingEntry = ira.getInjectionDistributionKeys().entrySet().stream()
                .filter(e -> e.getKey().getId().equals(expectedInjectionId))
                .findAny().orElse(null);

            assertNotNull(correspondingEntry);
            assertEquals(Double.parseDouble(expectedKey), correspondingEntry.getValue(), 1e-3);
        }
    }

    @Then("it should have the following network actions:")
    public void itShouldHaveTheFollowingNetworkActions(DataTable arg1) {
        List<Map<String, String>> expectedNetworkActions = arg1.asMaps(String.class, String.class);

        int number = expectedNetworkActions.stream().map(stringStringMap -> stringStringMap.get("NetworkActionId")).collect(Collectors.toSet()).size();
        assertEquals(number, crac.getNetworkActions().size());

        for (Map<String, String> expectedNetworkAction : expectedNetworkActions) {
            String id = expectedNetworkAction.get("NetworkActionId");
            NetworkAction networkAction = crac.getNetworkAction(id);
            assertNotNull(networkAction);
            assertEquals(expectedNetworkAction.get("NetworkActionName"), networkAction.getName());
            assertEquals(Integer.parseInt(expectedNetworkAction.get("ElementaryActions")), networkAction.getElementaryActions().size());
            String networkElementId = expectedNetworkAction.get("NetworkElementId");
            String action = expectedNetworkAction.get("Action/Setpoint");
            switch (expectedNetworkAction.get("ElementaryActionType")) {
                case "PhaseTapChangerTapPositionAction":
                    int tapPosition = Integer.parseInt(action);
                    assertTrue(networkAction.getElementaryActions().stream().anyMatch(elementaryAction ->
                        elementaryAction instanceof PhaseTapChangerTapPositionAction phaseTapChangerTapPositionAction
                            && Objects.equals(phaseTapChangerTapPositionAction.getTransformerId(), networkElementId)
                            && phaseTapChangerTapPositionAction.getTapPosition() == tapPosition));
                    break;
                case "TerminalsConnectionAction":
                    ActionType actionTypeTCA = ActionType.valueOf(action.toUpperCase());
                    assertTrue(networkAction.getElementaryActions().stream().anyMatch(elementaryAction ->
                        elementaryAction instanceof TerminalsConnectionAction terminalsConnectionAction
                            && Objects.equals(terminalsConnectionAction.getElementId(), networkElementId)
                            && terminalsConnectionAction.isOpen() == (actionTypeTCA == ActionType.OPEN)));
                    break;
                case "SwitchAction":
                    ActionType actionTypeSA = ActionType.valueOf(action.toUpperCase());
                    assertTrue(networkAction.getElementaryActions().stream().anyMatch(elementaryAction ->
                        elementaryAction instanceof SwitchAction switchAction
                            && Objects.equals(switchAction.getSwitchId(), networkElementId)
                            && switchAction.isOpen() == (actionTypeSA == ActionType.OPEN)));
                    break;
                case "GeneratorAction":
                    double activePowerValueGA = Double.parseDouble(action);
                    assertTrue(networkAction.getElementaryActions().stream().anyMatch(elementaryAction ->
                        elementaryAction instanceof GeneratorAction generatorAction
                            && Objects.equals(generatorAction.getGeneratorId(), networkElementId)
                            && generatorAction.getActivePowerValue().getAsDouble() == activePowerValueGA));
                    break;
                case "LoadAction":
                    double activePowerValueLA = Double.parseDouble(action);
                    assertTrue(networkAction.getElementaryActions().stream().anyMatch(elementaryAction ->
                        elementaryAction instanceof LoadAction loadAction
                            && Objects.equals(loadAction.getLoadId(), networkElementId)
                            && loadAction.getActivePowerValue().getAsDouble() == activePowerValueLA));
                    break;
                case "DanglingLineAction":
                    double activePowerValueDLA = Double.parseDouble(action);
                    assertTrue(networkAction.getElementaryActions().stream().anyMatch(elementaryAction ->
                        elementaryAction instanceof DanglingLineAction danglingLineAction
                            && Objects.equals(danglingLineAction.getDanglingLineId(), networkElementId)
                            && danglingLineAction.getActivePowerValue().getAsDouble() == activePowerValueDLA));
                    break;
                case "ShuntCompensatorPositionAction":
                    int sectionCount = Integer.parseInt(action);
                    assertTrue(networkAction.getElementaryActions().stream().anyMatch(elementaryAction ->
                        elementaryAction instanceof ShuntCompensatorPositionAction shuntCompensatorPositionAction
                            && Objects.equals(shuntCompensatorPositionAction.getShuntCompensatorId(), networkElementId)
                            && shuntCompensatorPositionAction.getSectionCount() == sectionCount));
                    break;
                case "SwitchPair":
                    assertEquals("OPEN/CLOSE", action);
                    String open = networkElementId.split("/")[0];
                    String close = networkElementId.split("/")[1];
                    assertTrue(networkAction.getElementaryActions().stream().anyMatch(elementaryAction ->
                        elementaryAction instanceof SwitchPair switchPair
                            && switchPair.getSwitchToOpen().getId().equals(open)
                            && switchPair.getSwitchToClose().getId().equals(close)));
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Unknown elementary network action type: %s", expectedNetworkAction.get("ElementaryActionType")));
            }
        }
    }

    @Then("the remedial actions should have the following usage rules:")
    public void raShouldHaveUsageRules(DataTable arg1) {
        List<Map<String, String>> expectedUsageRules = arg1.asMaps(String.class, String.class);
        for (Map<String, String> expectedUsageRule : expectedUsageRules) {
            String raId = expectedUsageRule.get("RemedialActionId");
            RemedialAction<?> remedialAction = crac.getRemedialAction(raId);
            assertNotNull(remedialAction);
            int number = Integer.parseInt(expectedUsageRule.get("UsageRules"));
            assertEquals(number, remedialAction.getUsageRules().size());
            Instant instant = crac.getInstant(expectedUsageRule.get("Instant").toLowerCase());
            UsageMethod usageMethod = UsageMethod.valueOf(expectedUsageRule.get("Method").toUpperCase());
            switch (expectedUsageRule.get("Rule")) {
                case "OnInstant":
                    assertTrue(remedialAction.getUsageRules().stream().anyMatch(usageRule ->
                        usageRule instanceof OnInstant onInstant
                            && onInstant.getUsageMethod().equals(usageMethod)
                            && onInstant.getInstant().equals(instant)
                    ));
                    break;
                case "OnContingencyState":
                    Contingency contingency = crac.getContingency(expectedUsageRule.get("ContingencyId"));
                    assertNotNull(contingency);
                    assertTrue(remedialAction.getUsageRules().stream().anyMatch(usageRule ->
                        usageRule instanceof OnContingencyState onContingencyState
                            && onContingencyState.getUsageMethod().equals(usageMethod)
                            && onContingencyState.getInstant().equals(instant)
                            && onContingencyState.getContingency().equals(contingency)
                    ));
                    break;
                case "OnFlowConstraint":
                    FlowCnec flowCnec = crac.getFlowCnec(expectedUsageRule.get("FlowCnecId"));
                    assertNotNull(flowCnec);
                    assertTrue(remedialAction.getUsageRules().stream().anyMatch(usageRule ->
                        usageRule instanceof OnConstraint<?> onFlowConstraint
                            && onFlowConstraint.getUsageMethod().equals(usageMethod)
                            && onFlowConstraint.getInstant().equals(instant)
                            && onFlowConstraint.getCnec().equals(flowCnec)
                    ));
                    break;
                case "OnFlowConstraintInCountry":
                    Country country = Country.valueOf(expectedUsageRule.get("Country"));
                    Optional<Contingency> optionalContingency = Optional.ofNullable(crac.getContingency(expectedUsageRule.get("ContingencyId")));
                    assertTrue(remedialAction.getUsageRules().stream().anyMatch(usageRule ->
                        usageRule instanceof OnFlowConstraintInCountry onFlowConstraintInCountry
                            && onFlowConstraintInCountry.getUsageMethod().equals(usageMethod)
                            && onFlowConstraintInCountry.getInstant().equals(instant)
                            && onFlowConstraintInCountry.getCountry().equals(country)
                            && onFlowConstraintInCountry.getContingency().equals(optionalContingency)
                    ));
                    break;
                case "OnAngleConstraint":
                    AngleCnec angleCnec = crac.getAngleCnec(expectedUsageRule.get("AngleCnecId"));
                    assertNotNull(angleCnec);
                    assertTrue(remedialAction.getUsageRules().stream().anyMatch(usageRule ->
                        usageRule instanceof OnConstraint<?> onAngleConstraint
                            && onAngleConstraint.getUsageMethod().equals(usageMethod)
                            && onAngleConstraint.getInstant().equals(instant)
                            && onAngleConstraint.getCnec().equals(angleCnec)
                    ));
                    break;
                default:
                    throw new IllegalArgumentException(String.format("UsageRule unknown: %s", expectedUsageRule.get("Rule")));
            }
        }
    }

    @Then("it should have {int} cnecs")
    public void itShouldHaveCnecs(int expectedCnecNumber) {
        assertEquals(expectedCnecNumber, crac.getCnecs().size());
    }

    @Then("it should have {int} mnecs")
    public void itShouldHaveMnecs(int expectedMnecNumber) {
        assertEquals(expectedMnecNumber, crac.getCnecs().stream().filter(Cnec::isMonitored).count());
    }

    @Then("it should have {int} network actions")
    public void itShouldHaveNetworkActions(int expectedNetworkActions) {
        assertEquals(expectedNetworkActions, crac.getNetworkActions().size());
    }

    @Then("it should have {int} range actions")
    public void itShouldHaveRangeActions(int expectedRangeActions) {
        assertEquals(expectedRangeActions, crac.getRangeActions().size());
    }

    @Then("groupId for range action {string} should be {string}")
    public void rangeActionGroupIdShouldBe(String rangeActionId, String expectedGroupId) {
        assertNotNull(crac.getRangeAction(rangeActionId));
        if (expectedGroupId.equals("null")) {
            assertTrue(crac.getRangeAction(rangeActionId).getGroupId().isEmpty());
        } else {
            assertTrue(crac.getRangeAction(rangeActionId).getGroupId().isPresent());
            assertEquals(expectedGroupId, crac.getRangeAction(rangeActionId).getGroupId().get());
        }
    }

    @Then("range action {string} should have {int} ranges")
    public void rangeActionShouldHaveRanges(String rangeActionId, int expectedRanges) {
        assertNotNull(crac.getRangeAction(rangeActionId));
        assertEquals(expectedRanges, ((PstRangeAction) crac.getRangeAction(rangeActionId)).getRanges().size());
    }

}
