package org.spring.self.aop;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 测试启动类
 *
 * @author tanjiquan [tan_jiquan@163.com]
 * @date 2020/03/02 21:13
 * @since 1.0
 */
public class AopMain {

	public static void main(String[] args) {
		// 容器初始化
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AopConfig.class);

		/**
		 * DefaultListableBeanFactory 中会调用 AbstractBeanFactory 的getBean 方法。
		 * 1、org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean
		 * 	 2、org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton
		 * 	   3、org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#singletonObjects 中的singletonObjects 这个map中已经存在。
		 * 	       3.1、既然已经能get 在哪里 put 呢？全局搜索 singletonObjects.put  发现 在 DefaultSingletonBeanRegistry
		 * 	       3.2、通过断点  singletonObjects.put ，通过调用栈，反推会找到在哪里调用，进行源码分析。 --发现在容器的初始化过程中被缓存。
		 * 	    4、org.springframework.beans.factory.support.AbstractBeanFactory#createBean（AbstractAutowireCapableBeanFactory）
		 * 	    		在createBean 中调用 AbstractAutowireCapableBeanFactory#resolveBeforeInstantiation 方法
		 * 		 * 		 resolveBeforeInstantiation 里面其实解释调用后置处理器，将切面信息进行解析并缓存。在 doCreateBean 中进行 initializeBean 时才会有切面信息
		 * 	      5、org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean
		 * 	          5.1、doCreateBean 方法里面会选择合适的实例化策略，
		 * 	               先进行实例化一个BeanWrapper 包装对象，然后在对属性进行赋值，在进行初始化（涉及到bean的生命周期）
		 * 	        6、org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean
		 * 	        	由此可见代理对象在，生成包装对象、设置属性暴露出对象后、进行对象生命周期的初始化时创建。会通过后置处理器进行创建
		 * 	        	6.1、发现在initializeBean的后置处理器中返回了代理对象。 在哪个后置处理器进行处理呢？
		 * 	            6.2、AOP @EnableAspectJAutoProxy 为我们容器中导入了一个 AnnotationAwareAspectJAutoProxyCreator 这是一个后置处理器
		 * 	            	（EnableAspectJAutoProxy 注解上 import 了一个 AspectJAutoProxyRegistrar（这个类会往IOC容器中注入一个
		 * 						 AnnotationAwareAspectJAutoProxyCreator（自动代理创建起））））
		 * 				6.3、什么时候将切面信息解析出来，生成了切面缓存的呢？
		 * 					在 AbstractAutowireCapableBeanFactory#resolveBeforeInstantiation 方法中。resolveBeforeInstantiation 在 createBean 中调用
		 * 					resolveBeforeInstantiation 里面其实解释调用后置处理器，已经将切面信息进行缓存了
		 *
		 * 			applyBeanPostProcessorsAfterInitialization
		 * 		applyBeanPostProcessorsBeforeInitialization
		 *
		 */
		Calculate calculate = context.getBean(Calculate.class);

		/**
		 * 返回的 calculate 是一个代理对象，把切面信息都保存下来了
		 * 放到代理对象里面去就是为了方便织入执行
		 *
		 * 如何调用到目标方法？
		 *
		 */
		calculate.add(1, 1);

		/*try {
			前置通知

			返回通知
		} catch () {
			异常通知
		} finally {
			后置通知
		}*/
	}



}
