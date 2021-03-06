/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.clustering.orchestration.id;

import static io.zeebe.broker.workflow.data.WorkflowInstanceEvent.PROP_STATE;

import io.zeebe.msgpack.UnpackedObject;
import io.zeebe.msgpack.property.EnumProperty;
import io.zeebe.msgpack.property.IntegerProperty;

public class IdEvent extends UnpackedObject
{

    private final EnumProperty<IdEventState> stateProp = new EnumProperty<>(PROP_STATE, IdEventState.class);

    private final IntegerProperty id = new IntegerProperty("id");

    public IdEvent()
    {
        this.declareProperty(stateProp).declareProperty(id);
    }

    public Integer getId()
    {
        return id.getValue();
    }

    public void setId(final int id)
    {
        this.id.setValue(id);
    }


    public IdEventState getState()
    {
        return stateProp.getValue();
    }

    public void setState(final IdEventState state)
    {
        stateProp.setValue(state);
    }
}
