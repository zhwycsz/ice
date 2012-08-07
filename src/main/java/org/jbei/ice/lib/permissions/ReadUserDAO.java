package org.jbei.ice.lib.permissions;

import java.util.HashSet;
import java.util.Set;

import org.jbei.ice.lib.account.model.Account;
import org.jbei.ice.lib.dao.DAOException;
import org.jbei.ice.lib.entry.model.Entry;
import org.jbei.ice.lib.logging.Logger;
import org.jbei.ice.lib.permissions.model.ReadUser;
import org.jbei.ice.server.dao.hibernate.HibernateRepository;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * @author Hector Plahar
 */
public class ReadUserDAO extends HibernateRepository<ReadUser> {

    /**
     * Set read permissions for specified user {@link org.jbei.ice.lib.account.model.Account}s to the given {@link
     * org.jbei.ice.lib.entry.model.Entry}.
     * <p/>
     * This method creates new {@link org.jbei.ice.lib.permissions.model.ReadUser} objects using the given {@link
     * org.jbei.ice.lib.account.model.Account}s.
     *
     * @param entry    Entry to give read permission to.
     * @param accounts Accounts to give read permission to.
     * @throws org.jbei.ice.lib.dao.DAOException
     *
     */
    public void setReadUser(Entry entry, Set<Account> accounts) throws DAOException {
        String queryString = "delete ReadUser readUser where readUser.entry = :entry";
        Session session = newSession();

        try {
            session.getTransaction().begin();
            Query query = session.createQuery(queryString);
            query.setEntity("entry", entry);
            query.executeUpdate();
            session.getTransaction().commit();
            for (Account account : accounts) {
                ReadUser readUser = new ReadUser(entry, account);
                super.saveOrUpdate(readUser);
            }
        } catch (HibernateException e) {
            session.getTransaction().rollback();
            String msg = "Could not set Read User to " + entry.getRecordId();
            throw new DAOException(msg, e);
        } finally {
            if (session != null)
                session.close();
        }
    }

    /**
     * Add read permission for the specified {@link Account} to the specified {@link Entry}.
     * <p/>
     * This method adds a new {@link ReadUser} object to the database..
     *
     * @param entry
     * @param account
     * @throws DAOException
     */
    public void addReadUser(Entry entry, Account account) throws DAOException {
        Set<Account> accounts = getReadUser(entry);
        boolean alreadyAdded = false;
        for (Account oldAccount : accounts) {
            if (oldAccount.getId() == account.getId()) {
                alreadyAdded = true;
                break;
            }
        }
        if (alreadyAdded == false) {
            accounts.add(account);
            setReadUser(entry, accounts);
        }
    }

    public void removeReadUser(Entry entry, Account account) throws DAOException {
        String queryString = "delete ReadUser readUser where readUser.entry = :entry and readUser.account = :account";
        Session session = newSession();

        try {
            session.getTransaction().begin();
            Query query = session.createQuery(queryString);
            query.setEntity("entry", entry);
            query.setEntity("account", account);
            query.executeUpdate();
            session.getTransaction().commit();
        } catch (HibernateException e) {
            session.getTransaction().rollback();
            String msg = "Could not remove read user \"" + account.getEmail() + "\" for entry \""
                    + entry.getId() + "\"";
            throw new DAOException(msg, e);
        } finally {
            if (session != null)
                session.close();
        }
    }

    /**
     * Retrieve {@link Account}s with read permissions set for the specified {@link Entry}.
     *
     * @param entry Entry to get ReadUsers about.
     * @return Set of Accounts with read permission for the given Entry.
     * @throws DAOException
     */
    public Set<Account> getReadUser(Entry entry) throws DAOException {
        Session session = newSession();
        try {
            String queryString = "select readUser.account from ReadUser readUser where readUser.entry = :entry";
            Query query = session.createQuery(queryString);
            query.setEntity("entry", entry);

            @SuppressWarnings("unchecked")
            HashSet<Account> result = new HashSet<Account>(query.list());
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            String msg = "Could not read ReadUser of " + entry.getRecordId();
            Logger.error(msg, e);
            throw new DAOException(msg, e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
    }
}
