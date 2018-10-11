/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.framework.deployer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.jar.Manifest;

import org.apache.karaf.framework.service.BundleActivatorHandler;
import org.apache.karaf.framework.service.BundleRegistry;
import org.apache.karaf.framework.service.OSGiServices;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSGiBundleLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(OSGiBundleLifecycle.class);

    private final BundleContextImpl context;
    private final BundleImpl bundle;
    private BundleActivatorHandler activator;

    public OSGiBundleLifecycle(final Manifest manifest, final File file, final OSGiServices services,
                               final BundleRegistry registry) {
        this.context = new BundleContextImpl(manifest, services, this::getBundle, registry);
        this.bundle = new BundleImpl(manifest, file, context);
    }

    public Bundle getBundle() {
        return bundle;
    }

    public OSGiBundleLifecycle start() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting {}", bundle);
        }

        final String activatorClass = context.getManifest().getMainAttributes().getValue("Bundle-Activator");
        if (activatorClass != null) {
            try {
                activator = new BundleActivatorHandler(BundleActivator.class.cast(
                        Thread.currentThread().getContextClassLoader()
                              .loadClass(activatorClass)
                              .getConstructor()
                              .newInstance()), context);
                activator.start();
            } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException(e.getTargetException());
            }
        }

        return this;
    }

    public void stop() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Stopping {}", bundle);
        }
        if (activator != null) {
            activator.stop();
        }
    }
}