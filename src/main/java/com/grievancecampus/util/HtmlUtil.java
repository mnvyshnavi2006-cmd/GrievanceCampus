package com.grievancecampus.util;
/** Escapes database/user text before it is written into JSP HTML. */
public final class HtmlUtil {
 private HtmlUtil() { }
 public static String escape(Object value) { if(value==null)return "";String s=String.valueOf(value);return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
}
