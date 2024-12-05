/**
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*
 */
package com.orientechnologies.security.ldap;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.List;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.Subject;

/**
 * LDAP Library
 */
public class OLDAPLibrary {

  public static DirContext openContext(
      final Subject subject, final List<OLDAPServer> ldapServers, final boolean debug) {
    return Subject.doAs(
        subject,
        new PrivilegedAction<DirContext>() {
          public DirContext run() {
            DirContext dc = null;

            // Set up environment for creating initial context
            Hashtable<String, String> env = new Hashtable<String, String>();

            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

            // Request the use of the "GSSAPI" SASL mechanism
            // Authenticate by using already established Kerberos credentials
            env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");

            for (OLDAPServer ldap : ldapServers) {
              try {
                String url = ldap.getURL();

                // If the LDAPServer info is marked as an alias, then the real hostname needs to be
                // acquired.
                if (ldap.isAlias()) {
                  url = getRealURL(ldap, debug);
                }

                // Must use fully qualified hostname
                env.put(Context.PROVIDER_URL, url);

                if (debug) {
                  LogManager.instance()
                      .info(
                          OLDAPLibrary.class,
                          "OLDAPLibrary.openContext() Trying ProviderURL: " + url);
                }

                // Create initial context
                dc = new InitialDirContext(env);
                break;
              } catch (Exception ex) {
                LogManager.instance()
                    .error(OLDAPLibrary.class, "OLDAPLibrary.openContext() Exception: ", ex);
              }
            }

            return dc;
          }
        });
  }

  // If the LDAPServer's isAlias() returns true, then the specified hostname is an alias, requiring
  // a reverse
  // look-up of its IP address to resolve the real hostname to use.  This is often used with DNS
  // round-robin.
  private static String getRealURL(OLDAPServer ldap, final boolean debug)
      throws UnknownHostException {
    String realURL = ldap.getURL();

    if (ldap.isAlias()) {
      if (debug) {
        LogManager.instance()
            .info(
                OLDAPLibrary.class,
                "OLDAPLibrary.getRealURL() Alias hostname = " + ldap.getHostname());
      }

      // Get the returned IP address from the alias.
      // May throw an UnknownHostException
      InetAddress ipAddress = InetAddress.getByName(ldap.getHostname());

      if (debug) {
        LogManager.instance()
            .info(
                OLDAPLibrary.class,
                "OLDAPLibrary.getRealURL() IP Address = " + ipAddress.getHostAddress());
      }

      // Now that we have the IP address, use it to get the real hostname.
      // We create a new InetAddress object, because hostnames are cached.
      InetAddress realAddress = InetAddress.getByName(ipAddress.getHostAddress());

      if (debug) {
        LogManager.instance()
            .info(
                OLDAPLibrary.class,
                "OLDAPLibrary.getRealURL() Real hostname = " + realAddress.getHostName());
      }

      realURL = ldap.getURL(realAddress.getHostName());

      if (debug) {
        LogManager.instance()
            .info(OLDAPLibrary.class, "OLDAPLibrary.getRealURL() Real URL = " + realURL);
      }
    }

    return realURL;
  }

  public static void retrieveUsers(
      DirContext ctx,
      final String baseDN,
      final String filter,
      final List<String> principalList,
      final boolean debug) {
    try {
      if (ctx != null) {
        // If we're just obtaining users matching a filterDN, switch to a SearchControl.
        //				traverse(ctx, startingDN, filterDN, principalList, debug);

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope(SearchControls.SUBTREE_SCOPE); // Recursive

        String[] attribFilter = {"userPrincipalName", "altSecurityIdentities"};
        sctls.setReturningAttributes(attribFilter);

        NamingEnumeration ne = ctx.search(baseDN, filter, sctls); // "(userPrincipalName=*)"

        while (ne.hasMore()) {
          SearchResult sr = (SearchResult) ne.next();

          addPrincipal(sr, principalList, debug);
        }
      } else {
        if (debug) {
          LogManager.instance()
              .error(OLDAPLibrary.class, "OLDAPLibrary.retrieveUsers() DirContext is null", null);
        }
      }
    } catch (Exception ex) {
      LogManager.instance()
          .error(OLDAPLibrary.class, "OLDAPLibrary.retrieveUsers() Exception: ", ex);
    }
  }

  private static void addPrincipal(
      SearchResult sr, List<String> principalList, final boolean debug) {
    try {
      Attributes attrs = sr.getAttributes();

      if (attrs != null) {
        /*
        			// userPrincipalName
        			String upn = getUserPrincipalName(attrs);

        			if(debug) LogManager.instance().info(null, "OLDAPLibrary.addPrincipal() userPrincipalName: " + upn);

        			if(upn != null)
        			{
        				// Some UPNs, especially in 'altSecurityIdentities' will store the UPNs as such "Kerberos: user@realm.com"
        				upn = removeKerberos(upn, debug);

        				principalList.add(upn); //upn.toLowerCase());
        			}
        */
        fillAttributeList(attrs, "userPrincipalName", principalList, debug);

        fillAttributeList(attrs, "altSecurityIdentities", principalList, debug);
      }
    } catch (Exception ex) {
      LogManager.instance()
          .error(OLDAPLibrary.class, "OLDAPLibrary.addPrincipal() Exception: ", ex);
    }
  }

  private static void traverse(
      DirContext ctx,
      String startingDN,
      String memberOfFilter,
      List<String> principalList,
      final boolean debug) {
    try {
      if (debug) {
        LogManager.instance()
            .info(
                OLDAPLibrary.class,
                "OLDAPLibrary.traverse() startingDN: %s, memberOfFilter: %s",
                startingDN,
                memberOfFilter);
      }

      Attributes attrs = ctx.getAttributes(startingDN);

      if (attrs != null) {
        if (debug) {
          LogManager.instance()
              .info(
                  OLDAPLibrary.class,
                  "OLDAPLibrary.traverse() Found attributes for startingDN: %s",
                  startingDN);
        }

        Attribute member = attrs.get("member");

        if (member != null) {
          for (NamingEnumeration ae = member.getAll(); ae.hasMore(); ) {
            String path = (String) ae.next();

            findMembers(ctx, path, memberOfFilter, principalList, debug);
          }
        } else {
          if (debug) {
            LogManager.instance()
                .info(
                    OLDAPLibrary.class,
                    "OLDAPLibrary.traverse() startingDN: %s has no \"member\" attributes.",
                    startingDN);
          }
        }
      } else {
        if (debug) {
          LogManager.instance()
              .error(
                  OLDAPLibrary.class,
                  "OLDAPLibrary.traverse() Unable to find attributes for startingDN: %s",
                  null,
                  startingDN);
        }
      }
    } catch (Exception ex) {
      LogManager.instance().error(OLDAPLibrary.class, "OLDAPLibrary.traverse() Exception: ", ex);
    }
  }

  private static void findMembers(
      DirContext ctx,
      String startingDN,
      String memberOfFilter,
      List<String> principalList,
      final boolean debug) {
    try {
      Attributes attrs = ctx.getAttributes(startingDN);

      if (attrs != null) {
        if (debug) {
          LogManager.instance()
              .info(
                  OLDAPLibrary.class,
                  "OLDAPLibrary.findMembers() Found attributes for startingDN: %s",
                  startingDN);
        }

        if (isGroup(attrs)) {
          if (debug) {
            LogManager.instance()
                .info(
                    OLDAPLibrary.class,
                    "OLDAPLibrary.findMembers() Found group for startingDN: %s",
                    startingDN);
          }

          Attribute member = attrs.get("member");

          if (member != null) {
            for (NamingEnumeration ae = member.getAll(); ae.hasMore(); ) {
              String path = (String) ae.next();
              findMembers(ctx, path, memberOfFilter, principalList, debug);
            }
          }
        } else if (isUser(attrs)) {
          if (debug) {
            LogManager.instance()
                .info(
                    OLDAPLibrary.class,
                    "OLDAPLibrary.findMembers() Found user for startingDN: %s",
                    startingDN);
          }

          if (isMemberOf(attrs, memberOfFilter)) {
            // userPrincipalName
            String upn = getUserPrincipalName(attrs);

            if (debug) {
              LogManager.instance()
                  .info(
                      OLDAPLibrary.class,
                      "OLDAPLibrary.findMembers() StartingDN: "
                          + startingDN
                          + ", userPrincipalName: "
                          + upn);
            }

            if (upn != null) {
              // Some UPNs, especially in 'altSecurityIdentities' will store the UPNs as such
              // "Kerberos: user@realm.com"
              upn = removeKerberos(upn, debug);
              principalList.add(upn); // upn.toLowerCase());
            }

            fillAttributeList(attrs, "altSecurityIdentities", principalList, debug);
          }
        }
      } else {
        LogManager.instance()
            .error(
                OLDAPLibrary.class,
                "OLDAPLibrary.findMembers() Unable to find attributes for startingDN: %s",
                null,
                startingDN);
      }
    } catch (Exception ex) {
      LogManager.instance()
          .error(OLDAPLibrary.class, "OLDAPLibrary.findMembers() Exception: ", ex);
    }
  }

  // Separates the distinguished name and returns the top-level name.
  private static String getName(final String dn) {
    String name = null;

    String[] names = dn.split(",");

    if (names.length >= 1) {
      // >= 4 because "CN=" is 3
      if (names[0].length() >= 4) {
        name = names[0].substring(3);
      }
    }

    return name;
  }

  private static void fillAttributeList(
      Attributes attrs, String name, List<String> list, final boolean debug) {
    try {
      Attribute attribute = attrs.get(name);

      if (attribute != null && attribute.size() > 0) {
        NamingEnumeration<?> ne = attribute.getAll();
        while (ne.hasMore()) {
          String value = (String) ne.next();

          // Some UPNs, especially in 'altSecurityIdentities' will store the UPNs as such "Kerberos:
          // user@realm.com"
          value = removeKerberos(value, debug);

          list.add(value); // value.toLowerCase());
        }
      }
    } catch (Exception ex) {
      LogManager.instance()
          .error(OLDAPLibrary.class, "OLDAPLibrary fillAttributeList(" + name + ")", ex);
    }
  }

  private static String getFirstValue(Attributes attrs, String name) {
    try {
      Attribute attribute = attrs.get(name);

      if (attribute != null && attribute.size() > 0) {
        return (String) attribute.get(0);
      }
    } catch (Exception ex) {
      LogManager.instance()
          .error(OLDAPLibrary.class, "OLDAPLibrary.getFirstValue(" + name + ") ", ex);
    }

    return null;
  }

  private static String getUserPrincipalName(Attributes attrs) {
    return getFirstValue(attrs, "userPrincipalName");
  }

  private static boolean isGroup(Attributes attrs) {
    String objCategoryDN = getFirstValue(attrs, "objectCategory");

    if (objCategoryDN != null) {
      String objCategory = getName(objCategoryDN);

      return objCategory.equalsIgnoreCase("Group");
    }

    return false;
  }

  private static boolean isUser(Attributes attrs) {
    String objCategoryDN = getFirstValue(attrs, "objectCategory");

    if (objCategoryDN != null) {
      String objCategory = getName(objCategoryDN);

      return objCategory.equalsIgnoreCase("User") || objCategory.equalsIgnoreCase("Person");
    }

    return false;
  }

  private static boolean isMemberOf(Attributes attrs, String memberOfFilter) {
    try {
      Attribute memberOfAttr = attrs.get("memberOf");

      if (memberOfAttr != null) {
        for (NamingEnumeration mo = memberOfAttr.getAll(); mo.hasMore(); ) {
          String value = (String) mo.next();

          if (value.equalsIgnoreCase(memberOfFilter)) {
            return true;
          }
        }
      } else {
        LogManager.instance()
            .error(
                OLDAPLibrary.class, "OLDAPLibrary.isMemberOf() Has no 'memberOf' attribute.", null);
      }
    } catch (Exception ex) {
      LogManager.instance().error(OLDAPLibrary.class, "OLDAPLibrary.isMemberOf()", ex);
    }

    return false;
  }

  // Some UPNs, especially in 'altSecurityIdentities' will store the UPNs as such "Kerberos:
  // user@realm.com"
  private static String removeKerberos(String upn, final boolean debug) {
    if ((upn.startsWith("kerberos:") || upn.startsWith("Kerberos:")) && upn.length() > 9) {
      if (debug) {
        LogManager.instance()
            .info(OLDAPLibrary.class, "OLDAPLibrary.removeKerberos() upn before: %s", upn);
      }

      upn = upn.substring(9);
      upn.trim();

      if (debug) {
        LogManager.instance()
            .info(OLDAPLibrary.class, "OLDAPLibrary.removeKerberos() upn after: %s", upn);
      }
    }

    return upn;
  }
}
