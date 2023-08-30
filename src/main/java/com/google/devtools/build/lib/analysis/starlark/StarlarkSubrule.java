// Copyright 2023 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.analysis.starlark;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.analysis.BazelRuleAnalysisThreadContext;
import com.google.devtools.build.lib.starlarkbuildapi.StarlarkSubruleApi;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

/** Represents a subrule which can be invoked in a Starlark rule's implementation function. */
public class StarlarkSubrule implements StarlarkCallable, StarlarkSubruleApi {
  // TODO(hvd) this class is a WIP, will be implemented over many commits

  private final StarlarkFunction implementation;

  public StarlarkSubrule(StarlarkFunction implementation) {
    this.implementation = implementation;
  }

  @Override
  public String getName() {
    return String.format("subrule(%s)", implementation.getName());
  }

  @Override
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
      throws EvalException, InterruptedException {
    StarlarkRuleContext ruleContext =
        BazelRuleAnalysisThreadContext.fromOrFail(thread, getName())
            .getRuleContext()
            .getStarlarkRuleContext();
    ImmutableSet<? extends StarlarkSubruleApi> declaredSubrules = ruleContext.getSubrules();
    if (!declaredSubrules.contains(this)) {
      throw getUndeclaredSubruleError(ruleContext);
    }
    SubruleContext subruleContext = new SubruleContext();
    ImmutableList<Object> positionals =
        ImmutableList.builder().add(subruleContext).addAll(args).build();
    return Starlark.call(thread, implementation, positionals, kwargs);
  }

  private EvalException getUndeclaredSubruleError(StarlarkRuleContext starlarkRuleContext) {
    if (starlarkRuleContext.isForAspect()) {
      return Starlark.errorf(
          "aspect '%s' must declare '%s' in 'subrules'",
          starlarkRuleContext.getRuleContext().getMainAspect().getAspectClass().getName(),
          this.getName());
    } else {
      return Starlark.errorf(
          "rule '%s' must declare '%s' in 'subrules'",
          starlarkRuleContext.getRuleContext().getRule().getRuleClass(), this.getName());
    }
  }

  private static class SubruleContext {}
}