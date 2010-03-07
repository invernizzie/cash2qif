package com.cash2qif.www;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    private static final String MINUS = "-";
	public static final SimpleDateFormat dateFormatter = new SimpleDateFormat("MM-dd-yyyy");

	/**
	 * Quicken imports positive amounts as debits and negative amounts
	 * as credits.  Convert between the two so don't have to hit a negative
	 * sign before each debit. 
	 * @param builder
	 * @param amount
	 */
	public static void invertSign(StringBuilder builder, String amount) {
		if (amount != null && amount.length() > 0) {
			if (!MINUS.equals(amount.substring(0, 1))) {
				builder.append(MINUS); // debit, so prepend a minus
				builder.append(amount);
			} else builder.append(amount.substring(1, amount.length())); // credit, so remove minus
		}
	}

	/**
	 * Given a date as a String (whether formatted or as a dateTime)
	 * and return a dateTime as a long.
	 * @param dateString
	 * @return
	 */
	public static long parseDateString(String dateString) {
		long dateTime = -1;
		if (dateString.contains("-")) {
			Date date;
			try {
				date = dateFormatter.parse(dateString);
				dateTime = date.getTime();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else dateTime = Long.parseLong(dateString);
		return dateTime;
	}

	/**
	 * Given a dateTime, return a date as a formatted String.
	 * @param dateTime
	 * @return
	 */
	public static String formatDateTime(Long dateTime) {
		Date date = new Date();
		date.setTime(dateTime);
		return dateFormatter.format(date);
	}
}