/*
 * Copyright 2010-2012 henryzhao81-at-gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.youtrack.db.internal.core.db.record;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.CommandScriptException;
import com.jetbrains.youtrack.db.api.exception.ConfigurationException;
import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.record.RecordHook;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.common.util.CommonConst;
import com.jetbrains.youtrack.db.internal.core.command.script.ScriptManager;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.metadata.function.Function;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaImmutableClass;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityInternalUtils;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.StringSerializerHelper;
import java.lang.reflect.Method;
import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Author : henryzhao81@gmail.com Feb 19, 2013
 *
 * <p>Create a class OTriggered which contains 8 additional class attributes, which link to
 * Function - beforeCreate - afterCreate - beforeRead - afterRead - beforeUpdate - afterUpdate -
 * beforeDelete - afterDelete
 */
public class ClassTrigger {

  public static final String CLASSNAME = "OTriggered";
  public static final String METHOD_SEPARATOR = ".";

  // Class Level Trigger (class custom attribute)
  public static final String ONBEFORE_CREATED = "onBeforeCreate";
  // Record Level Trigger (property name)
  public static final String PROP_BEFORE_CREATE = ONBEFORE_CREATED;
  public static final String ONAFTER_CREATED = "onAfterCreate";
  public static final String PROP_AFTER_CREATE = ONAFTER_CREATED;
  public static final String ONBEFORE_READ = "onBeforeRead";
  public static final String PROP_BEFORE_READ = ONBEFORE_READ;
  public static final String ONAFTER_READ = "onAfterRead";
  public static final String PROP_AFTER_READ = ONAFTER_READ;
  public static final String ONBEFORE_UPDATED = "onBeforeUpdate";
  public static final String PROP_BEFORE_UPDATE = ONBEFORE_UPDATED;
  public static final String ONAFTER_UPDATED = "onAfterUpdate";
  public static final String PROP_AFTER_UPDATE = ONAFTER_UPDATED;
  public static final String ONBEFORE_DELETE = "onBeforeDelete";
  public static final String PROP_BEFORE_DELETE = ONBEFORE_DELETE;
  public static final String ONAFTER_DELETE = "onAfterDelete";
  public static final String PROP_AFTER_DELETE = ONAFTER_DELETE;

  public static RecordHook.RESULT onRecordBeforeCreate(
      final EntityImpl entity, DatabaseSessionInternal database) {
    Object func = checkClzAttribute(entity, ONBEFORE_CREATED, database);
    if (func != null) {
      if (func instanceof Function) {
        return ClassTrigger.executeFunction(entity, (Function) func, database);
      } else if (func instanceof Object[]) {
        return ClassTrigger.executeMethod(entity, (Object[]) func);
      }
    }
    return RecordHook.RESULT.RECORD_NOT_CHANGED;
  }

  public static void onRecordAfterCreate(
      final EntityImpl entity, DatabaseSessionInternal database) {
    Object func = checkClzAttribute(entity, ONAFTER_CREATED, database);
    if (func != null) {
      if (func instanceof Function) {
        ClassTrigger.executeFunction(entity, (Function) func, database);
      } else if (func instanceof Object[]) {
        ClassTrigger.executeMethod(entity, (Object[]) func);
      }
    }
  }

  public static RecordHook.RESULT onRecordBeforeRead(
      final EntityImpl entity, DatabaseSessionInternal database) {
    Object func = checkClzAttribute(entity, ONBEFORE_READ, database);
    if (func != null) {
      if (func instanceof Function) {
        return ClassTrigger.executeFunction(entity, (Function) func, database);
      } else if (func instanceof Object[]) {
        return ClassTrigger.executeMethod(entity, (Object[]) func);
      }
    }
    return RecordHook.RESULT.RECORD_NOT_CHANGED;
  }

  public static void onRecordAfterRead(
      final EntityImpl entity, DatabaseSessionInternal database) {
    Object func = checkClzAttribute(entity, ONAFTER_READ, database);
    if (func != null) {
      if (func instanceof Function) {
        ClassTrigger.executeFunction(entity, (Function) func, database);
      } else if (func instanceof Object[]) {
        ClassTrigger.executeMethod(entity, (Object[]) func);
      }
    }
  }

  public static RecordHook.RESULT onRecordBeforeUpdate(
      final EntityImpl entity, DatabaseSessionInternal database) {
    Object func = checkClzAttribute(entity, ONBEFORE_UPDATED, database);
    if (func != null) {
      if (func instanceof Function) {
        return ClassTrigger.executeFunction(entity, (Function) func, database);
      } else if (func instanceof Object[]) {
        return ClassTrigger.executeMethod(entity, (Object[]) func);
      }
    }
    return RecordHook.RESULT.RECORD_NOT_CHANGED;
  }

  public static void onRecordAfterUpdate(
      final EntityImpl entity, DatabaseSessionInternal database) {
    Object func = checkClzAttribute(entity, ONAFTER_UPDATED, database);
    if (func != null) {
      if (func instanceof Function) {
        ClassTrigger.executeFunction(entity, (Function) func, database);
      } else if (func instanceof Object[]) {
        ClassTrigger.executeMethod(entity, (Object[]) func);
      }
    }
  }

  public static RecordHook.RESULT onRecordBeforeDelete(
      final EntityImpl entity, DatabaseSessionInternal database) {
    Object func = checkClzAttribute(entity, ONBEFORE_DELETE, database);
    if (func != null) {
      if (func instanceof Function) {
        return ClassTrigger.executeFunction(entity, (Function) func, database);
      } else if (func instanceof Object[]) {
        return ClassTrigger.executeMethod(entity, (Object[]) func);
      }
    }
    return RecordHook.RESULT.RECORD_NOT_CHANGED;
  }

  public static void onRecordAfterDelete(
      final EntityImpl entity, DatabaseSessionInternal database) {
    Object func = checkClzAttribute(entity, ONAFTER_DELETE, database);
    if (func != null) {
      if (func instanceof Function) {
        ClassTrigger.executeFunction(entity, (Function) func, database);
      } else if (func instanceof Object[]) {
        ClassTrigger.executeMethod(entity, (Object[]) func);
      }
    }
  }

  private static Object checkClzAttribute(
      final EntityImpl entity, String attr, DatabaseSessionInternal database) {
    final SchemaImmutableClass clz = EntityInternalUtils.getImmutableSchemaClass(database, entity);
    if (clz != null && clz.isTriggered()) {
      Function func = null;
      String fieldName = clz.getCustom(attr);
      SchemaClass superClz = clz.getSuperClass();
      while (fieldName == null || fieldName.length() == 0) {
        if (superClz == null || superClz.getName().equals(CLASSNAME)) {
          break;
        }
        fieldName = superClz.getCustom(attr);
        superClz = superClz.getSuperClass();
      }
      if (fieldName != null && fieldName.length() > 0) {
        // check if it is reflection or not
        final Object[] clzMethod = ClassTrigger.checkMethod(fieldName);
        if (clzMethod != null) {
          return clzMethod;
        }
        func = database.getMetadata().getFunctionLibrary().getFunction(fieldName);
        if (func == null) { // check if it is rid
          if (StringSerializerHelper.contains(fieldName, RID.SEPARATOR)) {
            try {
              try {
                EntityImpl funcEntity = database.load(new RecordId(fieldName));
                func =
                    database.getMetadata().getFunctionLibrary()
                        .getFunction(funcEntity.field("name"));
              } catch (RecordNotFoundException rnf) {
                // ignore
              }
            } catch (Exception ex) {
              LogManager.instance().error(ClassTrigger.class, "illegal record id : ", ex);
            }
          }
        }
      } else {
        final Object funcProp = entity.field(attr);
        if (funcProp != null) {
          final String funcName;
          if (funcProp instanceof EntityImpl) {
            funcName = ((EntityImpl) funcProp).field("name");
          } else {
            funcName = funcProp.toString();
          }
          func = database.getMetadata().getFunctionLibrary().getFunction(funcName);
        }
      }
      return func;
    }
    return null;
  }

  private static Object[] checkMethod(String fieldName) {
    String clzName = null;
    String methodName = null;
    if (fieldName.contains(METHOD_SEPARATOR)) {
      clzName = fieldName.substring(0, fieldName.lastIndexOf(METHOD_SEPARATOR));
      methodName = fieldName.substring(fieldName.lastIndexOf(METHOD_SEPARATOR) + 1);
    }
    if (clzName == null || methodName == null) {
      return null;
    }
    try {
      Class clz = ClassLoader.getSystemClassLoader().loadClass(clzName);
      Method method = clz.getMethod(methodName, EntityImpl.class);
      return new Object[]{clz, method};
    } catch (Exception ex) {
      LogManager.instance()
          .error(
              ClassTrigger.class, "illegal class or method : " + clzName + "/" + methodName, ex);
      return null;
    }
  }

  private static RecordHook.RESULT executeMethod(
      final EntityImpl entity, final Object[] clzMethod) {
    if (clzMethod[0] instanceof Class clz && clzMethod[1] instanceof Method method) {
      String result = null;
      try {
        result = (String) method.invoke(clz.newInstance(), entity);
      } catch (Exception ex) {
        throw BaseException.wrapException(
            new DatabaseException("Failed to invoke method " + method.getName()), ex);
      }
      if (result == null) {
        return RecordHook.RESULT.RECORD_NOT_CHANGED;
      }
      return RecordHook.RESULT.valueOf(result);
    }
    return RecordHook.RESULT.RECORD_NOT_CHANGED;
  }

  private static RecordHook.RESULT executeFunction(
      final EntityImpl entity, final Function func, DatabaseSessionInternal database) {
    if (func == null) {
      return RecordHook.RESULT.RECORD_NOT_CHANGED;
    }

    final ScriptManager scriptManager =
        database.getSharedContext().getYouTrackDB().getScriptManager();

    final ScriptEngine scriptEngine =
        scriptManager.acquireDatabaseEngine(database.getName(), func.getLanguage(database));
    try {
      final Bindings binding = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);

      scriptManager.bind(scriptEngine, binding, database, null, null);
      binding.put("doc", entity);

      String result = null;
      try {
        if (func.getLanguage(database) == null) {
          throw new ConfigurationException(
              "Database function '" + func.getName(database) + "' has no language");
        }
        final String funcStr = scriptManager.getFunctionDefinition(database, func);
        if (funcStr != null) {
          try {
            scriptEngine.eval(funcStr);
          } catch (ScriptException e) {
            scriptManager.throwErrorMessage(e, funcStr);
          }
        }
        if (scriptEngine instanceof Invocable invocableEngine) {
          Object[] empty = CommonConst.EMPTY_OBJECT_ARRAY;
          result = (String) invocableEngine.invokeFunction(func.getName(database), empty);
        }
      } catch (ScriptException e) {
        throw BaseException.wrapException(
            new CommandScriptException(
                "Error on execution of the script", func.getName(database), e.getColumnNumber()),
            e);
      } catch (NoSuchMethodException e) {
        throw BaseException.wrapException(
            new CommandScriptException("Error on execution of the script", func.getName(database),
                0), e);
      } catch (CommandScriptException e) {
        // PASS THROUGH
        throw e;

      } finally {
        scriptManager.unbind(scriptEngine, binding, null, null);
      }
      if (result == null) {
        return RecordHook.RESULT.RECORD_NOT_CHANGED;
      }
      return RecordHook.RESULT.valueOf(result);

    } finally {
      scriptManager.releaseDatabaseEngine(func.getLanguage(database), database.getName(),
          scriptEngine);
    }
  }
}
