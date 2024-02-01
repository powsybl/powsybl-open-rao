/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TsoEICode;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.RemedialActionAdder;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.COMBINATION_CONSTRAINT_KIND;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.NORMAL_ENABLED;

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
    private final PropertyBags tapPositionActionsPropertyBags;
    private final PropertyBags staticPropertyRangesPropertyBags;
    private final PropertyBags contingencyWithRemedialActionsPropertyBags;
    private final OnConstraintUsageRuleHelper onConstraintUsageRuleHelper;
    private final CsaProfileCracCreationContext cracCreationContext;
    Set<CsaProfileElementaryCreationContext> csaProfileRemedialActionCreationContexts = new HashSet<>();

    private final PropertyBags schemeRemedialActionsPropertyBags;
    private final PropertyBags remedialActionSchemePropertyBags;
    private final PropertyBags stagePropertyBags;
    private final PropertyBags gridStateAlterationCollectionPropertyBags;

    public CsaProfileRemedialActionsCreator(Crac crac, Network network, CsaProfileCracCreationContext cracCreationContext, PropertyBags gridStateAlterationRemedialActionPropertyBags, PropertyBags contingencyWithRemedialActionsPropertyBags,
                                            PropertyBags topologyActionsPropertyBags,
                                            PropertyBags rotatingMachineActionsPropertyBags,
                                            PropertyBags shuntCompensatorModificationsPropertyBags,
                                            PropertyBags tapPositionActionsPropertyBags,
                                            PropertyBags staticPropertyRangesPropertyBags,
                                            OnConstraintUsageRuleHelper onConstraintUsageRuleHelper,
                                            PropertyBags schemeRemedialActionsPropertyBags,
                                            PropertyBags remedialActionSchemePropertyBags,
                                            PropertyBags stagePropertyBags,
                                            PropertyBags gridStateAlterationCollectionPropertyBags) {
        this.crac = crac;
        this.network = network;
        this.gridStateAlterationRemedialActionPropertyBags = gridStateAlterationRemedialActionPropertyBags;
        this.contingencyWithRemedialActionsPropertyBags = contingencyWithRemedialActionsPropertyBags;
        this.topologyActionsPropertyBags = topologyActionsPropertyBags;
        this.rotatingMachineActionsPropertyBags = rotatingMachineActionsPropertyBags;
        this.shuntCompensatorModificationsPropertyBags = shuntCompensatorModificationsPropertyBags;
        this.tapPositionActionsPropertyBags = tapPositionActionsPropertyBags;
        this.staticPropertyRangesPropertyBags = staticPropertyRangesPropertyBags;
        this.cracCreationContext = cracCreationContext;
        this.onConstraintUsageRuleHelper = onConstraintUsageRuleHelper;
        this.schemeRemedialActionsPropertyBags = schemeRemedialActionsPropertyBags;
        this.remedialActionSchemePropertyBags = remedialActionSchemePropertyBags;
        this.stagePropertyBags = stagePropertyBags;
        this.gridStateAlterationCollectionPropertyBags = gridStateAlterationCollectionPropertyBags;
        createAndAddRemedialActions();
        createAutoRemedialActions();
    }

    private PropertyBags filterElementaryActions(PropertyBags elementaryActionsPropertyBags, boolean autoRemedialAction) {
        String parentRemedialActionField = autoRemedialAction ? CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION : CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION;
        Set<PropertyBag> relevantElementaryActionsPropertyBags = elementaryActionsPropertyBags.stream().filter(propertyBag -> Optional.ofNullable(propertyBag.get(parentRemedialActionField)).isPresent()).collect(Collectors.toSet());
        return new PropertyBags(relevantElementaryActionsPropertyBags);
    }

    private void createAndAddRemedialActions() {
        Map<String, Set<PropertyBag>> linkedContingencyWithRAs = CsaProfileCracUtils.getMappedPropertyBagsSet(contingencyWithRemedialActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedTopologyActions = CsaProfileCracUtils.getMappedPropertyBagsSet(filterElementaryActions(topologyActionsPropertyBags, false), CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedRotatingMachineActions = CsaProfileCracUtils.getMappedPropertyBagsSet(filterElementaryActions(rotatingMachineActionsPropertyBags, false), CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedShuntCompensatorModifications = CsaProfileCracUtils.getMappedPropertyBagsSet(filterElementaryActions(shuntCompensatorModificationsPropertyBags, false), CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedTapPositionActions = CsaProfileCracUtils.getMappedPropertyBagsSet(filterElementaryActions(tapPositionActionsPropertyBags, false), CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedStaticPropertyRanges = CsaProfileCracUtils.getMappedPropertyBagsSet(staticPropertyRangesPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION); // the id here is the id of the subclass of gridStateAlteration (tapPositionAction, RotatingMachine, ..)

        NetworkActionCreator networkActionCreator = new NetworkActionCreator(crac, network);
        PstRangeActionCreator pstRangeActionCreator = new PstRangeActionCreator(crac, network);

        for (PropertyBag parentRemedialActionPropertyBag : gridStateAlterationRemedialActionPropertyBags) {
            String remedialActionId = parentRemedialActionPropertyBag.get(CsaProfileConstants.MRID);

            try {
                List<String> alterations = new ArrayList<>();
                RemedialActionType remedialActionType = checkRemedialActionCanBeImportedAndIdentifyType(parentRemedialActionPropertyBag, linkedTopologyActions, linkedRotatingMachineActions, linkedShuntCompensatorModifications, linkedTapPositionActions, linkedStaticPropertyRanges);
                RemedialActionAdder<?> remedialActionAdder;
                String nativeRaName = parentRemedialActionPropertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME);
                String tsoName = parentRemedialActionPropertyBag.get(CsaProfileConstants.TSO);
                Optional<String> targetRemedialActionNameOpt = CsaProfileCracUtils.createElementName(nativeRaName, tsoName);
                Optional<Integer> speedOpt = getSpeedOpt(parentRemedialActionPropertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT), remedialActionId, false);

                if (remedialActionType.equals(RemedialActionType.NETWORK_ACTION)) {
                    remedialActionAdder = networkActionCreator.getNetworkActionAdder(linkedTopologyActions, linkedRotatingMachineActions, linkedShuntCompensatorModifications, linkedStaticPropertyRanges, remedialActionId, remedialActionId, alterations);
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
                    addOnStateUsageRules(parentRemedialActionPropertyBag, linkedContingencyWithRAs, remedialActionId, remedialActionAdder, alterations);
                } else { // no contingency linked to RA --> on instant usage rule
                    addOnInstantUsageRules(parentRemedialActionPropertyBag, remedialActionAdder, remedialActionId, alterations);
                }
                remedialActionAdder.add();
                if (alterations.isEmpty()) {
                    csaProfileRemedialActionCreationContexts.add(CsaProfileElementaryCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), "", false));
                } else {
                    csaProfileRemedialActionCreationContexts.add(CsaProfileElementaryCreationContext.imported(remedialActionId, remedialActionId, targetRemedialActionNameOpt.orElse(remedialActionId), String.join(". ", alterations), true));
                }

            } catch (OpenRaoImportException e) {
                csaProfileRemedialActionCreationContexts.add(CsaProfileElementaryCreationContext.notImported(remedialActionId, e.getImportStatus(), e.getMessage()));
            }
        }
        this.cracCreationContext.setRemedialActionCreationContexts(csaProfileRemedialActionCreationContexts);
    }

    private void addOnStateUsageRules(PropertyBag parentRemedialActionPropertyBag, Map<String, Set<PropertyBag>> linkedContingencyWithRAs, String remedialActionId, RemedialActionAdder<?> remedialActionAdder, List<String> alterations) {
        String randomCombinationConstraintKind = linkedContingencyWithRAs.get(remedialActionId).iterator().next().get(COMBINATION_CONSTRAINT_KIND);
        checkElementCombinationConstraintKindsCoherence(remedialActionId, linkedContingencyWithRAs);

        List<String> validContingenciesIds = new ArrayList<>();
        List<String> ignoredContingenciesMessages = new ArrayList<>();

        for (PropertyBag contingencyWithRemedialActionPropertyBag : linkedContingencyWithRAs.get(remedialActionId)) {
            if (!parentRemedialActionPropertyBag.get(CsaProfileConstants.RA_KIND).equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because it is linked to a contingency but it's kind is not curative");
            }
            String contingencyId = contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).substring(contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).lastIndexOf("_") + 1);
            if (!Boolean.parseBoolean(contingencyWithRemedialActionPropertyBag.get(NORMAL_ENABLED))) {
                ignoredContingenciesMessages.add("OnContingencyState usage rule for remedial action %s with contingency %s ignored because the link between the remedial action and the contingency is disabled or missing".formatted(remedialActionId, contingencyId));
                continue;
            }
            String combinationConstraintKind = contingencyWithRemedialActionPropertyBag.get(COMBINATION_CONSTRAINT_KIND);
            if (!combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
                ignoredContingenciesMessages.add("OnContingencyState usage rule for remedial action %s with contingency %s ignored because of an illegal combinationConstraintKind".formatted(remedialActionId, contingencyId));
                continue;
            }
            Optional<CsaProfileElementaryCreationContext> importedCsaProfileContingencyCreationContextOpt = cracCreationContext.getContingencyCreationContexts().stream().filter(co -> co.isImported() && co.getNativeId().equals(contingencyId)).findAny();
            if (importedCsaProfileContingencyCreationContextOpt.isEmpty()) {
                ignoredContingenciesMessages.add("OnContingencyState usage rule for remedial action %s with contingency %s ignored because this contingency does not exist or was not imported by Open RAO".formatted(remedialActionId, contingencyId));
                continue;
            }
            validContingenciesIds.add(importedCsaProfileContingencyCreationContextOpt.get().getElementId());
        }

        Instant curativeInstant = crac.getInstant(InstantKind.CURATIVE);
        boolean hasAtLeastOneOnConstraintUsageRule = addOnConstraintUsageRules(curativeInstant, remedialActionAdder, remedialActionId, alterations);
        if (!hasAtLeastOneOnConstraintUsageRule) {
            if (!randomCombinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString())) {
                addOnContingencyStateUsageRules(remedialActionAdder, validContingenciesIds, curativeInstant.getId(), UsageMethod.AVAILABLE);
                alterations.addAll(ignoredContingenciesMessages);
            } else {
                alterations.add(String.format("All OnContingencyState usage rules for remedial action %s ignored because of an illegal EXCLUDED combinationConstraintKind", remedialActionId));
            }
        }
        this.cracCreationContext.setRemedialActionCreationContexts(csaProfileRemedialActionCreationContexts);
    }

    private void addOnInstantUsageRules(PropertyBag parentRemedialActionPropertyBag, RemedialActionAdder<?> remedialActionAdder, String remedialActionId, List<String> alterations) {
        String kind = parentRemedialActionPropertyBag.get(CsaProfileConstants.RA_KIND);
        if (kind.equals(CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString())) {
            Instant preventiveInstant = crac.getPreventiveInstant();
            boolean hasAtLeastOneOnConstraintUsageRule = addOnConstraintUsageRules(preventiveInstant, remedialActionAdder, remedialActionId, alterations);
            if (!hasAtLeastOneOnConstraintUsageRule) {
                remedialActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(preventiveInstant.getId()).add();
            }
        } else {
            Instant curativeInstant = crac.getInstant(InstantKind.CURATIVE);
            boolean hasAtLeastOneOnConstraintUsageRule = addOnConstraintUsageRules(curativeInstant, remedialActionAdder, remedialActionId, alterations);
            if (!hasAtLeastOneOnConstraintUsageRule) {
                remedialActionAdder.newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(curativeInstant.getId()).add();
            }
        }
    }

    private void addOnContingencyStateUsageRules(RemedialActionAdder<?> remedialActionAdder, List<String> openRaoContingenciesIds, String instantId, UsageMethod usageMethod) {
        openRaoContingenciesIds.forEach(openRaoContingencyId -> remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(instantId)
            .withContingency(openRaoContingencyId)
            .withUsageMethod(usageMethod).add());
    }

    private void checkElementCombinationConstraintKindsCoherence(String remedialActionId, Map<String, Set<PropertyBag>> linkedContingencyWithRAs) {
        // The same contingency cannot have different Element Combination Constraint Kinds
        // The same contingency can appear several times, so we need to create a memory system
        Set<String> contingenciesWithIncluded = new HashSet<>();
        Set<String> contingenciesWithConsidered = new HashSet<>();
        Set<PropertyBag> linkedContingencyWithRA = linkedContingencyWithRAs.get(remedialActionId);
        for (PropertyBag propertyBag : linkedContingencyWithRA) {
            String combinationKind = propertyBag.get(COMBINATION_CONSTRAINT_KIND);
            String contingencyId = propertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY);
            if (combinationKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString())) {
                contingenciesWithIncluded.add(contingencyId);
            } else if (combinationKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
                contingenciesWithConsidered.add(contingencyId);
            } else if (combinationKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because of an illegal EXCLUDED ElementCombinationConstraintKind");
            } else {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because of an invalid Element Combination Constraint Kind");
            }
            if (contingenciesWithIncluded.contains(contingencyId) && contingenciesWithConsidered.contains(contingencyId)) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because the ElementCombinationConstraintKinds that link the remedial action to the contingency " + contingencyId + " are different");
            }
        }
    }

    private RemedialActionType checkRemedialActionCanBeImportedAndIdentifyType(PropertyBag remedialActionPropertyBag, Map<String, Set<PropertyBag>> linkedTopologyActions, Map<String, Set<PropertyBag>> linkedRotatingMachineActions, Map<String, Set<PropertyBag>> linkedShuntCompensatorModifications, Map<String, Set<PropertyBag>> linkedTapPositionActions, Map<String, Set<PropertyBag>> linkedStaticPropertyRanges) {
        String remedialActionId = remedialActionPropertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);

        String kind = remedialActionPropertyBag.get(CsaProfileConstants.RA_KIND);

        boolean normalAvailable = Boolean.parseBoolean(remedialActionPropertyBag.get(CsaProfileConstants.NORMAL_AVAILABLE));

        if (!normalAvailable) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because RemedialAction.normalAvailable must be 'true' to be imported");
        }
        if (!kind.equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString()) && !kind.equals(CsaProfileConstants.RemedialActionKind.PREVENTIVE.toString())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because Unsupported kind for remedial action " + remedialActionId);
        }

        if (linkedTopologyActions.containsKey(remedialActionId)) {
            checkEachInjectionSetPointActionHasExactlyOneStaticPropertyRangeElseThrowException(remedialActionId, linkedTopologyActions.get(remedialActionId), linkedStaticPropertyRanges);
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
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because there is no topology actions, no Set point actions, nor tap position action linked to that RA");
        }
    }

    private void checkEachInjectionSetPointActionHasExactlyOneStaticPropertyRangeElseThrowException(String remedialActionId, Set<PropertyBag> injectionSetPointActionsForOneRa, Map<String, Set<PropertyBag>> staticPropertyRangesLinkedToInjectionSetPointActions) {
        for (PropertyBag injectionSetPointAction : injectionSetPointActionsForOneRa) {
            Set<PropertyBag> staticPropertyRangePropertyBags = staticPropertyRangesLinkedToInjectionSetPointActions.get(injectionSetPointAction.getId("mRID"));
            if (staticPropertyRangePropertyBags != null) {
                if (staticPropertyRangePropertyBags.isEmpty()) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because there is no StaticPropertyRange linked to that RA");
                } else if (staticPropertyRangePropertyBags.size() > 1) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because several conflictual StaticPropertyRanges are linked to that RA's injection set point action");
                }
            }
        }
    }

    private String checkContingencyAndGetOpenRaoId(PropertyBag contingencyWithRemedialActionPropertyBag, String raKind, String
            remedialActionId, String combinationConstraintKind) {
        if (!raKind.equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because it is linked to a contingency but it's kind is not curative");
        }

        if (!combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString()) && !combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because combinationConstraintKind of a ContingencyWithRemedialAction must be 'included, 'excluded' or 'considered', but it was: " + combinationConstraintKind);
        }

        String contingencyId = contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).substring(contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).lastIndexOf("_") + 1);
        Optional<CsaProfileElementaryCreationContext> importedCsaProfileContingencyCreationContextOpt = cracCreationContext.getContingencyCreationContexts().stream().filter(co -> co.isImported() && co.getNativeId().equals(contingencyId)).findAny();
        if (importedCsaProfileContingencyCreationContextOpt.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because contingency" + contingencyId + " linked to that remedialAction does not exist or was not imported by Open RAO");
        } else {
            String openRaoContingencyId = importedCsaProfileContingencyCreationContextOpt.get().getElementId();
            CsaProfileCracUtils.checkNormalEnabled(contingencyWithRemedialActionPropertyBag, remedialActionId, "ContingencyWithRemedialAction");
            return openRaoContingencyId;
        }
    }

    private Optional<Integer> getSpeedOpt(String timeToImplement, String remedialActionId, boolean isAuto) {
        if (timeToImplement != null) {
            try {
                return Optional.of(CsaProfileCracUtils.convertDurationToSeconds(timeToImplement));
            } catch (RuntimeException e) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, (isAuto ? CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE : CsaProfileConstants.REMEDIAL_ACTION_MESSAGE) + remedialActionId + " will not be imported because of an irregular timeToImplement pattern");
            }

        } else {
            return Optional.empty();
        }
    }

    enum RemedialActionType {
        PST_RANGE_ACTION,
        NETWORK_ACTION
    }

    private boolean addOnConstraintUsageRules(Instant remedialActionInstant, RemedialActionAdder<?> remedialActionAdder, String importableRemedialActionId, List<String> alterations) {
        boolean flag1 = false;
        boolean flag2;
        boolean flag3;
        if (!onConstraintUsageRuleHelper.getExcludedCnecsByRemedialAction().containsKey(importableRemedialActionId)) {
            flag1 = processAvailableAssessedElementsCombinableWithRemedialActions(remedialActionInstant, remedialActionAdder, UsageMethod.AVAILABLE);
        } else {
            alterations.add(String.format("The association 'RemedialAction'/'Cnecs' '%s'/'%s' will be ignored because 'excluded' combination constraint kind is not supported", importableRemedialActionId, String.join(". ", onConstraintUsageRuleHelper.getExcludedCnecsByRemedialAction().get(importableRemedialActionId))));
        }
        flag2 = processAssessedElementsWithRemedialActions(remedialActionInstant, remedialActionAdder, importableRemedialActionId, UsageMethod.AVAILABLE, onConstraintUsageRuleHelper.getConsideredCnecsElementsByRemedialAction());
        flag3 = processAssessedElementsWithRemedialActions(remedialActionInstant, remedialActionAdder, importableRemedialActionId, UsageMethod.AVAILABLE, onConstraintUsageRuleHelper.getIncludedCnecsByRemedialAction());
        return flag1 || flag2 || flag3;
    }

    private boolean processAvailableAssessedElementsCombinableWithRemedialActions(Instant remedialActionInstant, RemedialActionAdder<?> remedialActionAdder, UsageMethod usageMethod) {
        List<Boolean> flags = onConstraintUsageRuleHelper.getImportedCnecsCombinableWithRas().stream().map(addOnConstraintUsageRuleForCnec(remedialActionInstant, remedialActionAdder, usageMethod)).toList();
        return flags.contains(true);
    }

    private boolean processAssessedElementsWithRemedialActions(Instant remedialActionInstant, RemedialActionAdder<?> remedialActionAdder, String importableRemedialActionId, UsageMethod usageMethod, Map<String, Set<String>> cnecsByRemedialAction) {
        if (cnecsByRemedialAction.containsKey(importableRemedialActionId)) {
            List<Boolean> flags = cnecsByRemedialAction.get(importableRemedialActionId).stream().map(addOnConstraintUsageRuleForCnec(remedialActionInstant, remedialActionAdder, usageMethod)).toList();
            return flags.contains(true);
        }
        return false;
    }

    private Function<String, Boolean> addOnConstraintUsageRuleForCnec(Instant remedialActionInstant, RemedialActionAdder<?> remedialActionAdder, UsageMethod usageMethod) {
        return cnecId -> {
            Cnec<?> cnec = crac.getCnec(cnecId);
            if (isOnConstraintInstantCoherent(cnec.getState().getInstant(), remedialActionInstant)) {
                if (cnec instanceof FlowCnec) {
                    remedialActionAdder.newOnFlowConstraintUsageRule()
                            .withInstant(remedialActionInstant.getId())
                            .withFlowCnec(cnecId)
                            .withUsageMethod(usageMethod)
                        .add();
                    return true;
                } else if (cnec instanceof VoltageCnec) {
                    remedialActionAdder.newOnVoltageConstraintUsageRule()
                            .withInstant(remedialActionInstant.getId())
                            .withVoltageCnec(cnecId)
                            .withUsageMethod(usageMethod)
                        .add();
                    return true;
                } else if (cnec instanceof AngleCnec) {
                    remedialActionAdder.newOnAngleConstraintUsageRule()
                            .withInstant(remedialActionInstant.getId())
                            .withAngleCnec(cnecId)
                            .withUsageMethod(usageMethod)
                        .add();
                    return true;
                } else {
                    throw new OpenRaoException(String.format("Unsupported cnec type %s", cnec.getClass().toString()));
                }
            }
            return false;
        };
    }

    private static boolean isOnConstraintInstantCoherent(Instant cnecInstant, Instant remedialInstant) {
        return !cnecInstant.comesBefore(remedialInstant);
    }

    /*
    Auto Remedial Actions (SPS)
     */

    private void createAutoRemedialActions() {
        NetworkActionCreator networkActionCreator = new NetworkActionCreator(crac, network);
        PstRangeActionCreator pstRangeActionCreator = new PstRangeActionCreator(crac, network);
        Map<String, Set<PropertyBag>> linkedStaticPropertyRanges = CsaProfileCracUtils.getMappedPropertyBagsSet(staticPropertyRangesPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION); // the id here is the id of the subclass of gridStateAlteration (tapPositionAction, RotatingMachine, ..)
        Map<String, Set<PropertyBag>> linkedContingencyWithRAs = CsaProfileCracUtils.getMappedPropertyBagsSet(contingencyWithRemedialActionsPropertyBags, CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        Map<String, Set<PropertyBag>> linkedTopologyActionsAuto = CsaProfileCracUtils.getMappedPropertyBagsSet(filterElementaryActions(topologyActionsPropertyBags, true), CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);
        Map<String, Set<PropertyBag>> linkedRotatingMachineActionsAuto = CsaProfileCracUtils.getMappedPropertyBagsSet(filterElementaryActions(rotatingMachineActionsPropertyBags, true), CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);
        Map<String, Set<PropertyBag>> linkedShuntCompensatorModificationAuto = CsaProfileCracUtils.getMappedPropertyBagsSet(filterElementaryActions(shuntCompensatorModificationsPropertyBags, true), CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);
        Map<String, Set<PropertyBag>> linkedTapPositionActionsAuto = CsaProfileCracUtils.getMappedPropertyBagsSet(filterElementaryActions(tapPositionActionsPropertyBags, true), CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);

        for (PropertyBag schemeRemedialActionPropertyBag : schemeRemedialActionsPropertyBags) {
            String spsId = schemeRemedialActionPropertyBag.get(CsaProfileConstants.MRID);

            try {
                checkSpsKind(schemeRemedialActionPropertyBag, spsId);
                checkSpsAvailability(schemeRemedialActionPropertyBag, spsId);
                String nativeSpsName = schemeRemedialActionPropertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME);
                String spsTsoName = schemeRemedialActionPropertyBag.get(CsaProfileConstants.TSO);
                Optional<String> spsName = CsaProfileCracUtils.createElementName(nativeSpsName, spsTsoName);
                Optional<Integer> spsSpeed = getSpeedOpt(schemeRemedialActionPropertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT), spsId, true);
                List<String> alterations = new ArrayList<>();

                String gridStateAlterationCollection = getGridStateAlterationCollection(spsId);

                RemedialActionType remedialActionType = getRemedialActionType(linkedStaticPropertyRanges, linkedTopologyActionsAuto, linkedRotatingMachineActionsAuto, linkedShuntCompensatorModificationAuto, linkedTapPositionActionsAuto, spsId, gridStateAlterationCollection);
                RemedialActionAdder<?> remedialActionAdder;

                if (remedialActionType.equals(RemedialActionType.NETWORK_ACTION)) {
                    remedialActionAdder = networkActionCreator.getNetworkActionAdder(linkedTopologyActionsAuto, linkedRotatingMachineActionsAuto, linkedShuntCompensatorModificationAuto, linkedStaticPropertyRanges, gridStateAlterationCollection, spsId, alterations);
                } else {
                    remedialActionAdder = pstRangeActionCreator.getPstRangeActionAdder(linkedTapPositionActionsAuto, linkedStaticPropertyRanges, gridStateAlterationCollection, spsId);
                }

                spsName.ifPresent(remedialActionAdder::withName);
                if (spsTsoName != null) {
                    remedialActionAdder.withOperator(TsoEICode.fromEICode(spsTsoName.substring(spsTsoName.lastIndexOf("/") + 1)).getDisplayName());
                }

                if (remedialActionType == RemedialActionType.PST_RANGE_ACTION && spsSpeed.isEmpty()) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because an auto PST range action must have a speed defined");
                }
                spsSpeed.ifPresent(remedialActionAdder::withSpeed);

                if (linkedContingencyWithRAs.containsKey(spsId)) {
                    manageContingencyWithRAsCombinations(spsId, linkedContingencyWithRAs, remedialActionAdder);
                } else {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because no contingency is linked to the remedial action");
                }
                remedialActionAdder.add();
                csaProfileRemedialActionCreationContexts.add(CsaProfileElementaryCreationContext.imported(spsId, spsId, spsName.orElse(nativeSpsName), "", false));

            } catch (OpenRaoImportException e) {
                csaProfileRemedialActionCreationContexts.add(CsaProfileElementaryCreationContext.notImported(spsId, e.getImportStatus(), e.getMessage()));
            }
        }
    }

    private void manageContingencyWithRAsCombinations(String spsId, Map<String, Set<PropertyBag>> linkedContingencyWithRAs, RemedialActionAdder<?> remedialActionAdder) {
        checkElementCombinationConstraintKindsCoherence(spsId, linkedContingencyWithRAs);
        Set<String> openRaoContingenciesIds = new HashSet<>();

        for (PropertyBag contingencyWithRemedialActionPropertyBag : linkedContingencyWithRAs.get(spsId)) {
            String contingencyId = contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).substring(contingencyWithRemedialActionPropertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCY).lastIndexOf("_") + 1);
            Optional<CsaProfileElementaryCreationContext> importedCsaProfileContingencyCreationContextOpt = cracCreationContext.getContingencyCreationContexts().stream().filter(co -> co.isImported() && co.getNativeId().equals(contingencyId)).findAny();
            if (importedCsaProfileContingencyCreationContextOpt.isEmpty()) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because contingency " + contingencyId + " linked to that remedial action does not exist or was not imported by Open RAO");
            }
            String openRaoContingencyId = importedCsaProfileContingencyCreationContextOpt.get().getElementId();
            CsaProfileCracUtils.checkNormalEnabled(contingencyWithRemedialActionPropertyBag, spsId, "ContingencyWithRemedialAction");
            String combinationConstraintKind = contingencyWithRemedialActionPropertyBag.get(COMBINATION_CONSTRAINT_KIND);
            if (!CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString().equals(combinationConstraintKind)) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because it must be linked to the contingency " + contingencyId + " with an 'included' ElementCombinationConstraintKind");
            }
            openRaoContingenciesIds.add(openRaoContingencyId);
        }

        boolean hasAtLeastOneOnConstraintUsageRule = addOnConstraintUsageRules(crac.getInstant(InstantKind.CURATIVE), remedialActionAdder, spsId, new ArrayList<>());
        if (!hasAtLeastOneOnConstraintUsageRule) {
            addOnContingencyStateUsageRules(remedialActionAdder, openRaoContingenciesIds.stream().toList(), crac.getInstant(InstantKind.AUTO).getId(), UsageMethod.FORCED);
        }
    }

    private static void checkSpsKind(PropertyBag schemeRemedialActionPropertyBag, String spsId) {
        String spsKind = schemeRemedialActionPropertyBag.get(CsaProfileConstants.RA_KIND);
        if (!spsKind.equals(CsaProfileConstants.RemedialActionKind.CURATIVE.toString())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because auto remedial action musty be of curative kind");
        }
    }

    private static void checkSpsAvailability(PropertyBag schemeRemedialActionPropertyBag, String spsId) {
        boolean normalAvailable = Boolean.parseBoolean(schemeRemedialActionPropertyBag.get(CsaProfileConstants.NORMAL_AVAILABLE));
        if (!normalAvailable) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because RemedialAction.normalAvailable must be 'true' to be imported");
        }
    }

    private String getAssociatedRemedialActionScheme(String spsId) {
        List<PropertyBag> linkedRemedialActionSchemePropertyBags = remedialActionSchemePropertyBags.stream().filter(pb -> spsId.equals(pb.getId(CsaProfileConstants.SCHEME_REMEDIAL_ACTION))).toList();
        if (linkedRemedialActionSchemePropertyBags.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because it has no associated RemedialActionScheme");
        } else if (linkedRemedialActionSchemePropertyBags.size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because it has several conflictual RemedialActionSchemes");
        }

        PropertyBag remedialActionSchemePropertyBag = linkedRemedialActionSchemePropertyBags.get(0);
        String remedialActionSchemeId = remedialActionSchemePropertyBag.getId(CsaProfileConstants.MRID);

        String remedialActionSchemeKind = remedialActionSchemePropertyBag.get(CsaProfileConstants.RA_KIND);
        if (!remedialActionSchemeKind.equals(CsaProfileConstants.SIPS)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because of an unsupported kind for remedial action schedule (only SIPS allowed)");
        }
        String remedialActionSchemeNormalArmed = remedialActionSchemePropertyBag.get(CsaProfileConstants.NORMAL_ARMED);
        if (!Boolean.parseBoolean(remedialActionSchemeNormalArmed)) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because RemedialActionScheme " + remedialActionSchemeId + " is not armed");
        }
        return remedialActionSchemeId;
    }

    private PropertyBag getAssociatedStagePropertyBag(String spsId, String remedialActionScheme) {
        List<PropertyBag> linkedStagePropertyBags = stagePropertyBags.stream().filter(pb -> remedialActionScheme.equals(pb.getId(CsaProfileConstants.REMEDIAL_ACTION_SCHEME))).toList();
        if (linkedStagePropertyBags.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because it has no associated Stage");
        } else if (linkedStagePropertyBags.size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because it has several conflictual Stages");
        }
        return linkedStagePropertyBags.get(0);
    }

    private String getAssociatedGridStateAlterationCollectionUsingStage(String spsId, String remedialActionScheme) {
        PropertyBag stagePropertyBag = getAssociatedStagePropertyBag(spsId, remedialActionScheme);
        String gridStateAlterationCollection = stagePropertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION);
        List<PropertyBag> linkedGridStateAlterationCollectionPropertyBags = gridStateAlterationCollectionPropertyBags.stream().filter(pb -> gridStateAlterationCollection.equals(pb.get(CsaProfileConstants.MRID))).toList();
        if (linkedGridStateAlterationCollectionPropertyBags.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because it has no associated GridStateAlterationCollection");
        }
        return gridStateAlterationCollection;
    }

    private String getGridStateAlterationCollection(String spsId) {
        String remedialActionScheme = getAssociatedRemedialActionScheme(spsId);
        return getAssociatedGridStateAlterationCollectionUsingStage(spsId, remedialActionScheme);
    }

    private RemedialActionType getRemedialActionType(Map<String, Set<PropertyBag>> linkedStaticPropertyRanges, Map<String, Set<PropertyBag>> linkedTopologyActionsAuto, Map<String, Set<PropertyBag>> linkedRotatingMachineActionsAuto, Map<String, Set<PropertyBag>> linkedShuntCompensatorModificationAuto, Map<String, Set<PropertyBag>> linkedTapPositionActionsAuto, String spsId, String gridStateAlterationCollection) {
        RemedialActionType remedialActionType;
        if (linkedTopologyActionsAuto.containsKey(gridStateAlterationCollection)) {
            remedialActionType = RemedialActionType.NETWORK_ACTION;
        } else if (linkedRotatingMachineActionsAuto.containsKey(gridStateAlterationCollection)) {
            checkEachInjectionSetPointActionHasExactlyOneStaticPropertyRangeElseThrowException(spsId, linkedRotatingMachineActionsAuto.get(gridStateAlterationCollection), linkedStaticPropertyRanges);
            remedialActionType = RemedialActionType.NETWORK_ACTION;
        } else if (linkedShuntCompensatorModificationAuto.containsKey(gridStateAlterationCollection)) {
            checkEachInjectionSetPointActionHasExactlyOneStaticPropertyRangeElseThrowException(spsId, linkedShuntCompensatorModificationAuto.get(gridStateAlterationCollection), linkedStaticPropertyRanges);
            remedialActionType = RemedialActionType.NETWORK_ACTION;
        } else if (linkedTapPositionActionsAuto.containsKey(gridStateAlterationCollection)) {
            // StaticPropertyRanges not mandatory in case of tapPositionsActions
            remedialActionType = RemedialActionType.PST_RANGE_ACTION;
        } else {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.AUTO_REMEDIAL_ACTION_MESSAGE + spsId + " will not be imported because there is no elementary action for that ARA");
        }
        return remedialActionType;
    }

}
