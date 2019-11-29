/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.afs;

import com.powsybl.afs.DependencyCache;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFileCreationContext;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.afs.storage.AppStorage;
import com.powsybl.afs.storage.NodeInfo;
import com.farao_community.farao.data.crac_file.CracFileProvider;
import com.farao_community.farao.data.flow_decomposition_results.FlowDecompositionResults;
import com.farao_community.farao.data.flow_decomposition_results.json.JsonFlowDecompositionResults;
import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;
import com.farao_community.farao.flow_decomposition.json.JsonFlowDecompositionParameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * Project file that represents a flow decomposition computation task
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowDecompositionRunner extends ProjectFile {

    static final String PSEUDO_CLASS = "FlowDecompositionRunner";

    static final int VERSION = 0;
    static final String CASE_DEPENDENCY_NAME = "case";
    static final String CRAC_FILE_PROVIDER_NAME = "cracFileProvider";

    private static final String PARAMETERS_JSON_NAME = "parametersJson";
    private static final String RESULT_JSON_NAME = "resultJson";

    private final DependencyCache<ProjectCase> caseDependency = new DependencyCache<>(this, CASE_DEPENDENCY_NAME, ProjectCase.class);
    private final DependencyCache<CracFileProvider> cracFileProviderDependency = new DependencyCache<>(this, CRAC_FILE_PROVIDER_NAME, CracFileProvider.class);

    public FlowDecompositionRunner(ProjectFileCreationContext context) {
        super(context, VERSION);
    }

    public Optional<ProjectCase> getCase() {
        return caseDependency.getFirst();
    }

    public void setCase(ProjectFile aCase) {
        Objects.requireNonNull(aCase);
        this.setDependencies(CASE_DEPENDENCY_NAME, Collections.singletonList(aCase));
        this.caseDependency.invalidate();
    }

    public Optional<CracFileProvider> getCracFileProvider() {
        return cracFileProviderDependency.getFirst();
    }

    public void setCracFileProvider(ProjectFile aCracFileProvider) {
        Objects.requireNonNull(aCracFileProvider);
        this.setDependencies(CRAC_FILE_PROVIDER_NAME, Collections.singletonList(aCracFileProvider));
        this.cracFileProviderDependency.invalidate();
    }

    public FlowDecompositionParameters readParameters() {
        try (InputStream is = storage.readBinaryData(info.getId(), PARAMETERS_JSON_NAME)
                .orElseThrow(AssertionError::new)) {
            return JsonFlowDecompositionParameters.read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeParameters(AppStorage storage, NodeInfo info, FlowDecompositionParameters parameters) {
        Objects.requireNonNull(parameters);
        try (OutputStream os = storage.writeBinaryData(info.getId(), PARAMETERS_JSON_NAME)) {
            JsonFlowDecompositionParameters.write(parameters, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        storage.flush();
    }

    public void writeParameters(FlowDecompositionParameters parameters) {
        writeParameters(storage, info, parameters);
    }

    public FlowDecompositionResults readResult() {
        try (InputStream is = storage.readBinaryData(info.getId(), RESULT_JSON_NAME).orElse(null)) {
            if (is != null) {
                return JsonFlowDecompositionResults.read(is);
            }
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeResult(FlowDecompositionResults result) {
        Objects.requireNonNull(result);
        try (OutputStream os = storage.writeBinaryData(info.getId(), RESULT_JSON_NAME)) {
            JsonFlowDecompositionResults.write(result, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        storage.flush();
    }

    public boolean hasResult() {
        return storage.dataExists(info.getId(), RESULT_JSON_NAME);
    }

    public void run() {
        findService(FlowDecompositionRunningService.class).run(this);
    }
}
