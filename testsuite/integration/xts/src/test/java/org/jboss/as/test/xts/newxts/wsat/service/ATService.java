/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.xts.newxts.wsat.service;

import org.jboss.as.test.xts.newxts.base.TestApplicationException;
import org.jboss.as.test.xts.newxts.util.EventLog;
import org.jboss.as.test.xts.newxts.util.ServiceCommand;
import static org.jboss.as.test.xts.newxts.util.ServiceCommand.*;
import org.jboss.logging.Logger;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.mw.wst11.TransactionManager;
import com.arjuna.mw.wst11.TransactionManagerFactory;
import com.arjuna.mw.wst11.UserTransaction;
import com.arjuna.mw.wst11.UserTransactionFactory;
import com.arjuna.wst.UnknownTransactionException;
import com.arjuna.wst.WrongStateException;

import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.servlet.annotation.WebServlet;


@WebService(serviceName = "ATService", portName = "AT", name = "AT", targetNamespace = "http://www.jboss.com/jbossas/test/xts/wsat/at/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@HandlerChain(file = "/context-handlers.xml")
@WebServlet(name="ATService", urlPatterns={"/ATService"})
public class ATService {
    private static final Logger log = Logger.getLogger(ATService.class);
    private final EventLog eventLog = new EventLog();
    
    /**
     * Adding 2 participants - Volatile and Durable
     * @throws WrongStateException 
     * @throws com.arjuna.wst.SystemException 
     * @throws UnknownTransactionException 
     * @throws SecurityException 
     * @throws javax.transaction.SystemException 
     * @throws IllegalStateException 
     */
    @WebMethod
    public void invoke(String callName, ServiceCommand[] serviceCommands) throws TestApplicationException {
        
        log.infof("[AT SERVICE] web method invoke(%s)", callName);
        eventLog.foundEventLogName(callName);
        UserTransaction userTransaction;
        
        try {
            userTransaction = UserTransactionFactory.userTransaction();
            String transactionId = userTransaction.transactionIdentifier();
            System.out.println("RestaurantServiceAT transaction id =" + transactionId);
            
            // Enlist the Durable Participant for this service
            TransactionManager transactionManager = TransactionManagerFactory.transactionManager();
            ATDurableParticipant durableParticipant = new ATDurableParticipant(callName, serviceCommands, eventLog, transactionId);
            log.info("[SERVICE] Enlisting a Durable2PC participant into the AT");
            transactionManager.enlistForDurableTwoPhase(durableParticipant, "ATServiceDurable:" + new Uid().toString());
            
            // Enlist the Volatile Participant for this service
            ATVolatileParticipant volatileParticipant = new ATVolatileParticipant(callName, serviceCommands, eventLog, transactionId);
            log.info("[SERVICE] Enlisting a VolatilePC participant into the AT");
            transactionManager.enlistForVolatileTwoPhase(volatileParticipant, "ATServiceVolatile:" + new Uid().toString());
        } catch (Exception e) {
            throw new RuntimeException("Error when enlisting participants", e);
        }
        
        if (ServiceCommand.isPresent(APPLICATION_EXCEPTION, serviceCommands)) {
            throw new TestApplicationException("Intentionally thrown Application Exception - service command was set to: " + APPLICATION_EXCEPTION);
        }
        
        if (ServiceCommand.isPresent(ROLLBACK_ONLY, serviceCommands)) {
            log.info("Intentionally the service settings transaction to rollback only - service command was set to: " + ROLLBACK_ONLY);
            try {
                userTransaction.rollback();
            } catch (Exception e) {
                throw new RuntimeException("The rollback is not possible", e);
            }
        }

        // There will be some business logic here normally
        log.info("|AT SERVICE] I'm working on nothing...");
    }

    @WebMethod
    public EventLog getEventLog() {
        return eventLog;
    }

    @WebMethod
    public void clearEventLog() {
        eventLog.clear();
    }
}
