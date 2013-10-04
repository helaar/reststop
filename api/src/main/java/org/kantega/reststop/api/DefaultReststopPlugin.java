/*
 * Copyright 2013 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kantega.reststop.api;


import org.kantega.reststop.classloaderutils.PluginClassLoader;

import javax.servlet.Filter;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public class DefaultReststopPlugin implements ReststopPlugin {

    private final List<Filter> servletFilters = new CopyOnWriteArrayList<>();
    private final List<PluginListener> pluginListeners = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    public DefaultReststopPlugin() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if(loader instanceof PluginClassLoader) {
            PluginClassLoader pluginClassLoader = (PluginClassLoader) loader;
            Properties properties = pluginClassLoader.getPluginInfo().getConfig();

            Class clazz = getClass();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                Config config = field.getAnnotation(Config.class);
                if(config != null) {
                    if(! String.class.equals(field.getType())) {
                        throw new IllegalArgumentException("Don't know how to inject value for @Config annotated field of type " + field.getType());
                    }

                    String name = config.property();

                    if( name == null || name.trim().isEmpty())  {
                        name = field.getName();
                    }
                    String defaultValue = config.defaultValue().isEmpty() ? null : config.defaultValue();

                    String value = properties.getProperty(name, defaultValue);

                    if( (value == null || value.trim().isEmpty()) && config.required()) {
                        throw new IllegalArgumentException("Configuration missing for required @Config field '" +field.getName() +"' in class " + field.getDeclaringClass().getName());
                    }

                    field.setAccessible(true);
                    try {
                        field.set(this, value);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }

                }
            }
        }
    }

    protected void addServletFilter(Filter filter) {
        servletFilters.add(filter);
    }

    public List<Filter> getServletFilters() {
        return servletFilters;
    }

    protected  <T> T addService(T service) {
        Class<T> type = (Class<T>) service.getClass();
        return addService(type, service);
    }

    protected <T> T addService(Class<T> type, T service) {
        if(services.containsKey(type)) {
            throw new IllegalArgumentException("Service already added with type " + type.getName());
        }
        services.put(type, service);
        return service;
    }


    @Override
    public <T> T getService(Class<T> type) {
        return type.cast(services.get(type));
    }

    @Override
    public Set<Class<?>> getServiceTypes() {
        return services.keySet();
    }

    @Override
    public Collection<PluginListener> getPluginListeners() {
        return pluginListeners;
    }

    protected void addPluginListener(PluginListener pluginListener) {
        pluginListeners.add(pluginListener);
    }


}
