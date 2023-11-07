/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.TsoEICode;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialActionAdder;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.util.FaraoImportException;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;
import java.util.function.Function;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CsaProfileRemedialActionsCreator {
    private final Crac crac;
    private final Network network;
    private final PropertyBags gridStateAlterationRemedialActionPropertyBags;
    private final PropertyBags topologyActionsPropertyBags;
    private final PropertyBags rotatingMachineActionsPropertyBags;
    private final PropertyBags shuntCompensatorModificationsPropertyBags;
    private final PropertyBags tapPositionPropertyBags;
    private final PropertyBags staticPropertyRangesPropertyBags;
    private final PropertyBags contingencyWithRemedialActionsPropertyBags;
    private final OnConstraintUsageRuleHelper onConstraintUsageRuleHelper;
    private final CsaProfileCracCreationContext cracCreationContext;
    Set<CsaProfileElementaryCreationContext> csaProfileRemedialActionCreationContexts = new HashSet<>();

    private final PropertyBags remedialActionsSchedulePropertyBags;
    private final PropertyBags schemeRemedialActionsPropertyBags;
    private final PropertyBags remedialActionSchemePropertyBags;
    private final PropertyBags stagePropertyBags;
    private final PropertyBags gridStateAlterationCollectionPropertyBags;
    private final PropertyBags topologyActionAutoPropertyBags;
    private final PropertyBags rotatingMachineActionAutoPropertyBags;
    private final PropertyBags shuntCompensatorModificationAutoPropertyBags;
    private final PropertyBags tapPositionActionsAutoPropertyBags;

    public CsaProfileRemedialActionsCreator(Crac crac, Network network, CsaProfileCracCreationContext cracCreationContext, PropertyBags gridStateAlterationRemedialActionPropertyBags, PropertyBags contingencyWithRemedialActionsPropertyBags,
                                            PropertyBags topologyActionsPropertyBags,
                                            PropertyBags rotatingMachineActionsPropertyBags,
                                            PropertyBags shuntCompensatorModificationsPropertyBags,
                                            PropertyBags tapPositionPropertyBags,
                                            PropertyBags staticPropertyRangesPropertyBags,
                                            OnConstraintUsageRuleHelper onConstraintUsageRuleHelper,
                                            PropertyBags remedialActionsSchedulePropertyBags,
                                            PropertyBags schemeRemedialActionsPropertyBags,
                                            PropertyBags remedialActionSchemePropertyBags,
                                            PropertyBags stagePropertyBags,
                                            PropertyBags gridStateAlterationCollectionPropertyBags,
                                            PropertyBags topologyActionAutoPropertyBags,
                                            PropertyBags rotatingMachineActionAutoPropertyBags,
                                            PropertyBags shuntCompensatorModificationAutoPropertyBags,
                                            PropertyBags tapPositionActionsAutoPropertyBags) {
        this.crac = crac;
        this.network = network;
        this.gridStateAlterationRemedialActionPropertyBags = gridStateAlterationRemedialActionPropertyBags;
        this.contingencyWithRemedialActionsPropertyBags = contingencyWithRemedialActionsPropertyBags;
        this.topologyActionsPropertyBags = topologyActionsPropertyBags;
        this.rotatingMachineActionsPropertyBags = rotatingMachineActionsPropertyBags;
        this.shuntCompensatorModificationsPropertyBags = shuntCompensatorModificationsPropertyBags;
        this.tapPositionPropertyBags = tapPositionPropertyBags;
        this.staticPropertyRangesPropertyBags = staticPropertyRangesPropertyBags;
        this.cracCreationContext = cracCreationContext;
        this.onConstraintUsageRuleHelper = onConstraintUsageRuleHelper;
        this.remedialActionsSchedulePropertyBags = remedialActionsSchedulePropertyBags;
        this.schemeRemedialActionsPropertyBags = schemeRemedialActionsPropertyBags;
        this.remedialActionSchemePropertyBags = remedialActionSchemePropertyBags;
        this.stagePropertyBags = stagePropertyBags;
        this.gridStateAlterationCollectionPropertyBags = gridStateAlterationCollectionPropertyBags;
        this.topologyActionAutoPropertyBags = topologyActionAutoPropertyBags;
        this.rotatingMachineActionAutoPropertyBags = rotatingMachineActionAutoPropertyBags;
        this.shuntCompensatorModificationAutoPropertyBags = shuntCompensatorModificationAutoPropertyBags;
        this.tapPositionActionsAutoPropertyBags = tapPositionActionsAutoPropertyBags;
        createAndAddRemedialActions();
        createAutoRemedialActions();
    }

    private void createAndAddRemedialActions() {
        Map<String, Set<PropertyBag>> linkedContingencyWithRAs = CsaProfileCracUtils.getMappedPropertyBagsSet(contingencyWithRemedialActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedTopologyActions = CsaProfileCracUtils.getMappedPropertyBagsSet(topologyActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedRotatingMachineActions = CsaProfileCracUtils.getMappedPropertyBagsSet(rotatingMachineActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedShuntCompensatorModifications = CsaProfileCracUtils.getMappedPropertyBagsSet(shuntCompensatorModificationsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedTapPositionActions = CsaProfileCracUtils.getMappedPropertyBagsSet(tapPositionPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedStaticPropertyRanges = CsaProfileCracUtils.getMappedPropertyBagsSet(staticPropertyRangesPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION); // the id here is the id of the subclass of gridStateAlteration (tapPositionAction, RotatingMachine, ..)

        NetworkActionCreator networkActionCreator = new NetworkActionCreator(crac, network);
        PstRangeActionCreator pstRangeActionCreator = new PstRangeActionCreator(crac, network);

        for (PropertyBag parentRemedialActionPropertyBag : gridStateAlterationRemedialActionPropertyBags) {
            String remedialActionId = parentRemedialActionPropertyBag.get(CsaProfileConstants.MRID);

            try {
                List<String> alterations = new ArrayList<>();
                RemedialActionType remedialActionType = checkRemedialActionCanBeImportedAndIdentifyType(parentRemedialActionPropertyBag, linkedTopologyActions, linkedRotatingMachineActions, linkedShuntCompensatorModifications, linkedTapPositionActions, linkedStaticPropertyRanges, CsaProfileConstants.CsaProfile.REMEDIAL_ACTION);
                RemedialActionAdder<?> remedialActionAdder;
                String nativeRaName = parentRemedialActionPropertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME);
                String tsoName = parentRemedialActionPropertyBag.get(CsaProfileConstants.TSO);
                Optional<String> targetRemedialActionNameOpt = CsaProfileCracUtils.createRemedialActionName(nativeRaName, tsoName);
                Optional<Integer> speedOpt = getSpeedOpt(parentRemedialActionPropertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT));

                if (remedialActionType.equals(RemedialActionType.NETWORK_ACTION)) {
                    remedialActionAdder = networkActionCreator.getNetworkActionAdder(linkedTopologyActions, linkedRotatingMachineActions, linkedShuntCompensatorModifications, linkedStaticPropertyRanges, remedialActionId, remedialActionId);
                } else {
                    remedialActionAdder = pstRangeActionCreator.getPstRangeActionAdder(linkedTapPositionActions, linkedStaticPropertyRanges, remedialActionId, remedialActionId);
                }

                targetRemedialActionNameOpt.ifPresent(remedialActionAdder::withName);
                if (tsoName != null) {
                    remedialActionAdder.withOperator(TsoEICode.fromEICode(tsoName.substring(tsoName.lastIndexOf("/") + 1)).getDisplayName());
                }
                speedOpt.ifPresent(remedialActionAdder::withSpeed);

                if (linkedContingencyWithRAs.containsKey(remedialActionId)) {
                    // on state usage rule
                    String randomCombinationConstraintKind = linkedContingencyWithRAs.get(remedialActionId).iterator().next().get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND);
                    checkElementCombinationConstraintKindsCoherence(remedialActionId, linkedContingencyWithRAs);

                    List<String> faraoContingenciesIds = linkedContingencyWithRAs.get(remedialActionId).stream()
                        .map(contingencyWithRemedialActionPropertyBag ->
                            checkContingencyAndGetFaraoId(
                                contingencyWithRemedialActionPropertyBag,
                                parentRemedialActionPropertyBag.get(CsaProfileConstants.RA_KIND),
                                remedialActionId,
                                randomCombinationConstraintKind
                            )
                        )
                        .toList();

                    boolean hasAtLeastOneOnConstraintUsageRule = addOnConstraintUsageRules(Instant.CURATIVE, remedialActionAdder, remedialActionId, alterations);
                    if (!hasAtLeastOneOnConstraintUsageRule) {
                        if (!randomCombinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString())) {
                            addOnContingencyStateUsageRules(remedialActionAdder, faraoContingenciesIds, randomCombinationConstraintKind, Instant.CURATIVE);
                        } else {
                            alterations.add(String.format("The association 'RemedialAction'/'Contingencies' '%s'/'%s' will be ignored because 'excluded' combination constraint kind is not supported", remedialActionId, String.join(". ", faraoContingenciesIds)));
                        }
                    }
                } else { // no contingency linked to RA --> on instant usage rule
                    String kind = parentRemedialActionPropertyBag.get(CsaProfileConstants.RA_KIND);
                    if (kind.equals(CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString())) {
                        boolean hasAtLeastOneOnConstraintUsageRule = addOnConstraintUsageRules(Instant.PREVENTIVE, remedialActionAdder, remedialActionId, alterations);
                        if (!hasAtLeastOneOnConstraintUsageRule) {
                            remedialActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add();
                        }
                    } else {
                        boolean hasAtLeastOneOnConstraintUsageRule = addOnConstraintUsageRules(Instant.CURATIVE, remedialActionAdder, remedialActionId, alterations);
                        if (!hasAtLeastOneOnConstraintUsageRule) {
                            remedialActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.CURATIVE).add();
                        }
                    }
                }
                remedialActionAdder.add();
                if (alterations.isEmpty()) {
                    csaProfileRemedialActionCreationContexts.add(CsaProfileElementaryCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), "", false));
                } else {
                    csaProfileRemedialActionCreationContexts.add(CsaProfileElementaryCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), String.join(". ", alterations), true));
                }

            } catch (FaraoImportException e) {
                csaProfileRemedialActionCreationContexts.add(CsaProfileElementaryCreationContext.notImported(remedialActionId, e.getImportStatus(), e.getMessage()));
            }
        }
        this.cracCreationContext.setRemedialActionCreationContexts(csaProfileRemedialActionCreationContexts);
    }

    private void addOnContingencyStateUsageRules(RemedialActionAdder<?> remedialActionAdder, List<String> faraoContingenciesIds, String randomCombinationConstraintKind, Instant instant) {
        UsageMethod usageMethod = CsaProfileCracUtils.getConstraintToUsageMethodMap().get(randomCombinationConstraintKind);
        faraoContingenciesIds.forEach(faraoContingencyId -> remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(instant)
            .withContingency(faraoContingencyId)
            .withUsageMethod(usageMethod).add());
    }

    private void checkElementCombinationConstraintKindsCoherence(String remedialActionId, Map<String, Set<PropertyBag>> linkedContingencyWithRAs) {
        // The same contingency cannot have different Element Combination Constraint Kinds
        // The same contingency can appear several times, so we need to create a memory system
        Set<String> contingenciesWithIncluded = new HashSet<>();
        Set<String> contingenciesWithConsidered = new HashSet<>();
        Set<PropertyBag> linkedContingencyWithRA = linkedContingencyWithRAs.get(remedialActionId);
        for (PropertyBag propertyBag : linkedContingencyWithRA) {
            String combinationKind = propertyBag.get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND);
            String contingencyId = propertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY);
            if (combinationKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString())) {
                contingenciesWithIncluded.add(contingencyId);
            } else if (combinationKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
                contingenciesWithConsidered.add(contingencyId);
            } else if (combinationKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString())) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because of an illegal EXCLUDED ElementCombinationConstraintKind");
            } else {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because of an invalid Element Combination Constraint Kind");
            }
            if (contingenciesWithIncluded.contains(contingencyId) && contingenciesWithConsidered.contains(contingencyId)) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because the ElementCombinationConstraintKinds that link the remedial action to the contingency " + contingencyId + " are different");
            }
        }
    }

    private RemedialActionType checkRemedialActionCanBeImportedAndIdentifyType(PropertyBag remedialActionPropertyBag, Map<String, Set<PropertyBag>> linkedTopologyActions, Map<String, Set<PropertyBag>> linkedRotatingMachineActions, Map<String, Set<PropertyBag>> linkedShuntCompensatorModifications, Map<String, Set<PropertyBag>> linkedTapPositionActions, Map<String, Set<PropertyBag>> linkedStaticPropertyRanges, CsaProfileConstants.CsaProfile profileKeyword) {
        String remedialActionId = remedialActionPropertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);

        String kind = remedialActionPropertyBag.get(CsaProfileConstants.RA_KIND);

        boolean normalAvailable = Boolean.parseBoolean(remedialActionPropertyBag.get(CsaProfileConstants.NORMAL_AVAILABLE));

        CsaProfileConstants.HeaderValidity headerValidity = CsaProfileCracUtils.checkProfileHeader(remedialActionPropertyBag, profileKeyword, cracCreationContext.getTimeStamp());
        if (headerValidity == CsaProfileConstants.HeaderValidity.INVALID_KEYWORD) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Model.keyword must be " + profileKeyword.getKeyword());
        } else if (headerValidity == CsaProfileConstants.HeaderValidity.INVALID_INTERVAL) {
            throw new FaraoImportException(ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP, "Required timestamp does not fall between Model.startDate and Model.endDate");
        }

        if (!normalAvailable) {
            throw new FaraoImportException(ImportStatus.NOT_FOR_RAO, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because RemedialAction.normalAvailable must be 'true' to be imported");
        }
        if (!kind.equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString()) && !kind.equals(CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because Unsupported kind for remedial action" + remedialActionId);
        }

        if (linkedTopologyActions.containsKey(remedialActionId)) {
            return RemedialActionType.NETWORK_ACTION;
        } else if (linkedRotatingMachineActions.containsKey(remedialActionId)) {
            checkEachInjectionSetPointActionHasExactlyOneStaticPropertyRangeElseThrowException(remedialActionId, linkedRotatingMachineActions.get(remedialActionId), linkedStaticPropertyRanges);
            return RemedialActionType.NETWORK_ACTION;
        } else if (linkedShuntCompensatorModifications.containsKey(remedialActionId)) {
            checkEachInjectionSetPointActionHasExactlyOneStaticPropertyRangeElseThrowException(remedialActionId, linkedShuntCompensatorModifications.get(remedialActionId), linkedStaticPropertyRanges);
            return RemedialActionType.NETWORK_ACTION;
        } else if (linkedTapPositionActions.containsKey(remedialActionId)) { // StaticPropertyRanges not mandatory in case of tapPositionsActions
            return RemedialActionType.PST_RANGE_ACTION;
        } else {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because there is no topology actions, no Set point actions, nor tap position action linked to that RA");
        }
    }

    private void checkEachInjectionSetPointActionHasExactlyOneStaticPropertyRangeElseThrowException(String remedialActionId, Set<PropertyBag> injectionSetPointActionsForOneRa, Map<String, Set<PropertyBag>> staticPropertyRangesLinkedToInjectionSetPointActions) {
        for (PropertyBag injectionSetPointAction : injectionSetPointActionsForOneRa) {
            Set<PropertyBag> staticPropertyRangePropertyBags = staticPropertyRangesLinkedToInjectionSetPointActions.get(injectionSetPointAction.getId("mRID"));
            if (staticPropertyRangePropertyBags != null) {
                if (staticPropertyRangePropertyBags.size() == 0) {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because there is no StaticPropertyRange linked to that RA");
                } else if (staticPropertyRangePropertyBags.size() > 1) {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because several conflictual StaticPropertyRanges are linked to that RA's injection set point action");
                }
            }
        }
    }

    private String checkContingencyAndGetFaraoId(PropertyBag contingencyWithRemedialActionPropertyBag, String raKind, String
        remedialActionId, String combinationConstraintKind) {
        if (!raKind.equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because it is linked to a contingency but it's kind is not curative");
        }

        if (!combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because combinationConstraintKind of a ContingencyWithRemedialAction must be 'included, 'excluded' or 'considered', but it was: " + combinationConstraintKind);
        }

        String contingencyId = contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).substring(contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).lastIndexOf("_") + 1);
        Optional<CsaProfileElementaryCreationContext> importedCsaProfileContingencyCreationContextOpt = cracCreationContext.getContingencyCreationContexts().stream().filter(co -> co.isImported() && co.getNativeId().equals(contingencyId)).findAny();
        if (importedCsaProfileContingencyCreationContextOpt.isEmpty()) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because contingency" + contingencyId + " linked to that remedialAction does not exist or was not imported by farao");
        } else {
            String faraoContingencyId = importedCsaProfileContingencyCreationContextOpt.get().getElementId();
            CsaProfileCracUtils.checkNormalEnabled(contingencyWithRemedialActionPropertyBag, remedialActionId, "ContingencyWithRemedialAction");
            return faraoContingencyId;
        }
    }

    private Optional<Integer> getSpeedOpt(String timeToImplement) {
        if (timeToImplement != null) {
            return Optional.of(CsaProfileCracUtils.convertDurationToSeconds(timeToImplement));
        } else {
            return Optional.empty();
        }
    }

    enum RemedialActionType {
        PST_RANGE_ACTION,
        NETWORK_ACTION
    }

    private boolean addOnConstraintUsageRules(Instant remedialActionInstant, RemedialActionAdder remedialActionAdder, String importableRemedialActionId, List<String> alterations) {
        boolean flag1 = false;
        boolean flag2;
        boolean flag3;
        if (!onConstraintUsageRuleHelper.getExcludedCnecsByRemedialAction().containsKey(importableRemedialActionId)) {
            flag1 = processAvailableAssessedElementsCombinableWithRemedialActions(remedialActionInstant, remedialActionAdder, UsageMethod.AVAILABLE);
        } else {
            alterations.add(String.format("The association 'RemedialAction'/'Cnecs' '%s'/'%s' will be ignored because 'excluded' combination constraint kind is not supported", importableRemedialActionId, String.join(". ", onConstraintUsageRuleHelper.getExcludedCnecsByRemedialAction().get(importableRemedialActionId))));
        }
        flag2 = processAssessedElementsWithRemedialActions(remedialActionInstant, remedialActionAdder, importableRemedialActionId, UsageMethod.AVAILABLE, onConstraintUsageRuleHelper.getConsideredCnecsElementsByRemedialAction());
        flag3 = processAssessedElementsWithRemedialActions(remedialActionInstant, remedialActionAdder, importableRemedialActionId, UsageMethod.FORCED, onConstraintUsageRuleHelper.getIncludedCnecsByRemedialAction());
        return flag1 || flag2 || flag3;
    }

    private boolean processAvailableAssessedElementsCombinableWithRemedialActions(Instant remedialActionInstant, RemedialActionAdder remedialActionAdder, UsageMethod usageMethod) {
        List<Boolean> flags = onConstraintUsageRuleHelper.getImportedCnecsCombinableWithRas().stream().map(addOnConstraintUsageRuleForCnec(remedialActionInstant, remedialActionAdder, usageMethod)).toList();
        return flags.contains(true);
    }

    private boolean processAssessedElementsWithRemedialActions(Instant remedialActionInstant, RemedialActionAdder remedialActionAdder, String importableRemedialActionId, UsageMethod usageMethod, Map<String, Set<String>> cnecsByRemedialAction) {
        if (cnecsByRemedialAction.containsKey(importableRemedialActionId)) {
            List<Boolean> flags = cnecsByRemedialAction.get(importableRemedialActionId).stream().map(addOnConstraintUsageRuleForCnec(remedialActionInstant, remedialActionAdder, usageMethod)).toList();
            return flags.contains(true);
        }
        return false;
    }

    private Function<String, Boolean> addOnConstraintUsageRuleForCnec(Instant remedialActionInstant, RemedialActionAdder remedialActionAdder, UsageMethod usageMethod) {
        return cnecId -> {
            Cnec cnec = crac.getCnec(cnecId);
            if (isOnConstraintInstantCoherent(cnec.getState().getInstant(), remedialActionInstant)) {
                if (cnec instanceof FlowCnec) {
                    remedialActionAdder.newOnFlowConstraintUsageRule()
                        .withInstant(remedialActionInstant)
                        .withFlowCnec(cnecId)
                        .add();
                    // TODO add .withUsageMethod(usageMethod) when API of OnFlowConstraintAdder is ready
                    return true;
                } else if (cnec instanceof VoltageCnec) {
                    remedialActionAdder.newOnVoltageConstraintUsageRule()
                        .withInstant(remedialActionInstant)
                        .withVoltageCnec(cnecId)
                        .add();
                    // TODO add .withUsageMethod(usageMethod) when API of OnFlowConstraintAdder is ready
                    return true;
                } else if (cnec instanceof AngleCnec) {
                    remedialActionAdder.newOnAngleConstraintUsageRule()
                        .withInstant(remedialActionInstant)
                        .withAngleCnec(cnecId)
                        .add();
                    // TODO add .withUsageMethod(usageMethod) when API of OnFlowConstraintAdder is ready
                    return true;
                } else {
                    throw new FaraoException(String.format("Unsupported cnec type %s", cnec.getClass().toString()));
                }
            }
            return false;
        };
    }

    public static boolean isOnConstraintInstantCoherent(Instant cnecInstant, Instant remedialInstant) {
        switch (remedialInstant) {
            case PREVENTIVE:
                return cnecInstant == Instant.PREVENTIVE || cnecInstant == Instant.OUTAGE || cnecInstant == Instant.CURATIVE;
            case AUTO:
                return cnecInstant == Instant.AUTO;
            case CURATIVE:
                return cnecInstant == Instant.CURATIVE;
            default:
                return false;
        }
    }

    private void checkProfileHeader(PropertyBags propertyBags, CsaProfileConstants.CsaProfile profileKeyword) {
        propertyBags.forEach(remedialActionsSchedulePropertyBag -> {
            CsaProfileConstants.HeaderValidity headerValidity = CsaProfileCracUtils.checkProfileHeader(remedialActionsSchedulePropertyBag, profileKeyword, cracCreationContext.getTimeStamp());
            if (headerValidity == CsaProfileConstants.HeaderValidity.INVALID_KEYWORD) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Model.keyword must be " + CsaProfileConstants.CsaProfile.REMEDIAL_ACTION_SCHEDULE);
            } else if (headerValidity == CsaProfileConstants.HeaderValidity.INVALID_INTERVAL) {
                throw new FaraoImportException(ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP, "Required timestamp does not fall between Model.startDate and Model.endDate");
            }
        });
    }

    private void createAutoRemedialActions() {
        NetworkActionCreator networkActionCreator = new NetworkActionCreator(crac, network);
        PstRangeActionCreator pstRangeActionCreator = new PstRangeActionCreator(crac, network);
        Map<String, Set<PropertyBag>> linkedStaticPropertyRanges = CsaProfileCracUtils.getMappedPropertyBagsSet(staticPropertyRangesPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION); // the id here is the id of the subclass of gridStateAlteration (tapPositionAction, RotatingMachine, ..)
        Map<String, Set<PropertyBag>> linkedContingencyWithRAs = CsaProfileCracUtils.getMappedPropertyBagsSet(contingencyWithRemedialActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedTopologyActionsAuto = CsaProfileCracUtils.getMappedPropertyBagsSet(topologyActionAutoPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);
        Map<String, Set<PropertyBag>> linkedRotatingMachineActionsAuto = CsaProfileCracUtils.getMappedPropertyBagsSet(rotatingMachineActionAutoPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);
        Map<String, Set<PropertyBag>> linkedShuntCompensatorModificationAuto = CsaProfileCracUtils.getMappedPropertyBagsSet(shuntCompensatorModificationAutoPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);
        Map<String, Set<PropertyBag>> linkedTapPositionActionsAuto = CsaProfileCracUtils.getMappedPropertyBagsSet(tapPositionActionsAutoPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);

        checkProfileHeader(remedialActionsSchedulePropertyBags, CsaProfileConstants.CsaProfile.REMEDIAL_ACTION_SCHEDULE);

        Map<PropertyBag, PropertyBag> schemeRemedialActionToRemedialActionSchemeMap = associateTwoPropertyBags(new HashSet<>(schemeRemedialActionsPropertyBags),
            new HashSet<>(remedialActionSchemePropertyBags),
            CsaProfileConstants.REMEDIAL_ACTION_SCHEME,
            CsaProfileConstants.MRID,
            "SchemeRemedialAction must have exactly one associated RemedialActionScheme");

        Map<PropertyBag, PropertyBag> remedialActionSchemeToStageMap = associateTwoPropertyBags((Set<PropertyBag>) schemeRemedialActionToRemedialActionSchemeMap.values(),
            new HashSet<>(stagePropertyBags),
            CsaProfileConstants.MRID,
            CsaProfileConstants.REMEDIAL_ACTION_SCHEME,
            "RemedialActionScheme must have exactly one associated Stage");

        Map<PropertyBag, PropertyBag> stageToGridStateAlterationCollectionMap = associateTwoPropertyBags((Set<PropertyBag>) remedialActionSchemeToStageMap.values(),
            new HashSet<>(gridStateAlterationCollectionPropertyBags),
            CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION,
            CsaProfileConstants.MRID,
            "Stage must have exactly one associated GridStateAlterationCollection");

        schemeRemedialActionToRemedialActionSchemeMap.forEach((schemeRemedialActionsPropertyBag, remedialActionSchemePropertyBag) -> {
            String autoRemedialActionId = schemeRemedialActionsPropertyBag.get(CsaProfileConstants.MRID);
            try {
                String raKind = schemeRemedialActionsPropertyBag.get(CsaProfileConstants.RA_KIND);
                boolean normalAvailable = Boolean.parseBoolean(schemeRemedialActionsPropertyBag.get(CsaProfileConstants.NORMAL_AVAILABLE));
                if (!normalAvailable) {
                    throw new FaraoImportException(ImportStatus.NOT_FOR_RAO, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + autoRemedialActionId + " will not be imported because RemedialAction.normalAvailable must be 'true' to be imported");
                }
                if (!raKind.equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString())) {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + autoRemedialActionId + " will not be imported because auto remedial action musty be of curative kind");
                }

                String nativeRaName = schemeRemedialActionsPropertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME);
                String tsoName = schemeRemedialActionsPropertyBag.get(CsaProfileConstants.TSO);
                Optional<String> targetAutoRemedialActionNameOpt = CsaProfileCracUtils.createRemedialActionName(nativeRaName, tsoName);
                Optional<Integer> speedOpt = getSpeedOpt(schemeRemedialActionsPropertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT));

                String remedialActionSchemeKind = remedialActionSchemePropertyBag.get(CsaProfileConstants.RA_KIND);
                if (!remedialActionSchemeKind.equals(CsaProfileConstants.SIPS)) {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + autoRemedialActionId + " will not be imported because Unsupported kind for remedial action schedule");
                }
                String remedialActionSchemeNormalArmed = remedialActionSchemePropertyBag.get(CsaProfileConstants.NORMAL_ARMED);
                if (!Boolean.parseBoolean(remedialActionSchemeNormalArmed)) {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + autoRemedialActionId + " will not be imported because normalArmed must be set to true");
                }

                PropertyBag stage = remedialActionSchemeToStageMap.get(remedialActionSchemePropertyBag);
                PropertyBag gridStateAlterationCollection = stageToGridStateAlterationCollectionMap.get(stage);

                String collectionId = gridStateAlterationCollection.get(CsaProfileConstants.MRID);

                RemedialActionType remedialActionType;
                if (linkedTopologyActionsAuto.containsKey(collectionId)) {
                    remedialActionType = RemedialActionType.NETWORK_ACTION;
                } else if (linkedRotatingMachineActionsAuto.containsKey(collectionId)) {
                    checkEachInjectionSetPointActionHasExactlyOneStaticPropertyRangeElseThrowException(autoRemedialActionId, linkedRotatingMachineActionsAuto.get(collectionId), linkedStaticPropertyRanges);
                    remedialActionType = RemedialActionType.NETWORK_ACTION;
                } else if (linkedShuntCompensatorModificationAuto.containsKey(collectionId)) {
                    checkEachInjectionSetPointActionHasExactlyOneStaticPropertyRangeElseThrowException(autoRemedialActionId, linkedShuntCompensatorModificationAuto.get(collectionId), linkedStaticPropertyRanges);
                    remedialActionType = RemedialActionType.NETWORK_ACTION;
                } else if (linkedTapPositionActionsAuto.containsKey(collectionId)) { // StaticPropertyRanges not mandatory in case of tapPositionsActions
                    remedialActionType = RemedialActionType.PST_RANGE_ACTION;
                } else {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + collectionId + " will not be imported because there is no topology actions, no Set point actions, nor tap position action linked to that ARA");
                }

                RemedialActionAdder<?> remedialActionAdder;

                if (remedialActionType.equals(RemedialActionType.NETWORK_ACTION)) {
                    remedialActionAdder = networkActionCreator.getNetworkActionAdder(linkedTopologyActionsAuto, linkedRotatingMachineActionsAuto, linkedShuntCompensatorModificationAuto, linkedStaticPropertyRanges, collectionId, autoRemedialActionId);
                } else {
                    remedialActionAdder = pstRangeActionCreator.getPstRangeActionAdder(linkedTapPositionActionsAuto, linkedStaticPropertyRanges, collectionId, autoRemedialActionId);
                }

                targetAutoRemedialActionNameOpt.ifPresent(remedialActionAdder::withName);
                if (tsoName != null) {
                    remedialActionAdder.withOperator(TsoEICode.fromEICode(tsoName.substring(tsoName.lastIndexOf("/") + 1)).getDisplayName());
                }
                speedOpt.ifPresent(remedialActionAdder::withSpeed);

                if (linkedContingencyWithRAs.containsKey(autoRemedialActionId)) {
                    // on state usage rule
                    checkElementCombinationConstraintKindsCoherence(autoRemedialActionId, linkedContingencyWithRAs);

                    List<String> faraoContingenciesIds = linkedContingencyWithRAs.get(autoRemedialActionId).stream()
                        .map(contingencyWithRemedialActionPropertyBag ->
                            checkContingencyAndGetFaraoId(
                                contingencyWithRemedialActionPropertyBag,
                                raKind,
                                autoRemedialActionId,
                                CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString()
                            )
                        )
                        .toList();

                    boolean hasAtLeastOneOnConstraintUsageRule = addOnConstraintUsageRules(Instant.CURATIVE, remedialActionAdder, autoRemedialActionId, new ArrayList<>());
                    if (!hasAtLeastOneOnConstraintUsageRule) {
                        addOnContingencyStateUsageRules(remedialActionAdder, faraoContingenciesIds, CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString(), Instant.AUTO);
                    }
                } else {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + autoRemedialActionId + " will not be imported because no contingency is linked to the remedial action");
                }
                remedialActionAdder.add();
                csaProfileRemedialActionCreationContexts.add(CsaProfileElementaryCreationContext.imported(autoRemedialActionId, autoRemedialActionId, targetAutoRemedialActionNameOpt.orElse(autoRemedialActionId), "", false));

            } catch (FaraoImportException e) {
                csaProfileRemedialActionCreationContexts.add(CsaProfileElementaryCreationContext.notImported(autoRemedialActionId, e.getImportStatus(), e.getMessage()));
            }
        });
        this.cracCreationContext.setRemedialActionCreationContexts(csaProfileRemedialActionCreationContexts);
    }

    private BiMap<PropertyBag, PropertyBag> associateTwoPropertyBags(Set<PropertyBag> propertyBags1, Set<PropertyBag> propertyBags2, String key1, String key2, String errorMessage) {
        Map<String, PropertyBag> propertyBags2ByIdMap = new HashMap<>();
        BiMap<PropertyBag, PropertyBag> propertyBags1ToPropertyBags2BiMap = HashBiMap.create();

        propertyBags2.forEach(propertyBag -> propertyBags2ByIdMap.put(CsaProfileCracUtils.removePrefix(propertyBag.get(key2)), propertyBag));

        propertyBags1.forEach(propertyBag -> {
            if (propertyBags2ByIdMap.containsKey(CsaProfileCracUtils.removePrefix(propertyBag.get(key1)))) {
                PropertyBag testedPropertyBag = propertyBags2ByIdMap.get(CsaProfileCracUtils.removePrefix(propertyBag.get(key1)));
                if (isAssociatedWithOnlyOneScheme(propertyBags1ToPropertyBags2BiMap, testedPropertyBag)) {
                    propertyBags1ToPropertyBags2BiMap.put(propertyBag, testedPropertyBag);
                } else {
                    // more than 1 element associated
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, errorMessage);
                }
            } else {
                // 0 element associated
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, errorMessage);
            }
        });
        return propertyBags1ToPropertyBags2BiMap;
    }

    // Helper method to check if RemedialActionScheme is associated with only one SchemeRemedialAction
    private boolean isAssociatedWithOnlyOneScheme(Map<PropertyBag, PropertyBag> schemeRaToRaScheme, PropertyBag testedRemedialActionScheme) {
        return schemeRaToRaScheme.keySet().stream().noneMatch(schemeRa -> schemeRa.get(CsaProfileConstants.REMEDIAL_ACTION_SCHEME).equals(testedRemedialActionScheme.get(CsaProfileConstants.MRID)));
    }

}
