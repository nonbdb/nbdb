package com.jetbrains.youtrack.db.internal.core.metadata.security;

import com.jetbrains.youtrack.db.internal.common.exception.YTException;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.exception.YTSecurityException;
import com.jetbrains.youtrack.db.internal.core.metadata.function.OFunction;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClass;
import com.jetbrains.youtrack.db.internal.core.record.Record;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.executor.YTResult;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OAndBlock;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OBooleanExpression;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OOrBlock;
import java.util.Map;
import java.util.Set;

public class OSecurityEngine {

  private static final OPredicateCache cache =
      new OPredicateCache(GlobalConfiguration.STATEMENT_CACHE_SIZE.getValueAsInteger());

  /**
   * Calculates a predicate for a security resource. It also takes into consideration the security
   * and schema hierarchies. ie. invoking it with a specific session for a specific class, the
   * method checks all the roles of the session, all the parent roles and all the parent classes
   * until it finds a valid predicarte (the most specific one).
   *
   * <p>For multiple-role session, the result is the OR of the predicates that would be calculated
   * for each single role.
   *
   * <p>For class hierarchies: - the most specific (ie. defined on subclass) defined predicate is
   * applied - in case a class does not have a direct predicate defined, the superclass predicate is
   * used (and recursively) - in case of multiple superclasses, the AND of the predicates for
   * superclasses (also recursively) is applied
   *
   * @param session
   * @param security
   * @param resourceString
   * @param scope
   * @return always returns a valid predicate (it is never supposed to be null)
   */
  static OBooleanExpression getPredicateForSecurityResource(
      YTDatabaseSessionInternal session,
      OSecurityShared security,
      String resourceString,
      OSecurityPolicy.Scope scope) {
    YTSecurityUser user = session.getUser();
    if (user == null) {
      return OBooleanExpression.FALSE;
    }

    Set<? extends OSecurityRole> roles = user.getRoles();
    if (roles == null || roles.isEmpty()) {
      return OBooleanExpression.FALSE;
    }

    OSecurityResource resource = getResourceFromString(resourceString);
    if (resource instanceof OSecurityResourceClass) {
      return getPredicateForClass(session, security, (OSecurityResourceClass) resource, scope);
    } else if (resource instanceof OSecurityResourceProperty) {
      return getPredicateForProperty(
          session, security, (OSecurityResourceProperty) resource, scope);
    } else if (resource instanceof OSecurityResourceFunction) {
      return getPredicateForFunction(
          session, security, (OSecurityResourceFunction) resource, scope);
    }
    return OBooleanExpression.FALSE;
  }

  private static OBooleanExpression getPredicateForFunction(
      YTDatabaseSessionInternal session,
      OSecurityShared security,
      OSecurityResourceFunction resource,
      OSecurityPolicy.Scope scope) {
    OFunction function =
        session.getMetadata().getFunctionLibrary().getFunction(resource.getFunctionName());
    Set<? extends OSecurityRole> roles = session.getUser().getRoles();
    if (roles == null || roles.size() == 0) {
      return null;
    }
    if (roles.size() == 1) {
      return getPredicateForRoleHierarchy(
          session, security, roles.iterator().next(), function, scope);
    }

    OOrBlock result = new OOrBlock(-1);

    for (OSecurityRole role : roles) {
      OBooleanExpression roleBlock =
          getPredicateForRoleHierarchy(session, security, role, function, scope);
      if (OBooleanExpression.TRUE.equals(roleBlock)) {
        return OBooleanExpression.TRUE;
      }
      result.getSubBlocks().add(roleBlock);
    }

    return result;
  }

  private static OBooleanExpression getPredicateForProperty(
      YTDatabaseSessionInternal session,
      OSecurityShared security,
      OSecurityResourceProperty resource,
      OSecurityPolicy.Scope scope) {
    YTClass clazz =
        session
            .getMetadata()
            .getImmutableSchemaSnapshot()
            .getClass(resource.getClassName());
    if (clazz == null) {
      clazz =
          session
              .getMetadata()
              .getImmutableSchemaSnapshot()
              .getView(resource.getClassName());
    }
    String propertyName = resource.getPropertyName();
    Set<? extends OSecurityRole> roles = session.getUser().getRoles();
    if (roles == null || roles.size() == 0) {
      return null;
    }
    if (roles.size() == 1) {
      return getPredicateForRoleHierarchy(
          session, security, roles.iterator().next(), clazz, propertyName, scope);
    }

    OOrBlock result = new OOrBlock(-1);

    for (OSecurityRole role : roles) {
      OBooleanExpression roleBlock =
          getPredicateForRoleHierarchy(session, security, role, clazz, propertyName, scope);
      if (OBooleanExpression.TRUE.equals(roleBlock)) {
        return OBooleanExpression.TRUE;
      }
      result.getSubBlocks().add(roleBlock);
    }

    return result;
  }

  private static OBooleanExpression getPredicateForClass(
      YTDatabaseSessionInternal session,
      OSecurityShared security,
      OSecurityResourceClass resource,
      OSecurityPolicy.Scope scope) {
    YTClass clazz =
        session
            .getMetadata()
            .getImmutableSchemaSnapshot()
            .getClass(resource.getClassName());
    if (clazz == null) {
      return OBooleanExpression.TRUE;
    }
    Set<? extends OSecurityRole> roles = session.getUser().getRoles();
    if (roles == null || roles.size() == 0) {
      return null;
    }
    if (roles.size() == 1) {
      return getPredicateForRoleHierarchy(session, security, roles.iterator().next(), clazz, scope);
    }

    OOrBlock result = new OOrBlock(-1);

    for (OSecurityRole role : roles) {
      OBooleanExpression roleBlock =
          getPredicateForRoleHierarchy(session, security, role, clazz, scope);
      if (OBooleanExpression.TRUE.equals(roleBlock)) {
        return OBooleanExpression.TRUE;
      }
      result.getSubBlocks().add(roleBlock);
    }

    return result;
  }

  private static OBooleanExpression getPredicateForRoleHierarchy(
      YTDatabaseSessionInternal session,
      OSecurityShared security,
      OSecurityRole role,
      OFunction function,
      OSecurityPolicy.Scope scope) {
    // TODO cache!

    OBooleanExpression result = getPredicateForFunction(session, security, role, function, scope);
    if (result != null) {
      return result;
    }

    if (role.getParentRole() != null) {
      return getPredicateForRoleHierarchy(session, security, role.getParentRole(), function, scope);
    }
    return OBooleanExpression.FALSE;
  }

  private static OBooleanExpression getPredicateForFunction(
      YTDatabaseSessionInternal session,
      OSecurityShared security,
      OSecurityRole role,
      OFunction clazz,
      OSecurityPolicy.Scope scope) {
    String resource = "database.function." + clazz.getName(session);
    Map<String, OSecurityPolicy> definedPolicies = security.getSecurityPolicies(session, role);
    OSecurityPolicy policy = definedPolicies.get(resource);

    String predicateString = policy != null ? policy.get(scope, session) : null;

    if (predicateString == null) {
      OSecurityPolicy wildcardPolicy = definedPolicies.get("database.function.*");
      predicateString = wildcardPolicy == null ? null : wildcardPolicy.get(scope, session);
    }

    if (predicateString != null) {
      return parsePredicate(session, predicateString);
    }
    return OBooleanExpression.FALSE;
  }

  private static OBooleanExpression getPredicateForRoleHierarchy(
      YTDatabaseSessionInternal session,
      OSecurityShared security,
      OSecurityRole role,
      YTClass clazz,
      OSecurityPolicy.Scope scope) {
    OBooleanExpression result;
    if (role != null) {
      result = security.getPredicateFromCache(role.getName(session), clazz.getName());
      if (result != null) {
        return result;
      }
    }

    result = getPredicateForClassHierarchy(session, security, role, clazz, scope);
    if (result != null) {
      return result;
    }

    if (role.getParentRole() != null) {
      result = getPredicateForRoleHierarchy(session, security, role.getParentRole(), clazz, scope);
    }
    if (result == null) {
      result = OBooleanExpression.FALSE;
    }
    security.putPredicateInCache(session, role.getName(session), clazz.getName(), result);
    return result;
  }

  private static OBooleanExpression getPredicateForRoleHierarchy(
      YTDatabaseSessionInternal session,
      OSecurityShared security,
      OSecurityRole role,
      YTClass clazz,
      String propertyName,
      OSecurityPolicy.Scope scope) {
    String cacheKey = "$CLASS$" + clazz.getName() + "$PROP$" + propertyName + "$" + scope;
    OBooleanExpression result;
    if (role != null) {
      result = security.getPredicateFromCache(role.getName(session), cacheKey);
      if (result != null) {
        return result;
      }
    }

    result = getPredicateForClassHierarchy(session, security, role, clazz, propertyName, scope);
    if (result == null && role.getParentRole() != null) {
      result =
          getPredicateForRoleHierarchy(
              session, security, role.getParentRole(), clazz, propertyName, scope);
    }
    if (result == null) {
      result = OBooleanExpression.FALSE;
    }
    if (role != null) {
      security.putPredicateInCache(session, role.getName(session), cacheKey, result);
    }
    return result;
  }

  private static OBooleanExpression getPredicateForClassHierarchy(
      YTDatabaseSessionInternal session,
      OSecurityShared security,
      OSecurityRole role,
      YTClass clazz,
      OSecurityPolicy.Scope scope) {
    String resource = "database.class." + clazz.getName();
    Map<String, OSecurityPolicy> definedPolicies = security.getSecurityPolicies(session, role);
    OSecurityPolicy classPolicy = definedPolicies.get(resource);

    String predicateString = classPolicy != null ? classPolicy.get(scope, session) : null;
    if (predicateString == null && !clazz.getSuperClasses().isEmpty()) {
      if (clazz.getSuperClasses().size() == 1) {
        return getPredicateForClassHierarchy(
            session, security, role, clazz.getSuperClasses().iterator().next(), scope);
      }
      OAndBlock result = new OAndBlock(-1);
      for (YTClass superClass : clazz.getSuperClasses()) {
        OBooleanExpression superClassPredicate =
            getPredicateForClassHierarchy(session, security, role, superClass, scope);
        if (superClassPredicate == null) {
          return OBooleanExpression.FALSE;
        }
        result.getSubBlocks().add(superClassPredicate);
      }
      return result;
    }

    if (predicateString == null) {
      OSecurityPolicy wildcardPolicy = definedPolicies.get("database.class.*");
      predicateString = wildcardPolicy == null ? null : wildcardPolicy.get(scope, session);
    }

    if (predicateString == null) {
      OSecurityPolicy wildcardPolicy = definedPolicies.get("*");
      predicateString = wildcardPolicy == null ? null : wildcardPolicy.get(scope, session);
    }
    if (predicateString != null) {
      return parsePredicate(session, predicateString);
    }
    return OBooleanExpression.FALSE;
  }

  private static OBooleanExpression getPredicateForClassHierarchy(
      YTDatabaseSessionInternal session,
      OSecurityShared security,
      OSecurityRole role,
      YTClass clazz,
      String propertyName,
      OSecurityPolicy.Scope scope) {
    String resource = "database.class." + clazz.getName() + "." + propertyName;
    Map<String, OSecurityPolicy> definedPolicies = security.getSecurityPolicies(session, role);
    OSecurityPolicy classPolicy = definedPolicies.get(resource);

    String predicateString = classPolicy != null ? classPolicy.get(scope, session) : null;
    if (predicateString == null && !clazz.getSuperClasses().isEmpty()) {
      if (clazz.getSuperClasses().size() == 1) {
        return getPredicateForClassHierarchy(
            session,
            security,
            role,
            clazz.getSuperClasses().iterator().next(),
            propertyName,
            scope);
      }
      OAndBlock result = new OAndBlock(-1);
      for (YTClass superClass : clazz.getSuperClasses()) {
        OBooleanExpression superClassPredicate =
            getPredicateForClassHierarchy(session, security, role, superClass, propertyName, scope);
        if (superClassPredicate == null) {
          return OBooleanExpression.TRUE;
        }
        result.getSubBlocks().add(superClassPredicate);
      }
      return result;
    }

    if (predicateString == null) {
      OSecurityPolicy wildcardPolicy =
          definedPolicies.get("database.class." + clazz.getName() + ".*");
      predicateString = wildcardPolicy == null ? null : wildcardPolicy.get(scope, session);
    }

    if (predicateString == null) {
      OSecurityPolicy wildcardPolicy = definedPolicies.get("database.class.*." + propertyName);
      predicateString = wildcardPolicy == null ? null : wildcardPolicy.get(scope, session);
    }

    if (predicateString == null) {
      OSecurityPolicy wildcardPolicy = definedPolicies.get("database.class.*.*");
      predicateString = wildcardPolicy == null ? null : wildcardPolicy.get(scope, session);
    }

    if (predicateString == null) {
      OSecurityPolicy wildcardPolicy = definedPolicies.get("*");
      predicateString = wildcardPolicy == null ? null : wildcardPolicy.get(scope, session);
    }
    // TODO

    if (predicateString != null) {
      return parsePredicate(session, predicateString);
    }
    return OBooleanExpression.TRUE;
  }

  public static OBooleanExpression parsePredicate(
      YTDatabaseSession session, String predicateString) {
    if ("true".equalsIgnoreCase(predicateString)) {
      return OBooleanExpression.TRUE;
    }
    if ("false".equalsIgnoreCase(predicateString)) {
      return OBooleanExpression.FALSE;
    }
    try {

      return cache.get(predicateString);
    } catch (Exception e) {
      System.out.println("Error parsing predicate: " + predicateString);
      throw e;
    }
  }

  static boolean evaluateSecuirtyPolicyPredicate(
      YTDatabaseSessionInternal session, OBooleanExpression predicate, Record record) {
    if (OBooleanExpression.TRUE.equals(predicate)) {
      return true;
    }
    if (OBooleanExpression.FALSE.equals(predicate)) {
      return false;
    }
    if (predicate == null) {
      return true; // TODO check!
    }
    try {
      // Create a new instance of EntityImpl with a user record id, this will lazy load the user data
      // at the first access with the same execution permission of the policy
      YTIdentifiable user = session.getUser().getIdentity(session);

      var sessionInternal = session;
      var recordCopy = ((RecordAbstract) record).copy();
      return sessionInternal
          .getSharedContext()
          .getYouTrackDB()
          .executeNoAuthorizationSync(
              sessionInternal,
              (db -> {
                BasicCommandContext ctx = new BasicCommandContext();
                ctx.setDatabase(db);
                ctx.setDynamicVariable("$currentUser", (inContext) -> user.getRecordSilently());

                recordCopy.setup(db);
                return predicate.evaluate(recordCopy, ctx);
              }));
    } catch (Exception e) {
      throw YTException.wrapException(
          new YTSecurityException("Cannot execute security predicate"), e);
    }
  }

  static boolean evaluateSecuirtyPolicyPredicate(
      YTDatabaseSessionInternal session, OBooleanExpression predicate, YTResult record) {
    if (OBooleanExpression.TRUE.equals(predicate)) {
      return true;
    }
    if (OBooleanExpression.FALSE.equals(predicate)) {
      return false;
    }
    try {
      // Create a new instance of EntityImpl with a user record id, this will lazy load the user data
      // at the first access with the same execution permission of the policy
      final EntityImpl user = session.getUser().getIdentity(session).getRecordSilently();
      return session
          .getSharedContext()
          .getYouTrackDB()
          .executeNoAuthorizationAsync(
              session.getName(),
              (db -> {
                BasicCommandContext ctx = new BasicCommandContext();
                ctx.setDatabase(db);
                ctx.setDynamicVariable(
                    "$currentUser",
                    (inContext) -> {
                      return user;
                    });
                return predicate.evaluate(record, ctx);
              }))
          .get();
    } catch (Exception e) {
      e.printStackTrace();
      throw new YTSecurityException("Cannot execute security predicate");
    }
  }

  /**
   * returns a resource from a resource string, eg. an OUser YTClass from "database.class.OUser"
   * string
   *
   * @param resource a resource string
   * @return
   */
  private static OSecurityResource getResourceFromString(String resource) {
    return OSecurityResource.getInstance(resource);
  }
}