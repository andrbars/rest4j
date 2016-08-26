package org.andrbars.rest4j;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class MethodParam
{

	public enum ParamType
	{
		QueryParam,
		MatrixParam,
		CookieParam,
		HeaderParam,
		FormParam;
	}
	private final String name;
	private final ParamType type;
	private final JsonFormat.Value format;
	private final String path;

	public MethodParam(String name, String path, ParamType type, JsonFormat.Value format)
	{
		this.name = name;
		this.path = path;
		this.type = type;
		this.format = format;
	}

	public String getName()
	{
		return name;
	}

	public String getPath()
	{
		return path;
	}

	public ParamType getType()
	{
		return type;
	}

	public JsonFormat.Value getFormat()
	{
		return format;
	}

	public String get(Object value)
	{
		return format(value, (type == ParamType.QueryParam) || (type == ParamType.MatrixParam));
	}

	private String format(Object value, boolean encode)
	{
		if (value == null)
		{
			return "";
		}

		if ((value.getClass() == Date.class) && (format != null))
		{
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
		}

		try
		{
			return encode
				? URLEncoder.encode(value.toString(), "UTF-8")
				: value.toString();
		}
		catch (UnsupportedEncodingException ex)
		{
			return value.toString();
		}
	}
}
