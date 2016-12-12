package com.genexus.util;

import java.util.Hashtable;
import java.io.*;
import java.util.Enumeration;
import java.util.Vector;
import com.genexus.*;

public class IniFile 
{
	private static final int SECTION  = 1;
	private static final int PROPERTY = 2;
	private static final int COMMENT  = 3;
	private static final int IGNORE   = 4;

	private File fileHandle;
	private Hashtable sections = new Hashtable();
	private BufferedReader br;

	private static final int RELOAD_INT = 5000;
	private boolean isAutomaticReloading = false;
	private long lastReloadTime = 0;

	private String defaultSiteKey    = "6A23DB2B6A619585C8B445D5949099E5";
	private String defaultServerKey  = "6A23DB2B6A619585C8B445D5949099E5";

	private String realSiteKey ;
	private String realServerKey ;
	
	public IniFile(InputStream in) throws IOException
	{
		if	(in == null)
		{
			throw new IOException("Null inputStream");
		}

		br = new BufferedReader(new InputStreamReader(in));
		read();
	}

	public IniFile(String filename)
	{	
		try
		{
			fileHandle = new File(filename);
			br = new BufferedReader(new InputStreamReader(new FileInputStream(fileHandle)));
			read();
		}
		catch (IOException e)
		{
		}
	}

	private InputStream encryptionIs;
	public void setEncryptionStream(InputStream is)
	{
		this.encryptionIs = is;
	}

	public String getServerKey() 
	{
		if	(realServerKey != null)
			return realServerKey;

		if	(encryptionIs == null)
			return defaultServerKey;

		try
		{	
			IniFile crypto = new IniFile(encryptionIs);
			realServerKey = crypto.getProperty("Encryption", "ServerKey", null);
		}
		catch (Exception e)
		{
		}

		return realServerKey == null?defaultServerKey:realServerKey;
	}

	public String getSiteKey() 
	{
		if	(realSiteKey != null)
			return realSiteKey;

		if	(encryptionIs == null)
			return defaultSiteKey;

		try
		{	
			IniFile crypto = new IniFile(encryptionIs);
			realSiteKey = crypto.getProperty("Encryption", "SiteKey", null);
		}
		catch (Exception e)
		{
		}

		return realSiteKey == null?defaultSiteKey:realSiteKey;
	}

	public String toString()
	{
		return "IniFile" + hashCode() + " : " + fileHandle.getName();
	}

	public void setAutomaticReloading(boolean isAutomaticReloading)
	{
		this.isAutomaticReloading = isAutomaticReloading;
	}

	private void reload() 
	{
		synchronized (lock)
		{
			try
			{
				read();
			}
			catch (IOException e)
			{
				System.err.println("Error reloading " + fileHandle.getName());
			}

			lastReloadTime = System.currentTimeMillis();
		}
	}

	public void read() throws IOException
	{
		sections = new Hashtable();

		String actSection = "";
		int ncomment 	  = 0;

		Hashtable properties = new Hashtable();

		IniLine il = new IniLine();
		String line;

		while ((line = br.readLine()) != null)
		{	
			il.parse(line);

			if      (il.typeLine == SECTION)
			{ 
				actSection = il.section.toUpperCase();
			  	properties = new Hashtable();
				sections.put(actSection, new Section(il.section, properties));
			}
			else if (il.typeLine == PROPERTY) 
			{ 
				properties.put(il.property.toUpperCase(), new Value(PROPERTY, il.property, il.value) );
			}
			else if (il.typeLine == COMMENT)
			{	
				ncomment = ncomment + 1;
				properties.put("Comment;" + ncomment, new Value(COMMENT, il.comment, "") );
				sections.put(actSection, properties);
			}	
		}
		br.close();
	}

	public Vector getPropertyList(String section, String prefix)
	{
		Vector out = new Vector();

		int idx = 0;
		String tmp;

		while ( (tmp = getProperty(section, prefix + (idx++))) != null)
		{
			out.addElement(tmp);
		}

		return out;
	}

	public void setPropertyList(String section, String prefix, Vector list)
	{
		int idx = 0;

		while (getProperty(section, prefix + idx) != null)
		{
			removeProperty(section, prefix + idx);
			idx++;
		}

		for (idx = 0; idx < list.size(); idx++)
		{
			setProperty(section, prefix + idx, (String) list.elementAt(idx));
		}
		while(!getProperty(section, prefix + idx, "").equals(""))
		{
			removeProperty(section, prefix + idx);
			idx++;
		}
	}

	public void setPropertyEncrypted(String section, String key, String value)
	{
		String siteKey = getSiteKey();
		if (key.equals("USER_PASSWORD"))
		{
			siteKey = siteKey.substring(16,32) + siteKey.substring(0, 16);
		}		
		setProperty(section, key, Encryption.encrypt64(value + Encryption.checksum(value, Encryption.getCheckSumLength()), siteKey));
	}

	public String getPropertyEncrypted(String section, String key)
	{
		String val = getProperty(section, key);
		if	(val != null)
		{
			int checkSumLength = Encryption.getCheckSumLength();
							  
			if	(val.length() > checkSumLength)
			{
				String siteKey = getSiteKey();
				if (key.equals("USER_PASSWORD"))
				{
					siteKey = siteKey.substring(16,32) + siteKey.substring(0, 16);
				}
				String dec = Encryption.decrypt64(val, siteKey);
				
				if	(dec.length() >= checkSumLength)
				{
					String checksum = GXutil.right(dec, checkSumLength);
					String nocheck  = GXutil.left(dec, dec.length() - checkSumLength);

					if	(checksum.equals(Encryption.checksum(nocheck , Encryption.getCheckSumLength())))
					{
						return nocheck;
					}
				}
			}
		}
		return val;
	}

	public String getPropertyEncrypted(String section, String key, String defaultValue)
	{
		String ret = getPropertyEncrypted(section, key);
		return ret == null?defaultValue:ret;
	}

	public boolean sectionExists(String section)
	{
		return sections.get(section.toUpperCase()) != null;
	}

	public void copySection(IniFile from, String sectionFrom, String sectionTo)
	{
		Section newSection = from.getSection(sectionFrom);
		newSection.key = sectionTo;
		sections.put(sectionTo.toUpperCase(), newSection);
	}

	public Section getSection(String name)
	{
		return (Section) sections.get(name.toUpperCase());
	}

	public int getIntegerProperty(String section, String key)
	{
		return  (int) GXutil.val(getProperty(section, key));
	}

	private Object lock = new Object();

	public String getProperty(String section, String key)
	{
		String output = null;

		synchronized (lock)
		{
			if	(isAutomaticReloading && lastReloadTime + RELOAD_INT < System.currentTimeMillis())
			{
				if	(fileHandle != null && fileHandle.lastModified() > lastReloadTime)
				{
					reload();
				}
			}

			Section sec  = (Section) sections.get(section.toUpperCase());

			Hashtable prop = null;
			if	(sec != null)
				prop = sec.values;

			if (prop != null)  
			{
				Value value = (Value) prop.get(key.toUpperCase());
				if	(value != null)
					output = value.value;
			}
		}
	
		return output;
	}

	public int getIntegerProperty(String section, String key, String defaultValue)
	{
		return (int) GXutil.val(getProperty(section, key, defaultValue));
	}

	public String getProperty(String section, String key, String defaultValue)
	{	
		String output = null;

		Section sec  = (Section) sections.get(section.toUpperCase());
		Hashtable prop = null;
		if	(sec != null)
		{
			prop = sec.values;
		}
	
		if (prop != null)  
		{
			Value value = (Value) prop.get(key.toUpperCase());
			if	(value != null)
			{
				output = value.value;
			}
		}
		return output == null?defaultValue:output;
	}

	public void setProperty(String section, String key, long value)
	{
		setProperty(section, key, GXutil.str(value, 12, 0).trim());
	}

	public void setProperty(String section, String key, double value)
	{
		setProperty(section, key, GXutil.str(value, 10, 0).trim());
	}

	public void setProperty(String section, String key, String value)
	{	
		Section sec = (Section) sections.get(section.toUpperCase());
		Hashtable sectionHash;

		if	(sec == null)
		{
			sectionHash = new Hashtable();
			sec = new Section(section, sectionHash);
			sections.put(section.toUpperCase(), sec);
		}
		else
			sectionHash = sec.values;

		sectionHash.put(key.toUpperCase(), new Value(PROPERTY, key, value));
	}

	public void removeProperty(String section, String key)
	{	
		Section sec = (Section) sections.get(section.toUpperCase());

		if	(sec != null)
			sec.values.remove(key.toUpperCase());
	}

	public void saveAs(String fileName)
	{
		saveFile(new File(fileName));
	}

	public void save()
	{
		saveFile(fileHandle);
	}

	public void saveFile(File fileHandle)
	{ 	
		try 
		{
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileHandle));
			
			for (Enumeration eSec = sections.elements();eSec.hasMoreElements(); )
			{ 	
				Section section = (Section) eSec.nextElement();

				bw.write('[');
				bw.write(section.key, 0, section.key.length());
				bw.write(']');
				bw.newLine();

				Hashtable properties = section.values;
				for (Enumeration eProp = properties.elements(); eProp.hasMoreElements(); )
				{	
					Value  value = (Value) eProp.nextElement();
					bw.write(value.key, 0, value.key.length());
					if (value.type != COMMENT)
					{
						bw.write('=');
						bw.write(value.value, 0, value.value.length());
					}
					bw.newLine();
				}
			}

			bw.close();
		}
	    catch(IOException e)
		{ 
			System.err.println("Error: " + e);
		}
	}

	class Section
	{
		String    key;
		Hashtable values;

		public Section(String key, Hashtable values)
		{
			this.key    = key;
			this.values = values;
		}
	}

	class Value
	{
		int type;
		String key;
		String value;

		public Value(int type, String key, String value)
		{
			this.type  = type;
			this.key   = key;
			this.value = value;
		}
	}

	class IniLine 
	{
		int typeLine;
		String section;
		String property;
		String value;
		String comment;

		public void parse(String line)
		{	
			section  = "";
			property = "";
			value    = "";
			typeLine = IniFile.PROPERTY;
		
			int left = line.indexOf('=');
			if	(left >= 0)
			{
				property = line.substring(0, line.indexOf('=')).trim();

					value = line.substring(line.indexOf('=') + 1, line.length()).trim();

				return;
			}

			left = line.indexOf('[');
			if	(left == 0)
			{
				section   = line.substring(left + 1, line.indexOf(']')).trim();
				typeLine  = IniFile.SECTION;
				return;
			}

			if (line.trim().indexOf(';') == 0)
			{
				comment  = line.trim();
				typeLine = IniFile.COMMENT;
				return;
			}
			
			if (line.trim().equals(""))
				typeLine = IniFile.IGNORE;
			else
				System.err.println("Unrecognized line " + line);
		}
	}
}

