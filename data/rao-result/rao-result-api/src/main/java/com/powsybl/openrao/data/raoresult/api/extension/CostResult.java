/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO: rename to CastorCostResult
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class CostResult extends AbstractExtension<RaoResult> {
    private static final String EXTENSION_NAME = "castor-cost-results";

    private final ElementaryCostResult initialCostResult;
    private final Map<Instant, ElementaryCostResult> costResultPerInstant;

    public CostResult() {
        this.initialCostResult = new ElementaryCostResult();
        this.costResultPerInstant = new HashMap<>();
    }

    /**
     * It gives the global cost of the situation at a given {@link Instant} according to the objective
     * function defined in the RAO.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @return The global cost of the situation state.
     */
    public double getCost(Instant optimizedInstant) {
        return getFunctionalCost(optimizedInstant) + getVirtualCost(optimizedInstant);
    }

    /**
     * It gives the functional cost of the situation at a given {@link Instant} according to the objective
     * function defined in the RAO. It represents the main part of the objective function.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @return The functional cost of the situation state.
     */
    public double getFunctionalCost(Instant optimizedInstant) {
        return getAppropriateResult(optimizedInstant).getFunctionalCost();
    }

    /**
     * It gives the sum of virtual costs of the situation at a given {@link Instant} according to the
     * objective function defined in the RAO. It represents the secondary parts of the objective
     * function.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @return The global virtual cost of the situation state.
     */
    public double getVirtualCost(Instant optimizedInstant) {
        return getAppropriateResult(optimizedInstant).getVirtualCost();
    }

    /**
     * It gives the names of the different virtual cost implied in the objective function defined in
     * the RAO.
     *
     * @return The set of virtual cost names.
     */
    public Set<String> getVirtualCostNames() {
        Set<String> allVirtualCostNames = new HashSet<>(initialCostResult.getVirtualCostNames());
        costResultPerInstant.values()
            .stream()
            .map(ElementaryCostResult::getVirtualCostNames)
            .forEach(allVirtualCostNames::addAll);
        return allVirtualCostNames;
    }

    /**
     * It gives the specified virtual cost of the situation at a given {@link Instant}. It represents the
     * secondary parts of the objective. If the specified name is not part of the virtual costs defined in the
     * objective function, this method could return {@code Double.NaN} values.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param virtualCostName  The name of the virtual cost.
     * @return The specific virtual cost of the situation state.
     */
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        return getAppropriateResult(optimizedInstant).getVirtualCost(virtualCostName);
    }

    public void addFunctionalCostResult(Instant optimizedInstant, double functionalCost) {
        getAppropriateResult(optimizedInstant).setFunctionalCost(functionalCost);
    }

    public void addVirtualCostResult(Instant optimizedInstant, String virtualCostName, double virtualCost) {
        getAppropriateResult(optimizedInstant).setVirtualCost(virtualCostName, virtualCost);
    }

    private static String getInstantId(Instant optimizedInstant) {
        return optimizedInstant == null ? "initial" : optimizedInstant.getId();
    }

    private ElementaryCostResult getAppropriateResult(Instant optimizedInstant) {
        return optimizedInstant == null ?
            initialCostResult :
            costResultPerInstant.computeIfAbsent(optimizedInstant, k -> new ElementaryCostResult());
    }

    public void serialize(JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        initialCostResult.serialize(jsonGenerator, "initial");
        for (Instant instant : costResultPerInstant.keySet().stream().sorted().toList()) {
            getAppropriateResult(instant).serialize(jsonGenerator, instant.getId());
        }
        jsonGenerator.writeEndObject();
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    private static final class ElementaryCostResult {
        private double functionalCost;
        private final Map<String, Double> virtualCosts;
        private static final double DEFAULT_COST = Double.NaN;

        private ElementaryCostResult() {
            this.functionalCost = DEFAULT_COST;
            this.virtualCosts = new HashMap<>();
        }

        private void setFunctionalCost(double functionalCost) {
            this.functionalCost = functionalCost;
        }

        private double getFunctionalCost() {
            return functionalCost;
        }

        private void setVirtualCost(String virtualCostName, double virtualCost) {
            virtualCosts.put(virtualCostName, virtualCost);
        }

        private double getVirtualCost(String virtualCostName) {
            return virtualCosts.getOrDefault(virtualCostName, DEFAULT_COST);
        }

        private double getVirtualCost() {
            return virtualCosts.isEmpty() ? DEFAULT_COST : virtualCosts.values().stream().reduce(0.0, Double::sum);
        }

        private Set<String> getVirtualCostNames() {
            return virtualCosts.keySet();
        }

        private void serialize(JsonGenerator jsonGenerator, String instantId) throws IOException {
            jsonGenerator.writeObjectFieldStart(instantId);
            jsonGenerator.writeNumberField("functionalCost", functionalCost);
            jsonGenerator.writeObjectFieldStart("virtualCost");
            for (String virtualCostName : getVirtualCostNames().stream().sorted().toList()) {
                jsonGenerator.writeNumberField(virtualCostName, virtualCosts.get(virtualCostName));
            }
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
        }
    }
}
