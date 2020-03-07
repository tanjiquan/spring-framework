/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.lang.Nullable;

/**
 * Spring's implementation of the AOP Alliance
 * {@link org.aopalliance.intercept.MethodInvocation} interface,
 * implementing the extended
 * {@link org.springframework.aop.ProxyMethodInvocation} interface.
 *
 * <p>Invokes the target object using reflection. Subclasses can override the
 * {@link #invokeJoinpoint()} method to change this behavior, so this is also
 * a useful base class for more specialized MethodInvocation implementations.
 *
 * <p>It is possible to clone an invocation, to invoke {@link #proceed()}
 * repeatedly (once per clone), using the {@link #invocableClone()} method.
 * It is also possible to attach custom attributes to the invocation,
 * using the {@link #setUserAttribute} / {@link #getUserAttribute} methods.
 *
 * <p><b>NOTE:</b> This class is considered internal and should not be
 * directly accessed. The sole reason for it being public is compatibility
 * with existing framework integrations (e.g. Pitchfork). For any other
 * purposes, use the {@link ProxyMethodInvocation} interface instead.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @see #invokeJoinpoint
 * @see #proceed
 * @see #invocableClone
 * @see #setUserAttribute
 * @see #getUserAttribute
 */
public class ReflectiveMethodInvocation implements ProxyMethodInvocation, Cloneable {

	protected final Object proxy;

	@Nullable
	protected final Object target;

	protected final Method method;

	protected Object[] arguments;

	@Nullable
	private final Class<?> targetClass;

	/**
	 * Lazily initialized map of user-specific attributes for this invocation.
	 */
	@Nullable
	private Map<String, Object> userAttributes;

	/**
	 * List of MethodInterceptor and InterceptorAndDynamicMethodMatcher
	 * that need dynamic checks.
	 */
	protected final List<?> interceptorsAndDynamicMethodMatchers;

	/**
	 * Index from 0 of the current interceptor we're invoking.
	 * -1 until we invoke: then the current interceptor.
	 */
	private int currentInterceptorIndex = -1;


	/**
	 * Construct a new ReflectiveMethodInvocation with the given arguments.
	 * @param proxy the proxy object that the invocation was made on
	 * @param target the target object to invoke
	 * @param method the method to invoke
	 * @param arguments the arguments to invoke the method with
	 * @param targetClass the target class, for MethodMatcher invocations
	 * @param interceptorsAndDynamicMethodMatchers interceptors that should be applied,
	 * along with any InterceptorAndDynamicMethodMatchers that need evaluation at runtime.
	 * MethodMatchers included in this struct must already have been found to have matched
	 * as far as was possibly statically. Passing an array might be about 10% faster,
	 * but would complicate the code. And it would work only for static pointcuts.
	 */
	protected ReflectiveMethodInvocation(
			Object proxy, @Nullable Object target, Method method, @Nullable Object[] arguments,
			@Nullable Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers) {

		this.proxy = proxy;
		this.target = target;
		this.targetClass = targetClass;
		this.method = BridgeMethodResolver.findBridgedMethod(method);
		this.arguments = AopProxyUtils.adaptArgumentsIfNecessary(method, arguments);
		this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
	}


	@Override
	public final Object getProxy() {
		return this.proxy;
	}

	@Override
	@Nullable
	public final Object getThis() {
		return this.target;
	}

	@Override
	public final AccessibleObject getStaticPart() {
		return this.method;
	}

	/**
	 * Return the method invoked on the proxied interface.
	 * May or may not correspond with a method invoked on an underlying
	 * implementation of that interface.
	 */
	@Override
	public final Method getMethod() {
		return this.method;
	}

	@Override
	public final Object[] getArguments() {
		return this.arguments;
	}

	@Override
	public void setArguments(Object... arguments) {
		this.arguments = arguments;
	}


	@Override
	@Nullable
	// 这个方法整合核心就是 责任链模式  + 递归
	// 通过5节点方法 链条中，通过责任链 + 递归模式，驱动执行。第0个驱动第一个，第一个驱动第二个...
	//    执行到最后一个节点的时候进行判断(4 == 5-1)，这个时候需要执行目标方法了，目标方法执行完了再往上一层返回，执行相关代码。
	//  这就是一个织入的操作

	//可以把这种精髓搬到业务代码中  依赖查找、递归、责任链模式、模板模式
	// 业务校验： 有很多个 if  else  不符合开闭原则
	// 改善： 1、定义校验规则接口、抽象校验类实现校验规则接口
	//       2、校验参数合法性、校验业务互斥....  把这些校验规则区分出一个一个校验规则类，并继承抽象校验类，然后交给抽象方法处理
	//       3、在引入一个驱动规则类，驱动一个一个校验规则执行
	//       4、使用，有可能A业务需要2个校验规则类、B业务需要3个校验规则类，通过一个注解依赖查找A 业务用了哪些规则。这样很灵活增删规则
	// 这就是  模式

	// 假如有： 前置、后置、返回、异常  都有这四个通知
	// 进链顺序：异常、返回、后置、前置(前置通知这里已经知道他的下一个链没有了，所以会调用目标方法，所以执行完后会进入到invokeJoinpoint()方法)、目标方法
	// 出链顺序：目标方法、前置、后置（后置通知对mi.proceed()进行了try finally 所以一定会执行。）、
	//   返回(返回通知不会执行，因为后置通知出来后抛出了异常，但是AfterReturningAdviceInterceptor.process()中并没有try catch)、
	//   异常(异常通知在执行方法是出现异常会执行，进行了 try  catch)
	// 前置通知 执行完就会调目标方法
	// 目标方法返回后就会掉后置通知、目标方法执行异常就会掉异常通知
	//
	public Object proceed() throws Throwable {
		// We start with an index of -1 and increment early.
		// 从-1 开始，结束条件执行目标方法是下标 =  拦截器的长度 - 1 （执行到了最后一个拦截器的时候）
		// 比如有5个通知， 第一次进来  -1  != 5 - 1   不进入，currentInterceptorIndex 递归调用时下面会被累加
		// 第二个通知（AspectJAfterThrowingAdvice）进来，还是不为空  0  !=  5 - 1
		// 第三个通知（AfterReturningAdviceInterceptor）进来，还是不为空  1 != 5 - 1
		// 第四个通知（AspectJAfterAdvice）进来，还是不为空  2 != 5 - 1
		// 第五个通知 (MethodBeforeAdviceInterceptor) 进来，不为空，3 != 5 - 1
		// 第五个通知(MethodBeforeAdviceInterceptor) 执行before 回来后，此时  4 == 5 - 1 进入此判断
		if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
			// 执行到最后一个通知方法的时候调用目标方法。(第五个拦截链执行
			// ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this); 以后会回来)
			// 这里是调用目标方法
			return invokeJoinpoint();
		}

		// 获取第一个通知（ExposeInvocationInterceptor, 默认的一个），使用的是前++ ，第一次进来  -1 + 1 = 0，即就是第一个链条（通知）
		// 获取第二个通知(AspectJAfterThrowingAdvice)  0 + 1 = 1 即第二个通知
		// 获取第三个通知(AfterReturningAdviceInterceptor)  1 + 1 = 2 即第三个通知
		// 获取第四个通知(AspectJAfterAdvice)   2 + 1 = 3 即第四个通知
		// 获取第五个通知(MethodBeforeAdviceInterceptor)  3 + 1 = 4 即第五个通知
		Object interceptorOrInterceptionAdvice =
				this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
		if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
			// Evaluate dynamic method matcher here: static part will already have
			// been evaluated and found to match.
			InterceptorAndDynamicMethodMatcher dm =
					(InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
			Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
			if (dm.methodMatcher.matches(this.method, targetClass, this.arguments)) {
				return dm.interceptor.invoke(this);
			}
			else {
				// Dynamic matching failed.
				// Skip this interceptor and invoke the next in the chain.
				return proceed();
			}
		}
		else {
			// It's an interceptor, so we just invoke it: The pointcut will have
			// been evaluated statically before this object was constructed.
			// 在这个地方注意，
			// 第一个通知 第一个拦截器的方法的 invoke 方法，  传入的是this，当前的方法拦截器对象
			//           this  当前对象， 第一个为 ExposeInvocationInterceptor
			// 第二个通知是 AspectJAfterThrowingAdvice，第二次传入的还是 ExposeInvocationInterceptor
			// 第三个通知是 AfterReturningAdviceInterceptor  先执行异常通知，在执行返回通知，
			// 第四个通知是 AspectJAfterAdvice
			// 第五个通知是 MethodBeforeAdviceInterceptor
			return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
		}
	}

	/**
	 * Invoke the joinpoint using reflection.
	 * Subclasses can override this to use custom invocation.
	 * @return the return value of the joinpoint
	 * @throws Throwable if invoking the joinpoint resulted in an exception
	 */
	@Nullable
	protected Object invokeJoinpoint() throws Throwable {
		return AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments);
	}


	/**
	 * This implementation returns a shallow copy of this invocation object,
	 * including an independent copy of the original arguments array.
	 * <p>We want a shallow copy in this case: We want to use the same interceptor
	 * chain and other object references, but we want an independent value for the
	 * current interceptor index.
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MethodInvocation invocableClone() {
		Object[] cloneArguments = this.arguments;
		if (this.arguments.length > 0) {
			// Build an independent copy of the arguments array.
			cloneArguments = this.arguments.clone();
		}
		return invocableClone(cloneArguments);
	}

	/**
	 * This implementation returns a shallow copy of this invocation object,
	 * using the given arguments array for the clone.
	 * <p>We want a shallow copy in this case: We want to use the same interceptor
	 * chain and other object references, but we want an independent value for the
	 * current interceptor index.
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MethodInvocation invocableClone(Object... arguments) {
		// Force initialization of the user attributes Map,
		// for having a shared Map reference in the clone.
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<>();
		}

		// Create the MethodInvocation clone.
		try {
			ReflectiveMethodInvocation clone = (ReflectiveMethodInvocation) clone();
			clone.arguments = arguments;
			return clone;
		}
		catch (CloneNotSupportedException ex) {
			throw new IllegalStateException(
					"Should be able to clone object of type [" + getClass() + "]: " + ex);
		}
	}


	@Override
	public void setUserAttribute(String key, @Nullable Object value) {
		if (value != null) {
			if (this.userAttributes == null) {
				this.userAttributes = new HashMap<>();
			}
			this.userAttributes.put(key, value);
		}
		else {
			if (this.userAttributes != null) {
				this.userAttributes.remove(key);
			}
		}
	}

	@Override
	@Nullable
	public Object getUserAttribute(String key) {
		return (this.userAttributes != null ? this.userAttributes.get(key) : null);
	}

	/**
	 * Return user attributes associated with this invocation.
	 * This method provides an invocation-bound alternative to a ThreadLocal.
	 * <p>This map is initialized lazily and is not used in the AOP framework itself.
	 * @return any user attributes associated with this invocation
	 * (never {@code null})
	 */
	public Map<String, Object> getUserAttributes() {
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<>();
		}
		return this.userAttributes;
	}


	@Override
	public String toString() {
		// Don't do toString on target, it may be proxied.
		StringBuilder sb = new StringBuilder("ReflectiveMethodInvocation: ");
		sb.append(this.method).append("; ");
		if (this.target == null) {
			sb.append("target is null");
		}
		else {
			sb.append("target is of class [").append(this.target.getClass().getName()).append(']');
		}
		return sb.toString();
	}

}
