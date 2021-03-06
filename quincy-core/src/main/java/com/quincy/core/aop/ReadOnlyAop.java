package com.quincy.core.aop;

import java.sql.Connection;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.quincy.core.db.DataSourceHolder;

@Aspect
@Order(11)
@Component
public class ReadOnlyAop {
	@Resource(name="dataSourceSlave")
	private DataSource dataSource;

	@Pointcut("@annotation(com.quincy.core.annotation.ReadOnly)")
    public void pointCut() {}

	@Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		Transactional transactionalAnnotation = AopHelper.getAnnotation(joinPoint, Transactional.class);
		if(transactionalAnnotation==null) {
			ConnectionHolder conHolder = null;
			Connection conn = null;
			boolean stackRoot = false;
			try {
				conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
				if(conHolder==null||conHolder.getConnectionHandle()==null) {
					DataSourceHolder.setSlave();
					conn = this.dataSource.getConnection();
					conHolder = new ConnectionHolder(conn);
					TransactionSynchronizationManager.bindResource(this.dataSource, conHolder);
					stackRoot = true;
				}
				return joinPoint.proceed();
			} finally {
				if(stackRoot) {
					if(conn!=null)
						conn.close();
					if(conHolder!=null)
						conHolder.clear();
					TransactionSynchronizationManager.unbindResource(dataSource);
				}
			}
		} else {
			return joinPoint.proceed();
		}
	}
}
