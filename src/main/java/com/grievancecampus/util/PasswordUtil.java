package com.grievancecampus.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {
	private PasswordUtil() {
	}

	/** Used only to keep logging in with legacy accounts created before role tables existed. */
	public static boolean verify(String password, String hash) {
		try {
			return hash != null && hash.startsWith("$2") && BCrypt.checkpw(password, hash);
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}
}
