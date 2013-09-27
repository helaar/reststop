package org.kantega.reststop.cxf;

import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.kantega.reststop.api.*;
import org.kantega.reststop.api.jaxws.EndpointConfiguration;
import org.kantega.reststop.api.jaxws.JaxWsPlugin;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.util.*;

/**
 *
 */
public class CxfPlugin extends DefaultReststopPlugin {


    private final ReststopPluginManager pluginManager;
    private List<Endpoint> endpoints = new ArrayList<>();

    public CxfPlugin(Reststop reststop, final ReststopPluginManager pluginManager, ServletContext servletContext) {
        this.pluginManager = pluginManager;

        CXFNonSpringServlet cxfNonSpringServlet = new CXFNonSpringServlet();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            cxfNonSpringServlet.init(new EmptyServletConfig(servletContext));
        } catch (ServletException e) {
            throw new RuntimeException("Failed starting CXF", e);
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
        CXFFilter cxfFilter = new CXFFilter(cxfNonSpringServlet);

        addServletFilter(reststop.createFilter(cxfFilter, "/ws/*", FilterPhase.USER));

        addPluginListener(new PluginListener() {
            @Override
            public void pluginsUpdated(Collection<ReststopPlugin> plugins) {
                deployEndpoints();
            }
        });
    }

    private void deployEndpoints() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            for (Endpoint endpoint : endpoints) {
                endpoint.stop();
            }
            for(JaxWsPlugin plugin : pluginManager.getPlugins(JaxWsPlugin.class)) {
                for(EndpointConfiguration config : plugin.getEndpointConfigurations()) {
                    Endpoint endpoint = Endpoint.create(config.getImplementor());
                    endpoint.publish(config.getPath());
                    CxfPlugin.this.endpoints.add(endpoint);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }

    class CXFFilter implements Filter {


        private final CXFNonSpringServlet cxfNonSpringServlet;

        public CXFFilter(CXFNonSpringServlet cxfNonSpringServlet) {

            this.cxfNonSpringServlet = cxfNonSpringServlet;
        }

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {

        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            HttpServletRequest req = (HttpServletRequest) servletRequest;
            HttpServletResponse resp = (HttpServletResponse) servletResponse;

            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

                cxfNonSpringServlet.service(new HttpServletRequestWrapper(req) {
                    @Override
                    public String getServletPath() {
                        return "/ws";
                    }

                    @Override
                    public String getPathInfo() {
                        String requestURI = getRequestURI();
                        return requestURI.substring("/ws".length());
                    }
                }, resp);
            } finally {
                Thread.currentThread().setContextClassLoader(loader);
            }
        }

        @Override
        public void destroy() {

        }
    }

    private class EmptyServletConfig implements ServletConfig {

        private final ServletContext servletContext;

        public EmptyServletConfig(ServletContext servletContext) {

            this.servletContext = servletContext;
        }

        @Override
        public String getServletName() {
            return "cxf";
        }

        @Override
        public ServletContext getServletContext() {
            return servletContext;
        }

        @Override
        public String getInitParameter(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.emptyEnumeration();
        }
    }
}
