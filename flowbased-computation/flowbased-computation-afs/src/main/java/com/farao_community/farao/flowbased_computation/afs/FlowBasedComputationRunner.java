/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.afs;

import com.farao_community.farao.flowbased_computation.FlowBasedGlskValuesProvider;
import com.powsybl.afs.DependencyCache;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFileCreationContext;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.afs.storage.AppStorage;
import com.powsybl.afs.storage.NodeInfo;
import com.farao_community.farao.data.crac_file.CracFileProvider;
import com.farao_community.farao.data.flowbased_domain.DataDomain;
import com.farao_community.farao.data.flowbased_domain.json.JsonFlowbasedDomain;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import com.farao_community.farao.flowbased_computation.json.JsonFlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.json.JsonFlowBasedComputationResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowBasedComputationRunner extends ProjectFile {

    static final String PSEUDO_CLASS = "FlowBasedRunner";

    static final int VERSION = 0;
    static final String CASE_DEPENDENCY_NAME = "case";
    static final String CRAC_FILE_PROVIDER_NAME = "cracFileProvider";
    static final String GLSK_PROVIDER_NAME = "glskProvider";

    private static final String PARAMETERS_JSON_NAME = "parametersJson";
    private static final String RESULT_JSON_NAME = "resultJson";

    private final DependencyCache<ProjectCase> caseDependency = new DependencyCache<>(this, CASE_DEPENDENCY_NAME, ProjectCase.class);

    private final DependencyCache<CracFileProvider> cracFileProviderDependency = new DependencyCache<>(this, CRAC_FILE_PROVIDER_NAME, CracFileProvider.class);

    private final DependencyCache<FlowBasedGlskValuesProvider> flowBasedGlskValuesProviderDependency = new DependencyCache<>(this, GLSK_PROVIDER_NAME, FlowBasedGlskValuesProvider.class);

    public FlowBasedComputationRunner(ProjectFileCreationContext context) {
        super(context, VERSION);
    }

    public Optional<ProjectCase> getCase() {
        return caseDependency.getFirst();
    }

    public void setCase(ProjectFile aCase) {
        Objects.requireNonNull(aCase);
        this.setDependencies("case", Collections.singletonList(aCase));
        this.caseDependency.invalidate();
    }

    public void setCracFileProvider(ProjectFile cracFile) {
        Objects.requireNonNull(cracFile);
        this.setDependencies("cracFile", Collections.singletonList(cracFile));
        this.cracFileProviderDependency.invalidate();
    }

    public Optional<CracFileProvider> getCracFileProvider() {
        return cracFileProviderDependency.getFirst();
    }

    public Optional<FlowBasedGlskValuesProvider> getGlskProvider() {
        return flowBasedGlskValuesProviderDependency.getFirst();
    }

    public void run() {
        findService(FlowBasedComputationRunningService.class).run(this);
    }

    public FlowBasedComputationParameters readParameters() {
        try (InputStream is = storage.readBinaryData(info.getId(), PARAMETERS_JSON_NAME)
                .orElseThrow(AssertionError::new)) {
            return JsonFlowBasedComputationParameters.read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeParameters(AppStorage storage, NodeInfo info, FlowBasedComputationParameters parameters) {
        Objects.requireNonNull(parameters);
        try (OutputStream os = storage.writeBinaryData(info.getId(), PARAMETERS_JSON_NAME)) {
            JsonFlowBasedComputationParameters.write(parameters, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        storage.flush();
    }

    public void writeParameters(FlowBasedComputationParameters parameters) {
        writeParameters(storage, info, parameters);
    }

    public DataDomain readResult() {
        try (InputStream is = storage.readBinaryData(info.getId(), RESULT_JSON_NAME).orElse(null)) {
            if (is != null) {
                return JsonFlowbasedDomain.read(is);
            }
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeResult(DataDomain result) {
        Objects.requireNonNull(result);
        try (OutputStream os = storage.writeBinaryData(info.getId(), RESULT_JSON_NAME)) {
            JsonFlowbasedDomain.write(result, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        storage.flush();
    }

    public void writeResult(FlowBasedComputationResult result) {
        Objects.requireNonNull(result);
        try (OutputStream os = storage.writeBinaryData(info.getId(), RESULT_JSON_NAME)) {
            JsonFlowBasedComputationResult.write(result, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        storage.flush();
    }

    public boolean hasResult() {
        return storage.dataExists(info.getId(), RESULT_JSON_NAME);
    }

}
