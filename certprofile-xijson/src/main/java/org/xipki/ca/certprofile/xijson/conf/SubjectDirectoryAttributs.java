/*
 *
 * Copyright (c) 2013 - 2020 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.ca.certprofile.xijson.conf;

import org.xipki.ca.certprofile.xijson.conf.Describable.DescribableOid;
import org.xipki.util.exception.InvalidConfException;
import org.xipki.util.ValidatableConf;

import java.util.LinkedList;
import java.util.List;

/**
 * Extension SubjectDirectoryAttributs.
 *
 * @author Lijun Liao
 */

public class SubjectDirectoryAttributs extends ValidatableConf {

  private List<DescribableOid> types;

  public List<DescribableOid> getTypes() {
    if (types == null) {
      types = new LinkedList<>();
    }
    return types;
  }

  public void setTypes(List<DescribableOid> types) {
    this.types = types;
  }

  @Override
  public void validate()
      throws InvalidConfException {
    notEmpty(types, "types");
    validate(types);
  }

} // class SubjectDirectoryAttributs
