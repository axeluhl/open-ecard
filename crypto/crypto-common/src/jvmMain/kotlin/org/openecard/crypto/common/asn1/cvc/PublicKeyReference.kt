/****************************************************************************
 * Copyright (C) 2012 ecsec GmbH.
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
package org.openecard.crypto.common.asn1.cvc

import org.openecard.common.util.ByteUtils

/**
 * See BSI-TR-03110, version 2.10, part 3, section A.6.1.
 *
 * @author Moritz Horsch
 */
class PublicKeyReference(private val reference: ByteArray) {
    /**
     * Returns the country code.
     *
     * @return Country code
     */
    // Country Code; Encoding: ISO 3166-1 ALPHA-2; Length: 2F
    val countryCode: String

    /**
     * Returns the holder mnemonic.
     *
     * @return Holder mnemonice
     */
    // Sequence Number; Encoding: ISO/IEC 8859-1; Length: 9V
    val holderMnemonic: String

    /**
     * Returns the sequence number.
     *
     * @return Sequence number.
     */
    // Sequence Number; Encoding: ISO/IEC 8859-1; Length: 5F
    val sequenceNumber: String

    /**
     * Creates a new public key reference.
     *
     * @param reference Public key reference
     */
    constructor(reference: String) : this(reference.toByteArray())

    /**
     * Creates a new public key reference.
     *
     * @param reference Public key reference
     */
    init {
        val length = reference.size

        countryCode = String(ByteUtils.copy(reference, 0, 2))
        holderMnemonic = String(ByteUtils.copy(reference, 2, length - 7))
        sequenceNumber = String(ByteUtils.copy(reference, length - 5, 5))
    }

    /**
     * Returns the public key reference as a byte array.
     *
     * @return Byte array
     */
    fun toByteArray(): ByteArray {
        return reference
    }

    /**
     * Compares the public key reference.
     *
     * @param publicKeyReference PublicKeyReference
     * @return True if they are equal, otherwise false
     */
    fun compare(publicKeyReference: PublicKeyReference): Boolean {
        return compare(publicKeyReference.toByteArray())
    }

    /**
     * Compares the public key reference.
     *
     * @param publicKeyReference PublicKeyReference
     * @return True if they are equal, otherwise false
     */
    fun compare(publicKeyReference: ByteArray): Boolean {
        return ByteUtils.compare(reference, publicKeyReference)
    }

    /**
     * Returns the public key reference as a hex string.
     *
     * @return Hex string
     */
    fun toHexString(): String {
        return ByteUtils.toHexString(reference, true)
    }

    override fun toString(): String {
        return String(reference)
    }
}
