package org.spring.self.aop;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * description
 *
 * @author tanjiquan [tan_jiquan@163.com]
 * @date 2020/03/02 21:01
 * @since 1.0
 */
@Configuration
@EnableAspectJAutoProxy(exposeProxy = true)
public class AopConfig {

	@Bean("calculate")
	public Calculate calculate() {
		return new Calculate();
	}

	@Bean("logAspect")
	public LogAspect logAspect() {
		return new LogAspect();
	}

}
