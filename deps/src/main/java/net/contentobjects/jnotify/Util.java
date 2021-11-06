package net.contentobjects.jnotify;

public class Util
{
	public static String getMaskDesc(int mask)
	{
		StringBuffer s = new StringBuffer();
		if ((mask & JNotify.FILE_CREATED) != 0)
		{
			s.append("FILE_CREATED|");
		}
		if ((mask & JNotify.FILE_DELETED) != 0)
		{
			s.append("FILE_DELETED|");
		}
		if ((mask & JNotify.FILE_MODIFIED) != 0)
		{
			s.append("FILE_MODIFIED|");
		}
		if ((mask & JNotify.FILE_RENAMED) != 0)
		{
			s.append("FILE_RENAMED|");
		}
		if (s.length() > 0)
		{
			return s.substring(0, s.length() - 1);
		}
		else
		{
			return "UNKNOWN";
		}
	}
}
