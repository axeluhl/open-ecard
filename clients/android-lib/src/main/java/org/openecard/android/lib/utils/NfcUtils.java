/****************************************************************************
 * Copyright (C) 2017 ecsec GmbH.
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

package org.openecard.android.lib.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.provider.Settings;
import org.openecard.android.lib.AppContext;
import org.openecard.android.lib.async.tasks.SetTagTask;
import org.openecard.android.lib.ex.ApduExtLengthNotSupported;
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

    private AppContext ctx;
    private boolean isNFCAvailable = false;
    private boolean isNFCEnabled = false;

    public static NfcUtils getInstance() {
	synchronized (NfcUtils.class) {
	    if (nfcUtils == null) {
		nfcUtils = new NfcUtils();
	    }
	}
	return nfcUtils;
    }

    public void setAppContext(AppContext ctx) {
	this.ctx = ctx;
	if (ctx != null) {
	    this.isNFCEnabled = ctx.isNFCEnabled();
	    this.isNFCAvailable = ctx.isNFCAvailable();
	}
	LOG.debug("NFC available: " + isNFCAvailable + " - NFC enabled: " + isNFCEnabled);
    }

    public void goToNFCSettings(Activity activity) {
	Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
	activity.startActivityForResult(intent, 0);
    }

    public void enableNFCDispatch(Activity activity) {
	if (isNFCAvailable && isNFCEnabled && isContextInitialized()) {
	    LOG.debug("Enable NFC foreground dispatch...");
	    Intent activityIntent = new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, activityIntent, 0);
	    NfcAdapter.getDefaultAdapter(ctx).enableForegroundDispatch(activity, pendingIntent, null, null);
	}
    }

    public void disableNFCDispatch(Activity activity) {
	if (isNFCAvailable && isNFCEnabled && isContextInitialized()) {
	    LOG.debug("Disable NFC foreground dispatch...");
	    NfcAdapter.getDefaultAdapter(ctx).disableForegroundDispatch(activity);
	}
    }

    public void retrievedNFCTag(Intent intent) throws ApduExtLengthNotSupported {
	if (isNFCAvailable && isNFCEnabled && isContextInitialized()) {
	    Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	    if (IsoDep.get(tagFromIntent).isExtendedLengthApduSupported()) {
		SetTagTask task = new SetTagTask(tagFromIntent);
		task.execute();
	    } else {
		throw new ApduExtLengthNotSupported("APDU Extended Length is not supported.");
	    }
	}
    }

    private boolean isContextInitialized() {
	if (ctx == null) {
	    throw new IllegalStateException("Please provide an AppContext.");
	} else {
	    return ctx.isInitialized();
	}
    }

}
