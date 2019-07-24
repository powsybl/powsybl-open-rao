/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.afs;

import com.powsybl.afs.*;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.afs.storage.NodeGenericMetadata;
import com.powsybl.afs.storage.NodeInfo;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;

import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowBasedComputationRunnerBuilder implements ProjectFileBuilder<FlowBasedComputationRunner> {

    private final ProjectFileBuildContext context;

    private final FlowBasedComputationParameters parameters;

    private String name;

    private ProjectFile aCase;

    private ProjectFile aCracFileProvider;

    public FlowBasedComputationRunnerBuilder(ProjectFileBuildContext context, FlowBasedComputationParameters parameters) {
        this.context = Objects.requireNonNull(context);
        this.parameters = Objects.requireNonNull(parameters);
    }

    public FlowBasedComputationRunnerBuilder withName(String name) {
        this.name = Objects.requireNonNull(name);
        return this;
    }

    public FlowBasedComputationRunnerBuilder withCase(ProjectFile aCase) {
        this.aCase = Objects.requireNonNull(aCase);
        return this;
    }

    public FlowBasedComputationRunnerBuilder withCracFileProvider(ProjectFile flowBasedComputationStore) {
        this.aCracFileProvider = Objects.requireNonNull(flowBasedComputationStore);
        return this;
    }

    @Override
    public FlowBasedComputationRunner build() {
        if (name == null) {
            throw new AfsException("Name is not set");
        }
        if (aCase == null) {
            throw new AfsException("Case is not set");
        } else {
            if (!(aCase instanceof ProjectCase)) {
                throw new AfsException("Case does not implement " + ProjectCase.class.getName());
            }
        }

        ProjectFolder folder = new ProjectFolder(new ProjectFileCreationContext(context.getFolderInfo(),
                context.getStorage(),
                context.getProject()));

        if (folder.getChild(name).isPresent()) {
            throw new AfsException("Folder '" + folder.getPath() + "' already contains a '" + name + "' node");
        }

        // check links belong to the same project
        if (!folder.getProject().getId().equals(aCase.getProject().getId())) {
            throw new AfsException("Case and folder do not belong to the same project");
        }
        if (aCracFileProvider != null && !folder.getProject().getId().equals(aCracFileProvider.getProject().getId())) {
            throw new AfsException("FlowBased computation store and folder do not belong to the same project");
        }

        // create project file
        NodeInfo info = context.getStorage().createNode(context.getFolderInfo().getId(), name, FlowBasedComputationRunner.PSEUDO_CLASS,
                "", FlowBasedComputationRunner.VERSION, new NodeGenericMetadata());

        // create case link
        context.getStorage().addDependency(info.getId(), FlowBasedComputationRunner.CASE_DEPENDENCY_NAME, aCase.getId());

        // create Crac File store link
        if (aCracFileProvider != null) {
            context.getStorage().addDependency(info.getId(), FlowBasedComputationRunner.CRAC_FILE_PROVIDER_NAME, aCracFileProvider.getId());
        }

        // write parameters using default one
        FlowBasedComputationRunner.writeParameters(context.getStorage(), info, parameters);

        context.getStorage().setConsistent(info.getId());
        context.getStorage().flush();

        return new FlowBasedComputationRunner(new ProjectFileCreationContext(info, context.getStorage(), context.getProject()));
    }
}
