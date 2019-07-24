/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.afs;

import com.powsybl.afs.*;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.afs.storage.NodeGenericMetadata;
import com.powsybl.afs.storage.NodeInfo;
import com.farao_community.farao.data.crac_file.CracFileProvider;
import com.farao_community.farao.ra_optimisation.RaoComputationParameters;

import java.util.Objects;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class RaoComputationRunnerBuilder implements ProjectFileBuilder<RaoComputationRunner> {

    private final ProjectFileBuildContext context;

    private final RaoComputationParameters parameters;

    private String name;

    private ProjectFile aCase;

    private ProjectFile aCracFileProvider;

    public RaoComputationRunnerBuilder(ProjectFileBuildContext context, RaoComputationParameters parameters) {
        this.context = Objects.requireNonNull(context);
        this.parameters = Objects.requireNonNull(parameters);
    }

    public RaoComputationRunnerBuilder withName(String name) {
        this.name = Objects.requireNonNull(name);
        return this;
    }

    public RaoComputationRunnerBuilder withCase(ProjectFile aCase) {
        this.aCase = Objects.requireNonNull(aCase);
        return this;
    }

    public RaoComputationRunnerBuilder withCracFileProvider(ProjectFile raoComputationStore) {
        this.aCracFileProvider = Objects.requireNonNull(raoComputationStore);
        return this;
    }

    @Override
    public RaoComputationRunner build() {
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
        if (aCracFileProvider == null) {
            throw new AfsException("CRAC file is not set");
        } else {
            if (!(aCracFileProvider instanceof CracFileProvider)) {
                throw new AfsException("CRAC file does not implement " + CracFileProvider.class.getName());
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
        if (!folder.getProject().getId().equals(aCracFileProvider.getProject().getId())) {
            throw new AfsException("CRAC file provider and folder do not belong to the same project");
        }

        // create project file
        NodeInfo info = context.getStorage().createNode(context.getFolderInfo().getId(), name, RaoComputationRunner.PSEUDO_CLASS,
                "", RaoComputationRunner.VERSION, new NodeGenericMetadata());

        // create case link
        context.getStorage().addDependency(info.getId(), RaoComputationRunner.CASE_DEPENDENCY_NAME, aCase.getId());

        // create crac file provider link
        context.getStorage().addDependency(info.getId(), RaoComputationRunner.CRAC_FILE_PROVIDER_NAME, aCracFileProvider.getId());

        // write parameters using default one
        RaoComputationRunner.writeParameters(context.getStorage(), info, parameters);

        context.getStorage().setConsistent(info.getId());
        context.getStorage().flush();

        return new RaoComputationRunner(new ProjectFileCreationContext(info, context.getStorage(), context.getProject()));
    }
}
