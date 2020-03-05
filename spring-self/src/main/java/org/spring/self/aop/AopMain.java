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
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AopConfig.class);

		Calculate calculate = context.getBean(Calculate.class);

		calculate.add(1, 1);
	}

}
