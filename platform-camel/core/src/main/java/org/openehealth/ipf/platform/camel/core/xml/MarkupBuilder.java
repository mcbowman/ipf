/*
 * Copyright 2008 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openehealth.ipf.platform.camel.core.xml;

import java.io.StringWriter;

/**
 * @author Martin Krasser
 */
public class MarkupBuilder extends groovy.xml.MarkupBuilder {

    private final StringWriter writer;
    
    private MarkupBuilder(StringWriter writer) {
        super(writer);
        this.writer = writer;
    }
    
    public String getResult() {
        return writer.getBuffer().toString();
    }
    
    public static MarkupBuilder newInstance() {
        return new MarkupBuilder(new StringWriter());
    }
    
}
