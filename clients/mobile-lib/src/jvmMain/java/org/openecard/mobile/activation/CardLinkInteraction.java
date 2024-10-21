/****************************************************************************
 * Copyright (C) 2024 ecsec GmbH.
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

package org.openecard.mobile.activation;

import org.openecard.robovm.annotations.FrameworkInterface;

@FrameworkInterface
public interface CardLinkInteraction extends ActivationInteraction {
	void onCanRequest(ConfirmPasswordOperation enterCan);
	void onCanRetry(ConfirmPasswordOperation enterCan, ErrorCodes.CardLinkErrorCodes.ClientCodes resultCode, String errorMessage);
	void onPhoneNumberRequest(ConfirmTextOperation enterPhoneNumber);
	void onPhoneNumberRetry(ConfirmTextOperation enterPhoneNumber, ErrorCodes.CardLinkErrorCodes.CardLinkCodes resultCode, String errorMessage);
	void onSmsCodeRequest(ConfirmPasswordOperation smsCode);
	void onSmsCodeRetry(ConfirmPasswordOperation smsCode, ErrorCodes.CardLinkErrorCodes.CardLinkCodes resultCode, String errorMessage);
}
