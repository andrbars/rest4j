package org.andrbars.rest4j;

import com.fasterxml.jackson.databind.JavaType;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map.Entry;
import javax.ws.rs.core.MediaType;

class RestInvoker
{

	private final ObjectMapper mapper;

	private String serviceUrl;

	private Proxy connectionProxy = Proxy.NO_PROXY;
	private int connectionTimeoutMillis = 60 * 1000;
	private int readTimeoutMillis = 60 * 1000 * 2;
	private SSLContext sslContext = null;
	private HostnameVerifier hostNameVerifier = null;

	private static String readString(InputStream inputStream)
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

	public RestInvoker(String serviceUrl)
	{
		this.mapper = new ObjectMapper();
		setServiceUrl(serviceUrl);
	}

	public Object invoke(InvokeParams params)
		throws Throwable
	{
		HttpURLConnection con = prepareConnection(params);
		con.connect();

		if (params.isRequestWithBody())
		{
			try (OutputStream ops = con.getOutputStream())
			{
				internalCreateRequest(params, ops);
				ops.flush();
			}
		}

		try
		{
			try (InputStream ips = con.getInputStream())
			{
				System.out.println("HTTP response code = " + con.getResponseCode());
				return readResponse(params.getReturnType(), ips);
			}
		}
		catch (IOException e)
		{
			try (InputStream stream = con.getErrorStream())
			{
				String message = readString(stream);
				throw getRemoteException(message);
			}
		}
	}

	private Object readResponse(Type returnType, InputStream ips)
		throws Throwable
	{
		if (returnType == null)
		{
			return null;
		}

		String response = readString(ips);

		System.out.println("response -> " + response);

		try
		{
			JavaType type = mapper.getTypeFactory().constructType(returnType);
			return mapper.readValue(response, type);
		}
		catch (Exception exc)
		{
			throw getRemoteException(response);
		}
	}

	private HttpURLConnection prepareConnection(InvokeParams params)
		throws IOException
	{
		String path = serviceUrl + "/" + params.getParametrizedPath();
		System.out.println("path -> " + path);
		URL url = new URL(path);

		HttpURLConnection con = (HttpURLConnection)url.openConnection(connectionProxy);
		con.setConnectTimeout(connectionTimeoutMillis);
		con.setReadTimeout(readTimeoutMillis);
		con.setAllowUserInteraction(false);
		con.setDefaultUseCaches(false);
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setInstanceFollowRedirects(true);

		con.setRequestMethod(params.getHttpMethod());

		// do stuff for ssl
		if (HttpsURLConnection.class.isInstance(con))
		{
			HttpsURLConnection https = HttpsURLConnection.class.cast(con);
			if (hostNameVerifier != null)
			{
				https.setHostnameVerifier(hostNameVerifier);
			}
			if (sslContext != null)
			{
				https.setSSLSocketFactory(sslContext.getSocketFactory());
			}
		}

		con.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

		String cookies = params.getCookieParams();
		if (!cookies.isEmpty())
		{
			con.setRequestProperty("Cookie", params.getCookieParams());
		}

		Map<String, String> headers = params.getHeaderParams();
		for (Entry<String, String> entry: headers.entrySet())
		{
			con.setRequestProperty(entry.getKey(), entry.getValue());
		}

		return con;
	}

	private Throwable getRemoteException(String message)
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
				exception = new RuntimeException("Ошибка создания исключения " + exceptionInfo,
					exc3);
			}
		}
		catch (Exception exc1)
		{
			exception = new RuntimeException("Ошибка при чтении исключения", exc1);
		}

		return exception;
	}

	private void internalCreateRequest(InvokeParams params, OutputStream ops)
		throws IOException
	{
		String formParams = params.getFormParams();
		if (!formParams.isEmpty())
		{
			ops.write(formParams.getBytes());
			return;
		}

		Object arguments = params.getArguments();

		// object array args
		if (arguments != null && arguments.getClass().isArray())
		{
			Object[] args = Object[].class.cast(arguments);
			if (args.length == 1)
			{
				mapper.writeValue(ops, args[0]);
			}
			else
			{
				mapper.writeValue(ops, args);
			}

			// collection args
		}
		else if (arguments != null && Collection.class.isInstance(arguments))
		{
			Collection<?> args = Collection.class.cast(arguments);
			if (args.size() == 1)
			{
				for (Object arg: args)
				{
					mapper.writeValue(ops, arg);
					break;
				}
			}
			else
			{
				mapper.writeValue(ops, args);
			}

			// map args
		}
		else if (arguments != null && Map.class.isInstance(arguments))
		{
			Collection args = Map.class.cast(arguments).values();
			if (args.size() == 1)
			{
				for (Object arg: args)
				{
					mapper.writeValue(ops, arg);
					break;
				}
			}
			else
			{
				mapper.writeValue(ops, args);
			}

			// other args
		}
		else if (arguments != null)
		{
			mapper.writeValue(ops, arguments);
		}
	}

	public String getServiceUrl()
	{
		return serviceUrl;
	}

	public final void setServiceUrl(String serviceUrl)
	{
		if ((serviceUrl != null) && serviceUrl.endsWith("/"))
		{
			serviceUrl = serviceUrl.substring(0, serviceUrl.length() - 1);
		}
		this.serviceUrl = serviceUrl;
	}

	public Proxy getConnectionProxy()
	{
		return connectionProxy;
	}

	public void setConnectionProxy(Proxy connectionProxy)
	{
		this.connectionProxy = connectionProxy;
	}

	public int getConnectionTimeoutMillis()
	{
		return connectionTimeoutMillis;
	}

	public void setConnectionTimeoutMillis(int connectionTimeoutMillis)
	{
		this.connectionTimeoutMillis = connectionTimeoutMillis;
	}

	public int getReadTimeoutMillis()
	{
		return readTimeoutMillis;
	}

	public void setReadTimeoutMillis(int readTimeoutMillis)
	{
		this.readTimeoutMillis = readTimeoutMillis;
	}

	public void setSslContext(SSLContext sslContext)
	{
		this.sslContext = sslContext;
	}

	public void setHostNameVerifier(HostnameVerifier hostNameVerifier)
	{
		this.hostNameVerifier = hostNameVerifier;
	}

	public ObjectMapper getObjectMapper()
	{
		return mapper;
	}

}
