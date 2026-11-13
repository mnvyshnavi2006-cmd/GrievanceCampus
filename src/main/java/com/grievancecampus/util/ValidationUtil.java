package com.grievancecampus.util;

public final class ValidationUtil {
	private ValidationUtil() {
	}

	public static String clean(String s, int max) {
		if (s == null)
			return "";
		String v = s.trim();
		return v.length() <= max ? v : v.substring(0, max);
	}

	public static boolean required(String s) {
		return s != null && !s.trim().isEmpty();
	}

	public static boolean validEmail(String s) {
		return s != null && s.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") && s.length() <= 254;
	}

	public static boolean validCoordinate(String s, double min, double max) {
		try {
			double d = Double.parseDouble(s);
			return Double.isFinite(d) && d >= min && d <= max;
		} catch (Exception e) {
			return false;
		}
	}
}
