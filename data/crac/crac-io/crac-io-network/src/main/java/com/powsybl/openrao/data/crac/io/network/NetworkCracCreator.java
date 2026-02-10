/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;


import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.util.SwitchPredicates;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.TapRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.PstHelper;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmPstHelper;
import com.powsybl.openrao.data.crac.io.network.parameters.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates a CRAC from a network.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkCracCreator {
    private NetworkCracCreationContext creationContext;
    private CracCreationParameters cracCreationParameters;
    private NetworkCracCreationParameters specificParameters;
    private Network network;
    private Crac crac;
    private final Map<Branch<?>, Contingency> contingencyPerBranch = new HashMap<>();

    NetworkCracCreationContext createCrac(Network network, CracCreationParameters cracCreationParameters) {
        String cracId = "CRAC_FROM_NETWORK_" + network.getNameOrId();
        this.network = network;
        crac = cracCreationParameters.getCracFactory().create(cracId, cracId, network.getCaseDate().toOffsetDateTime());
        creationContext = new NetworkCracCreationContext(crac, network.getNameOrId());
        if (cracCreationParameters.getExtension(NetworkCracCreationParameters.class) == null) {
            throw new OpenRaoException("Cannot create a CRAC from a network file unless a NetworkCracCreationParameters extension is defined in CracCreationParameters.");
        }
        this.cracCreationParameters = cracCreationParameters;
        specificParameters = cracCreationParameters.getExtension(NetworkCracCreationParameters.class);
        addInstants();
        addContingencies();
        new CnecCreator(crac, network, cracCreationParameters, creationContext, contingencyPerBranch).addCnecsAndMnecs();
        addPstRangeActions();
        addRedispatchRangeActions();
        creationContext.setCreationSuccessful(true);
        return creationContext;
    }

    private void addInstants() {
        specificParameters.getInstants().forEach((instantKind, ids) ->
            ids.forEach(id -> crac.newInstant(id, instantKind)));
    }

    private void addContingencies() {
        Contingencies params = specificParameters.getContingencies();
        network.getBranchStream().filter(b ->
                Utils.branchIsInCountries(b, params.getCountries().orElse(null))
                    && Utils.branchIsInVRange(b, params.getMinV(), params.getMaxV()))
            .forEach(
                branch -> {
                    Contingency contingency = crac.newContingency()
                        .withId("CO_" + branch.getNameOrId())
                        .withContingencyElement(branch.getId(), ContingencyElementType.BRANCH)
                        .add();
                    contingencyPerBranch.put(branch, contingency);
                }
            );
    }

    private void addPstRangeActions() {
        PstRangeActions params = specificParameters.getPstRangeActions();
        Set<Instant> instants = crac.getSortedInstants().stream().filter(instant -> !instant.isOutage())
            .filter(params::arePstsAvailableForInstant).collect(Collectors.toSet());
        network.getTwoWindingsTransformerStream()
            .filter(twt -> twt.getPhaseTapChanger() != null)
            .filter(twt -> Utils.branchIsInCountries(twt, params.getCountries().orElse(null)))
            .forEach(twt -> instants.forEach(instant -> addPstRangeActionForInstant(twt, instant)));
    }

    private void addPstRangeActionForInstant(TwoWindingsTransformer twt, Instant instant) {
        PstRangeActions params = specificParameters.getPstRangeActions();
        PstHelper pstHelper = new IidmPstHelper(twt.getId(), network);
        PstRangeActionAdder pstAdder = crac.newPstRangeAction()
            .withId("PST_RA_" + twt.getId() + "_" + instant.getId())
            .withNetworkElement(twt.getId())
            .withInitialTap(pstHelper.getInitialTap())
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(pstHelper.getLowTapPosition()).withMaxTap(pstHelper.getHighTapPosition()).add()
            .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap());
        // TODO align RAs if needed

        boolean availableForAllStates = crac.getStates(instant).stream().allMatch(state -> params.isAvailable(twt, state));
        if (availableForAllStates) {
            pstAdder.newOnInstantUsageRule().withInstant(instant.getId()).add();
        } else {
            crac.getStates().stream().filter(state -> params.isAvailable(twt, state))
                .forEach(
                    state -> pstAdder.newOnContingencyStateUsageRule()
                        .withInstant(instant.getId())
                        .withContingency(state.getContingency().orElseThrow().getId())
                        .add()
                );
        }
        if (params.getRangeMin(instant).isPresent() || params.getRangeMax(instant).isPresent()) {
            TapRangeAdder rangeAdder = pstAdder.newTapRange().withRangeType(RangeType.RELATIVE_TO_PREVIOUS_INSTANT);
            params.getRangeMin(instant).ifPresent(rangeAdder::withMinTap);
            params.getRangeMax(instant).ifPresent(rangeAdder::withMaxTap);
            rangeAdder.add();
        }
        pstAdder.add();
    }

    private void addRedispatchRangeActions() {
        RedispatchingRangeActions params = specificParameters.getRedispatchingRangeActions();
        Set<Instant> instants = crac.getSortedInstants().stream().filter(instant -> !instant.isOutage()).collect(Collectors.toSet());
        network.getGeneratorStream()
            .filter(generator -> Utils.injectionIsInCountries(generator, params.getCountries().orElse(null)))
            .forEach(generator ->
                instants.stream().filter(instant -> params.shouldCreateRedispatchingAction(generator, instant))
                    .forEach(instant -> addGeneratorActionForInstant(generator, instant)));
        network.getLoadStream()
            .filter(load -> Utils.injectionIsInCountries(load, params.getCountries().orElse(null)))
            .forEach(load ->
                instants.stream().filter(instant -> params.shouldCreateRedispatchingAction(load, instant))
                    .forEach(instant -> addLoadActionForInstant(load, instant)));
        // TODO add other injections (batteries...)
    }

    private void addGeneratorActionForInstant(Generator generator, Instant instant) {
        // TODO merge preventive & curative RA if ranges are the same?
        // advantage : simpler crac
        // disadvantage : more complex code + we might not see bugs in "detailed" version
        RedispatchingRangeActions params = specificParameters.getRedispatchingRangeActions();
        double initialP = Math.round(generator.getTargetP());
        // TODO round it in network too ?
        double minP = Math.min(generator.getMinP(), generator.getTargetP());
        if (params.getRaRange(generator, instant).getMin().isPresent()) {
            minP = Math.min(minP, params.getRaRange(generator, instant).getMin().get());
        }
        double maxP = Math.max(generator.getMaxP(), generator.getTargetP());
        if (params.getRaRange(generator, instant).getMax().isPresent()) {
            maxP = Math.max(maxP, params.getRaRange(generator, instant).getMax().get());
        }
        InjectionRangeActionCosts costs = params.getRaCosts(generator, instant);
        crac.newInjectionRangeAction()
            .withId("RD_GEN_" + generator.getId() + "_" + instant.getId())
            .withNetworkElementAndKey(1.0, generator.getId())
            .newRange()
            .withMin(minP)
            .withMax(maxP).add()
            .newOnInstantUsageRule().withInstant(instant.getId()).add()
            .withInitialSetpoint(initialP)
            .withVariationCost(costs.downVariationCost(), VariationDirection.DOWN)
            .withVariationCost(costs.upVariationCost(), VariationDirection.UP)
            .withActivationCost(costs.activationCost())
            .add();

        // connect the generator
        generator.connect(SwitchPredicates.IS_OPEN);
    }

    private void addLoadActionForInstant(Load load, Instant instant) {
        RedispatchingRangeActions params = specificParameters.getRedispatchingRangeActions();
        double initialP = Math.round(load.getP0());
        // TODO round it in network too ?
        if (params.getRaRange(load, instant).getMin().isEmpty() || params.getRaRange(load, instant).getMax().isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Could not create range action for load %s at instant %s, because you did not define its min or max value in the parameters.", load.getId(), instant.getId()));
        }
        double minP = Math.min(initialP, params.getRaRange(load, instant).getMin().get());
        double maxP = Math.max(initialP, params.getRaRange(load, instant).getMax().get());
        InjectionRangeActionCosts costs = params.getRaCosts(load, instant);
        crac.newInjectionRangeAction()
            .withId("RD_LOAD_" + load.getId() + "_" + instant.getId())
            .withNetworkElementAndKey(1.0, load.getId())
            .newRange()
            .withMin(minP)
            .withMax(maxP).add()
            .newOnInstantUsageRule().withInstant(instant.getId()).add()
            .withInitialSetpoint(initialP)
            .withVariationCost(costs.downVariationCost(), VariationDirection.DOWN)
            .withVariationCost(costs.upVariationCost(), VariationDirection.UP)
            .withActivationCost(costs.activationCost())
            .add();
    }
}
