package com.chessping.ejb;

import com.chessping.ejb.backend.service.GameConfigRemote;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

/**
 * Configuration pour se connecter à WildFly EJB via JNDI.
 * 
 * IMPORTANT: Ce code nécessite que le JAR client WildFly soit dans le classpath.
 * 
 * Pour ajouter le client WildFly:
 * 1. Copier C:\wildfly\bin\client\jboss-client.jar vers lib\jboss-client.jar
 * 2. Ajouter au classpath lors du lancement:
 *    java -cp "target/classes;lib/jboss-client.jar" com.chessping.ChessPingApp
 */
public class EjbConf {

    public static GameConfigRemote lookup() throws Exception {

        Hashtable<String, String> props = new Hashtable<>();

        // Factory WildFly (sera fournie par jboss-client.jar)
        props.put(Context.INITIAL_CONTEXT_FACTORY,
                "org.wildfly.naming.client.WildFlyInitialContextFactory");

        // Adresse du serveur WildFly
        props.put(Context.PROVIDER_URL,
                "http-remoting://localhost:8080");

        // OBLIGATOIRE pour EJB remote
        props.put("jboss.naming.client.ejb.context", "true");

        Context context = new InitialContext(props);

        // JNDI EXACT vu dans la console WildFly
        String jndi =
                "ejb:/chess-ejb/GameConfigBean!" +
                "com.chessping.ejb.backend.service.GameConfigRemote";

        return (GameConfigRemote) context.lookup(jndi);
    }
}
