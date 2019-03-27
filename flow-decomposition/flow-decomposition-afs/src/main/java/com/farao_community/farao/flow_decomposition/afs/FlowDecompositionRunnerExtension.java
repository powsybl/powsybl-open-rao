/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.afs;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ProjectFileBuildContext;
import com.powsybl.afs.ProjectFileBuilder;
import com.powsybl.afs.ProjectFileCreationContext;
import com.powsybl.afs.ProjectFileExtension;
import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;

import java.util.Objects;

/**
 * Project file extension for flow decomposition runner
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(ProjectFileExtension.class)
public class FlowDecompositionRunnerExtension implements ProjectFileExtension<FlowDecompositionRunner, FlowDecompositionRunnerBuilder> {

    private final FlowDecompositionParameters parameters;

    public FlowDecompositionRunnerExtension() {
        this(FlowDecompositionParameters.load());
    }

    public FlowDecompositionRunnerExtension(FlowDecompositionParameters parameters) {
        this.parameters = Objects.requireNonNull(parameters);
    }

    @Override
    public Class<FlowDecompositionRunner> getProjectFileClass() {
        return FlowDecompositionRunner.class;
    }

    @Override
    public String getProjectFilePseudoClass() {
        return FlowDecompositionRunner.PSEUDO_CLASS;
    }

    @Override
    public Class<FlowDecompositionRunnerBuilder> getProjectFileBuilderClass() {
        return FlowDecompositionRunnerBuilder.class;
    }

    @Override
    public FlowDecompositionRunner createProjectFile(ProjectFileCreationContext projectFileCreationContext) {
        return new FlowDecompositionRunner(projectFileCreationContext);
    }

    @Override
    public ProjectFileBuilder<FlowDecompositionRunner> createProjectFileBuilder(ProjectFileBuildContext projectFileBuildContext) {
        return new FlowDecompositionRunnerBuilder(projectFileBuildContext, parameters);
    }
}
