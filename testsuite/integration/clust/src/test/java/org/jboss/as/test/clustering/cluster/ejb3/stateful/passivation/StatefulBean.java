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

package org.jboss.as.test.clustering.cluster.ejb3.stateful.passivation;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Stateful;

import org.apache.log4j.Logger;
import org.jboss.ejb3.annotation.Clustered;

/**
 * @author Ondrej Chaloupka
 */
@Clustered
@Stateful
public class StatefulBean implements StatefulBeanRemote {
    private static Logger log = Logger.getLogger(StatefulBean.class);
    private int number;
    private String passivatedBy;
    private String actIfIsNode;

    /**
     * Getting number.
     */
    public int getNumber() {
        return number;
    }
    
    public String incrementNumber() {
        number++;
        log.info("Incrementing number: " + Integer.toString(number));
        return getNodeName();
    }

    /**
     * Setting number and returns node name where the method was called.
     */
    public String setNumber(int number) {
        log.info("Setting number: " + Integer.toString(number));
        this.number = number;
        return getNodeName();
    }

    public String getPassivatedBy() {
        return this.passivatedBy;
    }
    
    public void setPassivationNode(String nodeName) {
        this.actIfIsNode = nodeName;
    }
    
    private String getNodeName() {
        return System.getProperty("jboss.node.name");
    }
        
    @PrePassivate
    public void prePassivate() {
        log.info("Passivating with number: " + number + " and was passivated by " + getPassivatedBy());
        
        // when we should act on passivation - we change value of isPassivated variable
        if(getNodeName().equals(actIfIsNode)) {
            passivatedBy = getNodeName();
            log.info("I'm node " + actIfIsNode + " => changing passivatedBy to " + passivatedBy);
        }
    }
    
    @PostActivate
    public void postActivate() {
        log.info("Activating with number: " + number + " and was passivated by " + getPassivatedBy());
    }
}
