/****************************************************************************
 * Copyright (C) 2012-2019 ecsec GmbH.
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

import iso.std.iso_iec._24727.tech.schema.ConnectionHandleType;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticate;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticateResponse;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticationDataType;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import oasis.names.tc.dss._1_0.core.schema.Result;
import org.openecard.addon.Context;
import org.openecard.addon.sal.FunctionType;
import org.openecard.addon.sal.ProtocolStep;
import org.openecard.binding.tctoken.TR03112Keys;
import org.openecard.bouncycastle.tls.TlsServerCertificate;
import org.openecard.common.DynamicContext;
import org.openecard.common.ECardConstants;
import org.openecard.common.ECardException;
import org.openecard.common.I18n;
import org.openecard.common.ThreadTerminateException;
import org.openecard.common.WSHelper;
import org.openecard.common.anytype.AuthDataMap;
import org.openecard.common.ifd.anytype.PACEOutputType;
import org.openecard.common.interfaces.Dispatcher;
import org.openecard.common.interfaces.DispatcherException;
import org.openecard.common.util.Pair;
import org.openecard.common.util.Promise;
import org.openecard.common.util.TR03112Utils;
import org.openecard.crypto.common.asn1.cvc.CHAT;
import org.openecard.crypto.common.asn1.cvc.CHATVerifier;
import org.openecard.crypto.common.asn1.cvc.CardVerifiableCertificate;
import org.openecard.crypto.common.asn1.cvc.CardVerifiableCertificateChain;
import org.openecard.crypto.common.asn1.cvc.CardVerifiableCertificateVerifier;
import org.openecard.crypto.common.asn1.cvc.CertificateDescription;
import org.openecard.crypto.common.asn1.eac.AuthenticatedAuxiliaryData;
import org.openecard.crypto.common.asn1.eac.SecurityInfos;
import org.openecard.common.interfaces.EventDispatcher;
import org.openecard.gui.ResultStatus;
import org.openecard.gui.UserConsent;
import org.openecard.gui.UserConsentNavigator;
import org.openecard.gui.definition.UserConsentDescription;
import org.openecard.gui.executor.ExecutionEngine;
import org.openecard.ifd.protocol.pace.common.PasswordID;
import org.openecard.sal.protocol.eac.anytype.EAC1InputType;
import org.openecard.sal.protocol.eac.anytype.EAC1OutputType;
import org.openecard.sal.protocol.eac.anytype.ElementParsingException;
import org.openecard.sal.protocol.eac.gui.CHATStep;
import org.openecard.sal.protocol.eac.gui.CHATStepAction;
import org.openecard.sal.protocol.eac.gui.CVCStep;
import org.openecard.sal.protocol.eac.gui.CVCStepAction;
import org.openecard.sal.protocol.eac.gui.PINStep;
import org.openecard.sal.protocol.eac.gui.ProcessingStep;
import org.openecard.sal.protocol.eac.gui.ProcessingStepAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements PACE protocol step according to BSI TR-03112-7.
 *
 * @see "BSI-TR-03112, version 1.1.2., part 7, section 4.6.5."
 * @author Tobias Wich
 * @author Moritz Horsch
 * @author Dirk Petrautzki
 */
public class PACEStep implements ProtocolStep<DIDAuthenticate, DIDAuthenticateResponse> {

    private static final Logger LOG = LoggerFactory.getLogger(PACEStep.class.getName());

    private static final I18n LANG = I18n.getTranslation("eac");
    private static final I18n LANG_PACE = I18n.getTranslation("pace");

    // GUI translation constants
    private static final String TITLE = "eac_user_consent_title";

    private final Context addonCtx;
    private final Dispatcher dispatcher;
    private final UserConsent gui;
    private final EventDispatcher eventDispatcher;

    /**
     * Creates a new PACE protocol step.
     *
     * @param ctx Context
     */
    public PACEStep(Context ctx) {
	this.addonCtx = ctx;
	this.dispatcher = ctx.getDispatcher();
	this.gui = ctx.getUserConsent();
	this.eventDispatcher = ctx.getEventDispatcher();
    }

    @Override
    public FunctionType getFunctionType() {
	return FunctionType.DIDAuthenticate;
    }

    @Override
    public DIDAuthenticateResponse perform(DIDAuthenticate request, Map<String, Object> internalData) {
	// get context to save values in
	DynamicContext dynCtx = DynamicContext.getInstance(TR03112Keys.INSTANCE_KEY);

	DIDAuthenticate didAuthenticate = request;
	DIDAuthenticateResponse response = WSHelper.makeResponse(DIDAuthenticateResponse.class, WSHelper.makeResultOK());
	//EACProtocol.setEmptyResponseData(response);

	try {
	    EAC1InputType eac1Input = new EAC1InputType(didAuthenticate.getAuthenticationProtocolData());
	    EAC1OutputType eac1Output = eac1Input.getOutputType();

	    AuthenticatedAuxiliaryData aad = new AuthenticatedAuxiliaryData(eac1Input.getAuthenticatedAuxiliaryData());
	    PasswordID pwId = PasswordID.PIN;


	    // Certificate chain
	    CardVerifiableCertificateChain certChain = new CardVerifiableCertificateChain(eac1Input.getCertificates());
	    byte[] rawCertificateDescription = eac1Input.getCertificateDescription();
	    CertificateDescription certDescription = CertificateDescription.getInstance(rawCertificateDescription);

	    // put CertificateDescription into DynamicContext which is needed for later checks
	    dynCtx.put(TR03112Keys.ESERVICE_CERTIFICATE_DESC, certDescription);

	    // according to BSI-TR-03124-1 we MUST perform some checks immediately after receiving the eService cert
	    Result activationChecksResult = performChecks(certDescription, dynCtx);
	    if (! ECardConstants.Major.OK.equals(activationChecksResult.getResultMajor())) {
		response.setResult(activationChecksResult);
		dynCtx.put(EACProtocol.AUTHENTICATION_DONE, false);
		return response;
	    }


	    // Verify that the certificate description matches the terminal certificate
	    CardVerifiableCertificate taCert = certChain.getTerminalCertificate();
	    CardVerifiableCertificateVerifier.verify(taCert, certDescription);

	    // get CHAT values
	    CHAT taCHAT = taCert.getCHAT();
	    CHAT requiredCHAT = new CHAT(eac1Input.getRequiredCHAT());
	    CHAT optionalCHAT = new CHAT(eac1Input.getOptionalCHAT());

	    // Check that we got an authentication terminal terminal certificate. We abort the process in case there is
	    // an other role.
	    if (taCHAT.getRole() != CHAT.Role.AUTHENTICATION_TERMINAL) {
		String msg = "Unsupported terminal type in Terminal Certificate referenced. Referenced terminal type is " +
			taCHAT.getRole().toString() + ".";
		response.setResult(WSHelper.makeResultError(ECardConstants.Minor.App.INCORRECT_PARM, msg));
		dynCtx.put(EACProtocol.AUTHENTICATION_DONE, false);
		return response;
	    }

	    // enable CAN_ALLOWED in case it is set in the cert description
	    if (taCHAT.getSpecialFunctions().get(CHAT.SpecialFunction.CAN_ALLOWED)) {
		requiredCHAT.setSpecialFunctions(CHAT.SpecialFunction.CAN_ALLOWED, true);
		optionalCHAT.setSpecialFunctions(CHAT.SpecialFunction.CAN_ALLOWED, true);
		pwId = PasswordID.CAN;
	    }

	    // verify that required chat does not contain any prohibited values
	    CHATVerifier.verfiy(taCHAT, requiredCHAT);

	    // remove overlapping values from optional chat
	    optionalCHAT.restrictAccessRights(taCHAT);


	    // Prepare data in DIDAuthenticate for GUI
	    byte pinID = pwId.getByte();
	    final EACData eacData = new EACData();
	    eacData.didRequest = didAuthenticate;
	    eacData.certificate = certChain.getTerminalCertificate();
	    eacData.certificateDescription = certDescription;
	    eacData.rawCertificateDescription = rawCertificateDescription;
	    eacData.transactionInfo = eac1Input.getTransactionInfo();
	    // remove transaction info again if it is empty
	    if (eacData.transactionInfo != null && eacData.transactionInfo.trim().isEmpty()) {
		eacData.transactionInfo = null;
	    }
	    eacData.requiredCHAT = requiredCHAT;
	    eacData.optionalCHAT = optionalCHAT;
	    eacData.selectedCHAT = new CHAT(requiredCHAT.toByteArray());
	    eacData.aad = aad;
	    eacData.pinID = pinID;
	    dynCtx.put(EACProtocol.EAC_DATA, eacData);

	    // define GUI depending on the PIN status
	    final UserConsentDescription uc = new UserConsentDescription(LANG.translationForKey(TITLE));
	    uc.setDialogType("EAC");

	    // create GUI and init executor
	    CVCStep cvcStep = new CVCStep(eacData);
	    CVCStepAction cvcStepAction = new CVCStepAction(cvcStep);
	    cvcStep.setAction(cvcStepAction);
	    uc.getSteps().add(cvcStep);

	    CHATStep chatStep = new CHATStep(eacData);
	    CHATStepAction chatAction = new CHATStepAction(addonCtx, eacData, chatStep);
	    chatStep.setAction(chatAction);
	    uc.getSteps().add(chatStep);

	    uc.getSteps().add(PINStep.createDummy(pinID));
	    ProcessingStep procStep = new ProcessingStep();
	    ProcessingStepAction procStepAction = new ProcessingStepAction(procStep);
	    procStep.setAction(procStepAction);
	    uc.getSteps().add(procStep);

	    Thread guiThread = new Thread(() -> {
		// get context here because it is thread local
		DynamicContext dynCtx2 = DynamicContext.getInstance(TR03112Keys.INSTANCE_KEY);
		if (! uc.getSteps().isEmpty()) {
		    UserConsentNavigator navigator = gui.obtainNavigator(uc);
		    ExecutionEngine exec = new ExecutionEngine(navigator);
		    ResultStatus guiResult;
		    try {
			guiResult = exec.process();
		    } catch (ThreadTerminateException ex) {
			LOG.debug("GUI executer has been terminated.");
			guiResult = ResultStatus.INTERRUPTED;
		    }

		    if (guiResult == ResultStatus.CANCEL || guiResult == ResultStatus.INTERRUPTED) {
			LOG.debug("EAC GUI returned with CANCEL or INTERRUPTED.");
			dynCtx.put(EACProtocol.AUTHENTICATION_DONE, false);
			Promise<Object> paceErrorPromise = dynCtx2.getPromise(EACProtocol.PACE_EXCEPTION);
			Object paceError = paceErrorPromise.derefNonblocking();
			if (! paceErrorPromise.isDelivered()) {
			    LOG.debug("Setting PACE result to cancelled.");
			    paceErrorPromise.deliver(WSHelper.createException(WSHelper.makeResultError(
				    ECardConstants.Minor.SAL.CANCELLATION_BY_USER, "User canceled the PACE dialog.")));
			} else {
			    // determine if the error is cancel, or something else
			    boolean needsTermination = false;
			    if (paceError instanceof WSHelper.WSException) {
				WSHelper.WSException ex = (WSHelper.WSException) paceError;
				String minor = ex.getResultMinor();
				switch (minor) {
				    case ECardConstants.Minor.IFD.CANCELLATION_BY_USER:
				    case ECardConstants.Minor.SAL.CANCELLATION_BY_USER:
				    case ECardConstants.Minor.Disp.TIMEOUT:
					needsTermination = true;
				}
			    }
			    // terminate activation thread if it has not been interrupted already
			    if (needsTermination && guiResult != ResultStatus.INTERRUPTED) {
				Thread actThread = (Thread) dynCtx2.get(TR03112Keys.ACTIVATION_THREAD);
				if (actThread != null) {
				    LOG.debug("Interrupting activation thread.");
				    actThread.interrupt();
				}
			    }
			}
		    }
		}
	    }, "EAC-GUI");
	    dynCtx.put(TR03112Keys.OPEN_USER_CONSENT_THREAD, guiThread);
	    guiThread.start();

	    // wait for PACE to finish
	    Promise<Object> pPaceException = dynCtx.getPromise(EACProtocol.PACE_EXCEPTION);
	    Object pPaceError = pPaceException.deref();
	    if (pPaceError != null) {
		if (LOG.isDebugEnabled()) {
		    if (pPaceError instanceof Throwable) {
			LOG.debug("Received error object from GUI.", (Throwable) pPaceError);
		    } else {
			LOG.debug("Received error object from GUI: {}", pPaceError);
		    }
		}

		if (pPaceError instanceof WSHelper.WSException) {
		    response.setResult(((WSHelper.WSException) pPaceError).getResult());
		    return response;
		} else if (pPaceError instanceof DispatcherException | pPaceError instanceof InvocationTargetException) {
		    String msg = "Internal error while PACE authentication.";
		    Result r = WSHelper.makeResultError(ECardConstants.Minor.App.INT_ERROR, msg);
		    response.setResult(r);
		    return response;
		} else {
		    String msg = "Unknown error while PACE authentication.";
		    Result r = WSHelper.makeResultError(ECardConstants.Minor.App.UNKNOWN_ERROR, msg);
		    response.setResult(r);
		    return response;
		}
	    } else {
		LOG.debug("No error returned returned during PACE execution in GUI.");
	    }

	    // get challenge from card
	    ConnectionHandleType conHandle = (ConnectionHandleType) dynCtx.get(TR03112Keys.CONNECTION_HANDLE);
	    if (conHandle.getSlotHandle() == null) {
		throw new RuntimeException("The connection handle stored in TR03112Keys.CONNECTION_HANDLE does not have a slot handle!");
	    }
	    TerminalAuthentication ta = new TerminalAuthentication(dispatcher, conHandle.getSlotHandle());
	    byte[] challenge = ta.getChallenge();

	    // prepare DIDAuthenticationResponse
	    DIDAuthenticationDataType data = eacData.paceResponse.getAuthenticationProtocolData();
	    AuthDataMap paceOutputMap = new AuthDataMap(data);

	    //int retryCounter = Integer.valueOf(paceOutputMap.getContentAsString(PACEOutputType.RETRY_COUNTER));
	    byte[] efCardAccess = paceOutputMap.getContentAsBytes(PACEOutputType.EF_CARD_ACCESS);
	    byte[] currentCAR = paceOutputMap.getContentAsBytes(PACEOutputType.CURRENT_CAR);
	    byte[] previousCAR = paceOutputMap.getContentAsBytes(PACEOutputType.PREVIOUS_CAR);
	    byte[] idpicc = paceOutputMap.getContentAsBytes(PACEOutputType.ID_PICC);

	    // Store SecurityInfos
	    SecurityInfos securityInfos = SecurityInfos.getInstance(efCardAccess);
	    internalData.put(EACConstants.IDATA_SECURITY_INFOS, securityInfos);
	    // Store additional data
	    internalData.put(EACConstants.IDATA_AUTHENTICATED_AUXILIARY_DATA, aad);
	    internalData.put(EACConstants.IDATA_CERTIFICATES, certChain);
	    internalData.put(EACConstants.IDATA_CURRENT_CAR, currentCAR);
	    internalData.put(EACConstants.IDATA_PREVIOUS_CAR, previousCAR);
	    internalData.put(EACConstants.IDATA_CHALLENGE, challenge);

	    // Create response
	    //eac1Output.setRetryCounter(retryCounter);
	    eac1Output.setCHAT(eacData.selectedCHAT.toByteArray());
	    eac1Output.setCurrentCAR(currentCAR);
	    eac1Output.setPreviousCAR(previousCAR);
	    eac1Output.setEFCardAccess(efCardAccess);
	    eac1Output.setIDPICC(idpicc);
	    eac1Output.setChallenge(challenge);

	    response.setResult(WSHelper.makeResultOK());
	    response.setAuthenticationProtocolData(eac1Output.getAuthDataType());

	} catch (CertificateException ex) {
	    LOG.error(ex.getMessage(), ex);
	    String msg = ex.getMessage();
	    response.setResult(WSHelper.makeResultError(ECardConstants.Minor.SAL.EAC.DOC_VALID_FAILED, msg));
	    dynCtx.put(EACProtocol.AUTHENTICATION_DONE, false);
	} catch (ECardException e) {
	    LOG.error(e.getMessage(), e);
	    response.setResult(e.getResult());
	    dynCtx.put(EACProtocol.AUTHENTICATION_DONE, false);
	} catch (ElementParsingException ex) {
	    LOG.error(ex.getMessage(), ex);
	    response.setResult(WSHelper.makeResultError(ECardConstants.Minor.App.INCORRECT_PARM, ex.getMessage()));
	    dynCtx.put(EACProtocol.AUTHENTICATION_DONE, false);
	} catch (InterruptedException e) {
	    LOG.warn(e.getMessage(), e);
	    response.setResult(WSHelper.makeResultUnknownError(e.getMessage()));
	    dynCtx.put(EACProtocol.AUTHENTICATION_DONE, false);
	    Thread guiThread = (Thread) dynCtx.get(TR03112Keys.OPEN_USER_CONSENT_THREAD);
	    if (guiThread != null) {
		guiThread.interrupt();
	    }
	} catch (Exception e) {
	    LOG.error(e.getMessage(), e);
	    response.setResult(WSHelper.makeResultUnknownError(e.getMessage()));
	    dynCtx.put(EACProtocol.AUTHENTICATION_DONE, false);
	}

	return response;
    }


    private boolean convertToBoolean(Object o) {
	if (o instanceof Boolean) {
	    return ((Boolean) o);
	} else {
	    return false;
	}
    }

    /**
     * Perform all checks as described in BSI TR-03112-7 3.4.4.
     *
     * @param certDescription CertificateDescription of the eService CV Certificate
     * @param dynCtx Dynamic Context
     * @return a {@link Result} set according to the results of the checks
     */
    private Result performChecks(CertificateDescription certDescription, DynamicContext dynCtx) {
	Object tokenChecks = dynCtx.get(TR03112Keys.TCTOKEN_CHECKS);
	// omit these checks if explicitly disabled
	if (convertToBoolean(tokenChecks)) {
	    boolean checkPassed = checkEidServerCertificate(certDescription, dynCtx);
	    if (! checkPassed) {
		String msg = "Hash of eID-Server certificate is NOT contained in the CertificateDescription.";
		// TODO check for the correct minor type
		Result r = WSHelper.makeResultError(ECardConstants.Minor.SAL.PREREQUISITES_NOT_SATISFIED, msg);
		return r;
	    }

	    // perform checks according to TR-03124
	    checkPassed = checkTCTokenServerCertificates(certDescription, dynCtx);
	    if (! checkPassed) {
		String msg = "Hash of the TCToken server certificate is NOT contained in the CertificateDescription.";
		// TODO check for the correct minor type
		Result r = WSHelper.makeResultError(ECardConstants.Minor.SAL.PREREQUISITES_NOT_SATISFIED, msg);
		return r;
	    }

	    checkPassed = checkTCTokenAndSubjectURL(certDescription, dynCtx);
	    if (! checkPassed) {
		String msg = "TCToken does not come from the server to which the authorization certificate was issued.";
		// TODO check for the correct minor type
		Result r = WSHelper.makeResultError(ECardConstants.Minor.SAL.PREREQUISITES_NOT_SATISFIED, msg);
		return r;
	    }
	} else {
	    LOG.warn("Checks according to BSI TR03112 3.4.4 skipped.");
	}

	// all checks passed
	return WSHelper.makeResultOK();
    }

    private boolean checkTCTokenAndSubjectURL(CertificateDescription certDescription, DynamicContext dynCtx) {
	URL tcTokenURL = (URL) dynCtx.get(TR03112Keys.TCTOKEN_URL);
	if (tcTokenURL != null) {
	    try {
		URL subjectURL = new URL(certDescription.getSubjectURL());
		return TR03112Utils.checkSameOriginPolicy(tcTokenURL, subjectURL);
	    } catch (MalformedURLException e) {
		LOG.error("SubjectURL in CertificateDescription is not a well formed URL.");
		return false;
	    }
	} else {
	    LOG.error("No TC Token URL set in Dynamic Context.");
	    return false;
	}
    }

    private boolean checkEidServerCertificate(CertificateDescription certDescription, DynamicContext dynCtx) {
	Boolean sameChannel = (Boolean) dynCtx.get(TR03112Keys.SAME_CHANNEL);
	if (Boolean.TRUE.equals(sameChannel)) {
	    LOG.debug("eID-Server certificate is not check explicitly due to attached eID-Server case.");
	    return true;
	} else {
	    TlsServerCertificate certificate = (TlsServerCertificate) dynCtx.get(TR03112Keys.EIDSERVER_CERTIFICATE);
	    if (certificate != null) {
		return TR03112Utils.isInCommCertificates(certificate, certDescription.getCommCertificates(), "eID-Server");
	    } else {
		LOG.error("No eID-Server TLS Certificate set in Dynamic Context.");
		return false;
	    }
	}
    }

    private boolean checkTCTokenServerCertificates(CertificateDescription certDescription, DynamicContext dynCtx) {
	List<Pair<URL, TlsServerCertificate>> certificates;
	certificates = (List<Pair<URL, TlsServerCertificate>>) dynCtx.get(TR03112Keys.TCTOKEN_SERVER_CERTIFICATES);
	if (certificates != null) {
	    for (Pair<URL, TlsServerCertificate> cert : certificates) {
		if (cert instanceof Pair) {
		    URL u = cert.p1;
		    String host = u.getProtocol() + "://" + u.getHost() + (u.getPort() == -1 ? "" : (":" + u.getPort()));
		    TlsServerCertificate bcCert = cert.p2;
		    if (! TR03112Utils.isInCommCertificates(bcCert, certDescription.getCommCertificates(), host)) {
			return false;
		    }
		}
	    }
	    return true;
	} else {
	    LOG.error("No TC Token server certificates set in Dynamic Context.");
	    return false;
	}
    }



}
