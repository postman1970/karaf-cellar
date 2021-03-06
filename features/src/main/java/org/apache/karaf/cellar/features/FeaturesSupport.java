/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.features;

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Generic features support.
 */
public class FeaturesSupport extends CellarSupport {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(FeaturesSupport.class);

    protected FeaturesService featuresService;

    public void init() {
        // nothing to do
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Check if a feature is already installed locally.
     *
     * @param name the feature name.
     * @param version the feature version.
     * @return true if the feature is already installed locally, false else.
     */
    public Boolean isFeatureInstalledLocally(String name, String version) {
        if (featuresService != null) {
            try {
                Feature[] localFeatures = featuresService.listInstalledFeatures();

                if (localFeatures != null && localFeatures.length > 0) {
                    for (Feature localFeature : localFeatures) {
                        if (localFeature.getName().equals(name) && (localFeature.getVersion().equals(version) || version == null))
                            return true;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("CELLAR FEATURES: can't check if the feature {}/{} is installed locally", name, version, e);
            }
        }
        return false;
    }

    /**
     * Check if a features repository is already registered locally.
     *
     * @param uri the features repository URI.
     * @return true if the features repository is already registered locally, false else.
     */
    public Boolean isRepositoryRegisteredLocally(String uri) {
        try {
            Repository[] localRepositories = featuresService.listRepositories();
            for (Repository localRepository : localRepositories) {
                if (localRepository.getURI().toString().equals(uri)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("CELLAR FEATURES: can't check if the feature repository {} is registered locally", uri, e);
        }
        return false;
    }

    /**
     * Push a {@code Feature} and its status in a cluster group.
     *
     * @param feature the feature to push in the cluster group.
     * @param group the cluster group.
     */
    public void pushFeature(Feature feature, Group group) {
        if (feature != null) {
            String groupName = group.getName();
            Map<String, FeatureState> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);

            if (isAllowed(group, Constants.CATEGORY, feature.getName(), EventType.OUTBOUND)) {
                if (featuresService != null && clusterFeatures != null) {
                    FeatureState state = new FeatureState(feature.getName(), feature.getVersion(), featuresService.isInstalled(feature));
                    clusterFeatures.put(feature.getName() + "/" + feature.getVersion() , state);
                }
            } else LOGGER.trace("CELLAR FEATURES: feature {} is marked BLOCKED OUTBOUND for cluster group {}", feature.getName(), groupName);
        } else LOGGER.warn("CELLAR FEATURES: feature is null");
    }

    /**
     * Push a {@code Feature} and its status in a cluster group.
     * This version of the method force the bundle status, without looking the features service.
     *
     * @param feature the feature to push in the cluster group.
     * @param group the cluster group.
     * @param force true to force the bundle status (ignoring the features service), false else.
     */
    public void pushFeature(Feature feature, Group group, Boolean force) {
        if (feature != null) {
            String groupName = group.getName();
            Map<String, FeatureState> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);

            if (isAllowed(group, Constants.CATEGORY, feature.getName(), EventType.OUTBOUND)) {
                if (featuresService != null && clusterFeatures != null) {
                    FeatureState state = new FeatureState(feature.getName(), feature.getVersion(), force);
                    clusterFeatures.put(feature.getName() + "/" + feature.getVersion(), state);
                }
            } else LOGGER.trace("CELLAR FEATURES: feature {} is marked BLOCKED OUTBOUND for cluster group {}", feature.getName(), groupName);
        } else LOGGER.warn("CELLAR FEATURES: feature is null");
    }

    /**
     * Push a features {@code Repository} in a cluster group.
     *
     * @param repository the features repository to push in the cluster group.
     * @param group the cluster group.
     */
    public void pushRepository(Repository repository, Group group) {
        String groupName = group.getName();
        Map<String, String> clusterRepositories = clusterManager.getMap(Constants.REPOSITORIES_MAP + Configurations.SEPARATOR + groupName);

        boolean found = false;
        for (String clusterRepository : clusterRepositories.keySet()) {
            if (clusterRepository.equals(repository.getURI().toString())) {
                found = true;
                break;
            }
        }

        if (!found) {
            try {
                clusterRepositories.put(repository.getURI().toString(), repository.getName());
            } catch (Exception e) {
                LOGGER.warn("CELLAR FEATURES: can't push features repository on the cluster", e);
            }
        }
    }

    /**
     * Remove a features {@code Repository} from a cluster group.
     *
     * @param repository the features repository to remove from the cluster group.
     * @param group the cluster group.
     */
    public void removeRepository(Repository repository, Group group) {
        String groupName = group.getName();
        Map<String, String> clusterRepositories = clusterManager.getMap(Constants.REPOSITORIES_MAP + Configurations.SEPARATOR + groupName);

        if (featuresService != null && clusterRepositories != null) {
            URI uri = repository.getURI();
            clusterRepositories.remove(uri.toString());
        }
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

}
