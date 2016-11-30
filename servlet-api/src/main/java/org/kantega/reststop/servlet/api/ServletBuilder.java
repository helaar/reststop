/*
 * Copyright 2015 Kantega AS
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

package org.kantega.reststop.servlet.api;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import java.net.URL;
import java.util.Properties;

/**
 *
 */
public interface ServletBuilder {

    FilterChain newFilterChain(FilterChain filterChain);

    ServletConfig servletConfig(String name, Properties properties);
    FilterConfig filterConfig(String name, Properties properties);

    @Deprecated
    Filter servlet(HttpServlet servlet, String path);

    Filter servlet(HttpServlet servlet, String path, String... additionalPaths);

    @Deprecated
    Filter filter(Filter filter, String mapping, FilterPhase phase);

    Filter filter(Filter filter, FilterPhase phase, String path, String... additionalPaths);


    @Deprecated
    Filter resourceServlet(String path, URL url);

    Filter resourceServlet(URL url, String path, String... additionalPaths);


    @Deprecated
    Filter redirectServlet(String path, String location);


    RedirectBuilder redirectFrom(String fromPath, String... additionalFromPaths);

    interface RedirectBuilder {
        Filter to(String location);
    }
}