/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.faorao;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.craciojson.JsonExport;
import com.powsybl.openrao.data.craciojson.JsonImport;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.CastorFullOptimization;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.FastRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OneStateOnlyRaoResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class FastRao implements RaoProvider {
    private static final String FAST_RAO = "FastRao";

    // Do not store any big object in this class as it is a static RaoProvider
    // Objects stored in memory will not be released at the end of the RAO run

    @Override
    public String getName() {
        return FAST_RAO;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters) {
        return run(raoInput, parameters, null);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {
        RaoUtil.initData(raoInput, parameters);
        return launchFilteredRao(raoInput, parameters, targetEndInstant);
    }

    private CompletableFuture<RaoResult> launchFilteredRao(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {

        try {
            // 1. Retrieve input data
            Crac crac = raoInput.getCrac();
            Collection<String> initialNetworkVariants = new HashSet<>(raoInput.getNetwork().getVariantManager().getVariantIds());
            String startingVariant = raoInput.getNetwork().getVariantManager().getWorkingVariantId();

            System.out.println("**************************INITIAL SENSITIVITY*******************************");
            ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, parameters);

            PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                raoInput.getCrac().getFlowCnecs(),
                raoInput.getCrac().getRangeActions(),
                parameters,
                toolProvider);
            OpenRaoLogger logger = new RaoBusinessLogs();

            // 3. Run RA-free RAO and get 20 worst CNECs
            PrePerimeterResult initialResult = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork(), raoInput.getCrac());
            PrePerimeterResult ofResult = initialResult;
            RaoLogger.logMostLimitingElementsResults(logger, initialResult, parameters.getObjectiveFunctionParameters().getType(), 5);
            //computeAvailableRangeActions(initialResult, crac, network, parameters);

            Set<FlowCnec> worstCnecs = new HashSet<>();
            worstCnecs.add(getWorstPreventiveCnec(ofResult));
            FlowCnec worstCnec;
            Pair<PrePerimeterResult, RaoResult> postSensiAndFilteredRaoResult;
            do {
                cleanVariants(raoInput.getNetwork(), initialNetworkVariants);
                worstCnecs.addAll(ofResult.getMostLimitingElements(20));
                worstCnecs.addAll(getCostlyVirtualCnecs(ofResult));
                // run rao with filtered cnecs and rerun the sensi with all cnecs and applied ras
                postSensiAndFilteredRaoResult = runFilteredRao(raoInput, parameters, targetEndInstant, worstCnecs, prePerimeterSensitivityAnalysis, initialResult);
                ofResult = postSensiAndFilteredRaoResult.getLeft();
                RaoLogger.logMostLimitingElementsResults(logger, ofResult, parameters.getObjectiveFunctionParameters().getType(), 5);
                worstCnec = ofResult.getMostLimitingElements(1).get(0);
            } while (!(worstCnecs.contains(worstCnec) && worstCnecs.containsAll(getCostlyVirtualCnecs(ofResult))));

            return CompletableFuture.completedFuture(new FastRaoResultImpl(initialResult, ofResult, postSensiAndFilteredRaoResult.getRight(), crac));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void cleanVariants(Network network, Collection<String> initialNetworkVariants) {
        VariantManager variantManager = network.getVariantManager();
        Set<String> variantsToRemove = new HashSet<>();
        variantManager.getVariantIds().stream()
            .filter(id -> !initialNetworkVariants.contains(id))
            .forEach(variantsToRemove::add);
        variantsToRemove.forEach(variantManager::removeVariant);
    }

    private FlowCnec getWorstPreventiveCnec(ObjectiveFunctionResult ofResult) {
        List<FlowCnec> orderedCnecs = ofResult.getMostLimitingElements(Integer.MAX_VALUE);
        return orderedCnecs.stream().filter(cnec -> cnec.getState().isPreventive()).findFirst().orElseThrow();
    }

    private Set<FlowCnec> getCostlyVirtualCnecs(ObjectiveFunctionResult ofResult) {
        Set<FlowCnec> flowCnecs = new HashSet<>();
        ofResult.getVirtualCostNames().forEach(name -> flowCnecs.addAll(ofResult.getCostlyElements(name, Integer.MAX_VALUE)));
        return flowCnecs;
    }

    private Pair<PrePerimeterResult, RaoResult> runFilteredRao(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, Set<FlowCnec> flowCnecsToKeep, PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis, PrePerimeterResult initialResult) throws IOException {
        // 4. Filter CRAC to only keep the worst CNECs
        Crac filteredCrac = copyCrac(raoInput.getCrac());
        removeFlowCnecsFromCrac(filteredCrac, flowCnecsToKeep);
        // 5. Run filtered RAO
        System.out.println("**************************FILTERED RAO*******************************");
        RaoInput filteredRaoInput = createFilteredRaoInput(raoInput, filteredCrac);
        RaoResult raoResult;
        try {
            raoResult = new CastorFullOptimization(filteredRaoInput, parameters, targetEndInstant).run().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        String finalVariantId = raoInput.getNetwork().getVariantManager().getWorkingVariantId();
        raoInput.getNetwork().getVariantManager().setWorkingVariant(raoInput.getNetworkVariantId());
        // 6. Apply / Force optimal RAs found on filter RAO
        Network networkCopy = NetworkSerDe.copy(raoInput.getNetwork());
        applyOptimalPreventiveRemedialActions(networkCopy, filteredCrac.getPreventiveState(), raoResult);
        AppliedRemedialActions appliedRemedialActions = createAppliedRemedialActionsFromRaoResult(filteredCrac, raoResult);
        // 7. Run RAO with applied/forced RAs
        System.out.println("**************************FULL SENSI WITH RAS*******************************");
        PrePerimeterResult postRaSensi = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(networkCopy, raoInput.getCrac(), initialResult, initialResult, new HashSet<>(), appliedRemedialActions);
        raoInput.getNetwork().getVariantManager().setWorkingVariant(finalVariantId);
        return Pair.of(postRaSensi, raoResult);
    }

    private RaoInput createFilteredRaoInput(RaoInput raoInput, Crac filteredCrac) {
        return RaoInput.build(raoInput.getNetwork(), filteredCrac)
            .withPerimeter(raoInput.getPerimeter())
            .withGlskProvider(raoInput.getGlskProvider())
            .withRefProg(raoInput.getReferenceProgram())
            .withNetworkVariantId(raoInput.getNetworkVariantId())
            .build();
    }

    public static Crac copyCrac(Crac crac) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new JsonExport().exportCrac(crac, outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return new JsonImport().importCrac(inputStream);
    }

    public static void removeFlowCnecsFromCrac(Crac crac, Collection<FlowCnec> flowCnecsToKeep) {
        List<FlowCnec> flowCnecsToRemove = crac.getFlowCnecs().stream().filter(fc -> !flowCnecsToKeep.contains(fc)).toList();
        // Remove FlowCNECs
        Set<String> flowCnecsToRemoveIds = new HashSet<>();
        flowCnecsToRemove.forEach(cnec -> flowCnecsToRemoveIds.add(cnec.getId()));
        crac.removeFlowCnecs(flowCnecsToRemoveIds);

        //TODO: remove associated on constraint usage rules
    }

    private void applyOptimalPreventiveRemedialActions(Network networkCopy, State state, RaoResult raoResult) {
        raoResult.getActivatedRangeActionsDuringState(state).forEach(rangeAction -> rangeAction.apply(networkCopy, raoResult.getOptimizedSetPointOnState(state, rangeAction)));
        raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> networkAction.apply(networkCopy));
    }

    private AppliedRemedialActions createAppliedRemedialActionsFromRaoResult(Crac crac, RaoResult raoResult) {
        if (raoResult instanceof OneStateOnlyRaoResultImpl) {
            return new AppliedRemedialActions();
        }
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        crac.getStates().stream().filter(state -> !state.isPreventive() && !state.getInstant().getKind().equals(InstantKind.OUTAGE)).forEach(state -> {
            raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> appliedRemedialActions.addAppliedNetworkAction(state, networkAction));
            appliedRemedialActions.addAppliedNetworkActions(state, raoResult.getActivatedNetworkActionsDuringState(state));
            appliedRemedialActions.addAppliedRangeActions(state, raoResult.getOptimizedSetPointsOnState(state));
        });
        return appliedRemedialActions;
    }
}
