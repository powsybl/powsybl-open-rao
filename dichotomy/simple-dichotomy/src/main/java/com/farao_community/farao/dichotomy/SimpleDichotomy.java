/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.dichotomy;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.glsk.import_.scalable_provider.ScalableProvider;
import com.farao_community.farao.rao_api.Rao;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.google.auto.service.AutoService;
import com.powsybl.action.util.Scalable;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(DichotomyProvider.class)
public class SimpleDichotomy implements DichotomyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDichotomy.class);

    @Override
    public CompletableFuture<DichotomyResult> run(Network network, Crac crac, ScalableProvider scalableProvider,
                                                  Set<Country> region, DichotomyParameters parameters,
                                                  ComputationManager computationManager) {
        LOGGER.info("For simple dichotomy, computation manager will be ignored.");

        if (region.size() != 2) {
            throw new DichotomyException("Two (and only two) different countries have to be defined in the region.");
        }

        Iterator<Country> regionIterator = region.iterator();
        Country country1 = regionIterator.next();
        Country country2 = regionIterator.next();

        DichotomyResult result = new DichotomyResult();

        // Compute NTC in direction country1 -> country2
        BorderDichotomyResult borderResult1 = run(network, crac, scalableProvider.getScalable(country1.getName()),
            scalableProvider.getScalable(country2.getName()), parameters);
        result.addOrientedNtcValue(new ImmutablePair<>(country1, country2), borderResult1);

        // Compute NTC in direction country2 -> country1
        BorderDichotomyResult borderResult2 = run(network, crac, scalableProvider.getScalable(country2.getName()),
            scalableProvider.getScalable(country1.getName()), parameters);
        result.addOrientedNtcValue(new ImmutablePair<>(country2, country1), borderResult2);

        return CompletableFuture.completedFuture(result);
    }

    public BorderDichotomyResult run(Network network, Crac crac, Scalable scalableToIncrease,
                                     Scalable scalableToDecrease, DichotomyParameters parameters) {
        LOGGER.info("For simple dichotomy, computation manager will be ignored.");

        BorderDichotomyResult result = new BorderDichotomyResult();
        double step = parameters.getInitialStep();

        double initialValueToIncrease = scalableToIncrease.initialValue(network);
        double initialValueToDecrease = scalableToDecrease.initialValue(network);

        double currentValueToIncrease = initialValueToIncrease;
        double currentValueToDecrease = initialValueToDecrease;

        do {
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), "new-variant");
            network.getVariantManager().setWorkingVariant("new-variant");
            RaoInput raoInput = RaoInput.builder()
                .withNetwork(network)
                .withCrac(crac)
                .withVariantId("new-variant")
                .build();

            RaoParameters raoParameters = RaoParameters.load();
            LOGGER.warn("RAO parameters will be updated with max min margin in megawatt as objective function.");
            raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT);

            RaoResult raoResult = Rao.run(raoInput, raoParameters);
            CracResultExtension cracResultExtension = raoInput.getCrac().getExtension(CracResultExtension.class);
            if (cracResultExtension == null) {
                throw new DichotomyException("CRAC does not contain any results.");
            }
            double cost = cracResultExtension.getVariant(raoResult.getPostOptimVariantId()).getFunctionalCost();

            if (cost > 0) {
                currentValueToIncrease += step;
                currentValueToDecrease -= step;
            } else {
                step /= 2;
                currentValueToIncrease -= step;
                currentValueToDecrease += step;
            }

            // Check if dichotomy is limited by scalable values
            if (currentValueToIncrease > scalableToIncrease.maximumValue(network)
                || currentValueToDecrease < scalableToDecrease.minimumValue(network)) {
                result.setLimitType(DichotomyLimitType.SCALABLE_LIMITS);
                break;
            }

            scalableToIncrease.scale(network, currentValueToIncrease);
            scalableToDecrease.scale(network, currentValueToDecrease);
        } while (step > parameters.getMinimumStep());

        return null;
    }

    @Override
    public String getName() {
        return "simple-dichotomy";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
