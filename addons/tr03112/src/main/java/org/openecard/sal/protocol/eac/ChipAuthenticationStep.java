/****************************************************************************
 * Copyright (C) 2012-2015 ecsec GmbH.
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

package org.openecard.sal.protocol.eac;

import iso.std.iso_iec._24727.tech.schema.DIDAuthenticate;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticateResponse;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.openecard.addon.sal.FunctionType;
import org.openecard.addon.sal.ProtocolStep;
import org.openecard.binding.tctoken.TR03112Keys;
import org.openecard.common.DynamicContext;
import org.openecard.common.ECardConstants;
import org.openecard.common.WSHelper;
import org.openecard.common.interfaces.Dispatcher;
import org.openecard.common.interfaces.ObjectSchemaValidator;
import org.openecard.common.interfaces.ObjectValidatorException;
import org.openecard.common.sal.protocol.exception.ProtocolException;
import org.openecard.common.tlv.TLVException;
import org.openecard.common.util.Promise;
import org.openecard.sal.protocol.eac.anytype.EAC2OutputType;
import org.openecard.sal.protocol.eac.anytype.EACAdditionalInputType;
import org.openecard.sal.protocol.eac.anytype.ElementParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements Chip Authentication protocol step according to BSI-TR-03112-7. See BSI-TR-03112, version 1.1.2, part 7,
 * section 4.6.6.
 *
 * @author Moritz Horsch
 * @author Dirk Petrautzki
 * @author Tobias Wich
 * @author Hans-Martin Haase
 */
public class ChipAuthenticationStep implements ProtocolStep<DIDAuthenticate, DIDAuthenticateResponse> {

    private static final Logger logger = LoggerFactory.getLogger(ChipAuthenticationStep.class.getName());
    private final Dispatcher dispatcher;

    /**
     * Creates a new Chip Authentication step.
     *
     * @param dispatcher Dispatcher
     */
    public ChipAuthenticationStep(Dispatcher dispatcher) {
	this.dispatcher = dispatcher;
    }

    @Override
    public FunctionType getFunctionType() {
	return FunctionType.DIDAuthenticate;
    }

    @Override
    public DIDAuthenticateResponse perform(DIDAuthenticate didAuthenticate, Map<String, Object> internalData) {
	DIDAuthenticateResponse response = new DIDAuthenticateResponse();
	byte[] slotHandle = didAuthenticate.getConnectionHandle().getSlotHandle();
	DynamicContext dynCtx = DynamicContext.getInstance(TR03112Keys.INSTANCE_KEY);

	try {
	    ObjectSchemaValidator valid = (ObjectSchemaValidator) dynCtx.getPromise(EACProtocol.SCHEMA_VALIDATOR).deref();

	    boolean messageValid = valid.validateObject(didAuthenticate);
	    if (! messageValid) {
		String msg = "Validation of the EACAdditionalInputType message failed.";
		logger.error(msg);
		dynCtx.put(EACProtocol.AUTHENTICATION_FAILED, true);
		response.setResult(WSHelper.makeResultError(ECardConstants.Minor.App.INCORRECT_PARM, msg));
		return response;
	    }
	} catch (ObjectValidatorException ex) {
	    String msg = "Validation of the EACAdditionalInputType message failed due to invalid input data.";
	    logger.error(msg, ex);
	    dynCtx.put(EACProtocol.AUTHENTICATION_FAILED, true);
	    response.setResult(WSHelper.makeResultError(ECardConstants.Minor.App.INT_ERROR, msg));
	    return response;
	} catch (InterruptedException ex) {
	    String msg = "Thread interrupted while waiting for schema validator instance.";
	    logger.error(msg, ex);
	    dynCtx.put(EACProtocol.AUTHENTICATION_FAILED, true);
	    response.setResult(WSHelper.makeResultError(ECardConstants.Minor.App.INT_ERROR, msg));
	    return response;
	}

	try {

	    EACAdditionalInputType eacAdditionalInput = new EACAdditionalInputType(didAuthenticate.getAuthenticationProtocolData());
	    EAC2OutputType eac2Output = eacAdditionalInput.getOutputType();

	    TerminalAuthentication ta = new TerminalAuthentication(dispatcher, slotHandle);
	    ChipAuthentication ca = new ChipAuthentication(dispatcher, slotHandle);

	    // save signature, it is needed in the authentication step
	    byte[] signature = eacAdditionalInput.getSignature();
	    internalData.put(EACConstants.IDATA_SIGNATURE, signature);

	    // perform TA and CA authentication
	    AuthenticationHelper auth = new AuthenticationHelper(ta, ca);
	    eac2Output = auth.performAuth(eac2Output, internalData);

	    response.setResult(WSHelper.makeResultOK());
	    response.setAuthenticationProtocolData(eac2Output.getAuthDataType());
	} catch (ParserConfigurationException | ElementParsingException | ProtocolException | TLVException e) {
	    logger.error(e.getMessage(), e);
	    response.setResult(WSHelper.makeResultUnknownError(e.getMessage()));
	    dynCtx.put(EACProtocol.AUTHENTICATION_FAILED, true);
	}

	Promise<Object> p = (Promise<Object>) dynCtx.getPromise(TR03112Keys.PROCESSING_CANCALATION);
        if (p.derefNonblocking() == null) {
            // authentication finished, notify GUI
            dynCtx.put(EACProtocol.AUTHENTICATION_DONE, true);
            return response;
        } else {
            // authentication finished, notify GUI
            dynCtx.put(EACProtocol.AUTHENTICATION_DONE, false);
            response = new DIDAuthenticateResponse();
	    String msg = "Authentication canceled by the user.";
            response.setResult(WSHelper.makeResultError(ECardConstants.Minor.SAL.CANCELLATION_BY_USER, msg));
            return response;
        }
    }

}
