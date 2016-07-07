package org.andrbars.rest4j;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ExceptionInfo implements Serializable
{

	private Integer statusCode;
	private String exceptionType;
	private String requestUri;
	private String servletName;
	private String message;

	public ExceptionInfo()
	{
	}

	public ExceptionInfo(Integer statusCode, String exceptionType, String requestUri, String servletName, String message)
	{
		this.statusCode = statusCode;
		this.exceptionType = exceptionType;
		this.requestUri = requestUri;
		this.servletName = servletName;
		this.message = message;
	}

	public Integer getStatusCode()
	{
		return statusCode;
	}

	public String getExceptionType()
	{
		return exceptionType;
	}

	public String getRequestUri()
	{
		return requestUri;
	}

	public String getServletName()
	{
		return servletName;
	}

	public String getMessage()
	{
		return message;
	}

	public Throwable constructException()
		throws ClassNotFoundException, InstantiationException, IllegalAccessException,
		NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		Class<?> clazz = Class.forName(exceptionType);
		Constructor<?> ctr = clazz.getConstructor(String.class);
		return (Throwable)ctr.newInstance(message);
	}

	@Override
	public String toString()
	{
		return "ExceptionInfo{" + "statusCode=" + statusCode + ", exceptionType=" + exceptionType + ", requestUri=" + requestUri + ", servletName=" + servletName + ", message=" + message + '}';
	}

}
