package org.andrbars.rest4j;

import java.util.HashMap;
import javax.ws.rs.core.MediaType;

public abstract class RestConst
{

	public final static MediaType APPLICATION_JSON_UTF_8_TYPE = new MediaType("application", "json",
		new HashMap<String, String>()
	{
		{
			put("charset", "UTF-8");
		}
	});

	public final static String APPLICATION_JSON_UTF_8 = MediaType.APPLICATION_JSON + "; charset=UTF-8";
}
