package org.andrbars.rest4j;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

public class Utils
{

	public static String readString(InputStream inputStream)
	{
		if (inputStream == null)
		{
			return "";
		}

		try
		{
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while ((length = inputStream.read(buffer)) != -1)
			{
				result.write(buffer, 0, length);
			}
			return result.toString("UTF-8");
		}
		catch (IOException ex)
		{
			return "";
		}
	}

	public static Object readResponse(Type returnType, InputStream ips, ObjectMapper mapper)
		throws Throwable
	{
		if (returnType == null)
		{
			return null;
		}

		String response = Utils.readString(ips);
		try
		{
			JavaType type = mapper.getTypeFactory().constructType(returnType);
			return mapper.readValue(response, type);
		}
		catch (Exception exc)
		{
			throw getRemoteException(response, mapper);
		}
	}

	public static Throwable getRemoteException(String message, ObjectMapper mapper)
	{
		Throwable exception;

		try
		{
			ExceptionInfo exceptionInfo = mapper.readValue(message, ExceptionInfo.class);
			try
			{
				exception = exceptionInfo.constructException();
			}
			catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException exc3)
			{
				exception = new RuntimeException("Ошибка создания исключения " + exceptionInfo, exc3);
			}
		}
		catch (Exception exc1)
		{
			exception = new RuntimeException("Ошибка при чтении исключения", exc1);
		}

		return exception;
	}
}
