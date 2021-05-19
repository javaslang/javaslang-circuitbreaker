/*
 *
 * Copyright 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.feign;

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import feign.Target.HardCodedTarget;
import io.github.resilience4j.feign.test.TestFeignDecorator;
import io.github.resilience4j.feign.test.TestService;
import io.github.resilience4j.feign.test.TestServiceImpl;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DecoratorInvocationHandlerTest {

    private DecoratorInvocationHandler testSubject;
    private TestService testService;
    private Method greetingMethod;
    private TestFeignDecorator feignDecorator;
    private MethodHandler methodHandler;
    private Map<Method, MethodHandler> dispatch;
    private Target<TestService> target;

    @Before
    public void setUp() throws Throwable {
        target = new HardCodedTarget<TestService>(TestService.class,
            TestService.class.getSimpleName());
        testService = new TestServiceImpl();
        greetingMethod = testService.getClass().getDeclaredMethod("greeting");
        feignDecorator = new TestFeignDecorator();

        methodHandler = mock(MethodHandler.class);
        when(methodHandler.invoke(any())).thenReturn(testService.greeting());

        dispatch = new HashMap<>();
        dispatch.put(greetingMethod, methodHandler);

        testSubject = new DecoratorInvocationHandler(target, dispatch, feignDecorator);
    }

    @Test
    public void testInvoke() throws Throwable {
        final Object result = testSubject.invoke(testService, greetingMethod, new Object[0]);

        verify(methodHandler, times(1)).invoke(any());
        assertThat(feignDecorator.isCalled())
            .describedAs("FeignDecorator is called")
            .isTrue();
        assertThat(result)
            .describedAs("Return of invocation")
            .isEqualTo(testService.greeting());
    }

    @Test
    public void testDecorator() throws Throwable {
        feignDecorator.setAlternativeFunction(fnArgs -> "AlternativeFunction");
        testSubject = new DecoratorInvocationHandler(target, dispatch, feignDecorator);

        final Object result = testSubject.invoke(testService, greetingMethod, new Object[0]);

        verify(methodHandler, times(0)).invoke(any());
        assertThat(feignDecorator.isCalled())
            .describedAs("FeignDecorator is called")
            .isTrue();
        assertThat(result)
            .describedAs("Return of invocation")
            .isEqualTo("AlternativeFunction");
    }

    @Test
    public void testInvokeToString() throws Throwable {
        final Method toStringMethod = testService.getClass().getMethod("toString");

        final Object result = testSubject.invoke(testService, toStringMethod, new Object[0]);

        verify(methodHandler, times(0)).invoke(any());
        assertThat(feignDecorator.isCalled())
            .describedAs("FeignDecorator is called")
            .isTrue();
        assertThat(result)
            .describedAs("Return of invocation")
            .isEqualTo(target.toString());
    }

    @Test
    public void testInvokeEquals() throws Throwable {
        final Method equalsMethod = testService.getClass().getMethod("equals", Object.class);

        final Boolean result = (Boolean) testSubject
            .invoke(testService, equalsMethod, new Object[]{testSubject});

        verify(methodHandler, times(0)).invoke(any());
        assertThat(feignDecorator.isCalled())
            .describedAs("FeignDecorator is called")
            .isTrue();
        assertThat(result)
            .describedAs("Return of invocation")
            .isTrue();
    }


    @Test
    public void testInvokeHashcode() throws Throwable {
        final Method hashCodeMethod = testService.getClass().getMethod("hashCode");

        final Integer result = (Integer) testSubject
            .invoke(testService, hashCodeMethod, new Object[0]);

        verify(methodHandler, times(0)).invoke(any());
        assertThat(feignDecorator.isCalled())
            .describedAs("FeignDecorator is called")
            .isTrue();
        assertThat(result)
            .describedAs("Return of invocation")
            .isEqualTo(target.hashCode());
    }
}
