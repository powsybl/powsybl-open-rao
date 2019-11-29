/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.afs;

import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFileBuildContext;
import com.powsybl.afs.ProjectFileBuilder;
import com.powsybl.afs.ProjectFileCreationContext;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.afs.storage.NodeGenericMetadata;
import com.powsybl.afs.storage.NodeInfo;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.CracFileProvider;
import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;

import java.util.Objects;

/**
 * Project file builder for flow decomposition runner
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowDecompositionRunnerBuilder implements ProjectFileBuilder<FlowDecompositionRunner> {

    private final ProjectFileBuildContext context;

    private final FlowDecompositionParameters parameters;

    private String name;

    private ProjectFile aCase;

    private ProjectFile aCracFileProvider;

    public FlowDecompositionRunnerBuilder(ProjectFileBuildContext context, FlowDecompositionParameters parameters) {
        this.context = Objects.requireNonNull(context);
        this.parameters = Objects.requireNonNull(parameters);
    }

    public FlowDecompositionRunnerBuilder withName(String name) {
        this.name = Objects.requireNonNull(name);
        return this;
    }

    public FlowDecompositionRunnerBuilder withCase(ProjectFile aCase) {
        this.aCase = Objects.requireNonNull(aCase);
        return this;
    }

    public FlowDecompositionRunnerBuilder withCracFileProvider(ProjectFile cracFileProvider) {
        this.aCracFileProvider = Objects.requireNonNull(cracFileProvider);
        return this;
    }

    @Override
    public FlowDecompositionRunner build() {
        if (name == null) {
            throw new FaraoException("Name is not set");
        }
        if (aCase == null) {
            throw new FaraoException("Case is not set");
        } else {
            if (!(aCase instanceof ProjectCase)) {
                throw new FaraoException("Case does not implement " + ProjectCase.class.getName());
            }
        }
        if (aCracFileProvider == null) {
            throw new FaraoException("CRAC file is not set");
        } else {
            if (!(aCracFileProvider instanceof CracFileProvider)) {
                throw new FaraoException("CRAC file does not implement " + CracFileProvider.class.getName());
            }
        }

        ProjectFolder folder = new ProjectFolder(new ProjectFileCreationContext(context.getFolderInfo(),
                context.getStorage(),
                context.getProject()));

        if (folder.getChild(name).isPresent()) {
            throw new FaraoException("Folder '" + folder.getPath() + "' already contains a '" + name + "' node");
        }

        // check links belong to the same project
        if (!folder.getProject().getId().equals(aCase.getProject().getId())) {
            throw new FaraoException("Case and folder do not belong to the same project");
        }
        if (!folder.getProject().getId().equals(aCracFileProvider.getProject().getId())) {
            throw new FaraoException("CRAC file provider and folder do not belong to the same project");
        }

        // create project file
        NodeInfo info = context.getStorage().createNode(context.getFolderInfo().getId(), name, FlowDecompositionRunner.PSEUDO_CLASS,
                "", FlowDecompositionRunner.VERSION, new NodeGenericMetadata());

        // create case link
        context.getStorage().addDependency(info.getId(), FlowDecompositionRunner.CASE_DEPENDENCY_NAME, aCase.getId());

        // create crac file provider link
        context.getStorage().addDependency(info.getId(), FlowDecompositionRunner.CRAC_FILE_PROVIDER_NAME, aCracFileProvider.getId());

        // write parameters using default one
        FlowDecompositionRunner.writeParameters(context.getStorage(), info, parameters);

        context.getStorage().setConsistent(info.getId());
        context.getStorage().flush();

        return new FlowDecompositionRunner(new ProjectFileCreationContext(info, context.getStorage(), context.getProject()));
    }
}
