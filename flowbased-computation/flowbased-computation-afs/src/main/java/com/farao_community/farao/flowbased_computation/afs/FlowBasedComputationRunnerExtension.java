/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.afs;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ProjectFileBuildContext;
import com.powsybl.afs.ProjectFileCreationContext;
import com.powsybl.afs.ProjectFileExtension;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;

import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(ProjectFileExtension.class)
public class FlowBasedComputationRunnerExtension implements ProjectFileExtension<FlowBasedComputationRunner, FlowBasedComputationRunnerBuilder> {

    private final FlowBasedComputationParameters parameters;

    public FlowBasedComputationRunnerExtension() {
        this(FlowBasedComputationParameters.load());
    }

    public FlowBasedComputationRunnerExtension(FlowBasedComputationParameters parameters) {
        this.parameters = Objects.requireNonNull(parameters);
    }

    @Override
    public Class<FlowBasedComputationRunner> getProjectFileClass() {
        return FlowBasedComputationRunner.class;
    }

    @Override
    public String getProjectFilePseudoClass() {
        return FlowBasedComputationRunner.PSEUDO_CLASS;
    }

    @Override
    public Class<FlowBasedComputationRunnerBuilder> getProjectFileBuilderClass() {
        return FlowBasedComputationRunnerBuilder.class;
    }

    @Override
    public FlowBasedComputationRunner createProjectFile(ProjectFileCreationContext context) {
        return new FlowBasedComputationRunner(context);
    }

    @Override
    public FlowBasedComputationRunnerBuilder createProjectFileBuilder(ProjectFileBuildContext context) {
        return new FlowBasedComputationRunnerBuilder(context, parameters);
    }
}
