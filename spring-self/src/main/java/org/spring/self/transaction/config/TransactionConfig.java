package org.spring.self.transaction.config;

import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * description
 *
 * @author tanjiquan [tan_jiquan@163.com]
 * @date 2020/03/13 13:15
 * @since 1.0
 */
@EnableTransactionManagement(proxyTargetClass = true)
@EnableAspectJAutoProxy
public class TransactionConfig {

}
