// Copyright 2007, 2009, 2011 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry5.javadoc;

import java.util.Map;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Events;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.ioc.internal.util.CollectionFactory;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.Tag;

public class ClassDescription
{
    public final ClassDoc classDoc;

    public final Map<String, ParameterDescription> parameters = CollectionFactory.newCaseInsensitiveMap();

    /**
     * Case insensitive map, keyed on event name, value is optional description (often blank).
     */
    public final Map<String, String> events = CollectionFactory.newCaseInsensitiveMap();

    public ClassDescription()
    {
        this.classDoc = null;
    }

    public ClassDescription(ClassDoc classDoc, ClassDescriptionSource source)
    {
        this.classDoc = classDoc;

        loadEvents();
        loadParameters(source);

        ClassDoc parentDoc = classDoc.superclass();

        if (parentDoc != null)
        {
            ClassDescription parentDescription = source.getDescription(classDoc.superclass().qualifiedName());

            mergeInto(events, parentDescription.events);
            mergeInto(parameters, parentDescription.parameters);
        }
    }

    private void loadEvents()
    {
        AnnotationDesc eventsAnnotation = getAnnotation(classDoc, Events.class);

        if (eventsAnnotation == null)
            return;

        // Events has only a single attribute: value(), so we know its the first element
        // in the array.

        ElementValuePair pair = eventsAnnotation.elementValues()[0];

        AnnotationValue annotationValue = pair.value();
        AnnotationValue[] values = (AnnotationValue[]) annotationValue.value();

        for (AnnotationValue eventValue : values)
        {
            String event = (String) eventValue.value();
            int ws = event.indexOf(' ');

            String name = ws < 0 ? event : event.substring(0, ws);
            String description = ws < 0 ? "" : event.substring(ws + 1).trim();

            events.put(name, description);
        }
    }

    private static <K, V> void mergeInto(Map<K, V> target, Map<K, V> source)
    {
        for (K key : source.keySet())
        {
            if (!target.containsKey(key))
            {
                V value = source.get(key);
                target.put(key, value);
            }
        }
    }

    private void loadParameters(ClassDescriptionSource source)
    {
        for (FieldDoc fd : classDoc.fields(false))
        {
            if (fd.isStatic())
                continue;

            if (!fd.isPrivate())
                continue;

            Map<String, String> values = getAnnotationValues(fd, Parameter.class);

            if (values != null)
            {
                String name = values.get("name");

                if (name == null)
                    name = fd.name().replaceAll("^[$_]*", "");

                ParameterDescription pd = new ParameterDescription(fd, name, fd.type().qualifiedTypeName(), get(values,
                        "value", ""), get(values, "defaultPrefix", BindingConstants.PROP), getBoolean(values,
                        "required", false), getBoolean(values, "allowNull", true), getBoolean(values, "cache", true),
                        getSinceTagValue(fd), isDeprecated(fd));

                parameters.put(name, pd);

                continue;
            }

            values = getAnnotationValues(fd, Component.class);

            if (values != null)
            {
                String names = get(values, "publishParameters", "");

                if (InternalUtils.isBlank(names))
                    continue;

                for (String name : names.split("\\s*,\\s*"))
                {
                    ParameterDescription pd = getPublishedParameterDescription(source, fd, name);
                    parameters.put(name, pd);
                }

            }

        }
    }

    private ParameterDescription getPublishedParameterDescription(ClassDescriptionSource source, FieldDoc fd,
            String name)
    {
        String currentClassName = fd.type().qualifiedTypeName();

        while (true)
        {
            ClassDescription componentCD = source.getDescription(currentClassName);

            if (componentCD.classDoc == null)
                throw new IllegalArgumentException(String.format("Published parameter '%s' from %s not found.", name,
                        fd.qualifiedName()));

            if (componentCD.parameters.containsKey(name)) { return componentCD.parameters.get(name); }

            currentClassName = componentCD.classDoc.superclass().typeName();
        }
    }

    private static boolean isDeprecated(ProgramElementDoc doc)
    {
        return (getAnnotation(doc, Deprecated.class) != null) || (doc.tags("deprecated").length != 0);
    }

    private static String getSinceTagValue(Doc doc)
    {
        return getTagValue(doc, "since");
    }

    private static String getTagValue(Doc doc, String tagName)
    {
        Tag[] tags = doc.tags(tagName);

        return 0 < tags.length ? tags[0].text() : "";
    }

    private static boolean getBoolean(Map<String, String> map, String key, boolean defaultValue)
    {
        if (map.containsKey(key))
            return Boolean.parseBoolean(map.get(key));

        return defaultValue;
    }

    private static String get(Map<String, String> map, String key, String defaultValue)
    {
        if (map.containsKey(key))
            return map.get(key);

        return defaultValue;
    }

    private static AnnotationDesc getAnnotation(ProgramElementDoc source, Class annotationType)
    {
        String name = annotationType.getName();

        for (AnnotationDesc ad : source.annotations())
        {
            if (ad.annotationType().qualifiedTypeName().equals(name)) { return ad; }
        }

        return null;
    }

    private static Map<String, String> getAnnotationValues(ProgramElementDoc source, Class annotationType)
    {
        AnnotationDesc annotation = getAnnotation(source, annotationType);

        if (annotation == null)
            return null;

        Map<String, String> result = CollectionFactory.newMap();

        for (ElementValuePair pair : annotation.elementValues())
        {
            result.put(pair.element().name(), pair.value().value().toString());
        }

        return result;
    }
}
