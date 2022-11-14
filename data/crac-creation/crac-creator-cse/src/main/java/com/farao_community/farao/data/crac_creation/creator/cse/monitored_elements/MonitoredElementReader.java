/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.monitored_elements;

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
import java.util.Map;
import java.util.Set;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */

public class MonitoredElementReader {
    private final String monitoredBranchName;
    private final NativeBranch nativeBranch;
    private boolean isBaseCase;
    private final Map<Instant, String> createdCnecIds = new EnumMap<>(Instant.class);
    private String contingencyId;
    private final boolean isImported;
    private String invalidBranchReason;
    private boolean isDirectionInverted;
    private final ImportStatus monitoredBranchImportStatus;
    private Set<Side> monitoredSides;

    public String getMonitoredBranchName() {
        return monitoredBranchName;
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

    public MonitoredElementReader(TBranch tBranch, @Nullable TOutage tOutage, Crac crac, UcteNetworkAnalyzer ucteNetworkAnalyzer, Set<Side> defaultMonitoredSides) {
        String outage;
        if (tOutage == null) {
            outage = "basecase";
        } else {
            outage = tOutage.getName().getV();
        }
        this.monitoredBranchName = String.join(" - ", tBranch.getName().getV(), tBranch.getFromNode().getV(), tBranch.getToNode().getV(), outage);
        UcteFlowElementHelper branchHelper = new UcteFlowElementHelper(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), String.valueOf(tBranch.getOrder().getV()), ucteNetworkAnalyzer);
        this.nativeBranch = new NativeBranch(branchHelper.getOriginalFrom(), branchHelper.getOriginalTo(), branchHelper.getSuffix());
        if (outage.equals("mneHasTooManyBranches")) {
            this.isImported = false;
            this.invalidBranchReason = String.format("MonitoredElement has more than 1 Branch");
            this.monitoredBranchImportStatus = ImportStatus.NOT_YET_HANDLED_BY_FARAO;
            return;
        }
        if (!branchHelper.isValid()) {
            this.isImported = false;
            this.invalidBranchReason = branchHelper.getInvalidReason();
            this.monitoredBranchImportStatus = ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK;
            return;
        }
        this.monitoredSides = branchHelper.isHalfLine() ? Set.of(Side.fromIidmSide(branchHelper.getHalfLineSide())) : defaultMonitoredSides;
        if (tOutage != null && crac.getContingency(tOutage.getName().getV()) == null) {
            this.isImported = false;
            this.monitoredBranchImportStatus = ImportStatus.INCOMPLETE_DATA;
            this.invalidBranchReason = String.format("CNEC is defined on outage %s which is not defined", tOutage.getV());
        } else {
            this.isImported = true;
            this.isDirectionInverted = branchHelper.isInvertedInNetwork();
            if (tOutage == null) {
                // preventive
                this.isBaseCase = true;
                this.contingencyId = null;
                this.monitoredBranchImportStatus = ImportStatus.IMPORTED;
                importPreventiveCnec(tBranch, branchHelper, crac);
            } else {
                // curative
                this.isBaseCase = false;
                this.contingencyId = tOutage.getName().getV();
                this.monitoredBranchImportStatus = ImportStatus.IMPORTED;
                importCurativeCnecs(tBranch, branchHelper, tOutage, crac);
            }
        }
    }

    private void importPreventiveCnec(TBranch tBranch, UcteFlowElementHelper branchHelper, Crac crac) {
        importCnec(crac, tBranch, branchHelper, tBranch.getImax(), null, Instant.PREVENTIVE);
    }

    private void importCurativeCnecs(TBranch tBranch, UcteFlowElementHelper branchHelper, TOutage tOutage, Crac crac) {
        EnumMap<Instant, TImax> cnecCaracs = new EnumMap<>(Instant.class);
        cnecCaracs.put(Instant.OUTAGE, tBranch.getImaxAfterOutage());
        cnecCaracs.put(Instant.AUTO, tBranch.getImaxAfterSPS());
        cnecCaracs.put(Instant.CURATIVE, tBranch.getImaxAfterCRA());
        cnecCaracs.forEach((instant, iMax) -> importCnec(crac, tBranch, branchHelper, iMax, tOutage, instant));
    }

    private void importCnec(Crac crac, TBranch tBranch, UcteFlowElementHelper branchHelper, @Nullable TImax tImax, @Nullable TOutage tOutage, Instant instant) {
        if (tImax == null) {
            return;
        }
        String cnecId = getCnecId(tBranch, tOutage, instant);
        FlowCnecAdder cnecAdder = crac.newFlowCnec()
                .withId(cnecId)
                .withName(tBranch.getName().getV())
                .withInstant(instant)
                .withContingency(tOutage != null ? tOutage.getName().getV() : null)
                .withNetworkElement(branchHelper.getIdInNetwork())
                .withOptimized(false)
                .withMonitored(true)
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
        return String.format("%s - %s->%s  - %s - %s", tBranch.getName().getV(), tBranch.getFromNode().getV(), tBranch.getToNode().getV(), tOutage.getName().getV(), instant.toString());
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

    public ImportStatus getImportStatus() {
        return monitoredBranchImportStatus;
    }
}
