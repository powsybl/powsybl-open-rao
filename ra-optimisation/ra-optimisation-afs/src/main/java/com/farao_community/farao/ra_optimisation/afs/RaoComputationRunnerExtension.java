/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.afs;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ProjectFileBuildContext;
import com.powsybl.afs.ProjectFileCreationContext;
import com.powsybl.afs.ProjectFileExtension;
import com.farao_community.farao.ra_optimisation.RaoComputationParameters;

import java.util.Objects;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@AutoService(ProjectFileExtension.class)
public class RaoComputationRunnerExtension implements ProjectFileExtension<RaoComputationRunner, RaoComputationRunnerBuilder> {

    private final RaoComputationParameters parameters;

    public RaoComputationRunnerExtension() {
        this(RaoComputationParameters.load());
    }

    public RaoComputationRunnerExtension(RaoComputationParameters parameters) {
        this.parameters = Objects.requireNonNull(parameters);
    }

    @Override
    public Class<RaoComputationRunner> getProjectFileClass() {
        return RaoComputationRunner.class;
    }

    @Override
    public String getProjectFilePseudoClass() {
        return RaoComputationRunner.PSEUDO_CLASS;
    }

    @Override
    public Class<RaoComputationRunnerBuilder> getProjectFileBuilderClass() {
        return RaoComputationRunnerBuilder.class;
    }

    @Override
    public RaoComputationRunner createProjectFile(ProjectFileCreationContext context) {
        return new RaoComputationRunner(context);
    }

    @Override
    public RaoComputationRunnerBuilder createProjectFileBuilder(ProjectFileBuildContext context) {
        return new RaoComputationRunnerBuilder(context, parameters);
    }
}
