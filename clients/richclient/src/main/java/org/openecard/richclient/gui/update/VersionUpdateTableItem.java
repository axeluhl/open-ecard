/****************************************************************************
 * Copyright (C) 2018 ecsec GmbH.
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

package org.openecard.richclient.gui.update;

import javafx.scene.control.Hyperlink;


/**
 *
 * @author Sebastian Schuberth
 */
public class VersionUpdateTableItem {

    private final String version;
    private final String updateType;
    private final Hyperlink downloadLink;

    public VersionUpdateTableItem(String version, String updateType, Hyperlink downloadLink) {
	this.version = version;
	this.updateType = updateType;
	this.downloadLink = downloadLink;
    }

    public String getVersion() {
	return version;
    }

    public String getUpdateType() {
	return updateType;
    }

    public Hyperlink getDownloadLink() {
	return downloadLink;
    }

}
