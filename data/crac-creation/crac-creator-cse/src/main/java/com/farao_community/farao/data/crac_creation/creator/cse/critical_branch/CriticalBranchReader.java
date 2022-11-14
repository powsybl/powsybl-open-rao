/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.critical_branch;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.NativeBranch;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TBranch;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TImax;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TOutage;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteFlowElementHelper;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.powsybl.iidm.network.Branch;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CriticalBranchReader {
    private final String criticalBranchName;
    private final NativeBranch nativeBranch;
    private boolean isBaseCase;
    private final Map<Instant, String> createdCnecIds = new EnumMap<>(Instant.class);
    private String contingencyId;
    private final boolean isImported;
    private String invalidBranchReason;
    private boolean isDirectionInverted;
    private boolean selected;
    private final Set<String> remedialActionIds = new HashSet<>();
    private final ImportStatus criticalBranchImportStatus;
    private Set<Side> monitoredSides;

    public String getCriticalBranchName() {
        return criticalBranchName;
    }

    public NativeBranch getNativeBranch() {
        return nativeBranch;
    }

    public boolean isBaseCase() {
        return isBaseCase;
    }

    public Map<Instant, String> getCreatedCnecIds() {
        return new EnumMap<>(createdCnecIds);
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

    public CriticalBranchReader(TBranch tBranch, @Nullable TOutage tOutage, Crac crac, UcteNetworkAnalyzer ucteNetworkAnalyzer, Set<Side> defaultMonitoredSides) {
        String outage;
        final String basecase = "basecase";
        boolean isMonitored = false;
        if (tOutage == null) {
            outage = basecase;
        } else {
            if (tOutage.getV() == null && tOutage.getName() != null) {
                isMonitored = true;
                tOutage.setV(tOutage.getName().getV());
            }
            outage = tOutage.getV();
        }
        this.criticalBranchName = String.join(" - ", tBranch.getName().getV(), tBranch.getFromNode().getV(), tBranch.getToNode().getV(), outage);
        UcteFlowElementHelper branchHelper = new UcteFlowElementHelper(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), String.valueOf(tBranch.getOrder().getV()), ucteNetworkAnalyzer);
        this.nativeBranch = new NativeBranch(branchHelper.getOriginalFrom(), branchHelper.getOriginalTo(), branchHelper.getSuffix());
        if (!branchHelper.isValid()) {
            this.isImported = false;
            this.invalidBranchReason = branchHelper.getInvalidReason();
            this.criticalBranchImportStatus = ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK;
            return;
        }
        if (outage.equals("mneHasTooManyBranches")) {
            this.isImported = false;
            this.invalidBranchReason = "MonitoredElement has more than 1 Branch";
            this.criticalBranchImportStatus = ImportStatus.NOT_YET_HANDLED_BY_FARAO;
            return;
        }
        this.monitoredSides = branchHelper.isHalfLine() ? Set.of(Side.fromIidmSide(branchHelper.getHalfLineSide())) : defaultMonitoredSides;
        if (tOutage != null && crac.getContingency(tOutage.getV()) == null && !tOutage.getV().equals(basecase)) {
            this.isImported = false;
            this.criticalBranchImportStatus = ImportStatus.INCOMPLETE_DATA;
            this.invalidBranchReason = String.format("CNEC is defined on outage %s which is not defined", tOutage.getV());
        } else {
            this.isImported = true;
            this.isDirectionInverted = branchHelper.isInvertedInNetwork();
            if (isMonitored) {
                this.selected = false;
            } else {
                this.selected = isSelected(tBranch);
            }
            if (tOutage == null || tOutage.getV().equals(basecase)) {
                // preventive
                this.isBaseCase = true;
                this.contingencyId = null;
                this.criticalBranchImportStatus = ImportStatus.IMPORTED;
                importPreventiveCnec(tBranch, branchHelper, crac, isMonitored);
            } else {
                // curative
                this.isBaseCase = false;
                this.contingencyId = tOutage.getV();
                this.criticalBranchImportStatus = ImportStatus.IMPORTED;
                importCurativeCnecs(tBranch, branchHelper, tOutage, crac, isMonitored);
            }
        }
    }

    private void importPreventiveCnec(TBranch tBranch, UcteFlowElementHelper branchHelper, Crac crac, boolean isMonitored) {
        importCnec(crac, tBranch, branchHelper, tBranch.getImax(), null, Instant.PREVENTIVE, isMonitored);
    }

    private void importCurativeCnecs(TBranch tBranch, UcteFlowElementHelper branchHelper, TOutage tOutage, Crac crac, boolean isMonitored) {
        EnumMap<Instant, TImax> cnecCaracs = new EnumMap<>(Instant.class);
        cnecCaracs.put(Instant.OUTAGE, tBranch.getImaxAfterOutage());
        cnecCaracs.put(Instant.AUTO, tBranch.getImaxAfterSPS());
        cnecCaracs.put(Instant.CURATIVE, tBranch.getImaxAfterCRA());
        cnecCaracs.forEach((instant, iMax) -> importCnec(crac, tBranch, branchHelper, iMax, tOutage, instant, isMonitored));
    }

    private void importCnec(Crac crac, TBranch tBranch, UcteFlowElementHelper branchHelper, @Nullable TImax tImax, @Nullable TOutage tOutage, Instant instant, boolean isMonitored) {
        if (tImax == null) {
            return;
        }
        String cnecId = getCnecId(tBranch, tOutage, instant);
        FlowCnecAdder cnecAdder = crac.newFlowCnec()
                .withId(cnecId)
                .withName(tBranch.getName().getV())
                .withInstant(instant)
                .withContingency(tOutage != null ? tOutage.getV() : null)
                .withOptimized(selected).withMonitored(isMonitored)
                .withNetworkElement(branchHelper.getIdInNetwork())
                .withIMax(branchHelper.getCurrentLimit(Branch.Side.ONE), Side.LEFT)
                .withIMax(branchHelper.getCurrentLimit(Branch.Side.TWO), Side.RIGHT)
                .withNominalVoltage(branchHelper.getNominalVoltage(Branch.Side.ONE), Side.LEFT)
                .withNominalVoltage(branchHelper.getNominalVoltage(Branch.Side.TWO), Side.RIGHT);

        Set<Side> monitoredSidesForThreshold = monitoredSides;
        Unit unit = convertUnit(tImax.getUnit());
        // For transformers, if unit is absolute amperes, monitor low voltage side
        if (!branchHelper.isHalfLine() && unit.equals(Unit.AMPERE) &&
                Math.abs(branchHelper.getNominalVoltage(Branch.Side.ONE) - branchHelper.getNominalVoltage(Branch.Side.TWO)) > 1) {
            monitoredSidesForThreshold = (branchHelper.getNominalVoltage(Branch.Side.ONE) <= branchHelper.getNominalVoltage(Branch.Side.TWO)) ?
                    Set.of(Side.LEFT) : Set.of(Side.RIGHT);
        }
        addThreshold(cnecAdder, tImax.getV(), unit, tBranch.getDirection().getV(), isDirectionInverted, monitoredSidesForThreshold);
        cnecAdder.add();
        createdCnecIds.put(instant, cnecId);
        if (!isMonitored) {
            storeRemedialActions(tBranch);
        }
    }

    private void storeRemedialActions(TBranch tBranch) {
        tBranch.getRemedialActions().forEach(tRemedialActions ->
                tRemedialActions.getName().forEach(tName -> remedialActionIds.add(tName.getV()))
        );
    }

    private static void addThreshold(FlowCnecAdder cnecAdder, double positiveLimit, Unit unit, String direction, boolean invert, Set<Side> monitoredSides) {
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
        } else if ((direction.equals("DIRECT") && !invert) || (direction.equals("OPPOSITE") && invert)) {
            branchThresholdAdder.withMax(convertedPositiveLimit);
        } else if ((direction.equals("DIRECT") && invert) || (direction.equals("OPPOSITE") && !invert)) {
            branchThresholdAdder.withMin(-convertedPositiveLimit);
        } else {
            throw new IllegalArgumentException(String.format("%s is not a recognized direction", direction));
        }
    }

    private static String getCnecId(TBranch tBranch, @Nullable TOutage tOutage, Instant instant) {
        if (tOutage == null) {
            return String.format("%s - %s->%s - %s", tBranch.getName().getV(), tBranch.getFromNode().getV(), tBranch.getToNode().getV(), instant.toString());
        }
        return String.format("%s - %s->%s  - %s - %s", tBranch.getName().getV(), tBranch.getFromNode().getV(), tBranch.getToNode().getV(), tOutage.getV(), instant.toString());
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
