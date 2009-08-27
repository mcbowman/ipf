/*
 * Copyright 2008-2009 the original author or authors.
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
package org.openehealth.ipf.platform.camel.core.extend

import static org.openehealth.ipf.platform.camel.core.util.Expressions.exceptionMessageExpression
import static org.openehealth.ipf.platform.camel.core.util.Expressions.exceptionObjectExpression
import static org.openehealth.ipf.platform.camel.core.util.Expressions.headersExpression
import java.lang.IllegalArgumentException

import static org.apache.camel.builder.DataFormatClause.Operation.Marshal;
import static org.apache.camel.builder.DataFormatClause.Operation.Unmarshal;

import org.openehealth.ipf.commons.core.modules.api.Aggregator
import org.openehealth.ipf.commons.core.modules.api.Parser
import org.openehealth.ipf.commons.core.modules.api.Predicate
import org.openehealth.ipf.commons.core.modules.api.Renderer;
import org.openehealth.ipf.commons.core.modules.api.Transmogrifier
import org.openehealth.ipf.commons.core.modules.api.Validator
import org.openehealth.ipf.platform.camel.core.adapter.Adapter
import org.openehealth.ipf.platform.camel.core.adapter.AggregatorAdapter
import org.openehealth.ipf.platform.camel.core.adapter.DataFormatAdapter
import org.openehealth.ipf.platform.camel.core.adapter.PredicateAdapter
import org.openehealth.ipf.platform.camel.core.builder.RouteBuilder
import org.openehealth.ipf.platform.camel.core.closures.DelegatingAggregator
import org.openehealth.ipf.platform.camel.core.closures.DelegatingAggregationStrategy
import org.openehealth.ipf.platform.camel.core.closures.DelegatingCamelPredicate
import org.openehealth.ipf.platform.camel.core.closures.DelegatingExpression
import org.openehealth.ipf.platform.camel.core.closures.DelegatingInterceptor
import org.openehealth.ipf.platform.camel.core.closures.DelegatingPredicate
import org.openehealth.ipf.platform.camel.core.closures.DelegatingProcessor
import org.openehealth.ipf.platform.camel.core.closures.DelegatingTransmogrifier 
import org.openehealth.ipf.platform.camel.core.closures.DelegatingValidator 
import org.openehealth.ipf.platform.camel.core.dataformat.GnodeDataFormat
import org.openehealth.ipf.platform.camel.core.dataformat.GpathDataFormat
import org.openehealth.ipf.platform.camel.core.model.AuditDefinition
import org.openehealth.ipf.platform.camel.core.model.InterceptDefinition
import org.openehealth.ipf.platform.camel.core.model.IpfDefinition
import org.openehealth.ipf.platform.camel.core.model.SplitterDefinition
import org.openehealth.ipf.platform.camel.core.model.ParserAdapterDefinition
import org.openehealth.ipf.platform.camel.core.model.RendererAdapterDefinition
import org.openehealth.ipf.platform.camel.core.model.TransmogrifierAdapterDefinition
import org.openehealth.ipf.platform.camel.core.model.ValidatorAdapterDefinition
import org.openehealth.ipf.platform.camel.core.model.DataFormatAdapterDefinition
import org.openehealth.ipf.platform.camel.core.model.ValidationDefinition

import org.apache.camel.Expression
import org.apache.camel.Processor
import org.apache.camel.builder.DataFormatClause
import org.apache.camel.builder.ExpressionClause
import org.apache.camel.builder.NoErrorHandlerBuilder
import org.apache.camel.model.ChoiceDefinition
import org.apache.camel.model.OnExceptionDefinition
import org.apache.camel.model.ProcessorDefinition
import org.apache.camel.model.DataFormatDefinition
import org.apache.camel.processor.DelegateProcessor
import org.apache.camel.processor.aggregate.AggregationStrategy
import org.apache.camel.spi.DataFormat

/**
 * @author Martin Krasser
 */
class CoreModelExtension {
    
    RouteBuilder routeBuilder // for config backwards-compatibility only (not used internally)
    
    static extensions = { 
        
        // ----------------------------------------------------------------
        //  Core Extensions
        // ----------------------------------------------------------------

        ProcessorDefinition.metaClass.process = { String processorBeanName ->
            delegate.processRef(processorBeanName)
        }

        ProcessorDefinition.metaClass.process = { Closure processorLogic ->
            delegate.process(new DelegatingProcessor(processorLogic))
        }
            
        ProcessorDefinition.metaClass.intercept = {DelegateProcessor delegateProcessor ->
	        InterceptDefinition answer = new InterceptDefinition(delegateProcessor)
	        delegate.addOutput(answer)
	        return answer
        }

        ProcessorDefinition.metaClass.intercept = { Closure interceptorLogic ->
            delegate.intercept(new DelegatingInterceptor(interceptorLogic))
        }
        
        ProcessorDefinition.metaClass.unhandled = { ProcessorDefinition processorDefinition ->
            delegate.errorHandler(new NoErrorHandlerBuilder())
        }
        
        ProcessorDefinition.metaClass.filter = { Closure predicateLogic ->
            delegate.filter(new DelegatingCamelPredicate(predicateLogic))
        }
        
        ProcessorDefinition.metaClass.transform = { Closure transformExpression ->
            delegate.transform(new DelegatingExpression(transformExpression))
        }
    
        ProcessorDefinition.metaClass.setProperty = { String name, Closure propertyExpression ->
            delegate.setProperty(name, new DelegatingExpression(propertyExpression))
        }

        ProcessorDefinition.metaClass.setHeader = { String name, Closure headerExpression ->
            delegate.setHeader(name, new DelegatingExpression(headerExpression))
        }

        ProcessorDefinition.metaClass.setOutHeader = { String name, Closure headerExpression ->
            delegate.setOutHeader(name, new DelegatingExpression(headerExpression))
        }

        ProcessorDefinition.metaClass.setFaultHeader = { String name, Closure headerExpression ->
            delegate.setFaultHeader(name, new DelegatingExpression(headerExpression))
        }

        ProcessorDefinition.metaClass.setBody = {Closure bodyExpression ->
            delegate.setBody(new DelegatingExpression(bodyExpression))
        }
        
        ChoiceDefinition.metaClass.when = { Closure predicateLogic ->
            delegate.when(new DelegatingCamelPredicate(predicateLogic))
        }
    
        // ----------------------------------------------------------------
        //  Platform Processor Extensions
        // ----------------------------------------------------------------
        
        ProcessorDefinition.metaClass.validation = { Processor validator ->
            ValidationDefinition answer = new ValidationDefinition(validator)
            delegate.addOutput(answer)
            return answer
        }

        ProcessorDefinition.metaClass.validation = { String validationUri ->
            ValidationDefinition answer = new ValidationDefinition(validationUri)
            delegate.addOutput(answer)
            return answer
        }
        
        ProcessorDefinition.metaClass.validation = { Closure validatorLogic ->
            delegate.validation(new DelegatingProcessor(validatorLogic))
        }
            
        ProcessorDefinition.metaClass.enrich = { String resourceUri, Closure aggregationLogic ->
            delegate.enrich(resourceUri, new DelegatingAggregationStrategy(aggregationLogic))
        }
    
        ProcessorDefinition.metaClass.ipf = { ->
            new IpfDefinition(delegate)
	    }
                
        ProcessorDefinition.metaClass.audit = {-> 
            AuditDefinition answer = new AuditDefinition()        
            delegate.addOutput(answer)
            return answer
        }
    
         // ----------------------------------------------------------------
         //  Platform DataFormatClause extensions
         // ----------------------------------------------------------------
         
         DataFormatClause.metaClass.gnode = { String schemaResource, boolean namespaceAware ->
             delegate.dataFormat(new GnodeDataFormat(schemaResource, namespaceAware))
         }
        
         DataFormatClause.metaClass.gnode = { boolean namespaceAware ->
             delegate.dataFormat(new GnodeDataFormat(namespaceAware))
         }
     
         DataFormatClause.metaClass.gnode = { ->
             delegate.gnode(true)
         }

         DataFormatClause.metaClass.gpath = { String schemaResource, boolean namespaceAware ->
             delegate.dataFormat(new GpathDataFormat(schemaResource, namespaceAware))
         }       
        
         DataFormatClause.metaClass.gpath = { boolean namespaceAware ->
             delegate.dataFormat(new GpathDataFormat(namespaceAware))
         }

         DataFormatClause.metaClass.gpath = { ->
             delegate.gpath(true)
         }
     
         // ----------------------------------------------------------------
         //  Platform ExpressionClause extensions
         // ----------------------------------------------------------------
         
         ExpressionClause.metaClass.exceptionObject = { ->
             delegate.expression(exceptionObjectExpression())
         }
  
         ExpressionClause.metaClass.exceptionMessage = { ->
             delegate.expression(exceptionMessageExpression())
         }
  
        // ----------------------------------------------------------------
        //  Platform ExceptionDefinition extensions
        // ----------------------------------------------------------------
        
        OnExceptionDefinition.metaClass.onWhen = { Closure predicate ->
            delegate.onWhen(new DelegatingCamelPredicate(predicate))
        }

        // ----------------------------------------------------------------
        //  Adapter Extensions for RouteBuilder
        // ----------------------------------------------------------------

        org.apache.camel.spring.SpringRouteBuilder.metaClass.aggregationStrategy = { Aggregator aggregator ->
            new AggregatorAdapter(aggregator)
        }

        org.apache.camel.spring.SpringRouteBuilder.metaClass.aggregationStrategy = { String aggregatorBeanName ->
            delegate.aggregationStrategy(delegate.lookup(aggregatorBeanName, Aggregator.class))
        }

        org.apache.camel.spring.SpringRouteBuilder.metaClass.aggregationStrategy = { Closure aggregationLogic ->
            delegate.aggregationStrategy(new DelegatingAggregator(aggregationLogic))
        }
        
        org.apache.camel.spring.SpringRouteBuilder.metaClass.predicate = { Predicate predicate ->
            new PredicateAdapter(predicate)
        }

        org.apache.camel.spring.SpringRouteBuilder.metaClass.predicate = { String predicateBeanName ->
            delegate.predicate(delegate.lookup(predicateBeanName, Predicate.class))
        }

        org.apache.camel.spring.SpringRouteBuilder.metaClass.predicate = { Closure predicateLogic ->
            delegate.predicate(new DelegatingPredicate(predicateLogic))
        }
    
        // ----------------------------------------------------------------
        //  Adapter Extensions for ProcessorDefinition
        // ----------------------------------------------------------------
        
        ProcessorDefinition.metaClass.transmogrify = { Transmogrifier transmogrifier ->
            TransmogrifierAdapterDefinition answer = new TransmogrifierAdapterDefinition(transmogrifier)
            delegate.addOutput(answer)
            return answer
        }

        ProcessorDefinition.metaClass.transmogrify = { String transmogrifierBeanName ->
            TransmogrifierAdapterDefinition answer = new TransmogrifierAdapterDefinition(transmogrifierBeanName)
            delegate.addOutput(answer)
            return answer
        }

        ProcessorDefinition.metaClass.transmogrify = { Closure transmogrifierLogic ->
            delegate.transmogrify(new DelegatingTransmogrifier(transmogrifierLogic))
        }
        
        ProcessorDefinition.metaClass.transmogrify = { ->
            TransmogrifierAdapterDefinition answer = new TransmogrifierAdapterDefinition(null)
            delegate.addOutput(answer)
            return answer
        }        

        ProcessorDefinition.metaClass.validate = {->
            ValidatorAdapterDefinition answer = new ValidatorAdapterDefinition()
            delegate.addOutput(answer)
            return answer
        }
    
        ProcessorDefinition.metaClass.validate = { Validator validator ->
            ValidatorAdapterDefinition answer = new ValidatorAdapterDefinition(validator)
            delegate.addOutput(answer)
            return answer
        }
        
        ProcessorDefinition.metaClass.validate = { String validatorBeanName ->
            ValidatorAdapterDefinition answer = new ValidatorAdapterDefinition(validatorBeanName)
            delegate.addOutput(answer)
            return answer
        }
    
        ProcessorDefinition.metaClass.validate = { Closure validatorLogic ->
            delegate.validate(new DelegatingValidator(validatorLogic))
        }
    
        ProcessorDefinition.metaClass.parse = { Parser parser ->
            ParserAdapterDefinition answer = new ParserAdapterDefinition(parser)
            delegate.addOutput(answer)
            return answer
        }
        
        ProcessorDefinition.metaClass.parse = { String parserBeanName ->
            ParserAdapterDefinition answer = new ParserAdapterDefinition(parserBeanName)
            delegate.addOutput(answer)
            return answer
        }
    
        ProcessorDefinition.metaClass.render = { Renderer renderer ->
            RendererAdapterDefinition answer = new RendererAdapterDefinition(renderer)
            delegate.addOutput(answer)
            return answer
        }
        
        ProcessorDefinition.metaClass.render = { String rendererBeanName ->
            RendererAdapterDefinition answer = new RendererAdapterDefinition(rendererBeanName)
            delegate.addOutput(answer)
            return answer
        }
    
        // ----------------------------------------------------------------
        //  Adapter Extensions for DataFormatClause
        // ----------------------------------------------------------------

        DataFormatClause.metaClass.parse = { Parser parser ->
            delegate.processorType.unmarshal(new DataFormatAdapter((Parser)parser))
        }
        
        DataFormatClause.metaClass.render = { Renderer renderer ->
            delegate.processorType.marshal(new DataFormatAdapter((Renderer)renderer))
        }
    
        DataFormatClause.metaClass.parse = { String parserBeanName ->
            delegate.processorType.unmarshal((DataFormatDefinition)DataFormatAdapterDefinition.forParserBean(parserBeanName))
        }
    
        DataFormatClause.metaClass.render = { String rendererBeanName ->
            delegate.processorType.marshal((DataFormatDefinition)DataFormatAdapterDefinition.forRendererBean(rendererBeanName))
        }

        // ----------------------------------------------------------------
        //  Not part of DSL
        // ----------------------------------------------------------------

        DataFormatClause.metaClass.dataFormat = { DataFormat dataFormat ->
            if (operation == Marshal) {
                return delegate.processorType.marshal(dataFormat)
            } else if (operation == Unmarshal) {
                return delegate.processorType.unmarshal(dataFormat)
            } else {
                throw new IllegalArgumentException("Unknown data format operation: " + operation)
            }
        }
        
    }

}
