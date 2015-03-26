/*
 *
 * This file is part of the XiPKI project.
 * Copyright (c) 2014 - 2015 Lijun Liao
 * Author: Lijun Liao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * THE AUTHOR LIJUN LIAO. LIJUN LIAO DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the XiPKI software without
 * disclosing the source code of your own applications.
 *
 * For more information, please contact Lijun Liao at this
 * address: lijun.liao@gmail.com
 */

package org.xipki.ca.qa.impl.internal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.xipki.ca.certprofile.x509.jaxb.CertificatePolicies;
import org.xipki.ca.certprofile.x509.jaxb.CertificatePolicyInformationType;
import org.xipki.common.ParamChecker;

/**
 * @author Lijun Liao
 */

public class QaCertificatePolicies extends QaExtension
{
    private final List<QaCertificatePolicyInformation> policyInformations;

    public QaCertificatePolicies(
            final CertificatePolicies jaxb)
    {
        List<CertificatePolicyInformationType> types = jaxb.getCertificatePolicyInformation();
        List<QaCertificatePolicyInformation> list = new LinkedList<>();
        for(CertificatePolicyInformationType type : types)
        {
            list.add(new QaCertificatePolicyInformation(type));
        }

        this.policyInformations = Collections.unmodifiableList(list);
    }

    public List<QaCertificatePolicyInformation> getPolicyInformations()
    {
        return policyInformations;
    }

    public QaCertificatePolicyInformation getPolicyInformation(
            final String policyId)
    {
        for(QaCertificatePolicyInformation entry : policyInformations)
        {
            if(entry.getPolicyId().equals(policyId))
            {
                return entry;
            }
        }

        return null;
    }

    public static class QaCertificatePolicyInformation
    {
        private final String policyId;
        private final QaPolicyQualifiers policyQualifiers;

        public QaCertificatePolicyInformation(
                final CertificatePolicyInformationType jaxb)
        {
            ParamChecker.assertNotNull("jaxb", jaxb);
            this.policyId = jaxb.getPolicyIdentifier().getValue();
            if(jaxb.getPolicyQualifiers() == null)
            {
                this.policyQualifiers = null;
            }else
            {
                this.policyQualifiers = new QaPolicyQualifiers(jaxb.getPolicyQualifiers());
            }
        }

        public String getPolicyId()
        {
            return policyId;
        }

        public QaPolicyQualifiers getPolicyQualifiers()
        {
            return policyQualifiers;
        }
    }

}
