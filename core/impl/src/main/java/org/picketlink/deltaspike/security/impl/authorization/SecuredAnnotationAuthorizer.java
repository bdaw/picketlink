/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.picketlink.deltaspike.security.impl.authorization;

import org.picketlink.deltaspike.Secures;
import org.picketlink.deltaspike.core.api.provider.BeanProvider;
import org.picketlink.deltaspike.security.api.authorization.AccessDecisionState;
import org.picketlink.deltaspike.security.api.authorization.AccessDecisionVoter;
import org.picketlink.deltaspike.security.api.authorization.AccessDecisionVoterContext;
import org.picketlink.deltaspike.security.api.authorization.AccessDeniedException;
import org.picketlink.deltaspike.security.api.authorization.SecurityViolation;
import org.picketlink.deltaspike.security.api.authorization.annotations.Secured;
import org.picketlink.deltaspike.security.spi.authorization.EditableAccessDecisionVoterContext;
import org.picketlink.deltaspike.security.impl.util.SecurityUtils;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Authorizer implementation for the {@link @Secured} annotation
 */
@Dependent
@SuppressWarnings("UnusedDeclaration")
public class SecuredAnnotationAuthorizer
{
    @Inject
    private AccessDecisionVoterContext voterContext;

    @Secures @Secured({ })
    @SuppressWarnings("UnusedDeclaration")
    public boolean doSecuredCheck(InvocationContext invocationContext) throws Exception
    {
        Secured secured = null;

        List<Annotation> annotatedTypeMetadata = extractMetadata(invocationContext);

        for (Annotation annotation : annotatedTypeMetadata)
        {
            if (Secured.class.isAssignableFrom(annotation.annotationType()))
            {
                secured = (Secured) annotation;
            }
            else if (voterContext instanceof EditableAccessDecisionVoterContext)
            {
                ((EditableAccessDecisionVoterContext) voterContext)
                        .addMetaData(annotation.annotationType().getName(), annotation);
            }
        }

        if (secured != null)
        {
            Class<? extends AccessDecisionVoter>[] voterClasses = secured.value();

            invokeVoters(invocationContext, Arrays.asList(voterClasses));
        }

        //needed by @SecurityBindingType
        //X TODO check the use-cases for it
        return true;
    }

    private List<Annotation> extractMetadata(InvocationContext invocationContext)
    {
        List<Annotation> result = new ArrayList<Annotation>();

        Method method = invocationContext.getMethod();

        result.addAll(SecurityUtils.getAllAnnotations(method.getAnnotations()));
        result.addAll(SecurityUtils.getAllAnnotations(method.getDeclaringClass().getAnnotations()));

        return result;
    }

    /**
     * Helper for invoking the given {@link AccessDecisionVoter}s
     *
     * @param invocationContext    current invocation-context (might be null in case of secured views)
     * @param accessDecisionVoters current access-decision-voters
     */
    private void invokeVoters(InvocationContext invocationContext,
                              List<Class<? extends AccessDecisionVoter>> accessDecisionVoters)
    {
        if (accessDecisionVoters == null)
        {
            return;
        }

        AccessDecisionState voterState = AccessDecisionState.VOTE_IN_PROGRESS;
        try
        {
            if (voterContext instanceof EditableAccessDecisionVoterContext)
            {
                ((EditableAccessDecisionVoterContext) voterContext).setState(voterState);
                ((EditableAccessDecisionVoterContext) voterContext).setSource(invocationContext);
            }

            Set<SecurityViolation> violations;

            AccessDecisionVoter voter;
            for (Class<? extends AccessDecisionVoter> voterClass : accessDecisionVoters)
            {
                voter = BeanProvider.getContextualReference(voterClass, false);

                violations = voter.checkPermission(voterContext);

                if (violations != null && violations.size() > 0)
                {
                    if (voterContext instanceof EditableAccessDecisionVoterContext)
                    {
                        voterState = AccessDecisionState.VIOLATION_FOUND;
                        for (SecurityViolation securityViolation : violations)
                        {
                            ((EditableAccessDecisionVoterContext) voterContext).addViolation(securityViolation);
                        }
                    }
                    throw new AccessDeniedException(violations);
                }
            }
        }
        finally
        {
            if (voterContext instanceof EditableAccessDecisionVoterContext)
            {
                if (AccessDecisionState.VOTE_IN_PROGRESS.equals(voterState))
                {
                    voterState = AccessDecisionState.NO_VIOLATION_FOUND;
                }

                ((EditableAccessDecisionVoterContext) voterContext).setState(voterState);
            }
        }
    }
}
