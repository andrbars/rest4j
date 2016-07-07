package org.andrbars.rest4j;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

public class InvokeParams
{

	private static final List<Class> httpMethodAnnotations;

	static
	{
		httpMethodAnnotations = new ArrayList<Class>()
		{
			{
				add(GET.class);
				add(POST.class);
				add(PUT.class);
				add(DELETE.class);
			}
		};

	}

	private final Object[] arguments;
	private final Method method;

	private String methodName;
	private String path;

	public InvokeParams(Object[] arguments, Method method)
	{
		this.arguments = arguments != null
			? arguments
			: new Object[]
			{
			};
		this.method = method;
	}

	public String getMethodName()
	{
		if (methodName == null)
		{
			methodName = method.getName();
		}

		return methodName;
	}

	public Type getReturnType()
	{
		return method.getGenericReturnType();
	}

	public String getHttpMethod()
	{
		for (Class clazz: httpMethodAnnotations)
		{
			if (method.getAnnotation(clazz) != null)
			{
				return clazz.getSimpleName();
			}
		}
		return "";
	}

	public boolean isRequestWithBody()
	{
		String httpMethod = getHttpMethod();
		return "POST".equals(httpMethod) || "PUT".equals(httpMethod);
	}

	public String getPath()
	{
		if (path == null)
		{
			Path a = method.getAnnotation(Path.class);
			path = a != null
				? a.value()
				: "";

			if (path.startsWith("/"))
			{
				path = path.substring(1);
			}
		}
		return path;
	}

	private String queryParams;

	private String getQueryParams()
	{
		if (queryParams != null)
		{
			return queryParams;
		}

		queryParams = "";

		boolean hasQueryParam = false;
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < annotations.length; i++)
		{
			Annotation[] a = annotations[i];
			for (Annotation item: a)
			{
				if (item.annotationType() == QueryParam.class)
				{
					QueryParam queryParam = (QueryParam)item;
					queryParams += hasQueryParam
						? "&"
						: "";
					try
					{
						queryParams += queryParam.value() + "="
							+ (arguments[i] == null
								? ""
								: URLEncoder.encode(format(arguments[i], a), "UTF-8"));
					}
					catch (UnsupportedEncodingException ex)
					{
						throw new RuntimeException(ex);
					}
					hasQueryParam = true;
				}
			}
		}

		return queryParams;
	}

	private String matrixParams;

	private String getMatrixParams()
	{
		if (matrixParams != null)
		{
			return matrixParams;
		}

		matrixParams = "";

		boolean hasMatrixParam = false;
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < annotations.length; i++)
		{
			Annotation[] a = annotations[i];
			for (Annotation item: a)
			{
				if (item.annotationType() == MatrixParam.class)
				{
					MatrixParam matrixParam = (MatrixParam)item;
					matrixParams += hasMatrixParam
						? ";"
						: "";
					try
					{
						matrixParams += matrixParam.value() + "="
							+ (arguments[i] == null
								? ""
								: URLEncoder.encode(format(arguments[i], a), "UTF-8"));
					}
					catch (UnsupportedEncodingException ex)
					{
						throw new RuntimeException(ex);
					}
					hasMatrixParam = true;
				}
			}
		}

		return matrixParams;
	}

	private String parametrizedPath;

	public String getParametrizedPath()
	{
		if (parametrizedPath != null)
		{
			return parametrizedPath;
		}

		parametrizedPath = getPath();

		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < annotations.length; i++)
		{
			Annotation[] a = annotations[i];
			for (Annotation item: a)
			{
				if (item.annotationType() == PathParam.class)
				{
					PathParam pathParam = (PathParam)item;
					ParamToken token = ParamToken.parse(parametrizedPath, 0);// оптимизировать с использованием кэшей, т.к. PathParam и Annotation не изменяется на этапе выполнения, то достаточно парсить один раз
					while (token != ParamToken.empty)
					{
						if (pathParam.value().equals(token.getName()))
						{
							String val = arguments[i] == null
								? ""
								: arguments[i].toString();

							parametrizedPath = parametrizedPath.substring(0, token.getFirst())
								+ val
								+ parametrizedPath.substring(token.getLast() + 1);
						}
						token = ParamToken.parse(parametrizedPath, token.getLast());
					}
				}
			}
		}

		String qp = getQueryParams();
		if (!qp.isEmpty())
		{
			parametrizedPath += "?" + qp;
		}
		else
		{
			String mp = getMatrixParams();
			if (!mp.isEmpty())
			{
				parametrizedPath += ";" + mp;
			}
		}

		return parametrizedPath;
	}

	private String cookieParams;

	public String getCookieParams()
	{
		if (cookieParams != null)
		{
			return cookieParams;
		}

		cookieParams = "";

		boolean hasCookieParam = false;
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < annotations.length; i++)
		{
			Annotation[] a = annotations[i];
			for (Annotation item: a)
			{
				if (item.annotationType() == CookieParam.class)
				{
					CookieParam cookieParam = (CookieParam)item;
					cookieParams += hasCookieParam
						? ";"
						: "";
					cookieParams += cookieParam.value() + "=" + format(arguments[i], a);

					hasCookieParam = true;
				}
			}
		}

		return cookieParams;
	}

	private Map<String, String> headerParams;

	public Map<String, String> getHeaderParams()
	{
		if (headerParams != null)
		{
			return headerParams;
		}

		headerParams = new HashMap<>();

		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < annotations.length; i++)
		{
			Annotation[] a = annotations[i];
			for (Annotation item: a)
			{
				if (item.annotationType() == HeaderParam.class)
				{
					HeaderParam headerParam = (HeaderParam)item;
					headerParams.put(headerParam.value(), format(arguments[i], a));
				}
			}
		}

		return headerParams;
	}

	private String formParams;

	public String getFormParams()
	{
		if (formParams != null)
		{
			return formParams;
		}

		formParams = "";

		boolean hasFormParam = false;
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < annotations.length; i++)
		{
			Annotation[] a = annotations[i];
			for (Annotation item: a)
			{
				if (item.annotationType() == FormParam.class)
				{
					FormParam formParam = (FormParam)item;
					formParams += hasFormParam
						? "&"
						: "";
					try
					{
						formParams += formParam.value() + "="
							+ (arguments[i] == null
								? ""
								: URLEncoder.encode(format(arguments[i], a), "UTF-8"));
					}
					catch (UnsupportedEncodingException ex)
					{
						throw new RuntimeException(ex);
					}
					hasFormParam = true;
				}
			}
		}

		return formParams;
	}

	public Object[] getArguments()
	{
		return arguments;
	}

	private String format(Object value, Annotation[] annotations)
	{
		if (value == null)
		{
			return "";
		}

		if (value.getClass() == Date.class)
		{
			for (Annotation annotation: annotations)
			{
				if (annotation.annotationType() == JsonFormat.class)
				{
					JsonFormat.Value format = new JsonFormat.Value((JsonFormat)annotation);

					JsonFormat.Shape shape = format.getShape();
					if (shape.isNumeric())
					{
						return Long.toString(((Date)value).getTime());
					}
					else if ((shape == JsonFormat.Shape.STRING) || format.hasPattern()
						|| format.hasLocale() || format.hasTimeZone())
					{
						TimeZone tz = format.getTimeZone();
						final String pattern = format.hasPattern()
							? format.getPattern()
							: StdDateFormat.DATE_FORMAT_STR_ISO8601;
						final Locale loc = format.hasLocale()
							? format.getLocale()
							: Locale.getDefault();
						SimpleDateFormat df = new SimpleDateFormat(pattern, loc);
						if (tz == null)
						{
							tz = TimeZone.getTimeZone("GMT");
						}
						df.setTimeZone(tz);
						return df.format((Date)value);
					}

					return value.toString();
				}
			}
		}

		return value.toString();
	}

}
