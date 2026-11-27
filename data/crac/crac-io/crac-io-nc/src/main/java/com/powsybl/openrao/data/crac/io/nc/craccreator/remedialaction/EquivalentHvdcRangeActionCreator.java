/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.StandardRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.PropertyReference;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.RelativeDirectionKind;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.ValueOffsetKind;
import com.powsybl.openrao.data.crac.io.nc.objects.EquivalentInjectionAction;
import com.powsybl.openrao.data.crac.io.nc.objects.GridStateAlterationRemedialAction;
import com.powsybl.openrao.data.crac.io.nc.objects.StaticPropertyRange;

import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class EquivalentHvdcRangeActionCreator {
    private static final String MULTIPLE_EQUIVALENT_INJECTIONS_ERROR_MESSAGE = "Remedial action %s will not be imported because is has several EquivalentInjectionAction associated";
    private static final String NOT_ABSOLUTE_VALUE_KIND_ERROR_MESSAGE = "Remedial action %s will not be imported because its associated StaticPropertyRange %s has a non absolute valueKind";
    private static final String ALREADY_PROVIDED_BOUND_ERROR_MESSAGE = "Remedial action %s will not be imported because a %s bound was already provided for its range";
    private static final String UNSUPPORTED_DIRECTION_KIND_ERROR_MESSAGE = "Remedial action %s will not be imported because its associated StaticPropertyRange %s has a non supported '%s' direction";
    private static final String INJECTION_NOT_FOUND_ERROR_MESSAGE = "Remedial action %s will not be imported because no injection %s was found in the network";
    private static final String AMBIGUOUS_INITIAL_SET_POINT_ERROR_MESSAGE = "Remedial action %s will not be imported because its initial set-point is 0 MW which is ambiguous";

    private final Crac crac;
    private final Network network;

    public EquivalentHvdcRangeActionCreator(Crac crac, Network network) {
        this.crac = crac;
        this.network = network;
    }

    public InjectionRangeActionAdder createInjectionRangeActionAdder(GridStateAlterationRemedialAction gridStateAlterationRemedialAction,
                                                                     Set<EquivalentInjectionAction> equivalentInjectionActions,
                                                                     Map<String, Set<StaticPropertyRange>> staticPropertyRanges,
                                                                     String groupId) {
        String remedialActionId = gridStateAlterationRemedialAction.mrid();
        if (equivalentInjectionActions.size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format(MULTIPLE_EQUIVALENT_INJECTIONS_ERROR_MESSAGE, remedialActionId));
        }

        EquivalentInjectionAction equivalentInjectionAction = equivalentInjectionActions.iterator().next();
        NcCracUtils.checkPropertyReference(remedialActionId, "EquivalentInjectionAction", PropertyReference.EQUIVALENT_INJECTION, equivalentInjectionAction.propertyReference());

        // check equivalent injection
        Injection<?> equivalentInjection = getInjection(remedialActionId, equivalentInjectionAction.equivalentInjectionId());
        double distributionKey = getDistributionKey(remedialActionId, equivalentInjection);
        double initialP = equivalentInjection instanceof Generator generator
            ? generator.getTargetP()
            : ((Load) equivalentInjection).getP0();

        // add basic remedial action data
        InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
            .withId(remedialActionId)
            .withName(gridStateAlterationRemedialAction.getUniqueName())
            .withOperator(NcCracUtils.safeGetOperator(gridStateAlterationRemedialAction.operator()))
            .withSpeed(gridStateAlterationRemedialAction.getTimeToImplementInSeconds())
            .withNetworkElementAndKey(distributionKey, equivalentInjection.getId())
            .withInitialSetpoint(initialP / distributionKey)
            .withGroupId(groupId);

        // add range
        StandardRangeAdder<InjectionRangeActionAdder> rangeAdder = injectionRangeActionAdder.newRange().withRangeType(RangeType.ABSOLUTE);
        fillRangeDataFromStaticPropertyRanges(remedialActionId, rangeAdder, staticPropertyRanges.getOrDefault(equivalentInjectionAction.mrid(), Set.of()));
        rangeAdder.add();

        return injectionRangeActionAdder;
    }

    private Injection<?> getInjection(String remedialActionId, String equivalentInjectionId) {
        Generator generator = network.getGenerator(equivalentInjectionId);
        if (generator != null) {
            return generator;
        }

        Load load = network.getLoad(equivalentInjectionId);
        if (load != null) {
            return load;
        }

        throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format(
            INJECTION_NOT_FOUND_ERROR_MESSAGE,
            remedialActionId,
            equivalentInjectionId
        ));
    }

    private static double getDistributionKey(String remedialActionId, Injection<?> injection) {
        if (injection instanceof Generator generator) {
            double targetP = generator.getTargetP();
            if (targetP == 0.0) {
                // 0 MW set-point is ambiguous to determine the direction of the variation
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format(AMBIGUOUS_INITIAL_SET_POINT_ERROR_MESSAGE, remedialActionId));
            }
            return Math.signum(generator.getTargetP());
        }
        return -1.0;
    }

    private static void fillRangeDataFromStaticPropertyRanges(String remedialActionId, StandardRangeAdder<InjectionRangeActionAdder> rangeAdder, Set<StaticPropertyRange> staticPropertyRanges) {
        boolean hasMax = false;
        boolean hasMin = false;
        for (StaticPropertyRange staticPropertyRange : staticPropertyRanges) {
            // check property reference
            NcCracUtils.checkPropertyReference(remedialActionId, "EquivalentInjectionAction", PropertyReference.EQUIVALENT_INJECTION, staticPropertyRange.propertyReference());

            // check value kind
            if (!ValueOffsetKind.ABSOLUTE.matches(staticPropertyRange.valueKind())) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format(NOT_ABSOLUTE_VALUE_KIND_ERROR_MESSAGE, remedialActionId, staticPropertyRange.mrid()));
            }

            // check direction
            if (RelativeDirectionKind.UP.matches(staticPropertyRange.direction())) {
                if (hasMax) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format(ALREADY_PROVIDED_BOUND_ERROR_MESSAGE, remedialActionId, "max"));
                }
                rangeAdder.withMax(staticPropertyRange.normalValue());
                hasMax = true;
            } else if (RelativeDirectionKind.DOWN.matches(staticPropertyRange.direction())) {
                if (hasMin) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format(ALREADY_PROVIDED_BOUND_ERROR_MESSAGE, remedialActionId, "min"));
                }
                rangeAdder.withMin(staticPropertyRange.normalValue());
                hasMin = true;
            } else {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format(
                    UNSUPPORTED_DIRECTION_KIND_ERROR_MESSAGE,
                    remedialActionId,
                    staticPropertyRange.mrid(),
                    staticPropertyRange.direction()
                ));
            }
        }
    }
}
