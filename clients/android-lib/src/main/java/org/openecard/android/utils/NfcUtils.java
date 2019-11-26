/****************************************************************************
 * Copyright (C) 2017-2019 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of the Open eCard App.
 *
 * GNU General Public License Usage
 * This file may be used under the terms of the GNU General Public
 * License version 3.0 as published by the Free Software Foundation
 * and appearing in the file LICENSE.GPL included in the packaging of
 * this file. Please review the following information to ensure the
 * GNU General Public License version 3.0 requirements will be met:
 * http://www.gnu.org/copyleft/gpl.html.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package org.openecard.android.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.tech.TagTechnology;
import android.os.Build;
import android.provider.Settings;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.openecard.mobile.activation.NfcCapabilityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides methods to enable/disable the nfc dispatch or to jump to the nfc settings, ...
 *
 * @author Mike Prechtl
 */
public class NfcUtils {

    private static final Logger LOG = LoggerFactory.getLogger(NfcUtils.class);

    private static NfcUtils nfcUtils;

    private Context context;
    private NfcAdapter adapter;

    public static NfcUtils getInstance() {
	synchronized (NfcUtils.class) {
	    if (nfcUtils == null) {
		nfcUtils = new NfcUtils();
	    }
	}
	return nfcUtils;
    }

    /**
     * This method opens the nfc settings on the corresponding device. Now, the user can enable nfc.
     *
     * @param activity
     */
    public void goToNFCSettings(Activity activity) {
	Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
	activity.startActivityForResult(intent, 0);
    }

    public void enableNFCDispatch(Activity activity) {
	if (isNfcEnabled(activity)) {
	    LOG.debug("Enable NFC foreground dispatch...");
	    Intent activityIntent = new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, activityIntent, 0);
	    // enable dispatch of messages with nfc tag
	    NfcAdapter.getDefaultAdapter(activity).enableForegroundDispatch(activity, pendingIntent, null, null);
	}
    }

    public void disableNFCDispatch(Activity activity) {
	if (isNfcEnabled(activity)) {
	    LOG.debug("Disable NFC foreground dispatch...");
	    // disable dispatch of messages with nfc tag
	    NfcAdapter.getDefaultAdapter(activity).disableForegroundDispatch(activity);
	}
    }

    public static boolean isNfcEnabled(Context ctx) {
	setContext(ctx);
	return getInstance().isNFCEnabled();
    }

    public static boolean isNfcAvailable(Context ctx) {
	setContext(ctx);
	return getInstance().isNFCAvailable();
    }

    public static void setContext(Context ctx) {
	final NfcUtils instance = getInstance();
	instance.context = ctx;
	instance.adapter = null;
    }

    @Deprecated
    public static boolean supportsExtendedLength(Context context) {
	NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
	if (nfcAdapter != null) {
	    Object tagObj = getTagObject(nfcAdapter);
	    if (tagObj != null) {
		Boolean extSup = isExtendedLengthSupported(tagObj);
		if (extSup != null) {
		    return extSup;
		}
		Integer maxLen = getMaxTransceiveLength(tagObj);
		if (maxLen != null) {
		    return maxLen > 370; // This is roughly the size of the biggest APDU observed in EAC
		}
		LOG.info("maxLen = {} ; extSup = {}", maxLen, extSup);
	    }
	} else {
	    LOG.warn("NfcAdapter is not available.");
	}

	return false;
    }

    /**
     * Checks the support of extended length APDUs of the system.
     * <p>This function uses non public API in order to work without a card insert intent. As this is forbidden since
     * API 28, this function returns {@link NfcCapabilityResult#QUERY_NOT_ALLOWED} when the system runs on API level 28
     * or higher.</p>
     *
     * @param context
     * @return Result of the extended length determination.
     */
    public static NfcCapabilityResult checkExtendedLength(Context context) {
	if (canUseHiddenApi()) {
	    NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
	    if (nfcAdapter != null) {
		Object tagObj = getTagObject(nfcAdapter);
		if (tagObj != null) {
		    Boolean extSup = isExtendedLengthSupported(tagObj);
		    Integer maxLen = getMaxTransceiveLength(tagObj);
		    LOG.info("maxLen = {} ; extSup = {}", maxLen, extSup);
		    if (extSup != null) {
			return extSup ? NfcCapabilityResult.SUPPORTED : NfcCapabilityResult.NOT_SUPPORTED;
		    } else if (maxLen != null) {
			// This is roughly the size of the biggest APDU observed in EAC
			return maxLen > 370 ? NfcCapabilityResult.SUPPORTED : NfcCapabilityResult.NOT_SUPPORTED;
		    }
		}

		// not values determined, assume not supported for some other reason
		return NfcCapabilityResult.NOT_SUPPORTED;
	    } else {
		LOG.warn("NfcAdapter is not available.");
		return NfcCapabilityResult.NFC_SYSTEM_DISABLED;
	    }
	} else {
	    return NfcCapabilityResult.QUERY_NOT_ALLOWED;
	}
    }

    private static boolean canUseHiddenApi() {
	// we can only request this API, when we are < Android 9 (API 28)
	return Build.VERSION.SDK_INT < 28;
    }

    private static Object getTagObject(NfcAdapter nfcAdapter) {
	try {
	    Method getTagFun = nfcAdapter.getClass().getMethod("getTagService");
	    Object tagObj = getTagFun.invoke(nfcAdapter);
	    return tagObj;
	} catch (NoSuchMethodException ex) {
	    LOG.error("Error requesting TagService retrieval method.", ex);
	} catch (SecurityException | IllegalAccessException ex) {
	    LOG.error("Requesting TagService object is not allowed.");
	} catch (InvocationTargetException ex) {
	    LOG.error("Error requesting TagService object.", ex);
	}

	return null;
    }

    private static Integer getMaxTransceiveLength(Object tagObj) {
	int tech = 3; // taken from Android source and used as fallback if lookup fails
	try {
	    Field isoDep = TagTechnology.class.getDeclaredField("ISO_DEP");
	    tech = isoDep.getInt(null);
	} catch (NoSuchFieldException ex) {
	    LOG.error("Error requesting ISO_DEP field.", ex);
	} catch (SecurityException | IllegalAccessException ex) {
	    LOG.error("Requesting ISO_DEP tech constant is not allowed.");
	} catch (NullPointerException | IllegalArgumentException ex) {
	    LOG.error("Invalid parameters for requesting ISO_DEP tech constant.", ex);
	}

	try {
	    Method tlenFun = tagObj.getClass().getMethod("getMaxTransceiveLength", int.class);
	    Object lenObj = tlenFun.invoke(tagObj, tech);
	    LOG.debug("Transceive Length == {}", lenObj);
	    if (lenObj instanceof Integer) {
		return (Integer) lenObj;
	    }
	} catch (NoSuchMethodException ex) {
	    LOG.debug("Error requesting max transceive length retrieval method.", ex);
	} catch (SecurityException | IllegalAccessException ex) {
	    LOG.debug("Requesting max transceive length is not allowed.");
	} catch (InvocationTargetException ex) {
	    LOG.debug("Error requesting max transceive length.", ex);
	}

	return null;
    }

    private static Boolean isExtendedLengthSupported(Object tagObj) {
	try {
	    Method extSupFun = tagObj.getClass().getMethod("getExtendedLengthApdusSupported");
	    Object extSupObj = extSupFun.invoke(tagObj);
	    LOG.debug("Extended Length Support == {}", extSupObj);
	    if (extSupObj instanceof Boolean) {
		return (Boolean) extSupObj;
	    }
	} catch (NoSuchMethodException ex) {
	    LOG.debug("Error requesting extended length support retrieval method.", ex);
	} catch (SecurityException | IllegalAccessException ex) {
	    LOG.debug("Requesting extended length support is not allowed.");
	} catch (InvocationTargetException ex) {
	    LOG.debug("Error requesting extended length support.", ex);
	}

	return null;
    }
    /**
     * Proof if NFC is available on the corresponding device.
     *
     * @return true if nfc is available, otherwise false
     */
    public static boolean isNFCAvailable() {
       return getInstance().getNFCAdapter() != null;
    }

    /**
     * Proof if NFC is enabled on the corresponding device. If this method return {@code false} nfc should be activated
     * in the device settings.
     *
     * @return true if nfc is enabled, otherwise false
     */
    public static boolean isNFCEnabled() {
       return getInstance().getNFCAdapter() != null ? getInstance().getNFCAdapter().isEnabled() : false;
    }

    /**
     * Return the adapter for NFC.
     *
     * @return nfc adapter.
     */
    public NfcAdapter getNFCAdapter() {
       if (adapter == null) {
           LOG.info("Try to create new NFCAdapter...");
           NfcManager nfcManager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
           adapter = nfcManager.getDefaultAdapter();
       }
       return adapter;
    }

}
