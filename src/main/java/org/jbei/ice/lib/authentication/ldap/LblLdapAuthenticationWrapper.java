package org.jbei.ice.lib.authentication.ldap;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.jbei.ice.lib.logging.Logger;

/**
 * Little wrapper for lbl ldap.
 * <p/>
 * Simple usage: LdapAuthl = new LdapAuth(); if (l.isWikiUser(String Username)) { try {
 * l.authenticate(Username, password); } except { //auth failed } }
 *
 * @author Zinovii Dmytriv, Timothy Ham, Hector Plahar
 */
public class LblLdapAuthenticationWrapper {
    protected DirContext dirContext = null;
    protected String searchURL = null;
    protected String authenticationURL = null;
    private String givenName = "";
    private String sirName = "";
    private String eMail = "";
    private String org = "";
    private String description = "";

    private final Properties properties;

    /**
     * Constructor.
     */
    public LblLdapAuthenticationWrapper() {
        properties = new Properties();
        initialize();
    }

    /**
     * Authenticate user to the ldap server.
     *
     * @param userName
     * @param passWord
     * @return True if successfully authenticated.
     * @throws LblLdapAuthenticationWrapperException
     *
     */
    public boolean authenticate(String userName, String passWord) throws LblLdapAuthenticationWrapperException {
        DirContext authContext = null;

        try {
            //has to look up employee number for binding
            String query = properties.getProperty("LDAP_QUERY");
            String filter = "(uid=" + userName + ")";
            SearchControls cons = new SearchControls();
            cons.setSearchScope(SearchControls.SUBTREE_SCOPE);
            cons.setCountLimit(0);

            if (dirContext == null) {
                dirContext = getContext();
            }
            SearchResult searchResult = dirContext.search(query, filter, cons).nextElement();

            Attributes attributes = searchResult.getAttributes();
            String employeeNumber = (String) attributes.get("lblempnum").get();

            if (attributes.get("givenName") != null) {
                givenName = (String) attributes.get("givenName").get();
            }
            if (attributes.get("sn") != null) {
                sirName = (String) attributes.get("sn").get();
            }
            if (attributes.get("mail") != null) {
                eMail = (String) attributes.get("mail").get();
            }
            eMail = eMail.toLowerCase();
            org = "Lawrence Berkeley Laboratory";
            if (attributes.get("description") != null) {
                description = (String) attributes.get("description").get();
            }
            authContext = getAuthenticatedContext(employeeNumber, passWord);
            return true;
        } catch (NamingException e) {
            Logger.warn(e.getMessage());
            throw new LblLdapAuthenticationWrapperException("Got LDAP NamingException", e);
        } finally {
            if (authContext != null) {
                try {
                    authContext.close();
                } catch (NamingException e) {
                    Logger.error(e);
                }
            }
            try {
                dirContext.close();
            } catch (NamingException e) {
                Logger.error(e);
            }
        }
    }

    /**
     * Check if the user is in the settings file specified ldap group before authenticating.
     *
     * @param loginName
     * @return True if user is in the specified ldap group.
     * @throws LblLdapAuthenticationWrapperException
     *
     */
    public boolean isWikiUser(String loginName) throws LblLdapAuthenticationWrapperException {
        boolean result = false;
        ArrayList<String> whitelistGroups = new ArrayList<String>();
        String whitelistString = properties.getProperty("LBL_LDAP_WHITELIST_GROUPS");
        String[] whiteListArray = whitelistString.split(",");
        for (String element : whiteListArray) {
            whitelistGroups.add(element);
        }

        String groupDn = properties.getProperty("LBL_LDAP_GROUP_BASE_DN");
        String groupQueryString = properties.getProperty("LBL_LDAP_GROUP_QUERY");
        String userDn = properties.getProperty("LBL_LDAP_USER_BASE_DN");
        String userQueryString = properties.getProperty("LBL_LDAP_USER_QUERY");

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setCountLimit(0);
        try {
            if (dirContext == null) {
                dirContext = getContext();
            }

            // find user
            NamingEnumeration<SearchResult> userResults;
            String userQuery = MessageFormat.format(userQueryString, loginName);
            userResults = dirContext.search(userDn, userQuery, searchControls);
            if (userResults.hasMore()) {
                // find user groups
                NamingEnumeration<SearchResult> groupResults;
                SearchResult user = userResults.next();
                String query = MessageFormat.format(groupQueryString, user.getNameInNamespace());
                groupResults = dirContext.search(groupDn, query, searchControls);
                while (groupResults.hasMore()) {
                    String name = groupResults.next().getAttributes().get("cn").get().toString();
                    if (whitelistGroups.contains(name)) {
                        result = true;
                    }
                }
                groupResults.close();
            }
            userResults.close();
        } catch (NamingException e) {
            Logger.error(e);
            throw new LblLdapAuthenticationWrapperException("Failed to fetch wiki user!", e);
        }

        String msg;
        if (result) {
            msg = loginName.toLowerCase() + " is in wiki.";
        } else {
            msg = loginName.toLowerCase() + " is not in wiki.";
        }

        Logger.info(msg);
        return result;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getSirName() {
        return sirName;
    }

    public String geteMail() {
        return eMail;
    }

    public String getOrg() {
        return org;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get unauthenticated ldap context.
     *
     * @return {@link DirContext} object.
     * @throws NamingException
     */
    protected DirContext getContext() throws NamingException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put("com.sun.jndi.ldap.connect.pool.timeout", "10000");
        env.put("com.sun.jndi.ldap.read.timeout", "5000");
        env.put("com.sun.jndi.ldap.connect.timeout", "10000");

        env.put(Context.PROVIDER_URL, searchURL);
        env.put(Context.SECURITY_AUTHENTICATION, "none");

        InitialDirContext result = null;
        try {
            result = new InitialDirContext(env);
        } catch (NamingException e) {
            Logger.error(e);
        }

        return result;
    }

    /**
     * Get authenticated context from the ldap server. Failure means bad user or password.
     *
     * @param lblEmployeeNumber
     * @param passWord
     * @return {@link DirContext} object.
     * @throws NamingException
     */
    protected DirContext getAuthenticatedContext(String lblEmployeeNumber, String passWord) throws NamingException {
        String baseDN = "o=Lawrence Berkeley Laboratory,c=US";
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put("com.sun.jndi.ldap.connect.pool.timeout", "1000");

        env.put("com.sun.jndi.ldap.read.timeout", "3000");
        env.put("com.sun.jndi.ldap.connect.timeout", "1000");

        env.put(Context.PROVIDER_URL, authenticationURL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "lblempnum=" + lblEmployeeNumber + ",ou=People," + baseDN);
        env.put(Context.SECURITY_CREDENTIALS, passWord);

        InitialDirContext result = new InitialDirContext(env);
        return result;
    }

    private void initialize() {
        try {
            String name = "ldap-config.properties";
            properties.load(LblLdapAuthenticationWrapper.class.getClassLoader().getResourceAsStream(name));
            searchURL = properties.getProperty("LDAP_SEARCH_URL");
            authenticationURL = properties.getProperty("LDAP_AUTHENTICATION_URL");
        } catch (IOException e) {
            Logger.warn(e.getLocalizedMessage());
            throw new RuntimeException("Cannot initialize LDAP", e);
        }
    }

    /**
     * Exception
     *
     * @author Zinovii Dmytriv
     */
    public static class LblLdapAuthenticationWrapperException extends Exception {
        private static final long serialVersionUID = 1L;

        public LblLdapAuthenticationWrapperException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
