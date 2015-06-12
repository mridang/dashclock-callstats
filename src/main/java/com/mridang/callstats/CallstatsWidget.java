package com.mridang.callstats;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.util.Log;

import com.google.android.apps.dashclock.api.ExtensionData;

import org.acra.ACRA;

import java.util.Calendar;

/*
 * This class is the main class that provides the widget
 */
public class CallstatsWidget extends ImprovedExtension {

	/*
	 * (non-Javadoc)
	 * @see com.mridang.battery.ImprovedExtension#getIntents()
	 */
	@Override
	protected IntentFilter getIntents() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.battery.ImprovedExtension#getTag()
	 */
	@Override
	protected String getTag() {
		return getClass().getSimpleName();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.mridang.address.ImprovedExtension#getUris()
	 */
	@Override
	protected String[] getUris() {
		return new String[] {"content://call_log/calls"};
	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int intReason) {

		Log.d(getTag(), "Calculating call statistics");
		ExtensionData edtInformation = new ExtensionData();
		setUpdateWhenScreenOn(false);

		try {

			Log.d(getTag(), "Checking period that user has selected");
			Calendar calCalendar = Calendar.getInstance();
			calCalendar.set(Calendar.MINUTE, 0);
			calCalendar.set(Calendar.HOUR, 0);
			calCalendar.set(Calendar.SECOND, 0);
			calCalendar.set(Calendar.MILLISECOND, 0);
			calCalendar.set(Calendar.HOUR_OF_DAY, 0);

			switch (Integer.parseInt(getString("period", "4"))) {

			case 0: // Day
				Log.d(getTag(), "Fetch calls for the day");
				calCalendar.set(Calendar.HOUR_OF_DAY, 0);
				break;

			case 1: // Week
				Log.d(getTag(), "Fetch calls for the week");
				calCalendar.set(Calendar.DAY_OF_WEEK, calCalendar.getFirstDayOfWeek());
				break;

			case 2: // Month
				Log.d(getTag(), "Fetch calls for the month");
				calCalendar.set(Calendar.DAY_OF_MONTH, 1);
				break;

			case 3: // Year
				Log.d(getTag(), "Fetch calls for the year");
				calCalendar.set(Calendar.DAY_OF_YEAR, 1);
				break;

			default:
				Log.d(getTag(), "Fetch all calls");
				calCalendar.clear();
				break;

			}

			Log.d(getTag(), "Querying the database to get the phonecalls since " + calCalendar.getTime());
			String strClause = android.provider.CallLog.Calls.DATE + " >= ?";
			String[] strValues = { String.valueOf(calCalendar.getTimeInMillis()) };

			Cursor curCalls = getContentResolver().query(Uri.parse("content://call_log/calls"), null, strClause,
					strValues, null);

			Integer intIncoming = 0;
			Integer intTotal = 0;
			Integer intOutgoing = 0;

			while (curCalls != null && curCalls.moveToNext()) {

				switch (curCalls.getInt(curCalls.getColumnIndex(Calls.TYPE))) {

				case Calls.INCOMING_TYPE:
					intIncoming = intIncoming + curCalls.getInt(curCalls.getColumnIndex(Calls.DURATION));
					intTotal = intTotal + curCalls.getInt(curCalls.getColumnIndex(Calls.DURATION));
					break;

				case Calls.OUTGOING_TYPE:
					intOutgoing = intOutgoing + curCalls.getInt(curCalls.getColumnIndex(Calls.DURATION));
					intTotal = intTotal + curCalls.getInt(curCalls.getColumnIndex(Calls.DURATION));
					break;

				}

			}

			if (curCalls != null) {
				curCalls.close();
			}

			String strIncoming;
			String strTotal;
			String strOutgoing;

			intIncoming = intIncoming / 60;
			if (intIncoming < 60) {
				strIncoming = getQuantityString(R.plurals.minutes, intIncoming, intIncoming);
			} else {
				strIncoming = getQuantityString(R.plurals.hours, intIncoming / 60, intIncoming / 60);
				if (intIncoming % 60 > 0) {
					strIncoming = String.format(getString(R.string.and), strIncoming,
							getQuantityString(R.plurals.minutes, intIncoming % 60, intIncoming % 60));
				}
			}
			Log.v(getTag(), "Incoming : " + intIncoming);
			Log.d(getTag(), "Incoming : " + strIncoming);

			intOutgoing = intOutgoing / 60;
			if (intOutgoing < 60) {
				strOutgoing = getQuantityString(R.plurals.minutes, intOutgoing, intOutgoing);
			} else {
				strOutgoing = getQuantityString(R.plurals.hours, intOutgoing / 60, intOutgoing / 60);
				if (intOutgoing % 60 > 0) {
					strOutgoing = String.format(getString(R.string.and), strOutgoing,
							getQuantityString(R.plurals.minutes, intOutgoing % 60, intOutgoing % 60));
				}
			}
			Log.v(getTag(), "Outgoing : " + intOutgoing);
			Log.d(getTag(), "Outgoing : " + strOutgoing);

			intTotal = (intTotal - ((intIncoming % 60) + (intOutgoing % 60))) / 60;
			if (intTotal < 60) {
				strTotal = getQuantityString(R.plurals.minutes, intTotal, intTotal);
			} else {
				strTotal = getQuantityString(R.plurals.hours, intTotal / 60, intTotal / 60);
				if (intTotal % 60 > 0) {
					strTotal = String.format(getString(R.string.and), strTotal,
							getQuantityString(R.plurals.minutes, intTotal % 60, intTotal % 60));
				}
			}
			Log.v(getTag(), "Total : " + intTotal);
			Log.d(getTag(), "Total : " + strTotal);

			edtInformation.expandedBody((edtInformation.expandedBody() == null ? "" : edtInformation.expandedBody()
					+ "\n")
					+ String.format(getString(R.string.incoming), strIncoming));
			edtInformation.status(String.format(getString(R.string.total_calls), strTotal));
			edtInformation.expandedTitle(String.format(getString(R.string.total_calls), strTotal));
			edtInformation.expandedBody((edtInformation.expandedBody() == null ? "" : edtInformation.expandedBody()
					+ "\n")
					+ String.format(getString(R.string.outgoing), strOutgoing));
			edtInformation.clickIntent(new Intent(Intent.ACTION_VIEW));
			edtInformation.clickIntent().setType(CallLog.Calls.CONTENT_TYPE);
			edtInformation.visible(true);

		} catch (Exception e) {
			edtInformation.visible(false);
			Log.e(getTag(), "Encountered an error", e);
			ACRA.getErrorReporter().handleSilentException(e);
		}

		edtInformation.icon(R.drawable.ic_dashclock);
		doUpdate(edtInformation);

	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.alarmer.ImprovedExtension#onReceiveIntent(android.content.Context, android.content.Intent)
	 */
	@Override
	protected void onReceiveIntent(Context ctxContext, Intent ittIntent) {
		onUpdateData(UPDATE_REASON_MANUAL);
	}

}