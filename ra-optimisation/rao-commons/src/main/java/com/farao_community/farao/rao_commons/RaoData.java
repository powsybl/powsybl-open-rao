/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;

import java.util.*;

/**
 * A RaoData is an object that gathers Network, Crac and SystematicSensitivityResult data. It manages
 * variants of these objects to ensure data consistency at any moment. Network will remain the same at any moment
 * with no variant management. It is a single point of entry to manipulate all data related to linear rao with
 * variant management.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoData {
    static final String NO_WORKING_VARIANT = "No working variant is defined.";
    private static final String UNKNOWN_VARIANT = "Unknown variant %s";

    private List<String> variantIds;
    private String workingVariantId;
    private Network network;
    private Crac crac;
    private State optimizedState;
    private Set<State> perimeter;
    private Map<String, SystematicSensitivityResult> systematicSensitivityResultMap;
    private RaoDataManager raoDataManager;
    private ReferenceProgram referenceProgram;
    private GlskProvider glskProvider;

    /**
     * This constructor creates a new data variant with a pre-optimisation prefix and set it as the working variant.
     * So accessing data after this constructor will lead directly to the newly created variant data. CRAC and
     * sensitivity data will be empty. It will create a CRAC ResultVariantManager if it does not exist yet.
     *
     * @param network:          Network object.
     * @param crac:             CRAC object.
     * @param optimizedState:   State in which the remedial actions are optimized
     * @param perimeter:        set of State for which the Cnecs are monitored
     * @param referenceProgram: ReferenceProgram object (needed only for loopflows and relative margin)
     * @param glskProvider:     GLSK provider (needed only for loopflows)
     * @param initialVariantId: initial variant id (optional or null)
     */
    public RaoData(Network network, Crac crac, State optimizedState, Set<State> perimeter, ReferenceProgram referenceProgram, GlskProvider glskProvider, String initialVariantId) {
        this.network = network;
        this.crac = crac;
        this.optimizedState = optimizedState;
        this.perimeter = perimeter;
        this.variantIds = new ArrayList<>();
        this.systematicSensitivityResultMap = new HashMap<>();
        this.referenceProgram = referenceProgram;
        this.glskProvider = glskProvider;

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager == null) {
            resultVariantManager = new ResultVariantManager();
            crac.addExtension(ResultVariantManager.class, resultVariantManager);
        }

        String variantId = initialVariantId;
        if (Objects.isNull(variantId)) {
            variantId = createVariantFromWorkingVariant(VariantType.PRE_OPTIM);
        } else {
            variantIds.add(variantId);
            systematicSensitivityResultMap.put(variantId, null);
        }
        setWorkingVariant(variantId);
        raoDataManager = new RaoDataManager(this);
        raoDataManager.fillRangeActionResultsWithNetworkValues();
    }

    /**
     * This constructor creates a new data variant with a pre-optimisation prefix and set it as the working variant.
     * So accessing data after this constructor will lead directly to the newly created variant data. CRAC and
     * sensitivity data will be empty. It will create a CRAC ResultVariantManager if it does not exist yet.
     *
     * @param network:          Network object.
     * @param crac:             CRAC object.
     * @param optimizedState:   State in which the remedial actions are optimized
     * @param perimeter:        set of State for which the Cnecs are monitored
     * @param referenceProgram: ReferenceProgram object (needed only for loopflows and relative margin)
     * @param glskProvider:     GLSK provider (needed only for loopflows)
     */
    public RaoData(Network network, Crac crac, State optimizedState, Set<State> perimeter, ReferenceProgram referenceProgram, GlskProvider glskProvider) {
        this(network, crac, optimizedState, perimeter, referenceProgram, glskProvider, null);
    }

    /**
     * This constructor creates a new data variant with a pre-optimisation prefix and set it as the working variant.
     * So accessing data after this constructor will lead directly to the newly created variant data. CRAC and
     * sensitivity data will be empty. It will create a CRAC ResultVariantManager if it does not exist yet.
     *
     * @param network: Network object.
     * @param crac:    CRAC object.
     * @param optimizedState:   State in which the remedial actions are optimized
     * @param perimeter:        set of State for which the Cnecs are monitored
     */
    public RaoData(Network network, Crac crac, State optimizedState, Set<State> perimeter) {
        this(network, crac, optimizedState, perimeter, null, null, null);
    }

    public List<String> getVariantIds() {
        return variantIds;
    }

    public String getWorkingVariantId() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        return workingVariantId;
    }

    public String getInitialVariantId() {
        if (variantIds.isEmpty()) {
            throw new FaraoException("No variants are present in the data");
        }
        return variantIds.get(0);
    }

    public void setWorkingVariant(String variantId) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(String.format(UNKNOWN_VARIANT, variantId));
        }
        workingVariantId = variantId;
    }

    public Network getNetwork() {
        return network;
    }

    public Crac getCrac() {
        return crac;
    }

    public ReferenceProgram getReferenceProgram() {
        return referenceProgram;
    }

    public GlskProvider getGlskProvider() {
        return glskProvider;
    }

    public Set<Cnec> getCnecs() {
        Set<Cnec> cnecs = new HashSet<>();
        perimeter.forEach(state -> cnecs.addAll(crac.getCnecs(state)));
        return cnecs;
    }

    public Set<RangeAction> getAvailableRangeActions() {
        return crac.getRangeActions(network, optimizedState, UsageMethod.AVAILABLE);
    }

    public Set<NetworkAction> getAvailableNetworkActions() {
        return crac.getNetworkActions(network, optimizedState, UsageMethod.AVAILABLE);
    }

    public CracResult getCracResult(String variantId) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(String.format(UNKNOWN_VARIANT, variantId));
        }
        return crac.getExtension(CracResultExtension.class).getVariant(variantId);
    }

    public State getOptimizedState() {
        return optimizedState;
    }

    public Set<State> getPerimeter() {
        return perimeter;
    }

    public CracResult getCracResult() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        return getCracResult(workingVariantId);
    }

    public SystematicSensitivityResult getSystematicSensitivityResult() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        return systematicSensitivityResultMap.get(workingVariantId);
    }

    public void setSystematicSensitivityResult(SystematicSensitivityResult systematicSensitivityResult) {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        systematicSensitivityResultMap.put(workingVariantId, systematicSensitivityResult);
    }

    public boolean hasSensitivityValues() {
        return getSystematicSensitivityResult() != null;
    }

    public double getReferenceFlow(Cnec cnec) {
        return getSystematicSensitivityResult().getReferenceFlow(cnec);
    }

    public double getSensitivity(Cnec cnec, RangeAction rangeAction) {
        return getSystematicSensitivityResult().getSensitivityOnFlow(rangeAction, cnec);
    }

    public RaoDataManager getRaoDataManager() {
        return raoDataManager;
    }

    // VARIANTS MANAGEMENT METHODS

    public enum VariantType {
        PRE_OPTIM,
        POST_OPTIM
    }

    private String createVariantFromWorkingVariant(VariantType variantType) {
        String prefix;
        SystematicSensitivityResult systematicSensitivityResult;
        if (variantType == VariantType.PRE_OPTIM) {
            prefix = "preOptimisationResults";
            systematicSensitivityResult = null;
        } else {
            prefix = "postOptimisationResults";
            systematicSensitivityResult = getSystematicSensitivityResult();
        }
        String variantId = crac.getExtension(ResultVariantManager.class).createNewUniqueVariantId(prefix);
        //TODO: Copy crac result in the copy ?
        variantIds.add(variantId);
        systematicSensitivityResultMap.put(variantId, systematicSensitivityResult);
        return variantId;
    }

    /**
     * This methods creates a variant from an already existing data variant. CRAC result variant will be
     * created but empty and sensitivity computation results will be copied from the reference variant. The
     * variant ID will contain a post optimisation prefix.
     *
     * @return The data variant ID of the newly created variant.
     * @throws FaraoException if referenceVariantId is not an existing variant of the data.
     */
    public String cloneWorkingVariant() {
        return createVariantFromWorkingVariant(VariantType.POST_OPTIM);
    }

    /**
     * This method deletes a variant according to its ID. If the working variant is the variant to be deleted nothing
     * would be done. CRAC result can be kept.
     *
     * @param variantId:      Variant ID that is required to delete.
     * @param keepCracResult: If true it will delete the variant as data variant and the related network variant
     *                        but it will keep the crac variant.
     * @throws FaraoException if variantId is not an existing data variant.
     */
    public void deleteVariant(String variantId, boolean keepCracResult) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(String.format(UNKNOWN_VARIANT, variantId));
        }
        if (!variantId.equals(workingVariantId)) {
            systematicSensitivityResultMap.remove(variantId);
            if (!keepCracResult) {
                crac.getExtension(ResultVariantManager.class).deleteVariant(variantId);
            }
            variantIds.remove(variantId);
        }
    }

    /**
     * This method clears all the data variants with their related variants in the different data objects. It
     * enables to keep some CRAC result variants as it is results of computation.
     *
     * @param remainingCracResults IDs of the variants we want to keep the results in CRAC
     */
    public void clearWithKeepingCracResults(List<String> remainingCracResults) {
        workingVariantId = null;
        String[] copiedIds = new String[variantIds.size()];
        variantIds.toArray(copiedIds);
        for (String variantId: copiedIds) {
            deleteVariant(variantId, remainingCracResults.contains(variantId));
        }
        variantIds.clear();
    }

    /**
     * This method clear all the data variants with their related variants in the different data objects.
     */
    public void clear() {
        clearWithKeepingCracResults(Collections.emptyList());
    }
}
