/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.execution.testframework.sm.runner.events;

import jetbrains.buildServer.messages.serviceMessages.TestFinished;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Simonchik
 */
public class TestFinishedEvent extends TreeNodeEvent {

  private final int myDuration;

  public TestFinishedEvent(@NotNull TestFinished testFinished, int duration) {
    this(testFinished.getTestName(), TreeNodeEvent.getNodeId(testFinished), duration);
  }

  public TestFinishedEvent(@Nullable String name, int id, int duration) {
    super(name, id);
    myDuration = duration;
  }

  public TestFinishedEvent(@NotNull String name, int duration) {
    this(name, -1, duration);
  }

  public int getDuration() {
    return myDuration;
  }

  @Override
  protected void appendToStringInfo(@NotNull StringBuilder buf) {
    append(buf, "duration", myDuration);
  }
}
