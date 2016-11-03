/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.machine.server.jpa;

import org.eclipse.che.api.core.model.workspace.Workspace;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

/**
 * Default implementation of {@link Workspace}
 * can't be used due to circular dependency.
 *
 * @author Yevhenii Voevodin
 */
@Entity(name = "Workspace")
@Table(name = "workspace")
public class TestWorkspaceEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "accountid")
    private String accountId;

    public TestWorkspaceEntity() {}

    public TestWorkspaceEntity(String id, String accountId) {
        this.id = id;
        this.accountId = accountId;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TestWorkspaceEntity)) {
            return false;
        }
        final TestWorkspaceEntity that = (TestWorkspaceEntity)obj;
        return Objects.equals(id, that.id)
               && Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(id);
        hash = 31 * hash + Objects.hashCode(accountId);
        return hash;
    }

    @Override
    public String toString() {
        return "TestWorkspaceEntity{" +
               "id='" + id + '\'' +
               ", accountId='" + accountId + '\'' +
               '}';
    }
}