/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.cse.criticalbranch;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnecAdder;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.threshold.BranchThresholdAdder;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.api.stdcreationcontext.NativeBranch;
import com.powsybl.openrao.data.craccreation.creator.cse.xsd.TBranch;
import com.powsybl.openrao.data.craccreation.creator.cse.xsd.TImax;
import com.powsybl.openrao.data.craccreation.creator.cse.xsd.TOutage;
import com.powsybl.openrao.data.craccreation.util.ucte.UcteFlowElementHelper;
import com.powsybl.openrao.data.craccreation.util.ucte.UcteNetworkAnalyzer;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CriticalBranchReader {
    private final String criticalBranchName;
    private final NativeBranch nativeBranch;
    private boolean isBaseCase;
    private final Map<String, String> createdCnecIds = new HashMap<>();
    private String contingencyId;
    private final boolean isImported;
    private String invalidBranchReason;
    private boolean isDirectionInverted;
    private boolean selected;
    private final Set<String> remedialActionIds = new HashSet<>();
    private final ImportStatus criticalBranchImportStatus;
    private Set<TwoSides> monitoredSides;

    public String getCriticalBranchName() {
        return criticalBranchName;
    }

    public NativeBranch getNativeBranch() {
        return nativeBranch;
    }

    public boolean isBaseCase() {
        return isBaseCase;
    }

    public Map<String, String> getCreatedCnecIds() {
        return Collections.unmodifiableMap(createdCnecIds);
    }

    public String getContingencyId() {
        return contingencyId;
    }

    public boolean isImported() {
        return isImported;
    }

    public String getInvalidBranchReason() {
        return invalidBranchReason;
    }

    public boolean isDirectionInverted() {
        return isDirectionInverted;
    }

    public boolean isSelected() {
        return selected;
    }

    public CriticalBranchReader(List<TBranch> tBranches, @Nullable TOutage tOutage, Crac crac, UcteNetworkAnalyzer ucteNetworkAnalyzer, Set<TwoSides> defaultMonitoredSides, boolean isMonitored) {
        if (tBranches.size() > 1) {
            this.criticalBranchName = tBranches.stream().map(tBranch -> tBranch.getName().getV()).collect(Collectors.joining(" ; "));
            this.nativeBranch = new NativeBranch(tBranches.get(0).getFromNode().toString(), tBranches.get(0).getToNode().toString(), tBranches.get(0).getOrder().toString());
            this.isImported = false;
            this.invalidBranchReason = "MonitoredElement has more than 1 Branch";
            this.criticalBranchImportStatus = ImportStatus.INCONSISTENCY_IN_DATA;
            return;
        }
        String outage = defineOutage(tOutage);
        TBranch tBranch = tBranches.get(0);
        this.criticalBranchName = String.join(" - ", tBranch.getName().getV(), tBranch.getFromNode().getV(), tBranch.getToNode().getV(), outage);
        UcteFlowElementHelper branchHelper = new UcteFlowElementHelper(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), String.valueOf(tBranch.getOrder().getV()), ucteNetworkAnalyzer);
        this.nativeBranch = new NativeBranch(branchHelper.getOriginalFrom(), branchHelper.getOriginalTo(), branchHelper.getSuffix());
        if (!branchHelper.isValid()) {
            this.isImported = false;
            this.invalidBranchReason = branchHelper.getInvalidReason();
            this.criticalBranchImportStatus = ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK;
            return;
        }
        this.monitoredSides = branchHelper.isHalfLine() ? Set.of(branchHelper.getHalfLineSide()) : defaultMonitoredSides;
        if (tOutage != null && crac.getContingency(outage) == null) {
            this.isImported = false;
            this.criticalBranchImportStatus = ImportStatus.INCOMPLETE_DATA;
            this.invalidBranchReason = String.format("CNEC is defined on outage %s which is not defined", outage);
        } else {
            this.isDirectionInverted = branchHelper.isInvertedInNetwork();
            this.selected = !isMonitored && isSelected(tBranch);
            if (tOutage == null) {
                this.isImported = true;
                // preventive
                this.isBaseCase = true;
                this.contingencyId = null;
                this.criticalBranchImportStatus = ImportStatus.IMPORTED;
                importPreventiveCnec(tBranch, branchHelper, crac, isMonitored);
            } else {
                // curative
                this.isBaseCase = false;
                this.contingencyId = outage;
                if (importCurativeCnecs(tBranch, branchHelper, outage, crac, isMonitored)) {
                    this.criticalBranchImportStatus = ImportStatus.IMPORTED;
                    this.isImported = true;
                } else {
                    this.criticalBranchImportStatus = ImportStatus.INCOMPLETE_DATA;
                    this.invalidBranchReason = "Incomplete Imax data to create at least one post-outage CNEC for the branch.";
                    this.isImported = false;
                }
            }
        }
    }

    private String defineOutage(TOutage tOutage) {
        if (tOutage == null) {
            return "basecase";
        }
        return tOutage.getName() == null ? tOutage.getV() : tOutage.getName().getV();
    }

    private void importPreventiveCnec(TBranch tBranch, UcteFlowElementHelper branchHelper, Crac crac, boolean isMonitored) {
        importCnec(crac, tBranch, branchHelper, isMonitored ? tBranch.getIlimitMNE() : tBranch.getImax(), null, crac.getPreventiveInstant().getId(), isMonitored);
    }

    /**
     * Returns true if at least one CNEC has been correctly imported.
     * @param tBranch
     * @param branchHelper
     * @param outage
     * @param crac
     * @param isMonitored
     * @return
     */
    private boolean importCurativeCnecs(TBranch tBranch, UcteFlowElementHelper branchHelper, String outage, Crac crac, boolean isMonitored) {
        HashMap<Instant, TImax> cnecCaracs = new HashMap<>();
        cnecCaracs.put(crac.getOutageInstant(), isMonitored ? tBranch.getIlimitMNEAfterOutage() : tBranch.getImaxAfterOutage());
        cnecCaracs.put(crac.getInstant(InstantKind.AUTO), isMonitored ? tBranch.getIlimitMNEAfterSPS() : findTargetImaxAfterSPS(tBranch));
        cnecCaracs.put(crac.getInstant(InstantKind.CURATIVE), isMonitored ? tBranch.getIlimitMNEAfterCRA() : findTargetImaxAfterCRA(tBranch.getImaxAfterCRA()));
        cnecCaracs.forEach((instant, iMax) -> importCnec(crac, tBranch, branchHelper, iMax, outage, instant.getId(), isMonitored));
        return cnecCaracs.values().stream().anyMatch(Objects::nonNull);
    }

    private TImax findTargetImaxAfterCRA(final TImax imaxAfterCRA) {
        return imaxAfterCRA != null ? imaxAfterCRA : createDefaultImaxAfterCRA();
    }

    private TImax createDefaultImaxAfterCRA() {
        final TImax tempImax = new TImax();
        tempImax.setUnit("Pct");
        tempImax.setV((short) 100);
        return tempImax;
    }

    private TImax findTargetImaxAfterSPS(final TBranch tBranch) {
        return tBranch.getImaxAfterSPS() != null ? tBranch.getImaxAfterSPS() : tBranch.getImaxAfterOutage();
    }

    private void importCnec(Crac crac, TBranch tBranch, UcteFlowElementHelper branchHelper, @Nullable TImax tImax, String outage, String instantId, boolean isMonitored) {
        if (tImax == null) {
            return;
        }
        String cnecId = getCnecId(tBranch, outage, instantId);
        FlowCnecAdder cnecAdder = crac.newFlowCnec()
                .withId(cnecId)
                .withName(tBranch.getName().getV())
                .withInstant(instantId)
                .withContingency(outage)
                .withOptimized(selected).withMonitored(isMonitored)
                .withNetworkElement(branchHelper.getIdInNetwork())
                .withIMax(branchHelper.getCurrentLimit(TwoSides.ONE), TwoSides.ONE)
                .withIMax(branchHelper.getCurrentLimit(TwoSides.TWO), TwoSides.TWO)
                .withNominalVoltage(branchHelper.getNominalVoltage(TwoSides.ONE), TwoSides.ONE)
                .withNominalVoltage(branchHelper.getNominalVoltage(TwoSides.TWO), TwoSides.TWO);

        Set<TwoSides> monitoredSidesForThreshold = monitoredSides;
        Unit unit = convertUnit(tImax.getUnit());
        // For transformers, if unit is absolute amperes, monitor low voltage side
        if (!branchHelper.isHalfLine() && unit.equals(Unit.AMPERE) &&
                Math.abs(branchHelper.getNominalVoltage(TwoSides.ONE) - branchHelper.getNominalVoltage(TwoSides.TWO)) > 1) {
            monitoredSidesForThreshold = (branchHelper.getNominalVoltage(TwoSides.ONE) <= branchHelper.getNominalVoltage(TwoSides.TWO)) ?
                    Set.of(TwoSides.ONE) : Set.of(TwoSides.TWO);
        }
        addThreshold(cnecAdder, tImax.getV(), unit, tBranch.getDirection().getV(), isDirectionInverted, monitoredSidesForThreshold);
        cnecAdder.add();
        createdCnecIds.put(instantId, cnecId);
        if (!isMonitored) {
            storeRemedialActions(tBranch);
        }
    }

    private void storeRemedialActions(TBranch tBranch) {
        tBranch.getRemedialActions().forEach(tRemedialActions ->
                tRemedialActions.getName().forEach(tName -> remedialActionIds.add(tName.getV()))
        );
    }

    private static void addThreshold(FlowCnecAdder cnecAdder, double positiveLimit, Unit unit, String direction, boolean invert, Set<TwoSides> monitoredSides) {
        monitoredSides.forEach(side -> {
            BranchThresholdAdder branchThresholdAdder = cnecAdder.newThreshold()
                    .withSide(side)
                    .withUnit(unit);
            convertMinMax(branchThresholdAdder, positiveLimit, direction, invert, unit.equals(Unit.PERCENT_IMAX));
            branchThresholdAdder.add();
        });
    }

    private static void convertMinMax(BranchThresholdAdder branchThresholdAdder, double positiveLimit, String direction, boolean invert, boolean isPercent) {
        double convertedPositiveLimit = positiveLimit * (isPercent ? 0.01 : 1.);
        if (direction.equals("BIDIR")) {
            branchThresholdAdder.withMax(convertedPositiveLimit);
            branchThresholdAdder.withMin(-convertedPositiveLimit);
        } else if (direction.equals("DIRECT") && !invert
            || direction.equals("OPPOSITE") && invert) {
            branchThresholdAdder.withMax(convertedPositiveLimit);
        } else if (direction.equals("DIRECT") && invert
            || direction.equals("OPPOSITE") && !invert) {
            branchThresholdAdder.withMin(-convertedPositiveLimit);
        } else {
            throw new IllegalArgumentException(String.format("%s is not a recognized direction", direction));
        }
    }

    private static String getCnecId(TBranch tBranch, String outage, String instantId) {
        if (outage == null) {
            return String.format("%s - %s->%s - %s", tBranch.getName().getV(), tBranch.getFromNode().getV(), tBranch.getToNode().getV(), instantId);
        }
        return String.format("%s - %s->%s  - %s - %s", tBranch.getName().getV(), tBranch.getFromNode().getV(), tBranch.getToNode().getV(), outage, instantId);
    }

    private static Unit convertUnit(String unit) {
        switch (unit) {
            case "Pct":
                return Unit.PERCENT_IMAX;
            case "A":
            default:
                return Unit.AMPERE;
        }
    }

    // In case it is not specified we consider it as selected
    private static boolean isSelected(TBranch tBranch) {
        return tBranch.getSelected() == null || "true".equals(tBranch.getSelected().getV());
    }

    public Set<String> getRemedialActionIds() {
        return remedialActionIds;
    }

    public ImportStatus getImportStatus() {
        return criticalBranchImportStatus;
    }
}
