/****************************************************************************
 * Copyright (C) 2015-2024 ecsec GmbH.
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
package org.openecard.ws.jaxb

import java.util.Comparator

/**
 * Comparator implementation using the full name of the classes to compare the difference.
 *
 * @author Tobias Wich
 */
class ClassComparator : Comparator<Class<*>> {
    override fun compare(c1: Class<*>, c2: Class<*>): Int {
        val n1: String = c1.getName()
        val n2: String = c2.getName()
        return n1.compareTo(n2)
    }
}
