package org.andrbars.rest4j;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.andrbars.rest4j.MethodParam.ParamType;

class InvokeParams
{

	private static final List<Class> httpMethodAnnotations;
	private static final Map<Method, List<MethodParam>> methodParamsCache;

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

		methodParamsCache = new HashMap<>();
	}

	private final Object[] arguments;
	private final Method method;
	private final List<MethodParam> methodParams;

	private String methodName;
	private String httpMethod;
	private String path;

	private String parametrizedPath;
	private String queryParams;
	private String matrixParams;
	private String cookieParams;
	private String formParams;
	private Map<String, String> headerParams;

	public InvokeParams(Object[] arguments, Method method)
	{
		this.arguments = arguments != null
			? arguments
			: new Object[]
			{
			};
		this.method = method;
		this.methodParams = parse();
	}

	private List<MethodParam> parse()
	{
		List<MethodParam> result = methodParamsCache.get(method);
		if (result != null)
		{
			return result;
		}

		result = new ArrayList<>();

		Annotation[][] annotations = method.getParameterAnnotations();
		for (Annotation[] annotation: annotations)
		{
			String name = null;
			ParamType type = null;
			JsonFormat.Value format = null;
			String paramPath = null;

			for (Annotation item: annotation)
			{
				Class<? extends Annotation> t = item.annotationType();
				if (t == QueryParam.class)
				{
					QueryParam param = (QueryParam)item;
					name = param.value();
					type = ParamType.QueryParam;
				}
				else if (t == MatrixParam.class)
				{
					MatrixParam param = (MatrixParam)item;
					name = param.value();
					type = ParamType.MatrixParam;
				}
				else if (t == CookieParam.class)
				{
					CookieParam param = (CookieParam)item;
					name = param.value();
					type = ParamType.CookieParam;
				}
				else if (t == HeaderParam.class)
				{
					HeaderParam param = (HeaderParam)item;
					name = param.value();
					type = ParamType.HeaderParam;
				}
				else if (t == FormParam.class)
				{
					FormParam param = (FormParam)item;
					name = param.value();
					type = ParamType.FormParam;
				}
				else if (t == JsonFormat.class)
				{
					format = new JsonFormat.Value((JsonFormat)item);
				}
				else if (t == PathParam.class)
				{
					PathParam pathParam = (PathParam)item;
					paramPath = pathParam.value();
				}
			}
			result.add(new MethodParam(name, paramPath, type, format));
		}

		methodParamsCache.put(method, result);
		return result;
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
		if (httpMethod != null)
		{
			return httpMethod;
		}

		httpMethod = "";
		for (Class clazz: httpMethodAnnotations)
		{
			if (method.getAnnotation(clazz) != null)
			{
				httpMethod = clazz.getSimpleName();
				break;
			}
		}
		return httpMethod;
	}

	public boolean isRequestWithBody()
	{
		String hm = getHttpMethod();
		return "POST".equals(hm) || "PUT".equals(hm);
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

	private String getQueryParams()
	{
		if (queryParams != null)
		{
			return queryParams;
		}

		queryParams = paramsChain(ParamType.QueryParam, "&");
		return queryParams;
	}

	private String getMatrixParams()
	{
		if (matrixParams != null)
		{
			return matrixParams;
		}

		matrixParams = paramsChain(ParamType.MatrixParam, ";");
		return matrixParams;
	}

	public String getCookieParams()
	{
		if (cookieParams != null)
		{
			return cookieParams;
		}

		cookieParams = paramsChain(ParamType.CookieParam, ";");
		return cookieParams;
	}

	public Map<String, String> getHeaderParams()
	{
		if (headerParams != null)
		{
			return headerParams;
		}

		headerParams = paramsMap(ParamType.HeaderParam);
		return headerParams;
	}

	public String getFormParams()
	{
		if (formParams != null)
		{
			return formParams;
		}

		formParams = paramsChain(ParamType.FormParam, "&");
		return formParams;
	}

	public String getParametrizedPath()
	{
		if (parametrizedPath != null)
		{
			return parametrizedPath;
		}

		parametrizedPath = getPath();
		for (int i = 0; i < methodParams.size(); i++)
		{
			MethodParam methodParam = methodParams.get(i);
			String p = methodParam.getPath();
			if (p == null)
			{
				continue;
			}

			ParamToken token = ParamToken.parse(parametrizedPath, 0);// оптимизировать с использованием кэшей, т.к. PathParam и Annotation не изменяется на этапе выполнения, то достаточно парсить один раз
			while (token != ParamToken.empty)
			{
				if (p.equals(token.getName()))
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

	public Object[] getArguments()
	{
		return arguments;
	}

	private Map<String, String> paramsMap(ParamType type)
	{
		Map<String, String> result = new HashMap<>();

		for (int i = 0; i < methodParams.size(); i++)
		{
			MethodParam methodParam = methodParams.get(i);
			if (methodParam.getType() == type)
			{
				result.put(methodParam.getName(), methodParam.get(arguments[i]));
			}
		}

		return result;
	}

	private String paramsChain(ParamType type, String splitter)
	{
		String result = "";

		boolean hasParam = false;
		for (int i = 0; i < methodParams.size(); i++)
		{
			MethodParam methodParam = methodParams.get(i);
			if (methodParam.getType() == type)
			{
				result += hasParam
					? splitter
					: "";
				result += methodParam.getName() + "=" + methodParam.get(arguments[i]);
				hasParam = true;
			}
		}

		return result;
	}
}
