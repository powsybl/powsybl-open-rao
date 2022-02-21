/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.NativeBranch;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.CriticalBranchType;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteCnecElementHelper;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThresholdAdder;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
class CriticalBranchReader {

    private static final List<String> DIRECT = List.of("DIRECT", "MONODIR");
    private static final List<String> OPPOSITE = List.of("OPPOSITE");

    private String importStatusDetail = null;
    private ImportStatus importStatus;

    private final CriticalBranchType criticalBranch;
    private boolean isBaseCase;
    private boolean isInvertedInNetwork;
    private OutageReader outageReader;
    private UcteCnecElementHelper ucteCnecElementHelper;

    boolean isCriticialBranchValid() {
        return importStatus.equals(ImportStatus.IMPORTED);
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }

    String getImportStatusDetail() {
        return importStatusDetail;
    }

    OutageReader getOutageReader() {
        return outageReader;
    }

    boolean isBaseCase() {
        return isBaseCase;
    }

    String getBaseCaseCnecId() {
        if (isBaseCase) {
            return criticalBranch.getId().concat(" - ").concat(Instant.PREVENTIVE.toString());
        }
        return null;
    }

    String getOutageCnecId() {
        if (!isBaseCase) {
            return criticalBranch.getId().concat(" - ").concat(Instant.OUTAGE.toString());
        }
        return null;
    }

    String getCurativeCnecId() {
        if (!isBaseCase) {
            return criticalBranch.getId().concat(" - ").concat(Instant.CURATIVE.toString());
        }
        return null;
    }

    CriticalBranchType getCriticalBranch() {
        return criticalBranch;
    }

    NativeBranch getNativeBranch() {
        return new NativeBranch(ucteCnecElementHelper.getOriginalFrom(), ucteCnecElementHelper.getOriginalTo(), ucteCnecElementHelper.getSuffix());
    }

    boolean isInvertedInNetwork() {
        return isInvertedInNetwork;
    }

    CriticalBranchReader(CriticalBranchType criticalBranch, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        this.criticalBranch = criticalBranch;
        interpretWithNetwork(ucteNetworkAnalyzer);
    }

    void addCnecs(Crac crac) {
        if (isBaseCase) {
            addCnecWithPermanentThreshold(crac, Instant.PREVENTIVE);
        } else {
            addOutageCnecWithTemporaryThreshold(crac);
            addCnecWithPermanentThreshold(crac, Instant.CURATIVE);
        }
    }

    private void interpretWithNetwork(UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        this.importStatus = ImportStatus.IMPORTED;
        this.ucteCnecElementHelper = new UcteCnecElementHelper(criticalBranch.getBranch().getFrom(),
                criticalBranch.getBranch().getTo(),
                criticalBranch.getBranch().getOrder(),
                criticalBranch.getBranch().getElementName(),
                ucteNetworkAnalyzer);

        if (ucteCnecElementHelper.isValid()) {
            this.isInvertedInNetwork = ucteCnecElementHelper.isInvertedInNetwork();
        } else {
            this.importStatus = ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK;
            this.importStatusDetail = String.format("critical branch %s was removed as %s", criticalBranch.getId(), ucteCnecElementHelper.getInvalidReason());
            return;
        }

        if (Objects.isNull(criticalBranch.getOutage())) {
            this.isBaseCase = true;
        } else {
            this.isBaseCase = false;
            OutageReader tmpOutageReader = new OutageReader(criticalBranch.getOutage(), ucteNetworkAnalyzer);
            if (tmpOutageReader.isOutageValid()) {
                this.outageReader = tmpOutageReader;
            } else {
                this.importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
                this.importStatusDetail = String.format("critical branch %s was removed as %s", criticalBranch.getId(), tmpOutageReader.getInvalidOutageReason());
                return;
            }
        }

        if (!criticalBranch.isCNEC() && !criticalBranch.isMNEC()) {
            this.importStatus = ImportStatus.NOT_FOR_RAO;
            this.importStatusDetail = String.format("critical branch %s was removed as it is neither a CNEC, nor a MNEC", criticalBranch.getId());
            return;
        }

        if (!criticalBranch.getDirection().equals("DIRECT") && !criticalBranch.getDirection().equals("MONODIR") && !criticalBranch.getDirection().equals("OPPOSITE")) {
            this.importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
            this.importStatusDetail = String.format("critical branch %s was removed as its direction %s is unknown", criticalBranch.getId(), criticalBranch.getDirection());
        }
    }

    private void addCnecWithPermanentThreshold(Crac crac, Instant instant) {
        FlowCnecAdder preventiveCnecAdder = createCnecAdder(crac, instant);
        addPermanentThresholds(preventiveCnecAdder);
        FlowCnec cnec = preventiveCnecAdder.add();
        addLoopFlowExtension(cnec, criticalBranch);
    }

    private void addOutageCnecWithTemporaryThreshold(Crac crac) {
        FlowCnecAdder curativeCnecAdder = createCnecAdder(crac, Instant.OUTAGE);
        addTemporaryThresholds(curativeCnecAdder);
        FlowCnec cnec = curativeCnecAdder.add();
        addLoopFlowExtension(cnec, criticalBranch);
    }

    private FlowCnecAdder createCnecAdder(Crac crac, Instant instant) {
        CriticalBranchType.Branch branch = criticalBranch.getBranch();

        FlowCnecAdder adder = crac.newFlowCnec()
            .withId(criticalBranch.getId().concat(" - ").concat(instant.toString()))
            .withName(branch.getName())
            .withNetworkElement(ucteCnecElementHelper.getIdInNetwork())
            .withInstant(instant)
            .withReliabilityMargin(criticalBranch.getFrmMw())
            .withOperator(criticalBranch.getTsoOrigin())
            .withMonitored(criticalBranch.isMNEC())
            .withOptimized(criticalBranch.isCNEC())
            .withIMax(ucteCnecElementHelper.getCurrentLimit(Branch.Side.ONE), Side.LEFT)
            .withIMax(ucteCnecElementHelper.getCurrentLimit(Branch.Side.TWO), Side.RIGHT)
            .withNominalVoltage(ucteCnecElementHelper.getNominalVoltage(Branch.Side.ONE), Side.LEFT)
            .withNominalVoltage(ucteCnecElementHelper.getNominalVoltage(Branch.Side.TWO), Side.RIGHT);

        if (!isBaseCase) {
            adder.withContingency(outageReader.getOutage().getId());
        }
        return adder;
    }

    private void addPermanentThresholds(FlowCnecAdder cnecAdder) {
        if (!Objects.isNull(criticalBranch.getPermanentImaxFactor())) {
            addThresholdInPercentImax(cnecAdder, criticalBranch.getPermanentImaxFactor().doubleValue());
        } else if (!Objects.isNull(criticalBranch.getImaxFactor())) {
            addThresholdInPercentImax(cnecAdder, criticalBranch.getImaxFactor().doubleValue());
        }

        if (!Objects.isNull(criticalBranch.getPermanentImaxA())) {
            addThresholdInAmps(cnecAdder, criticalBranch.getPermanentImaxA().doubleValue());
        } else if (!Objects.isNull(criticalBranch.getImaxA())) {
            addThresholdInAmps(cnecAdder, criticalBranch.getImaxA().doubleValue());
        }

        if (Objects.isNull(criticalBranch.getPermanentImaxFactor()) && Objects.isNull(criticalBranch.getImaxFactor())
                && Objects.isNull(criticalBranch.getPermanentImaxA()) && Objects.isNull(criticalBranch.getImaxA())) {
            addThresholdInPercentImax(cnecAdder, 1.);
        }
    }

    private void addTemporaryThresholds(FlowCnecAdder cnecAdder) {
        if (!Objects.isNull(criticalBranch.getTemporaryImaxFactor())) {
            addThresholdInPercentImax(cnecAdder, criticalBranch.getTemporaryImaxFactor().doubleValue());
        } else if (!Objects.isNull(criticalBranch.getImaxFactor())) {
            addThresholdInPercentImax(cnecAdder, criticalBranch.getImaxFactor().doubleValue());
        }

        if (!Objects.isNull(criticalBranch.getTemporaryImaxA())) {
            addThresholdInAmps(cnecAdder, criticalBranch.getTemporaryImaxA().doubleValue());
        } else if (!Objects.isNull(criticalBranch.getImaxA())) {
            addThresholdInAmps(cnecAdder, criticalBranch.getImaxA().doubleValue());
        }

        if (Objects.isNull(criticalBranch.getTemporaryImaxFactor()) && Objects.isNull(criticalBranch.getImaxFactor())
                && Objects.isNull(criticalBranch.getTemporaryImaxA()) && Objects.isNull(criticalBranch.getImaxA())) {
            addThresholdInPercentImax(cnecAdder, 1.);
        }
    }

    private void addThresholdInAmps(FlowCnecAdder cnecAdder, double amps) {
        BranchThresholdAdder branchThresholdAdder = cnecAdder.newThreshold()
                .withUnit(Unit.AMPERE);
        addLimitsGivenDirection(amps, branchThresholdAdder);
        addRule(branchThresholdAdder);
        branchThresholdAdder.add();

    }

    private void addThresholdInPercentImax(FlowCnecAdder cnecAdder, double percentImax) {

        //idea: create threshold in AMPERE instead of PERCENT_IMAX to avoid synchronisation afterwards
        //      can be tricky for transformers

        BranchThresholdAdder branchThresholdAdder = cnecAdder.newThreshold()
                .withUnit(Unit.PERCENT_IMAX);
        addLimitsGivenDirection(percentImax, branchThresholdAdder);
        addRule(branchThresholdAdder);
        branchThresholdAdder.add();
    }

    private void addRule(BranchThresholdAdder branchThresholdAdder) {
        if (!ucteCnecElementHelper.isHalfLine()) {
            // default rule for all branches but tie-line
            branchThresholdAdder.withRule(BranchThresholdRule.ON_REGULATED_SIDE);
        } else if (ucteCnecElementHelper.getHalfLineSide() == Branch.Side.ONE) {
            // respecting the side is important for tie-lines as the two sides of a tie-line might not have the same Imax
            branchThresholdAdder.withRule(BranchThresholdRule.ON_LEFT_SIDE);
        } else {
            branchThresholdAdder.withRule(BranchThresholdRule.ON_RIGHT_SIDE);
        }
    }

    private void addLimitsGivenDirection(double positiveLimit, BranchThresholdAdder branchThresholdAdder) {
        if ((DIRECT.contains(criticalBranch.getDirection()) && !ucteCnecElementHelper.isInvertedInNetwork())
                || (OPPOSITE.contains(criticalBranch.getDirection()) && ucteCnecElementHelper.isInvertedInNetwork())) {
            branchThresholdAdder.withMax(positiveLimit);
        }

        if ((DIRECT.contains(criticalBranch.getDirection()) && ucteCnecElementHelper.isInvertedInNetwork())
                || (OPPOSITE.contains(criticalBranch.getDirection()) && !ucteCnecElementHelper.isInvertedInNetwork())) {
            branchThresholdAdder.withMin(-positiveLimit);
        }
    }

    private static void addLoopFlowExtension(FlowCnec cnec, CriticalBranchType criticalBranch) {
        if (criticalBranch.getMinRAMfactor() != null && isCrossZonal(criticalBranch.getBranch())) {
            cnec.newExtension(LoopFlowThresholdAdder.class)
                    .withUnit(Unit.PERCENT_IMAX)
                    .withValue((100.0 - criticalBranch.getMinRAMfactor().doubleValue()) / 100.)
                    .add();
        }
    }

    static boolean isCrossZonal(CriticalBranchType.Branch branch) {

        // check if first characters of the from/to nodes are different
        // if 1st characters are different, it means that:
        //   - the nodes come from two different countries,
        //   - or one the nodes is a Xnode

        if (!branch.getFrom().substring(0, 1).equals(branch.getTo().substring(0, 1))) {
            return true;
        }

        // check if the name of branch finishes with [XX] with XX a country code

        if (Pattern.compile("\\[[A-Za-z]{2}\\]$").matcher(branch.getName()).find()) {
            String countryCode = branch.getName().substring(branch.getName().length() - 3, branch.getName().length() - 1);
            try {
                Country.valueOf(countryCode);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return false;
    }

}
