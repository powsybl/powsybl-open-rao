package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.rao_commons.CnecResults;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.Map;
import java.util.Set;

public class IteratingLinearOptimizerInput {
    private Set<BranchCnec> loopflowCnecs;
    private Set<BranchCnec> cnecs;
    private Set<RangeAction> rangeActions;
    private Network network;
    private Map<RangeAction, Double> preperimeterSetpoints; // can be removed if we don't change taps in the network after each depth
    private CnecResults initialCnecResults;
    private Map<BranchCnec, Double> prePerimeterCnecMarginsInAbsoluteMW;
    private SensitivityAndLoopflowResults preOptimSensitivityResults;
    private SystematicSensitivityInterface systematicSensitivityInterface;
    private ObjectiveFunctionEvaluator objectiveFunctionEvaluator;
    private ZonalData<LinearGlsk> glskProvider;
    private ReferenceProgram referenceProgram;

    public static IteratingLinearOptimizerInputBuilder create() {
        return new IteratingLinearOptimizerInputBuilder();
    }

    public Set<BranchCnec> getLoopflowCnecs() {
        return loopflowCnecs;
    }

    public Set<BranchCnec> getCnecs() {
        return cnecs;
    }

    public Set<RangeAction> getRangeActions() {
        return rangeActions;
    }

    public Network getNetwork() {
        return network;
    }

    public Map<RangeAction, Double> getPreperimeterSetpoints() {
        return preperimeterSetpoints;
    }

    public CnecResults getInitialCnecResults() {
        return initialCnecResults;
    }

    public Map<BranchCnec, Double> getPrePerimeterCnecMarginsInAbsoluteMW() {
        return prePerimeterCnecMarginsInAbsoluteMW;
    }

    public SensitivityAndLoopflowResults getPreOptimSensitivityResults() {
        return preOptimSensitivityResults;
    }

    public SystematicSensitivityInterface getSystematicSensitivityInterface() {
        return systematicSensitivityInterface;
    }

    public ObjectiveFunctionEvaluator getObjectiveFunctionEvaluator() {
        return objectiveFunctionEvaluator;
    }

    public ZonalData<LinearGlsk> getGlskProvider() {
        return glskProvider;
    }

    public ReferenceProgram getReferenceProgram() {
        return referenceProgram;
    }

    public static final class IteratingLinearOptimizerInputBuilder {
        private Set<BranchCnec> loopflowCnecs;
        private Set<BranchCnec> cnecs;
        private Set<RangeAction> rangeActions;
        private Network network;
        private Map<RangeAction, Double> preperimeterSetpoints;
        private CnecResults initialCnecResults;
        private Map<BranchCnec, Double> prePerimeterCnecMarginsInAbsoluteMW;
        private SensitivityAndLoopflowResults preOptimSensitivityResults;
        private SystematicSensitivityInterface systematicSensitivityInterface;
        private ObjectiveFunctionEvaluator objectiveFunctionEvaluator;
        private ZonalData<LinearGlsk> glskProvider;
        private ReferenceProgram referenceProgram;

        public IteratingLinearOptimizerInputBuilder withLoopflowCnecs(Set<BranchCnec> loopflowCnecs) {
            this.loopflowCnecs = loopflowCnecs;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withCnecs(Set<BranchCnec> cnecs) {
            this.cnecs = cnecs;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withRangeActions(Set<RangeAction> rangeActions) {
            this.rangeActions = rangeActions;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withPreperimeterSetpoints(Map<RangeAction, Double> preperimeterSetpoints) {
            this.preperimeterSetpoints = preperimeterSetpoints;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withInitialCnecResults(CnecResults initialCnecResults) {
            this.initialCnecResults = initialCnecResults;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withPrePerimeterCnecMarginsInAbsoluteMW(Map<BranchCnec, Double> prePerimeterCnecMarginsInAbsoluteMW) {
            this.prePerimeterCnecMarginsInAbsoluteMW = prePerimeterCnecMarginsInAbsoluteMW;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withPreOptimSensitivityResults(SensitivityAndLoopflowResults preOptimSensitivityResults) {
            this.preOptimSensitivityResults = preOptimSensitivityResults;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withSystematicSensitivityInterface(SystematicSensitivityInterface systematicSensitivityInterface) {
            this.systematicSensitivityInterface = systematicSensitivityInterface;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withObjectiveFunctionEvaluator(ObjectiveFunctionEvaluator objectiveFunctionEvaluator) {
            this.objectiveFunctionEvaluator = objectiveFunctionEvaluator;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withGlskProvider(ZonalData<LinearGlsk> glskProvider) {
            this.glskProvider = glskProvider;
            return this;
        }

        public IteratingLinearOptimizerInputBuilder withReferenceProgram(ReferenceProgram referenceProgram) {
            this.referenceProgram = referenceProgram;
            return this;
        }

        public IteratingLinearOptimizerInput build() {
            // TODO : check non null arguments
            IteratingLinearOptimizerInput iteratingLinearOptimizerInput = new IteratingLinearOptimizerInput();
            iteratingLinearOptimizerInput.loopflowCnecs = this.loopflowCnecs;
            iteratingLinearOptimizerInput.cnecs = this.cnecs;
            iteratingLinearOptimizerInput.rangeActions = this.rangeActions;
            iteratingLinearOptimizerInput.network = this.network;
            iteratingLinearOptimizerInput.preperimeterSetpoints = this.preperimeterSetpoints;
            iteratingLinearOptimizerInput.initialCnecResults = this.initialCnecResults;
            iteratingLinearOptimizerInput.prePerimeterCnecMarginsInAbsoluteMW = this.prePerimeterCnecMarginsInAbsoluteMW;
            iteratingLinearOptimizerInput.preOptimSensitivityResults = this.preOptimSensitivityResults;
            iteratingLinearOptimizerInput.systematicSensitivityInterface = this.systematicSensitivityInterface;
            iteratingLinearOptimizerInput.objectiveFunctionEvaluator = this.objectiveFunctionEvaluator;
            iteratingLinearOptimizerInput.glskProvider = this.glskProvider;
            iteratingLinearOptimizerInput.referenceProgram = this.referenceProgram;
            return iteratingLinearOptimizerInput;
        }
    }
}
