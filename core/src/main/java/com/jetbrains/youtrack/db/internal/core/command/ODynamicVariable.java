package com.jetbrains.youtrack.db.internal.core.command;

public interface ODynamicVariable {

  Object resolve(CommandContext contex);
}
