package org.andrbars.rest4j;

public class ParamToken
{

	public static final ParamToken empty = new ParamToken(0, 0, "");

	private final int first;
	private final int last;
	private final String name;

	private ParamToken(int first, int last, String name)
	{
		this.first = first;
		this.last = last;
		this.name = name;
	}

	public int getFirst()
	{
		return first;
	}

	public int getLast()
	{
		return last;
	}

	public String getName()
	{
		return name;
	}

	public boolean isEmpty()
	{
		return this == empty;
	}

	public static ParamToken parse(String paramStr, int fromIndex)//!!! unit-test
	{
		if ((paramStr == null) || (paramStr.length() < 2) || (fromIndex >= paramStr.length()))
		{
			return empty;
		}

		int first = paramStr.indexOf('{', fromIndex);
		if (first == -1)
		{
			return empty;
		}

		int last = paramStr.indexOf('}', fromIndex + 1);
		if (last == -1)
		{
			return empty;
		}

		String name = paramStr.substring(first, last);
		int sep = paramStr.indexOf(':');
		return sep == -1
			? new ParamToken(first, last, name.substring(first + 1, last))
			: new ParamToken(first, last, name.substring(first + 1, sep));
	}

	@Override
	public String toString()
	{
		return isEmpty()
			? "[ParamToken.empty]"
			: "ParamToken{" + "first=" + first + ", last=" + last + ", name=" + name + '}';
	}

}
