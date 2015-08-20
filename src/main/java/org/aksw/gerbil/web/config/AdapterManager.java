package org.aksw.gerbil.web.config;

import java.util.List;
import java.util.Set;

import org.aksw.gerbil.annotators.AnnotatorConfiguration;
import org.aksw.gerbil.annotators.NIFWebserviceAnnotatorConfiguration;
import org.aksw.gerbil.config.GerbilConfiguration;
import org.aksw.gerbil.datasets.DatasetConfiguration;
import org.aksw.gerbil.datasets.NIFFileDatasetConfig;
import org.aksw.gerbil.datatypes.ExperimentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AdapterManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdapterManager.class);

    private static final String NIF_WS_PREFIX = "NIFWS_";
    private static final String NIF_WS_SUFFIX = " (NIF WS)";
    private static final String UPLOADED_FILES_PATH_PROPERTY_KEY = "org.aksw.gerbil.UploadPath";
    private static final String UPLOADED_DATASET_SUFFIX = " (uploaded)";
    private static final String UPLOADED_DATASET_PREFIX = "NIFDS_";

    @Autowired
    @Qualifier("annotators")
    private AdapterList<AnnotatorConfiguration> annotators;

    @Autowired
    @Qualifier("datasets")
    private AdapterList<DatasetConfiguration> datasets;

    public Set<String> getAnnotatorNamesForExperiment(ExperimentType type) {
        return annotators.getAdapterNamesForExperiment(type);
    }

    public Set<String> getDatasetNamesForExperiment(ExperimentType type) {
        return datasets.getAdapterNamesForExperiment(type);
    }

    public AnnotatorConfiguration getAnnotatorConfig(String name, ExperimentType type) {
        List<AnnotatorConfiguration> configs = annotators.getAdaptersForName(name);
        if (configs != null) {
            for (AnnotatorConfiguration config : configs) {
                if (config.isApplicableForExperiment(type)) {
                    return config;
                }
            }
            LOGGER.error("Couldn't find an annotator with the name \"" + name
                    + "\" that is applicable for the experiment \"" + type + "\". Returning null.");
        } else {
            if (name.startsWith(NIF_WS_PREFIX)) {
                // This describes a NIF based web service
                // The name should have the form NIF_WS_PREFIX + "name(uri)"
                int pos = name.lastIndexOf('(');
                if (pos < 0) {
                    LOGGER.error("Couldn't parse the definition of this NIF based web service \"" + name
                            + "\". Returning null.");
                    return null;
                }
                String uri = name.substring(pos + 1, name.length() - 1);
                // remove "NIFWS_" from the name
                name = name.substring(NIF_WS_PREFIX.length(), pos) + NIF_WS_SUFFIX;
                return new NIFWebserviceAnnotatorConfiguration(uri, name, false, type);
            }
            LOGGER.error("Got an unknown annotator name \"" + name + "\". Returning null.");
        }
        return null;
    }

    public DatasetConfiguration getDatasetConfig(String name, ExperimentType type) {
        List<DatasetConfiguration> configs = datasets.getAdaptersForName(name);
        if (configs != null) {
            for (DatasetConfiguration config : configs) {
                if (config.isApplicableForExperiment(type)) {
                    return config;
                }
            }
        } else {
            if (name.startsWith(UPLOADED_DATASET_PREFIX)) {
                String uploadedFilesPath = GerbilConfiguration.getInstance()
                        .getString(UPLOADED_FILES_PATH_PROPERTY_KEY);
                if (uploadedFilesPath == null) {
                    LOGGER.error(
                            "Couldn't process uploaded file request, because the upload path is not set (\"{}\"). Returning null.",
                            UPLOADED_FILES_PATH_PROPERTY_KEY);
                    return null;
                }
                // This describes a NIF based web service
                // The name should have the form "NIFDS_name(uri)"
                int pos = name.lastIndexOf('(');
                if (pos < 0) {
                    LOGGER.error("Couldn't parse the definition of this NIF based web service \"" + name
                            + "\". Returning null.");
                    return null;
                }
                String uri = uploadedFilesPath + name.substring(pos + 1, name.length() - 1);
                // remove dataset prefix from the name
                name = name.substring(UPLOADED_DATASET_PREFIX.length(), pos) + UPLOADED_DATASET_SUFFIX;
                return new NIFFileDatasetConfig(name, uri, false, type);
            }
            LOGGER.error("Got an unknown annotator name\"" + name + "\". Returning null.");
            return null;
        }
        return null;
    }
}