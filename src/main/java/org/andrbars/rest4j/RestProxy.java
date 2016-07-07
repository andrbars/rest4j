package org.andrbars.rest4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RestProxy
{

	public static <I> I get(Class<I> restInterface, final String url)
	{
		return get(RestProxy.class.getClassLoader(), restInterface, url);
	}

	public static <I> I get(ClassLoader classLoader, Class<I> restInterface, final String url)
	{
		return (I)Proxy.newProxyInstance(
			classLoader,
			new Class<?>[]
			{
				restInterface
			},
			new InvocationHandler()
		{
			private final RestInvoker invoker = new RestInvoker(url);

			@Override
			public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable
			{
				InvokeParams params = new InvokeParams(args, method);

				return invoker.invoke(params);
			}
		});
	}
}