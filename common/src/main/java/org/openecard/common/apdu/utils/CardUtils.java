/****************************************************************************
 * Copyright (C) 2012-2013 ecsec GmbH.
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
package org.openecard.common.apdu.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.openecard.common.apdu.ReadBinary;
import org.openecard.common.apdu.ReadRecord;
import org.openecard.common.apdu.Select;
import org.openecard.common.apdu.Select.MasterFile;
import org.openecard.common.apdu.UpdateRecord;
import org.openecard.common.apdu.common.CardCommandAPDU;
import org.openecard.common.apdu.common.CardCommandStatus;
import org.openecard.common.apdu.common.CardResponseAPDU;
import org.openecard.common.apdu.exception.APDUException;
import org.openecard.common.interfaces.Dispatcher;
import org.openecard.common.tlv.TLVException;
import org.openecard.common.tlv.iso7816.DataElements;
import org.openecard.common.tlv.iso7816.FCP;
import org.openecard.common.util.ShortUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Moritz Horsch <horsch@cdc.informatik.tu-darmstadt.de>
 * @author Dirk Petrautzki <petrautzki@hs-coburg.de>
 * @author Simon Potzernheim <potzernheim@hs-coburg.de>
 */
public class CardUtils {

    private static final Logger logger = LoggerFactory.getLogger(CardUtils.class);

    public static final int NO_RESPONSE_DATA = 0;
    public static final int FCP_RESPONSE_DATA = 1;
    public static final int FCI_RESPONSE_DATA = 2;
    public static final int FMD_RESPONSE_DATA = 3;

    /**
     * Selects the Master File.
     *
     * @param dispatcher Dispatcher
     * @param slotHandle Slot handle
     * @throws APDUException
     */
    public static void selectMF(Dispatcher dispatcher, byte[] slotHandle) throws APDUException {
	CardCommandAPDU selectMF = new Select.MasterFile();
	selectMF.transmit(dispatcher, slotHandle);
    }

    /**
     * Selects a File.
     *
     * @param dispatcher Dispatcher
     * @param slotHandle Slot handle
     * @param fileID File ID
     * @return The CardResponseAPDU from the selection of the file
     * @throws APDUException
     */
    public static CardResponseAPDU selectFile(Dispatcher dispatcher, byte[] slotHandle, short fileID) throws APDUException {
	return selectFile(dispatcher, slotHandle, ShortUtils.toByteArray(fileID));
    }

    /**
     * Selects a File.
     *
     * @param dispatcher Dispatcher
     * @param slotHandle Slot handle
     * @param fileID File ID
     * @return CardREsponseAPDU containing the File Control Parameters
     * @throws APDUException
     */
    public static CardResponseAPDU selectFile(Dispatcher dispatcher, byte[] slotHandle, byte[] fileID) throws APDUException {
	return selectFileWithOptions(dispatcher, slotHandle, fileID, null, NO_RESPONSE_DATA);
    }

    /**
     * Select a file with different options.
     *
     * @param dispatcher The Dispatcher for dispatching of the card commands.
     * @param slotHandle The SlotHandle which identifies the card terminal.
     * @param fileIdOrPath File identifier or path to the file to select.
     * @param responses List of byte arrays with the trailers which should not thrown as errors.
     * @param resultType Int value which indicates whether the select should be performed with a request of the FCP, FCI,
     * FMD or without any data. There are four public variables available in this class to use.
     * @return A CardResponseAPDU object with the requested response data.
     * @throws APDUException Thrown if the selection of a file failed.
     */
    public static CardResponseAPDU selectFileWithOptions(Dispatcher dispatcher, byte[] slotHandle, byte[] fileIdOrPath,
	    List<byte[]> responses, int resultType) throws APDUException {
	Select selectFile;
	CardResponseAPDU result = null;

	// respect the possibility that fileID could be a path
	int i = 0;
	while (i < fileIdOrPath.length) {
	    if (fileIdOrPath[i] == (byte) 0x3F && fileIdOrPath[i + 1] == (byte) 0x00 && i == 0 && i + 1 == 1) {
		selectFile = new MasterFile();
		i = i + 2;
	    } else if (i == fileIdOrPath.length - 2) {
		selectFile = new Select.ChildFile(new byte[]{fileIdOrPath[i], fileIdOrPath[i + 1]});
		switch(resultType) {
		    case 0:
			// do nothing except of break 0x0C is the initialization value of P2
			break;
		    case 1:
			selectFile.setFCP();
			break;
		    case 2:
			selectFile.setFCI();
			break;
		    case 3:
			selectFile.setFMD();
			break;
		    default:
			throw new APDUException("There is no value assoziated with the returnType value " + resultType);
		}

		i = i + 2;
	    } else {
		selectFile = new Select.ChildDirectory(new byte[]{fileIdOrPath[i], fileIdOrPath[i + 1]});
		i = i + 2;
	    }

	    if (responses == null) {
		result = selectFile.transmit(dispatcher, slotHandle);
	    } else {
		result = selectFile.transmit(dispatcher, slotHandle, responses);
	    }

	}

	return result;
    }

    /**
     * Select an application by it's file identifier.
     * 
     * @param dispatcher The message dispatcher for the interaction with the card.
     * @param slotHandle
     * @param fileID File identitfier of an application or a path to the application.
     * @return The {@link CardResponseAPDU} from the last select which means the select of the application to select.
     * @throws APDUException 
     */
    public static CardResponseAPDU selectApplicationByFID(Dispatcher dispatcher, byte[] slotHandle, byte[] fileID) throws APDUException {
	Select selectApp;
	CardResponseAPDU result = null;
	
	// respect the possibility that fileID could be a path
	int i = 0;
	while (i < fileID.length) {
	    if (fileID[i] == (byte) 0x3F && fileID[i + 1] == (byte) 0x00 && i == 0 && i + 1 == 1) {
		selectApp = new MasterFile();
		i = i + 2;
	    } else {
		selectApp = new Select.ChildDirectory(new byte[]{fileID[i], fileID[i + 1]});
		selectApp.setLE((byte) 0xFF);
		selectApp.setFCP();
		i = i + 2;
	    }

	    result = selectApp.transmit(dispatcher, slotHandle);
	}

	return result;
    }
    
    /**
     * Select an application by the application identifier.
     * This method requests the FCP of the application.
     * 
     * @param dispatcher
     * @param slotHandle
     * @param aid Application identifier
     * @return
     * @throws APDUException 
     */
    public static CardResponseAPDU selectApplicationByAID(Dispatcher dispatcher, byte[] slotHandle, byte[] aid) throws APDUException {
	Select selectApp = new Select((byte) 0x04, (byte) 0x04);
	selectApp.setData(aid);
	selectApp.setLE((byte) 0xFF);
	CardResponseAPDU result = selectApp.transmit(dispatcher, slotHandle);
	return result;
    }

    /**
     * Reads a file.
     *
     * @param dispatcher Dispatcher
     * @param slotHandle Slot handle
     * @param fcp File Control Parameters
     * @return File content
     * @throws APDUException
     */
    public static byte[] readFile(FCP fcp, Dispatcher dispatcher, byte[] slotHandle) throws APDUException {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	// Read 255 bytes per APDU
	byte length = (byte) 0xFF;
	boolean isRecord = isRecordEF(fcp);
	int i = isRecord ? 1 : 0; // records start at index 1

	try {
	    CardResponseAPDU response;
	    do {
		if (! isRecord) {
		    CardCommandAPDU readBinary = new ReadBinary((short) (i * (length & 0xFF)), length);
		    // 0x6A84 code for the estonian identity card. The card returns this code
		    // after the last read process.
		    response = readBinary.transmit(dispatcher, slotHandle, CardCommandStatus.response(0x9000, 0x6282,
			    0x6A84, 0x6A83, 0x6A86));
		} else {
		    CardCommandAPDU readRecord = new ReadRecord((byte) i);
		    response = readRecord.transmit(dispatcher, slotHandle, CardCommandStatus.response(0x9000, 0x6282,
			    0x6A84, 0x6A83));
		}

		if (! Arrays.equals(response.getTrailer(), new byte[] {(byte) 0x6A, (byte) 0x84}) &&
			! Arrays.equals(response.getTrailer(), new byte[] {(byte) 0x6A, (byte) 0x83}) &&
			! Arrays.equals(response.getTrailer(), new byte[] {(byte) 0x6A, (byte) 0x86})) {
		    baos.write(response.getData());
		}
		i++;
	    } while (response.isNormalProcessed() ||
		    (Arrays.equals(response.getTrailer(), new byte[] {(byte) 0x62, (byte) 0x82}) && isRecord));
	    baos.close();
	} catch (IOException e) {
	    throw new APDUException(e);
	}

	return baos.toByteArray();
    }

    /**
     * Selects and reads a file.
     *
     * @param dispatcher Dispatcher
     * @param slotHandle Slot handle
     * @param fileID File ID
     * @return File content
     * @throws APDUException
     */
    @Deprecated
    public static byte[] readFile(Dispatcher dispatcher, byte[] slotHandle, short fileID) throws APDUException {
	return readFile(dispatcher, slotHandle, ShortUtils.toByteArray(fileID));
    }

    /**
     * Selects and reads a file.
     *
     * @param dispatcher Dispatcher
     * @param slotHandle Slot handle
     * @param fileID File ID
     * @return File content
     * @throws APDUException
     */
    @Deprecated
    public static byte[] readFile(Dispatcher dispatcher, byte[] slotHandle, byte[] fileID) throws APDUException {
	CardResponseAPDU selectResponse = selectFile(dispatcher, slotHandle, fileID);
	FCP fcp = null;
	try {
	    fcp = new FCP(selectResponse.getData());
	} catch (TLVException e) {
	    logger.warn("Couldn't get File Control Parameters from Select response.", e);
	}
	return readFile(fcp, dispatcher, slotHandle);
    }

    public static byte[] selectReadFile(Dispatcher dispatcher, byte[] slotHandle, short fileID) throws APDUException {
	return readFile(dispatcher, slotHandle, fileID);
    }

    public static byte[] selectReadFile(Dispatcher dispatcher, byte[] slotHandle, byte[] fileID) throws APDUException {
	return readFile(dispatcher, slotHandle, fileID);
    }

    private static boolean isRecordEF(FCP fcp) {
	if (fcp == null) {
	    // TODO inspect EF.ATR as described in ISO/IEC 7816-4 Section 8.4
	    return false;
	} else {
	    DataElements dataElements = fcp.getDataElements();
	    if (dataElements.isLinear() || dataElements.isCyclic()) {
		return true;
	    } else {
		return false;
	    }
	}
    }

    public static void writeFile(Dispatcher dispatcher, byte[] slotHandle, byte[] fileID, byte[] data) throws APDUException {
	CardResponseAPDU selectResponse = selectFile(dispatcher, slotHandle, fileID);
	FCP fcp = null;
	try {
	    fcp = new FCP(selectResponse.getData());
	} catch (TLVException e) {
	    logger.warn("Couldn't get File Control Parameters from Select response.", e);
	}
	writeFile(fcp, dispatcher, slotHandle, data);
    }

    private static void writeFile(FCP fcp, Dispatcher dispatcher, byte[] slotHandle, byte[] data) throws APDUException {
	if (isRecordEF(fcp)) {
	    UpdateRecord updateRecord = new UpdateRecord(data);
	    updateRecord.transmit(dispatcher, slotHandle);
	} else {
	    // TODO implement writing for non record files
	    throw new UnsupportedOperationException("Not yet implemented.");
	}
    }

}
