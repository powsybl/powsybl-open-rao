package com.farao_community.farao.data.crac_util;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintInCountry;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class CracValidator {

    private CracValidator() {
        // should not be used
    }

    public static List<String> validateCrac(Crac crac, Network network) {
        // TODO : add unit test
        List<String> report = new ArrayList<>();
        report.addAll(addOutageCnecsForAutoCnecsWithoutRas(crac, network));
        return report;
    }

    /**
     * Since auto CNECs that have no RA associated cannot be secured by the RAO, this function duplicates these CNECs
     * but on the OUTAGE instant.
     * Beware that the CRAC is modified since extra CNECs are added.
     */
    private static List<String> addOutageCnecsForAutoCnecsWithoutRas(Crac crac, Network network) {
        List<String> report = new ArrayList<>();
        crac.getStates(Instant.AUTO).forEach(state ->
            crac.getFlowCnecs(state).forEach(cnec -> {
                    if (crac.getRemedialActions().stream().noneMatch(ra -> isRaUsefulForCnec(ra, cnec, network))) {
                        report.add(String.format("[ADDED] CNEC %s has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO.", cnec.getId()));
                        FlowCnecAdder adder = crac.newFlowCnec()
                            .withId(cnec.getId() + " - HYPOTHETICAL ON OUTAGE INSTANT")
                            .withNetworkElement(cnec.getNetworkElement().getId())
                            .withIMax(cnec.getIMax(Side.LEFT), Side.LEFT)
                            .withIMax(cnec.getIMax(Side.RIGHT), Side.RIGHT)
                            .withNominalVoltage(cnec.getNominalVoltage(Side.LEFT), Side.LEFT)
                            .withNominalVoltage(cnec.getNominalVoltage(Side.RIGHT), Side.RIGHT)
                            .withReliabilityMargin(cnec.getReliabilityMargin())
                            .withInstant(Instant.OUTAGE).withContingency(cnec.getState().getContingency().orElseThrow().getId())
                            .withOptimized(cnec.isOptimized())
                            .withMonitored(cnec.isMonitored());
                        cnec.getThresholds().forEach(tr -> {
                                BranchThresholdAdder trAdder = adder.newThreshold()
                                    .withSide(tr.getSide())
                                    .withUnit(tr.getUnit());
                                if (tr.limitsByMax()) {
                                    trAdder.withMax(tr.max().orElseThrow());
                                }
                                if (tr.limitsByMin()) {
                                    trAdder.withMin(tr.min().orElseThrow());
                                }
                                trAdder.add();
                            }
                        );
                        adder.add();
                    }
                }
            )
        );
        return report;
    }

    private static boolean isRaUsefulForCnec(RemedialAction<?> ra, FlowCnec cnec, Network network) {
        if (Set.of(UsageMethod.AVAILABLE, UsageMethod.FORCED).contains(ra.getUsageMethod(cnec.getState()))) {
            return true;
        }
        if (ra.getUsageMethod(cnec.getState()).equals(UsageMethod.TO_BE_EVALUATED)) {
            return ra.getUsageRules().stream()
                .filter(OnFlowConstraint.class::isInstance)
                .map(OnFlowConstraint.class::cast)
                .anyMatch(ofc -> isOfcUsefulForCnec(ofc, cnec))
                ||
                ra.getUsageRules().stream()
                    .filter(OnFlowConstraintInCountry.class::isInstance)
                    .map(OnFlowConstraintInCountry.class::cast)
                    .anyMatch(ofc -> isOfccUsefulForCnec(ofc, cnec, network));
        }
        return false;
    }

    private static boolean isOfcUsefulForCnec(OnFlowConstraint ofc, FlowCnec cnec) {
        return ofc.getFlowCnec().equals(cnec);
    }

    private static boolean isOfccUsefulForCnec(OnFlowConstraintInCountry ofcc, FlowCnec cnec, Network network) {
        return cnec.getLocation(network).stream().anyMatch(
            country -> country.isPresent() && country.get().equals(ofcc.getCountry())
        );
    }
}
