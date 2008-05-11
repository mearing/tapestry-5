// Copyright 2007 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry.integration.app1.pages;

import org.apache.tapestry.annotations.Component;
import org.apache.tapestry.corelib.components.Form;
import org.apache.tapestry.integration.app1.data.ToDoItem;
import org.apache.tapestry.integration.app1.services.ToDoDatabase;
import org.apache.tapestry.ioc.annotations.Inject;

import java.util.List;

public class ToDoListVolatile
{
    @Inject
    private ToDoDatabase database;

    private ToDoItem item;

    private List<ToDoItem> items;

    @Component
    private Form form;

    public List<ToDoItem> getItems()
    {
        return items;
    }

    public ToDoItem getItem()
    {
        return item;
    }

    public void setItem(ToDoItem item)
    {
        this.item = item;
    }

    public ToDoDatabase getDatabase()
    {
        return database;
    }

    void onPrepare()
    {
        items = database.findAll();
    }

    void onSuccess()
    {
        int order = 0;

        for (ToDoItem item : items)
        {
            item.setOrder(order++);
            database.update(item);
        }
    }

    void onSelectedFromAddNew()
    {
        if (form.isValid())
        {
            ToDoItem item = new ToDoItem();
            item.setTitle("<New To Do>");
            item.setOrder(items.size());

            database.add(item);
        }
    }

    void onActionFromReset()
    {
        database.reset();
    }
}
