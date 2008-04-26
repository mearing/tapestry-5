// Copyright 2006, 2007, 2008 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry.internal.transform;

import javassist.CtClass;
import javassist.Loader;
import javassist.LoaderClassPath;
import org.apache.tapestry.Binding;
import org.apache.tapestry.TapestryConstants;
import org.apache.tapestry.internal.InternalComponentResources;
import org.apache.tapestry.internal.services.Instantiator;
import org.apache.tapestry.internal.services.InternalClassTransformation;
import org.apache.tapestry.internal.services.InternalClassTransformationImpl;
import org.apache.tapestry.internal.test.InternalBaseTestCase;
import org.apache.tapestry.internal.transform.components.DefaultParameterBindingMethodComponent;
import org.apache.tapestry.internal.transform.components.DefaultParameterComponent;
import org.apache.tapestry.internal.transform.components.ParameterComponent;
import org.apache.tapestry.ioc.internal.services.ClassFactoryClassPool;
import org.apache.tapestry.ioc.internal.services.ClassFactoryImpl;
import org.apache.tapestry.ioc.internal.services.PropertyAccessImpl;
import org.apache.tapestry.ioc.services.ClassFactory;
import org.apache.tapestry.ioc.services.PropertyAccess;
import org.apache.tapestry.model.MutableComponentModel;
import org.apache.tapestry.runtime.Component;
import org.apache.tapestry.services.BindingSource;
import org.slf4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * There's no point in trying to unit test the code generated by {@link org.apache.tapestry.internal.transform.ParameterWorker}.
 * Instead, we excercize ParameterWorker, and test that the generated code works correctly in a number of scenarios.
 */
public class ParameterWorkerTest extends InternalBaseTestCase
{
    private final ClassLoader _contextClassLoader = Thread.currentThread().getContextClassLoader();

    private PropertyAccess _access = new PropertyAccessImpl();

    /**
     * Accessed by DefaultParameterBindingMethodComponent.
     */
    public static Binding _binding;

    @AfterClass
    public void cleanup()
    {
        _access = null;
        _binding = null;
    }

    @Test
    public void page_load_behavior() throws Exception
    {
        InternalComponentResources resources = mockInternalComponentResources();

        assertNotNull(setupForIntegrationTest(resources));
    }

    @Test
    public void invariant_object_retained_after_detach() throws Exception
    {
        InternalComponentResources resources = mockInternalComponentResources();

        Component component = setupForIntegrationTest(resources);

        // On first invocation, the resources are queried.

        String value = "To be in Tapestry in the spring time ...";

        train_isLoaded(resources, true);
        train_isBound(resources, "invariantObject", true);
        train_readParameter(resources, "invariantObject", String.class, value);

        replay();

        assertSame(_access.get(component, "invariantObject"), value);

        verify();

        // No further training needed here.

        replay();

        // Still cached ...

        assertSame(_access.get(component, "invariantObject"), value);

        component.postRenderCleanup();

        // Still cached ...

        assertSame(_access.get(component, "invariantObject"), value);

        component.containingPageDidDetach();

        // Still cached ...

        assertSame(_access.get(component, "invariantObject"), value);

        verify();
    }

    @Test
    public void invariant_primitive_retained_after_detach() throws Exception
    {
        InternalComponentResources resources = mockInternalComponentResources();

        Component component = setupForIntegrationTest(resources);

        // On first invocation, the resources are queried.

        long value = 123456;

        train_isLoaded(resources, true);
        train_isBound(resources, "invariantPrimitive", true);
        train_readParameter(resources, "invariantPrimitive", Long.class, value);

        replay();

        assertEquals(_access.get(component, "invariantPrimitive"), value);

        verify();

        // No further training needed here.

        replay();

        // Still cached ...

        assertEquals(_access.get(component, "invariantPrimitive"), value);

        component.postRenderCleanup();

        // Still cached ...

        assertEquals(_access.get(component, "invariantPrimitive"), value);

        verify();
    }

    /**
     * This actually checks several things: <ul> <li>Changing a parameter property before the page loads doesn't update
     * the binding</li> <li>Changing a parameter property changes the property AND the default value for the
     * property</li> <li>Unbound parameters to do not attempt to read or update their bindings (they'll be
     * optional)</li> </ul>
     *
     * @throws Exception
     */
    @Test
    public void changes_before_load_become_defaults_and_dont_update_bindings() throws Exception
    {
        InternalComponentResources resources = mockInternalComponentResources();

        Component component = setupForIntegrationTest(resources);

        train_isLoaded(resources, false);

        replay();

        assertNull(_access.get(component, "object"));

        verify();

        train_isLoaded(resources, false);

        replay();

        _access.set(component, "object", "new-default");

        verify();

        train_isLoaded(resources, false);

        replay();

        assertEquals(_access.get(component, "object"), "new-default");

        verify();

        trainForPageDidLoad(resources);

        replay();

        component.containingPageDidLoad();

        verify();

        // For the set ...

        train_isLoaded(resources, true);
        train_isBound(resources, "object", false);
        train_isRendering(resources, false);

        // For the first read ...

        train_isLoaded(resources, true);
        train_isBound(resources, "object", false);

        // For the second read (after postRenderCleanup) ...

        train_isLoaded(resources, true);
        train_isBound(resources, "object", false);

        replay();

        _access.set(component, "object", "new-value");
        assertEquals(_access.get(component, "object"), "new-value");

        component.postRenderCleanup();

        assertEquals(_access.get(component, "object"), "new-default");

        verify();
    }

    @Test
    public void cached_object_read() throws Exception
    {
        InternalComponentResources resources = mockInternalComponentResources();

        Component component = setupForIntegrationTest(resources);

        train_isLoaded(resources, true);
        train_isBound(resources, "object", true);
        train_readParameter(resources, "object", String.class, "first");
        train_isRendering(resources, false);

        replay();

        assertEquals(_access.get(component, "object"), "first");

        verify();

        // Keeps re-reading the parameter when not rendering.

        train_isLoaded(resources, true);
        train_isBound(resources, "object", true);
        train_readParameter(resources, "object", String.class, "second");
        train_isRendering(resources, false);

        replay();

        assertEquals(_access.get(component, "object"), "second");

        verify();

        // Now, when rendering is active, the value is cached

        train_isLoaded(resources, true);
        train_isBound(resources, "object", true);
        train_readParameter(resources, "object", String.class, "third");
        train_isRendering(resources, true);

        replay();

        assertEquals(_access.get(component, "object"), "third");

        // Does not cause readParameter() to be invoked:

        assertEquals(_access.get(component, "object"), "third");

        verify();

        train_isLoaded(resources, true);
        train_isBound(resources, "object", true);
        train_readParameter(resources, "object", String.class, "fourth");
        train_isRendering(resources, false);

        replay();

        component.postRenderCleanup();

        assertEquals(_access.get(component, "object"), "fourth");

        verify();
    }

    @Test
    public void cached_object_write() throws Exception
    {
        InternalComponentResources resources = mockInternalComponentResources();

        Component component = setupForIntegrationTest(resources);

        train_isLoaded(resources, true);
        train_isBound(resources, "object", true);
        resources.writeParameter("object", "first");
        train_isRendering(resources, false);

        train_isLoaded(resources, true);
        train_isBound(resources, "object", true);
        train_readParameter(resources, "object", String.class, "second");
        train_isRendering(resources, false);

        replay();

        _access.set(component, "object", "first");
        assertEquals(_access.get(component, "object"), "second");

        verify();

        // Now try during rendering ...

        train_isLoaded(resources, true);
        train_isBound(resources, "object", true);
        resources.writeParameter("object", "third");
        train_isRendering(resources, true);

        replay();

        _access.set(component, "object", "third");
        assertEquals(_access.get(component, "object"), "third");

        verify();

        // And the cached value is lost after rendering is complete.

        train_isLoaded(resources, true);
        train_isBound(resources, "object", true);
        train_readParameter(resources, "object", String.class, "fourth");
        train_isRendering(resources, false);

        replay();

        component.postRenderCleanup();

        assertEquals(_access.get(component, "object"), "fourth");

        verify();
    }

    @Test
    public void cached_primitive_write() throws Exception
    {
        InternalComponentResources resources = mockInternalComponentResources();

        Component component = setupForIntegrationTest(resources);

        train_isLoaded(resources, true);
        train_isBound(resources, "primitive", true);
        resources.writeParameter("primitive", 321);

        train_isRendering(resources, false);

        train_isLoaded(resources, true);
        train_isBound(resources, "primitive", true);
        train_readParameter(resources, "primitive", Integer.class, 123);
        train_isRendering(resources, false);

        replay();

        _access.set(component, "primitive", 321);
        assertEquals(_access.get(component, "primitive"), 123);

        verify();

        // Now try during rendering ...

        train_isLoaded(resources, true);
        train_isBound(resources, "primitive", true);
        resources.writeParameter("primitive", 567);
        train_isRendering(resources, true);

        replay();

        _access.set(component, "primitive", 567);
        assertEquals(_access.get(component, "primitive"), 567);

        verify();

        // And the cached value is lost after rendering is complete.

        train_isLoaded(resources, true);
        train_isBound(resources, "primitive", true);
        train_readParameter(resources, "primitive", Integer.class, 890);
        train_isRendering(resources, false);

        replay();

        component.postRenderCleanup();

        assertEquals(_access.get(component, "primitive"), 890);

        verify();
    }

    @Test
    public void uncached_object_read() throws Exception
    {
        InternalComponentResources resources = mockInternalComponentResources();

        Component component = setupForIntegrationTest(resources);

        // Notice no check for isRendering() since that is irrelevant to uncached parameters.
        // Also note difference between field name and parameter name, due to Parameter.name() being
        // specified.

        train_isLoaded(resources, true);
        train_isBound(resources, "uncached", true);
        train_readParameter(resources, "uncached", String.class, "first");
        train_isLoaded(resources, true);
        train_isBound(resources, "uncached", true);
        train_readParameter(resources, "uncached", String.class, "second");

        replay();

        assertEquals(_access.get(component, "uncachedObject"), "first");
        assertEquals(_access.get(component, "uncachedObject"), "second");

        verify();
    }

    protected void train_isBound(InternalComponentResources resources, String parameterName, boolean isBound)
    {
        expect(resources.isBound(parameterName)).andReturn(isBound);
    }

    @Test
    public void uncached_object_write() throws Exception
    {
        InternalComponentResources resources = mockInternalComponentResources();

        Component component = setupForIntegrationTest(resources);

        // Notice no check for isRendering() since that is irrelevant to uncached parameters.
        // Also note difference between field name and parameter name, due to Parameter.name() being
        // specified.

        train_isLoaded(resources, true);
        train_isBound(resources, "uncached", true);
        resources.writeParameter("uncached", "first");

        train_isLoaded(resources, true);
        train_isBound(resources, "uncached", true);
        train_readParameter(resources, "uncached", String.class, "second");

        replay();

        _access.set(component, "uncachedObject", "first");
        assertEquals(_access.get(component, "uncachedObject"), "second");

        verify();
    }

    @Test
    public void parameter_with_default() throws Exception
    {
        final BindingSource source = mockBindingSource();
        final InternalComponentResources resources = mockInternalComponentResources();
        final Binding binding = mockBinding();
        String boundValue = "howdy!";
        final Logger logger = mockLogger();

        MutableComponentModel model = mockMutableComponentModel(logger);

        model.addParameter("value", false, TapestryConstants.PROP_BINDING_PREFIX);

        Runnable phaseTwoTraining = new Runnable()
        {
            public void run()
            {
                train_isBound(resources, "value", false);

                expect(source.newBinding("default value", resources, TapestryConstants.PROP_BINDING_PREFIX,
                                         "literal:greeting")).andReturn(binding);

                resources.bindParameter("value", binding);

                train_isInvariant(resources, "value", true);

                stub_isDebugEnabled(logger, false);
            }
        };

        Component component = setupForIntegrationTest(resources, logger, DefaultParameterComponent.class.getName(),
                                                      model, source, phaseTwoTraining);

        train_isLoaded(resources, true);
        train_isBound(resources, "value", true);
        train_readParameter(resources, "value", String.class, boundValue);
        stub_isDebugEnabled(logger, false);

        replay();

        assertEquals(_access.get(component, "value"), boundValue);

        verify();
    }

    @Test
    public void default_binding_method() throws Exception
    {
        BindingSource source = mockBindingSource();
        final InternalComponentResources resources = mockInternalComponentResources();
        _binding = mockBinding();
        String boundValue = "yowza!";
        final Logger logger = mockLogger();

        MutableComponentModel model = mockMutableComponentModel(logger);

        model.addParameter("value", false, TapestryConstants.PROP_BINDING_PREFIX);

        Runnable phaseTwoTraining = new Runnable()
        {
            public void run()
            {
                train_isBound(resources, "value", false);

                // How can this happen? Only if the generated code invokes defaultValue().

                resources.bindParameter("value", _binding);

                train_isInvariant(resources, "value", true);
                stub_isDebugEnabled(logger, false);
            }
        };


        Component component = setupForIntegrationTest(resources, logger,
                                                      DefaultParameterBindingMethodComponent.class.getName(), model,
                                                      source, phaseTwoTraining);

        train_isLoaded(resources, true);
        train_isBound(resources, "value", true);
        train_readParameter(resources, "value", String.class, boundValue);
        stub_isDebugEnabled(logger, false);
        replay();

        assertEquals(_access.get(component, "value"), boundValue);

        verify();
    }

    protected final void train_isRendering(InternalComponentResources resources, boolean rendering)
    {
        expect(resources.isRendering()).andReturn(rendering);
    }

    protected final <T> void train_readParameter(InternalComponentResources resources, String parameterName,
                                                 Class<T> expectedType, T value)
    {
        expect(resources.readParameter(parameterName, expectedType.getName())).andReturn(value);
    }

    /**
     * This is for the majority of tests.
     */
    private Component setupForIntegrationTest(final InternalComponentResources resources) throws Exception
    {
        final Logger logger = mockLogger();
        MutableComponentModel model = mockMutableComponentModel(logger);

        model.addParameter("invariantObject", false, TapestryConstants.PROP_BINDING_PREFIX);
        model.addParameter("invariantPrimitive", false, TapestryConstants.PROP_BINDING_PREFIX);
        model.addParameter("object", false, TapestryConstants.PROP_BINDING_PREFIX);
        model.addParameter("primitive", true, TapestryConstants.PROP_BINDING_PREFIX);
        model.addParameter("uncached", false, TapestryConstants.LITERAL_BINDING_PREFIX);


        Runnable phaseTwoTraining = new Runnable()
        {
            public void run()
            {
                trainForPageDidLoad(resources);
                stub_isDebugEnabled(logger, false);
            }
        };


        stub_isDebugEnabled(logger, false);

        return setupForIntegrationTest(resources, logger, ParameterComponent.class.getName(), model,
                                       mockBindingSource(), phaseTwoTraining);
    }

    private Component setupForIntegrationTest(InternalComponentResources resources, Logger logger,
                                              String componentClassName, MutableComponentModel model,
                                              BindingSource source, Runnable phaseTwoTraining) throws Exception
    {
        ClassFactoryClassPool pool = new ClassFactoryClassPool(_contextClassLoader);

        Loader loader = new TestPackageAwareLoader(_contextClassLoader, pool);

        pool.appendClassPath(new LoaderClassPath(loader));

        ClassFactory cf = new ClassFactoryImpl(loader, pool, logger);

        CtClass ctClass = pool.get(componentClassName);

        replay();

        InternalClassTransformation transformation = new InternalClassTransformationImpl(cf, null, ctClass, model);

        new ParameterWorker(source).transform(transformation, model);

        verify();


        phaseTwoTraining.run();

        replay();

        transformation.finish();

        Instantiator instantiator = transformation.createInstantiator();

        Component component = instantiator.newInstance(resources);

        component.containingPageDidLoad();

        verify();

        return component;
    }

    private void trainForPageDidLoad(InternalComponentResources resources)
    {
        train_isInvariant(resources, "invariantObject", true);
        train_isInvariant(resources, "invariantPrimitive", true);
        train_isInvariant(resources, "object", false);
        train_isInvariant(resources, "primitive", false);
        train_isInvariant(resources, "uncached", false);
    }

    protected final void train_isInvariant(InternalComponentResources resources, String parameterName,
                                           boolean invariant)
    {
        expect(resources.isInvariant(parameterName)).andReturn(invariant);
    }
}
