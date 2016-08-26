package org.andrbars.rest4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.MediaType;

class RestInvoker
{

	private static final ObjectMapper mapper = new ObjectMapper();

	private String serviceUrl;

	private Proxy connectionProxy = Proxy.NO_PROXY;
	private int connectionTimeoutMillis = 60 * 1000;
	private int readTimeoutMillis = 60 * 1000 * 2;
	private SSLContext sslContext = null;
	private HostnameVerifier hostNameVerifier = null;

	public RestInvoker(String serviceUrl)
	{
		if (serviceUrl == null)
		{
			throw new IllegalArgumentException("Argument serviceUrl must be defined.");
		}

		this.serviceUrl = serviceUrl.endsWith("/")
			? serviceUrl = serviceUrl.substring(0, serviceUrl.length() - 1)
			: serviceUrl;
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
				return Utils.readResponse(params.getReturnType(), ips, mapper);
			}
		}
		catch (IOException e)
		{
			try (InputStream stream = con.getErrorStream())
			{
				String message = Utils.readString(stream);
				throw Utils.getRemoteException(message, mapper);
			}
		}
	}

	private HttpURLConnection prepareConnection(InvokeParams params)
		throws IOException
	{
		String path = serviceUrl + "/" + params.getParametrizedPath();
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
}
