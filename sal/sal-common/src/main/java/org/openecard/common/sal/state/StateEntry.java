/****************************************************************************
 * Copyright (C) 2019 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file may be used in accordance with the terms and conditions
 * contained in a signed written agreement between you and ecsec GmbH.
 *
 ***************************************************************************/

package org.openecard.common.sal.state;

import org.openecard.addon.sal.SALProtocol;


/**
 *
 * @author Tobias Wich
 */
public class StateEntry {

    private final String session;
    private SALProtocol protocol;
    private ConnectedCardEntry cardEntry;

    public StateEntry(String session) {
	this.session = session;
    }

    public String getSession() {
	return session;
    }


    public ConnectedCardEntry setConnectedCard(byte[] slotHandle, CardEntry card) {
	this.cardEntry = new ConnectedCardEntry(slotHandle, card);
	return this.cardEntry;
    }

    public void removeCard() {
	cardEntry = null;
    }

    public void setProtocol(SALProtocol protocol) {
	this.protocol = protocol;
    }

    public SALProtocol getProtocol() {
	return protocol;
    }


    public ConnectedCardEntry getCardEntry() {
	return cardEntry;
    }

}
