/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.fbconstraint;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnecAdder;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.threshold.BranchThresholdAdder;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.NativeBranch;
import com.powsybl.openrao.data.cracio.fbconstraint.xsd.CriticalBranchType;
import com.powsybl.openrao.data.cracio.commons.ucte.UcteFlowElementHelper;
import com.powsybl.openrao.data.cracio.commons.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.cracloopflowextension.LoopFlowThresholdAdder;
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
    private Set<TwoSides> monitoredSides;
    private boolean isBaseCase;
    private boolean isInvertedInNetwork;
    private OutageReader outageReader;
    private UcteFlowElementHelper ucteFlowElementHelper;
    private TwoSides ampereThresholdSide = null;

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
            return criticalBranch.getId().concat(" - ").concat("preventive");
        }
        return null;
    }

    String getOutageCnecId() {
        if (!isBaseCase) {
            return criticalBranch.getId().concat(" - ").concat("outage");
        }
        return null;
    }

    String getCurativeCnecId() {
        if (!isBaseCase) {
            return criticalBranch.getId().concat(" - ").concat("curative");
        }
        return null;
    }

    CriticalBranchType getCriticalBranch() {
        return criticalBranch;
    }

    NativeBranch getNativeBranch() {
        return new NativeBranch(ucteFlowElementHelper.getOriginalFrom(), ucteFlowElementHelper.getOriginalTo(), ucteFlowElementHelper.getSuffix());
    }

    boolean isInvertedInNetwork() {
        return isInvertedInNetwork;
    }

    CriticalBranchReader(CriticalBranchType criticalBranch, UcteNetworkAnalyzer ucteNetworkAnalyzer, Set<TwoSides> defaultMonitoredSides) {
        this.criticalBranch = criticalBranch;
        this.monitoredSides = defaultMonitoredSides;
        interpretWithNetwork(ucteNetworkAnalyzer);
    }

    void addCnecs(Crac crac) {
        if (isBaseCase) {
            addCnecWithPermanentThreshold(crac, crac.getPreventiveInstant().getId());
        } else {
            addOutageCnecWithTemporaryThreshold(crac);
            addCnecWithPermanentThreshold(crac, crac.getInstant(InstantKind.CURATIVE).getId());
        }
    }

    private void interpretWithNetwork(UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        this.importStatus = ImportStatus.IMPORTED;
        this.ucteFlowElementHelper = new UcteFlowElementHelper(criticalBranch.getBranch().getFrom(),
            criticalBranch.getBranch().getTo(),
            criticalBranch.getBranch().getOrder(),
            criticalBranch.getBranch().getElementName(),
            ucteNetworkAnalyzer);

        if (ucteFlowElementHelper.isValid()) {
            this.isInvertedInNetwork = ucteFlowElementHelper.isInvertedInNetwork();
            if (ucteFlowElementHelper.isHalfLine()) {
                this.monitoredSides = Set.of(ucteFlowElementHelper.getHalfLineSide());
            }
        } else {
            this.importStatus = ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK;
            this.importStatusDetail = String.format("critical branch %s was removed as %s", criticalBranch.getId(), ucteFlowElementHelper.getInvalidReason());
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

        initAmpereThresholdSide();
    }

    private void initAmpereThresholdSide() {
        if (!ucteFlowElementHelper.isHalfLine()
            && Math.abs(ucteFlowElementHelper.getNominalVoltage(TwoSides.ONE) - ucteFlowElementHelper.getNominalVoltage(TwoSides.TWO)) > 1) {
            // For transformers, if unit is absolute amperes, monitor low voltage side
            ampereThresholdSide = ucteFlowElementHelper.getNominalVoltage(TwoSides.ONE) <= ucteFlowElementHelper.getNominalVoltage(TwoSides.TWO) ?
                TwoSides.ONE : TwoSides.TWO;
        }
    }

    private void addCnecWithPermanentThreshold(Crac crac, String instantId) {
        FlowCnecAdder preventiveCnecAdder = createCnecAdder(crac, instantId);
        addPermanentThresholds(preventiveCnecAdder);
        FlowCnec cnec = preventiveCnecAdder.add();
        addLoopFlowExtension(cnec, criticalBranch);
    }

    private void addOutageCnecWithTemporaryThreshold(Crac crac) {
        FlowCnecAdder curativeCnecAdder = createCnecAdder(crac, crac.getOutageInstant().getId());
        addTemporaryThresholds(curativeCnecAdder);
        FlowCnec cnec = curativeCnecAdder.add();
        addLoopFlowExtension(cnec, criticalBranch);
    }

    private FlowCnecAdder createCnecAdder(Crac crac, String instantId) {
        CriticalBranchType.Branch branch = criticalBranch.getBranch();

        FlowCnecAdder adder = crac.newFlowCnec()
            .withId(criticalBranch.getId().concat(" - ").concat(instantId))
            .withName(branch.getName())
            .withNetworkElement(ucteFlowElementHelper.getIdInNetwork())
            .withInstant(instantId)
            .withReliabilityMargin(criticalBranch.getFrmMw())
            .withOperator(criticalBranch.getTsoOrigin())
            .withMonitored(criticalBranch.isMNEC())
            .withOptimized(criticalBranch.isCNEC())
            .withIMax(ucteFlowElementHelper.getCurrentLimit(TwoSides.ONE), TwoSides.ONE)
            .withIMax(ucteFlowElementHelper.getCurrentLimit(TwoSides.TWO), TwoSides.TWO)
            .withNominalVoltage(ucteFlowElementHelper.getNominalVoltage(TwoSides.ONE), TwoSides.ONE)
            .withNominalVoltage(ucteFlowElementHelper.getNominalVoltage(TwoSides.TWO), TwoSides.TWO);

        if (!isBaseCase) {
            adder.withContingency(outageReader.getOutage().getId());
        }
        return adder;
    }

    private void addPermanentThresholds(FlowCnecAdder cnecAdder) {
        if (!Objects.isNull(criticalBranch.getPermanentImaxFactor())) {
            addThreshold(cnecAdder, criticalBranch.getPermanentImaxFactor().doubleValue(), Unit.PERCENT_IMAX);
        } else if (!Objects.isNull(criticalBranch.getImaxFactor())) {
            addThreshold(cnecAdder, criticalBranch.getImaxFactor().doubleValue(), Unit.PERCENT_IMAX);
        }

        if (!Objects.isNull(criticalBranch.getPermanentImaxA())) {
            addThreshold(cnecAdder, criticalBranch.getPermanentImaxA().doubleValue(), Unit.AMPERE);
        } else if (!Objects.isNull(criticalBranch.getImaxA())) {
            addThreshold(cnecAdder, criticalBranch.getImaxA().doubleValue(), Unit.AMPERE);
        }

        if (Objects.isNull(criticalBranch.getPermanentImaxFactor()) && Objects.isNull(criticalBranch.getImaxFactor())
            && Objects.isNull(criticalBranch.getPermanentImaxA()) && Objects.isNull(criticalBranch.getImaxA())) {
            addThreshold(cnecAdder, 1., Unit.PERCENT_IMAX);
        }
    }

    private void addTemporaryThresholds(FlowCnecAdder cnecAdder) {
        if (!Objects.isNull(criticalBranch.getTemporaryImaxFactor())) {
            addThreshold(cnecAdder, criticalBranch.getTemporaryImaxFactor().doubleValue(), Unit.PERCENT_IMAX);
        } else if (!Objects.isNull(criticalBranch.getImaxFactor())) {
            addThreshold(cnecAdder, criticalBranch.getImaxFactor().doubleValue(), Unit.PERCENT_IMAX);
        }

        if (!Objects.isNull(criticalBranch.getTemporaryImaxA())) {
            addThreshold(cnecAdder, criticalBranch.getTemporaryImaxA().doubleValue(), Unit.AMPERE);
        } else if (!Objects.isNull(criticalBranch.getImaxA())) {
            addThreshold(cnecAdder, criticalBranch.getImaxA().doubleValue(), Unit.AMPERE);
        }

        if (Objects.isNull(criticalBranch.getTemporaryImaxFactor()) && Objects.isNull(criticalBranch.getImaxFactor())
            && Objects.isNull(criticalBranch.getTemporaryImaxA()) && Objects.isNull(criticalBranch.getImaxA())) {
            addThreshold(cnecAdder, 1., Unit.PERCENT_IMAX);
        }
    }

    private void addThreshold(FlowCnecAdder cnecAdder, double threshold, Unit unit) {

        //idea: create threshold in AMPERE instead of PERCENT_IMAX to avoid synchronisation afterwards
        //      can be tricky for transformers

        Set<TwoSides> monitoredSidesForThreshold = (unit.equals(Unit.AMPERE) && ampereThresholdSide != null) ?
            Set.of(ampereThresholdSide) : monitoredSides;
        monitoredSidesForThreshold.forEach(side -> {
            BranchThresholdAdder branchThresholdAdder = cnecAdder.newThreshold()
                .withUnit(unit)
                .withSide(side);
            addLimitsGivenDirection(threshold, branchThresholdAdder);
            branchThresholdAdder.add();
        });
    }

    private void addLimitsGivenDirection(double positiveLimit, BranchThresholdAdder branchThresholdAdder) {
        if (DIRECT.contains(criticalBranch.getDirection()) && !ucteFlowElementHelper.isInvertedInNetwork()
            || OPPOSITE.contains(criticalBranch.getDirection()) && ucteFlowElementHelper.isInvertedInNetwork()) {
            branchThresholdAdder.withMax(positiveLimit);
        }

        if (DIRECT.contains(criticalBranch.getDirection()) && ucteFlowElementHelper.isInvertedInNetwork()
            || OPPOSITE.contains(criticalBranch.getDirection()) && !ucteFlowElementHelper.isInvertedInNetwork()) {
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
