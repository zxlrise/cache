package com.github.houbb.cache.core.support.proxy.cglib;

import com.github.houbb.cache.api.ICache;
import com.github.houbb.cache.core.support.proxy.ICacheProxy;
import com.github.houbb.cache.core.support.proxy.bs.CacheProxyBs;
import com.github.houbb.cache.core.support.proxy.bs.CacheProxyBsContext;
import com.github.houbb.cache.core.support.proxy.bs.ICacheProxyBsContext;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * CGLIB 代理类
 * @author binbin.hou
 * date 2019/3/7
 * @since 0.0.4
 */
public class CglibProxy implements MethodInterceptor, ICacheProxy {

    /**
     * 被代理的对象
     */
    private final ICache target;

    public CglibProxy(ICache target) {
        this.target = target;
    }

    /**
     *
     * @param o   表示要进行增强的对象
     * @param method 表示要被拦截的方法
     * @param params 表示要被拦截方法的参数
     * @param methodProxy 表示要触发父类的方法对象
     * @return
     * @throws Throwable
     */
    @Override
    public Object intercept(Object o, Method method, Object[] params, MethodProxy methodProxy) throws Throwable {
        ICacheProxyBsContext context = CacheProxyBsContext.newInstance()
                .method(method).params(params).target(target);

        return CacheProxyBs.newInstance().context(context).execute();
    }

    @Override
    public Object proxy() {
        Enhancer enhancer = new Enhancer();
        //目标对象类
        enhancer.setSuperclass(target.getClass());
        //设置单一回调对象，在调用中拦截对目标方法的调用
        enhancer.setCallback(this);
        //通过字节码技术创建目标对象类的子类实例作为代理
        return enhancer.create();
    }

}
