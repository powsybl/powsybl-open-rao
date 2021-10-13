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
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.NativeBranch;
import com.farao_community.farao.data.crac_creation.creator.cse.TBranch;
import com.farao_community.farao.data.crac_creation.creator.cse.TImax;
import com.farao_community.farao.data.crac_creation.creator.cse.TOutage;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteCnecElementHelper;
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
    private final Set<String> remedialActionIds = new HashSet<>();
    private final ImportStatus criticalBranchImportStatus;

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

    public CriticalBranchReader(TBranch tBranch, @Nullable TOutage tOutage, Crac crac, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        this.criticalBranchName = tBranch.getName().getV();
        UcteCnecElementHelper branchHelper = new UcteCnecElementHelper(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), String.valueOf(tBranch.getOrder().getV()), ucteNetworkAnalyzer);
        this.nativeBranch = new NativeBranch(branchHelper.getOriginalFrom(), branchHelper.getOriginalTo(), branchHelper.getSuffix());
        if (!branchHelper.isValid()) {
            this.isImported = false;
            this.invalidBranchReason = branchHelper.getInvalidReason();
            this.criticalBranchImportStatus = ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK;
        } else if (tOutage != null && crac.getContingency(tOutage.getV()) == null) {
            this.isImported = false;
            this.criticalBranchImportStatus = ImportStatus.INCOMPLETE_DATA;
            this.invalidBranchReason = String.format("CNEC is defined on outage %s which is not defined", tOutage.getV());
        } else {
            this.isImported = true;
            this.isDirectionInverted = branchHelper.isInvertedInNetwork();
            if (tOutage == null) {
                // preventive
                this.isBaseCase = true;
                this.contingencyId = null;
                this.criticalBranchImportStatus = ImportStatus.IMPORTED;
                importPreventiveCnec(tBranch, branchHelper, crac);
            } else {
                // curative
                this.isBaseCase = false;
                this.contingencyId = tOutage.getV();
                this.criticalBranchImportStatus = ImportStatus.IMPORTED;
                importCurativeCnecs(tBranch, branchHelper, tOutage, crac);
            }
        }
    }

    private void importPreventiveCnec(TBranch tBranch, UcteCnecElementHelper branchHelper, Crac crac) {
        importCnec(crac, tBranch, branchHelper, tBranch.getImax(), null, Instant.PREVENTIVE);
    }

    private void importCurativeCnecs(TBranch tBranch, UcteCnecElementHelper branchHelper, TOutage tOutage, Crac crac) {
        EnumMap<Instant, TImax> cnecCaracs = new EnumMap<>(Instant.class);
        cnecCaracs.put(Instant.OUTAGE, tBranch.getImaxAfterOutage());
        cnecCaracs.put(Instant.AUTO, tBranch.getImaxAfterSPS());
        cnecCaracs.put(Instant.CURATIVE, tBranch.getImaxAfterCRA());
        cnecCaracs.forEach((instant, iMax) -> importCnec(crac, tBranch, branchHelper, iMax, tOutage, instant));
    }

    private void importCnec(Crac crac, TBranch tBranch, UcteCnecElementHelper branchHelper, @Nullable TImax tImax, @Nullable TOutage tOutage, Instant instant) {
        if (tImax == null) {
            return;
        }
        String cnecId = getCnecId(tBranch, tOutage, instant);
        FlowCnecAdder cnecAdder = crac.newFlowCnec()
                .withId(cnecId)
                .withName(tBranch.getName().getV())
                .withInstant(instant)
                .withContingency(tOutage != null ? tOutage.getV() : null)
                .withOptimized(true)
                .withNetworkElement(branchHelper.getIdInNetwork())
                .withIMax(branchHelper.getCurrentLimit(Branch.Side.ONE), Side.LEFT)
                .withIMax(branchHelper.getCurrentLimit(Branch.Side.TWO), Side.RIGHT)
                .withNominalVoltage(branchHelper.getNominalVoltage(Branch.Side.ONE), Side.LEFT)
                .withNominalVoltage(branchHelper.getNominalVoltage(Branch.Side.TWO), Side.RIGHT);

        addThreshold(cnecAdder, tImax.getV(), tImax.getUnit(), tBranch.getDirection().getV(), isDirectionInverted);
        cnecAdder.add();
        createdCnecIds.put(instant, cnecId);
        storeRemedialActions(tBranch);
    }

    private void storeRemedialActions(TBranch tBranch) {
        tBranch.getRemedialActions().forEach(tRemedialActions ->
            tRemedialActions.getName().forEach(tName -> remedialActionIds.add(tName.getV()))
        );
    }

    private static void addThreshold(FlowCnecAdder cnecAdder, double positiveLimit, String unit, String direction, boolean invert) {
        Unit convertedUnit = convertUnit(unit);
        BranchThresholdAdder branchThresholdAdder = cnecAdder.newThreshold()
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withUnit(convertedUnit);
        convertMinMax(branchThresholdAdder, positiveLimit, direction, invert, convertedUnit == Unit.PERCENT_IMAX);
        branchThresholdAdder.add();
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

    public Set<String> getRemedialActionIds() {
        return remedialActionIds;
    }

    public ImportStatus getImportStatus() {
        return criticalBranchImportStatus;
    }
}
