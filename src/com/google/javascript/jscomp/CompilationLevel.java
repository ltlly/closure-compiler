/*
 * Copyright 2009 The Closure Compiler Authors.
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

package com.google.javascript.jscomp;

import com.google.javascript.jscomp.CompilerOptions.PropertyCollapseLevel;
import com.google.javascript.jscomp.CompilerOptions.Reach;
import org.jspecify.nullness.Nullable;

/**
 * A CompilationLevel represents the level of optimization that should be
 * applied when compiling JavaScript code.
 */
public enum CompilationLevel {
  /**
   * BUNDLE Simply orders and concatenates files to the output.
   */
  BUNDLE,

  /**
   * WHITESPACE_ONLY removes comments and extra whitespace in the input JS.
   */
  WHITESPACE_ONLY,

  /**
   * SIMPLE_OPTIMIZATIONS performs transformations to the input JS that do not
   * require any changes to JS that depend on the input JS. For example,
   * function arguments are renamed (which should not matter to code that
   * depends on the input JS), but functions themselves are not renamed (which
   * would otherwise require external code to change to use the renamed function
   * names).
   */
  SIMPLE_OPTIMIZATIONS,

  /**
   * ADVANCED_OPTIMIZATIONS aggressively reduces code size by renaming function
   * names and variables, removing code which is never called, etc.
   */
  ADVANCED_OPTIMIZATIONS,
  ;

  public static @Nullable CompilationLevel fromString(String value) {
    if (value == null) {
      return null;
    }
    switch (value) {
      case "BUNDLE":
        return CompilationLevel.BUNDLE;
      case "WHITESPACE_ONLY":
      case "WHITESPACE":
        return CompilationLevel.WHITESPACE_ONLY;
      case "SIMPLE_OPTIMIZATIONS":
      case "SIMPLE":
        return CompilationLevel.SIMPLE_OPTIMIZATIONS;
      case "ADVANCED_OPTIMIZATIONS":
      case "ADVANCED":
        return CompilationLevel.ADVANCED_OPTIMIZATIONS;
      default:
        return null;
    }
  }

  private CompilationLevel() {
  }

  public void setOptionsForCompilationLevel(CompilerOptions options) {
    switch (this) {
      case BUNDLE:
        break;
      case WHITESPACE_ONLY:
        applyBasicCompilationOptions(options);
        break;
      case SIMPLE_OPTIMIZATIONS:
        applySafeCompilationOptions(options);
        break;
      case ADVANCED_OPTIMIZATIONS:
        applyFullCompilationOptions(options);
        break;
    }
  }

  public void setDebugOptionsForCompilationLevel(CompilerOptions options) {
    options.generatePseudoNames = true;
    options.removeClosureAsserts = false;
    options.removeJ2clAsserts = false;
  }

  /**
   * Gets options that only strip whitespace and comments.
   *
   * @param options The CompilerOptions object to set the options on.
   */
  private static void applyBasicCompilationOptions(CompilerOptions options) {
    options.skipAllCompilerPasses();
  }

  /**
   * Add options that are safe. Safe means options that won't break the
   * JavaScript code even if no symbols are exported and no coding convention
   * is used.
   *
   * @param options The CompilerOptions object to set the options on.
   */
  private static void applySafeCompilationOptions(CompilerOptions options) {
    // TODO(tjgq): Remove this.
    options.setDependencyOptions(DependencyOptions.sortOnly());

    // ReplaceIdGenerators is on by default, but should run in simple mode.
    options.replaceIdGenerators = false;

    // Does not call applyBasicCompilationOptions(options) because the call to
    // skipAllCompilerPasses() cannot be easily undone.
    options.setClosurePass(true);
    options.setRenamingPolicy(VariableRenamingPolicy.LOCAL, PropertyRenamingPolicy.OFF);
    options.setInlineVariables(Reach.LOCAL_ONLY);
    options.setInlineFunctions(Reach.LOCAL_ONLY);
    options.setAssumeClosuresOnlyCaptureReferences(false);
    options.setWarningLevel(DiagnosticGroups.GLOBAL_THIS, CheckLevel.OFF);
    options.setFoldConstants(true);
    options.setCoalesceVariableNames(true);
    options.setDeadAssignmentElimination(true);
    options.setCollapseVariableDeclarations(true);
    options.convertToDottedProperties = true;
    options.labelRenaming = true;
    options.setRemoveUnreachableCode(true);
    options.setOptimizeArgumentsArray(true);
    options.setRemoveUnusedVariables(Reach.LOCAL_ONLY);
    options.collapseObjectLiterals = true;
    options.setProtectHiddenSideEffects(true);
  }

  /**
   * Add the options that will work only if the user exported all the symbols
   * correctly.
   *
   * @param options The CompilerOptions object to set the options on.
   */
  private static void applyFullCompilationOptions(CompilerOptions options) {
    // TODO(tjgq): Remove this.
    options.setDependencyOptions(DependencyOptions.sortOnly());

    // Do not call applySafeCompilationOptions(options) because the call can
    // create possible conflicts between multiple diagnostic groups.

    options.setCheckSymbols(true);
    options.setCheckTypes(true);

    // All the safe optimizations.
    options.setClosurePass(true);
    options.setFoldConstants(true);
    options.setCoalesceVariableNames(true);
    options.setDeadAssignmentElimination(true);
    options.setExtractPrototypeMemberDeclarations(true);
    options.setCollapseVariableDeclarations(true);
    options.setConvertToDottedProperties(true);
    options.setLabelRenaming(true);
    options.setRemoveUnreachableCode(true);
    options.setOptimizeArgumentsArray(true);
    options.setCollapseObjectLiterals(true);
    options.setProtectHiddenSideEffects(true);

    // All the advanced optimizations.
    //高级pass todo(ltlly) 阅读pass

    //closureCodeRemoval     /** Remove variables set to goog.abstractMethod. */
    //不知道什么情况下 会是 goog.abstractMethod
    options.setRemoveClosureAsserts(true);
    options.setRemoveAbstractMethods(true);


    //gatherRawExports       /** Raw exports processing pass. */
    //应该是收集js中的export的
    options.setReserveRawExports(true);

    //renameVars 重命名变量 也许改成 VariableRenamingPolicy.LOCAL 更合理
    //renameProperties 重命名属性  PropertyRenamingPolicy.ALL_UNQUOTED 会重命名未显式引用与未在externs文件中定义的属性 也许off更好


    //如果为ALL_UNQUOTED 会重命名wx的api，变成wx.G 这样的形式， 两种解法 1是为所有wx的api externs 2为local
    // todo(ltlly)这里改成了off 待观察
    options.setRenamingPolicy(VariableRenamingPolicy.ALL, PropertyRenamingPolicy.OFF);

    //removeUnusedCode 的一项 移除未使用的Prototype属性 同时 inline getter
    options.setRemoveUnusedPrototypeProperties(true);

    //removeUnusedCode 移除未使用的class属性
    options.setRemoveUnusedClassProperties(true);

    //collapseAnonymousFunctions  折叠匿名函数,来避免使用var关键字
    options.setCollapseAnonymousFunctions(true);

    //inlineAndCollapseProperties 内联别名 折叠限定名称 即 a.b变成 a$b
    options.setCollapsePropertiesLevel(PropertyCollapseLevel.ALL);

    //warn等级... 没啥用
    options.setWarningLevel(DiagnosticGroups.GLOBAL_THIS, CheckLevel.WARNING);

    //重写FunctionExpression 为啥是关啊...
    options.setRewriteFunctionExpressions(false);

    //removeUnusedCode 移除未使用的变量 和 PrototypeProperties
    options.setSmartNameRemoval(true);

    //inlineConstants 折叠常量
    options.setInlineConstantVars(true);

    //inlineFunctions 内联函数
    options.setInlineFunctions(Reach.ALL);

    //inlineFunctions  假设闭包只捕获引用 但是是关的
    options.setAssumeClosuresOnlyCaptureReferences(false);

    //inlineVariables  earlyInlineVariables  flowSensitiveInlineVariables 内联变量
    options.setInlineVariables(Reach.ALL);

    //checkRegExp 检查正则的调用
    //markPureFunctions 标记纯函数（无副作用函数）
    options.setComputeFunctionSideEffects(true);

    //无对应pass
    options.setAssumeStrictThis(true);

    // Remove unused vars also removes unused functions.
    //removeUnusedCode 移除未使用的var 和localvar
    options.setRemoveUnusedVariables(Reach.ALL);

    // Move code around based on the defined modules.
    //无对应pass
    options.setCrossChunkCodeMotion(true);
    options.setCrossChunkMethodMotion(true);

    // Call optimizations

    //devirtualizeMethods 实例方法改为static方法
    options.setDevirtualizeMethods(true);


    //optimizeCalls 优化未使用的函数参数、未使用的返回值，并内联常量参数。此外，它还会运行移除未使用代码的优化。
    options.setOptimizeCalls(true);


    //optimizeConstructors 如果显式构造函数没用，就删除
    options.setOptimizeESClassConstructors(true);
  }


  private static void applyFullCompilationOptions_back(CompilerOptions options) {
    // TODO(tjgq): Remove this.
    options.setDependencyOptions(DependencyOptions.sortOnly());

    // Do not call applySafeCompilationOptions(options) because the call can
    // create possible conflicts between multiple diagnostic groups.

    options.setCheckSymbols(true);
    options.setCheckTypes(true);

    // All the safe optimizations.
    options.setClosurePass(true);
    options.setFoldConstants(true);
    options.setCoalesceVariableNames(true);
    options.setDeadAssignmentElimination(true);
    options.setExtractPrototypeMemberDeclarations(true);
    options.setCollapseVariableDeclarations(true);
    options.setConvertToDottedProperties(true);
    options.setLabelRenaming(true);
    options.setRemoveUnreachableCode(true);
    options.setOptimizeArgumentsArray(true);
    options.setCollapseObjectLiterals(true);
    options.setProtectHiddenSideEffects(true);

    // All the advanced optimizations.
    //高级pass todo(ltlly) 阅读pass

    //closureCodeRemoval     /** Remove variables set to goog.abstractMethod. */
    //不知道什么情况下 会是 goog.abstractMethod
    options.setRemoveClosureAsserts(true);
    options.setRemoveAbstractMethods(true);

    //gatherRawExports       /** Raw exports processing pass. */
    //应该是收集js中的export的
    options.setReserveRawExports(true);

    //renameVars 重命名变量 也许改成 VariableRenamingPolicy.LOCAL 更合理
    //renameProperties 重命名属性  PropertyRenamingPolicy.ALL_UNQUOTED 会重命名未显式引用与未在externs文件中定义的属性 也许off更好


    options.setRenamingPolicy(VariableRenamingPolicy.ALL, PropertyRenamingPolicy.ALL_UNQUOTED);

    //removeUnusedCode 的一项 移除未使用的Prototype属性 同时 inline getter
    options.setRemoveUnusedPrototypeProperties(true);

    //removeUnusedCode 移除未使用的class属性
    options.setRemoveUnusedClassProperties(true);

    //collapseAnonymousFunctions  折叠匿名函数,来避免使用var关键字
    options.setCollapseAnonymousFunctions(true);

    //inlineAndCollapseProperties 内联别名 折叠限定名称 即 a.b变成 a$b
    options.setCollapsePropertiesLevel(PropertyCollapseLevel.ALL);

    //warn等级... 没啥用
    options.setWarningLevel(DiagnosticGroups.GLOBAL_THIS, CheckLevel.WARNING);

    //重写FunctionExpression 为啥是关啊...
    options.setRewriteFunctionExpressions(false);

    //removeUnusedCode 移除未使用的变量 和 PrototypeProperties
    options.setSmartNameRemoval(true);

    //inlineConstants 折叠常量
    options.setInlineConstantVars(true);

    //inlineFunctions 内联函数
    options.setInlineFunctions(Reach.ALL);

    //inlineFunctions  假设闭包只捕获引用 但是是关的
    options.setAssumeClosuresOnlyCaptureReferences(false);

    //inlineVariables  earlyInlineVariables  flowSensitiveInlineVariables 内联变量
    options.setInlineVariables(Reach.ALL);

    //checkRegExp 检查正则的调用
    //markPureFunctions 标记纯函数（无副作用函数）
    options.setComputeFunctionSideEffects(true);

    //无对应pass
    options.setAssumeStrictThis(true);

    // Remove unused vars also removes unused functions.
    //removeUnusedCode 移除未使用的var 和localvar
    options.setRemoveUnusedVariables(Reach.ALL);

    // Move code around based on the defined modules.
    //无对应pass
    options.setCrossChunkCodeMotion(true);
    options.setCrossChunkMethodMotion(true);

    // Call optimizations

    //devirtualizeMethods 实例方法改为static方法
    options.setDevirtualizeMethods(true);
    //optimizeCalls 优化未使用的函数参数、未使用的返回值，并内联常量参数。此外，它还会运行移除未使用代码的优化。
    options.setOptimizeCalls(true);
    //optimizeConstructors 如果显式构造函数没用，就删除
    options.setOptimizeESClassConstructors(true);
  }

  /**
   * Enable additional optimizations that use type information. Only has
   * an effect for ADVANCED_OPTIMIZATIONS; this is a no-op for other modes.
   *
   * @param options The CompilerOptions object to set the options on.
   */
  public void setTypeBasedOptimizationOptions(CompilerOptions options) {
    switch (this) {
      case ADVANCED_OPTIMIZATIONS:
        options.setDisambiguateProperties(true);
        options.setAmbiguateProperties(true);
        options.setInlineProperties(true);
        options.setUseTypesForLocalOptimization(true);
        break;
      case SIMPLE_OPTIMIZATIONS:
      case WHITESPACE_ONLY:
      case BUNDLE:
        break;
    }
  }

  /**
   * Enable additional optimizations that operate on global declarations. Advanced mode does
   * this by default, but this isn't valid in simple mode in the general case and should only
   * be enabled when code is self contained (such as when it is enclosed by a function wrapper.
   *
   * @param options The CompilerOptions object to set the options on.
   */
  public void setWrappedOutputOptimizations(CompilerOptions options) {
    // Global variables and properties names can't conflict.
    options.reserveRawExports = false;
    switch (this) {
      case SIMPLE_OPTIMIZATIONS:
        // Enable global variable optimizations (but not property optimizations)
        options.setVariableRenaming(VariableRenamingPolicy.ALL);
        options.setCollapsePropertiesLevel(PropertyCollapseLevel.MODULE_EXPORT);
        options.setCollapseAnonymousFunctions(true);
        options.setInlineConstantVars(true);
        options.setInlineFunctions(Reach.ALL);
        options.setInlineVariables(Reach.ALL);
        options.setRemoveUnusedVariables(Reach.ALL);
        break;
      case ADVANCED_OPTIMIZATIONS:
      case WHITESPACE_ONLY:
      case BUNDLE:
        break;
    }
  }
}

