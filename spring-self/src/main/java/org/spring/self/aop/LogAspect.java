package org.spring.self.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * 切面
 *
 * @author tanjiquan [tan_jiquan@163.com]
 * @date 2020/03/02 21:08
 * @since 1.0
 */
@Aspect
public class LogAspect {

	/**
	 * 要到类级别，不能到包级别
	 */
	@Pointcut("execution(* org.spring.self.aop.Calculate.*(..))")
	public void pointCut() {
		//该方法就是一个标识方法，为pointcut提供一个依附的地方
	}

	@Before(value = "pointCut()")
	public void methodBefore(JoinPoint joinPoint) throws Throwable {
		String methodName = joinPoint.getSignature().getName();
		System.out.printf("methodName: " + methodName + " param : " + joinPoint.getArgs().toString());
	}

}
