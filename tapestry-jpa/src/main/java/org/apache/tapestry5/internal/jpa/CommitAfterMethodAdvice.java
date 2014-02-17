// Copyright 2011, 2012, 2014 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry5.internal.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;

import org.apache.tapestry5.jpa.EntityManagerManager;
import org.apache.tapestry5.jpa.annotations.CommitAfter;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;

public class CommitAfterMethodAdvice implements MethodAdvice
{
    private final EntityManagerManager manager;

    public CommitAfterMethodAdvice(final EntityManagerManager manager)
    {
        this.manager = manager;
    }

    public void advise(final MethodInvocation invocation)
    {
    	
    	if (invocation.hasAnnotation(CommitAfter.class))
    	{
    	
			final PersistenceContext annotation = invocation.getAnnotation(PersistenceContext.class);
	        final EntityTransaction transaction = getTransaction(annotation);
	
	        if (transaction != null && !transaction.isActive())
	        {
	            transaction.begin();
	        }
	
	        try
	        {
	            invocation.proceed();
	        } catch (final RuntimeException e)
	        {
	            if (transaction != null && transaction.isActive())
	            {
	                rollbackTransaction(transaction);
	            }
	
	            throw e;
	        }
	
	        // Success or checked exception:
	
	        if (transaction != null && transaction.isActive())
	        {
	            transaction.commit();
	        }
	        
    	}
    	else
    	{
    		invocation.proceed();
    	}

    }

    private void rollbackTransaction(EntityTransaction transaction)
    {
        try
        {
            transaction.rollback();
        } catch (Exception e)
        { // Ignore
        }
    }

    private EntityTransaction getTransaction(PersistenceContext annotation)
    {
        EntityManager em = JpaInternalUtils.getEntityManager(manager, annotation);

        if (em == null)
            return null;

        return em.getTransaction();
    }

}
