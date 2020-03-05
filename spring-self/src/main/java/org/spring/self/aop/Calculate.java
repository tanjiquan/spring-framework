package org.spring.self.aop;

/**
 * 接口实现
 *
 * @author tanjiquan [tan_jiquan@163.com]
 * @date 2020/03/02 21:09
 * @since 1.0
 */
public class Calculate {

	/**
	 * 相加， 通过aop 在方法执行前输出一条日志
	 * @param i
	 * @param j
	 * @return
	 */
	public int add(int i, int j) {
		return i + j;
	}


}
