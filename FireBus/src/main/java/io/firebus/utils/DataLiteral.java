package io.firebus.utils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataLiteral extends DataEntity
{
	protected String stringValue;
	protected boolean boolValue;
	protected Number numberValue;
	protected ZonedDateTime dateValue;
	protected ZonedTime timeValue;
	protected int valueType;

	static protected Pattern datePattern = Pattern.compile("^(?:[1-9]\\d{3}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1\\d|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[1-9]\\d(?:0[48]|[2468][048]|[13579][26])|(?:[2468][048]|[13579][26])00)-02-29)T(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:\\.\\d{1,9})?(?:Z|[+-][01]\\d:[0-5]\\d)$");
	static protected Pattern timePattern = Pattern.compile("^T(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:\\.\\d{1,9})?(?:Z|[+-][01]\\d:[0-5]\\d|\\[[a-zA-Z]+\\/[a-zA-Z]+\\])$");
	static protected Pattern doublePattern = Pattern.compile("[-+]?\\d*\\.\\d+");
	static protected Pattern longPattern = Pattern.compile("[-+]?\\d+");
	
	static public int TYPE_NULL = 0;
	static public int TYPE_STRING = 1;
	static public int TYPE_NUMBER = 2;
	static public int TYPE_BOOLEAN = 3;
	static public int TYPE_DATE = 4;
	static public int TYPE_TIME = 5;
	
	public DataLiteral(Object v)
	{
		if(v == null)
		{
			valueType = TYPE_NULL;
		}
		else if(v instanceof String)
		{
			valueType = TYPE_STRING;
			stringValue = (String)v;
		}
		else if(v instanceof Number)
		{
			valueType = TYPE_NUMBER;
			numberValue = (Number)v;
		}
		else if(v instanceof Boolean)
		{
			valueType = TYPE_BOOLEAN;
			boolValue = (Boolean)v;
		}
		else if(v instanceof Date)
		{
			valueType = TYPE_DATE;
			dateValue = ZonedDateTime.ofInstant(Instant.ofEpochMilli(((Date)v).getTime()), ZoneId.systemDefault());
		}
		else if(v instanceof ZonedDateTime)
		{
			valueType = TYPE_DATE;
			dateValue = (ZonedDateTime)v;
		}
		else if(v instanceof ZonedTime)
		{
			valueType = TYPE_TIME;
			timeValue = (ZonedTime)v;
		}		
	}
	
	public DataLiteral(InputStream is) throws DataException, IOException
	{
		StringBuilder sb = null;
		boolean inQuotes = false;
		boolean escaping = false;
		int cInt = -1;
		char c = ' ';
		//char previousC = ' ';
		int readState = 0; 
		boolean hadQuotes = false;

		PositionTrackingInputStream bis = null;
		if(is instanceof PositionTrackingInputStream)
			bis = (PositionTrackingInputStream)is;
		else
			bis = new PositionTrackingInputStream(is);
		
		bis.mark(1);
		while((cInt = bis.read()) != -1)
		{
			//previousC = c;
			c = (char)cInt;
			if(readState == 0) // Before value
			{
				if(c != ' '  &&  c != '\r' && c != '\n' && c != '\t')
				{
					bis.reset();
					sb = new StringBuilder();
					readState = 1;
				}					
			}
			else if(readState == 1) // In value
			{
				if(inQuotes)
				{
					if(c == '"'  &&  !escaping)
					{
						inQuotes = false;
						hadQuotes = true;
						String tempString = sb.toString();
						if(datePattern.matcher(tempString).matches())		
						{
							try
							{
								valueType = TYPE_DATE;
								dateValue = ZonedDateTime.parse(tempString);
							} 
							catch (Exception e)
							{
								throw new DataException("Error processing date value");
							}
						}
						else if(timePattern.matcher(tempString).matches()) 
						{
							try
							{
								valueType = TYPE_TIME;
								timeValue = ZonedTime.parse(tempString);
							} 
							catch (Exception e)
							{
								throw new DataException("Error processing time value");
							}
						}
						else
						{
							valueType = TYPE_STRING;
							stringValue = tempString;
						}
						break;
					}
					else
					{
						if(escaping)
						{
							if(c == '\\') sb.append('\\');
							else if(c == 'n') sb.append('\n');
							else if(c == 'r') sb.append('\r');
							else if(c == 't') sb.append('\t');
							else if(c == '/') sb.append('/');
							else if(c == '\"') sb.append('\"');
							escaping = false;
						}
						else if(c == '\\')
						{
							escaping = true;
						}
						else
						{
							sb.append(c);
						}
					}
				}
				else
				{
					if(c == '"')
					{
						if(sb.length() == 0)
							inQuotes = true;
						else
							throw new DataException("Illegal character at line " + bis.getLine() + " column " + bis.getColumn());
					}
					else if(c == ' ' || c == '\r' || c == '\n' || c == '\t' || c == ',' || c == '}' || c == ']')
					{
						String tempString = sb.toString();
						bis.reset();
						if(!hadQuotes)
						{
							Matcher doubleMatcher = doublePattern.matcher(tempString);
							Matcher longMatcher = longPattern.matcher(tempString);
							if(tempString.equalsIgnoreCase("true")  ||  tempString.equalsIgnoreCase("false"))
							{
								valueType = TYPE_BOOLEAN;
								boolValue = tempString.equalsIgnoreCase("true") ? true : false;
							}
							else if(tempString.equalsIgnoreCase("null"))
							{
								valueType = TYPE_NULL;
							}
							else if(doubleMatcher.matches())
							{
								valueType = TYPE_NUMBER;
								numberValue = Double.parseDouble(tempString);
							}
							else if(longMatcher.matches())
							{
								valueType = TYPE_NUMBER;
								numberValue = Long.parseLong(tempString);
							}
							else
							{
								valueType = TYPE_STRING;
								stringValue = tempString;
							}
						}
						else
						{
							valueType = TYPE_STRING;
							stringValue = tempString;
						}
						break;
					}
					else
					{
						sb.append(c);
					}
				}
			}
			bis.mark(1);
		}
	}
	
	public String getString()
	{
		if(valueType == TYPE_NULL)
			return null;
		if(valueType == TYPE_STRING)
			return stringValue;
		else if(valueType == TYPE_NUMBER)
			return "" + numberValue;
		else if(valueType == TYPE_BOOLEAN)
			return "" + boolValue;
		else if(valueType == TYPE_DATE)
			return dateValue.toInstant().toString();
		else if(valueType == TYPE_TIME)
			return timeValue.toString();
		return "";
	}

	public boolean getBoolean()
	{
		if(valueType == TYPE_BOOLEAN)
			return boolValue;
		else if(valueType == TYPE_STRING)
			return stringValue.equalsIgnoreCase("true");
		return false;
	}
	
	public Number getNumber()
	{
		if(valueType == TYPE_STRING)
			try
			{
				return Double.parseDouble(stringValue);
			}
			catch(NumberFormatException e)
			{
				return 0;
			}
		else if(valueType == TYPE_NUMBER)
			return numberValue;
		else if(valueType == TYPE_BOOLEAN)
			return boolValue == true ? 1 : 0;
		else if(valueType == TYPE_DATE)
			return dateValue.toInstant().toEpochMilli();
		return 0;
	}
	
	public Date getDate()
	{
		if(valueType == TYPE_STRING)
			return null;
		else if(valueType == TYPE_NUMBER)
			return new Date(numberValue.longValue());
		else if(valueType == TYPE_BOOLEAN)
			return null;
		else if(valueType == TYPE_DATE)
			return Date.from(dateValue.toInstant());
		else if(valueType == TYPE_TIME)
			return Date.from(timeValue.atDate(ZonedDateTime.now()).toInstant());
		return null;
	}	
	
	public ZonedTime getTime()
	{
		if(valueType == TYPE_DATE)
			return new ZonedTime(dateValue);
		else if(valueType == TYPE_TIME)
			return timeValue;
		return null;
	}
	
	public Object getObject()
	{
		if(valueType == TYPE_NULL)
			return null;
		if(valueType == TYPE_STRING)
			return stringValue;
		else if(valueType == TYPE_NUMBER)
			return numberValue;
		else if(valueType == TYPE_BOOLEAN)
			return boolValue;
		else if(valueType == TYPE_DATE)
			return Date.from(dateValue.toInstant());
		else if(valueType == TYPE_TIME)
			return timeValue;
		return null;
	}
	
	public int getType()
	{
		return valueType;
	}
	
	public boolean equals(Object o)
	{
		Object otherObject = o;
		if(o instanceof DataLiteral)
			otherObject = ((DataLiteral)o).getObject();
		Object thisObject = getObject();
		if(thisObject == null && otherObject == null)
			return true;
		else if(thisObject != null && otherObject != null && thisObject.equals(otherObject))
			return true;
		else
			return false;
	}
	
	public String toString()
	{
		return toString(0, false);
	}

	public String toString(int indent, boolean flat)
	{
		if(valueType == TYPE_NULL)
			return "null";
		else if(valueType == TYPE_STRING) 
			return "\"" + escape(getString()) + "\"";
		else if(valueType == TYPE_DATE)
			return "\"" + getString() + "\"";
		else if(valueType == TYPE_TIME)
			return "\"" + getString() + "\"";
		else
			return getString();
	}
	
	protected String escape(String s) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if(c == '\\') sb.append("\\\\");
			else if(c == '\"') sb.append("\\\"");
			else if(c == '\n') sb.append("\\n");
			else if(c == '\r') sb.append("\\r");
			else if(c == '\t') sb.append("\\t");
			else if(c == '/') sb.append("\\/");
			else sb.append(c);
		}
		return sb.toString();
	}
	
	public DataLiteral getCopy()
	{
		if(valueType == TYPE_NULL)
			return new DataLiteral((Object)null);
		if(valueType == TYPE_STRING)
			return new DataLiteral(new String(stringValue));
		else if(valueType == TYPE_NUMBER) 
		{
			if(numberValue instanceof Double)
				return new DataLiteral(new Double(numberValue.doubleValue()));
			else
				return new DataLiteral(new Long(numberValue.longValue()));
		}
		else if(valueType == TYPE_BOOLEAN)
			return new DataLiteral(new Boolean(boolValue));
		else if(valueType == TYPE_DATE)
			return new DataLiteral(ZonedDateTime.ofInstant(dateValue.toInstant(), dateValue.getZone()) );
		else if(valueType == TYPE_TIME)
			return new DataLiteral(new ZonedTime(timeValue));
		return null;
	}
}
