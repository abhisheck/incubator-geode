/*
 * =========================================================================
 *  Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.

 *  This product is protected by U.S. and international copyright
 *  and intellectual property laws. Pivotal products are covered by
 *  more patents listed at http://www.pivotal.io/patents.
 * ========================================================================
 */
package com.gemstone.gemfire.management.internal.cli.functions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.execute.Execution;
import com.gemstone.gemfire.cache.execute.Function;
import com.gemstone.gemfire.cache.execute.FunctionContext;
import com.gemstone.gemfire.cache.execute.FunctionException;
import com.gemstone.gemfire.cache.execute.FunctionService;
import com.gemstone.gemfire.cache.execute.ResultCollector;
import com.gemstone.gemfire.distributed.DistributedMember;
import com.gemstone.gemfire.internal.ClassPathLoader;
import com.gemstone.gemfire.internal.InternalEntity;
import com.gemstone.gemfire.management.internal.cli.GfshParser;
import com.gemstone.gemfire.management.internal.cli.i18n.CliStrings;

/**
 * @author Ajay Pande
 * 
 * @since 7.0
 */
public class UserFunctionExecution implements Function, InternalEntity {
  public static final String ID = UserFunctionExecution.class.getName();

  private static final long serialVersionUID = 1L;

  @Override
  public void execute(FunctionContext context) {   
    try {      
      Cache cache = CacheFactory.getAnyInstance();
      DistributedMember member = cache.getDistributedSystem()
          .getDistributedMember();
      String[] functionArgs = null;
      Object[] args = (Object[]) context.getArguments();
      if (args != null) {
        String functionId = ((String) args[0]);
        String filterString = ((String) args[1]);
        String resultCollectorName = ((String) args[2]);
        String argumentsString = ((String) args[3]);
        String onRegion = ((String) args[4]);

        try {
          if (argumentsString != null && argumentsString.length() > 0) {
            functionArgs = argumentsString.split(",");
          }
          Set<String> filters = new HashSet<String>();
          ResultCollector resultCollectorInstance = null;
          if (resultCollectorName != null && resultCollectorName.length() > 0) {
            resultCollectorInstance = (ResultCollector) ClassPathLoader
                .getLatest().forName(resultCollectorName).newInstance();
          }
          if (filterString != null && filterString.length() > 0) {
            filters.add(filterString);
          }
          Function function = FunctionService.getFunction(functionId);

          if (function == null) {
            context
                .getResultSender()
                .lastResult(
                    CliStrings
                        .format(
                            CliStrings.EXECUTE_FUNCTION__MSG__DOES_NOT_HAVE_FUNCTION_0_REGISTERED,
                            functionId));
          } else {
            Execution execution = null;
            if (onRegion != null && onRegion.length() > 0) {
              Region region = cache.getRegion(onRegion);
              if (region != null) {
                execution = FunctionService.onRegion(region);
              }
            } else {
              execution = FunctionService.onMember(member);
            }

            if (execution != null) {
              if (resultCollectorInstance != null) {
                execution = execution.withCollector(resultCollectorInstance);
              }

              if (functionArgs != null && functionArgs.length > 0) {
                execution = execution.withArgs(functionArgs);
              }
              if (filters != null && filters.size() > 0) {
                execution = execution.withFilter(filters);
              }

              List<Object> results = (List<Object>) execution.execute(function)
                  .getResult();

              StringBuilder resultMessege = new StringBuilder();
              if (results != null) {
                for (Object resultObj : results) {
                  if (resultObj != null) {
                    if (resultObj instanceof String) {
                      resultMessege.append(((String) resultObj));
                      resultMessege.append(GfshParser.LINE_SEPARATOR);
                    } else if (resultObj instanceof Exception) {
                      resultMessege
                          .append(((IllegalArgumentException) resultObj)
                              .getMessage());
                    } else {
                      resultMessege.append(resultObj);
                      resultMessege.append(GfshParser.LINE_SEPARATOR);
                    }
                  }
                }
              }
              context.getResultSender().lastResult(resultMessege);
            } else {
              context
                  .getResultSender()
                  .lastResult(
                      CliStrings
                          .format(
                              CliStrings.EXECUTE_FUNCTION__MSG__ERROR_IN_EXECUTING_0_ON_MEMBER_1_ON_REGION_2_DETAILS_3,
                              functionId,
                              member.getId(),
                              onRegion,
                              CliStrings.EXECUTE_FUNCTION__MSG__ERROR_IN_RETRIEVING_EXECUTOR));
            }
          }

        } catch (ClassNotFoundException e) {
          context
              .getResultSender()
              .lastResult(
                  CliStrings
                      .format(
                          CliStrings.EXECUTE_FUNCTION__MSG__RESULT_COLLECTOR_0_NOT_FOUND_ERROR_1,
                          resultCollectorName, e.getMessage()));
        } catch (FunctionException e) {
          context
              .getResultSender()
              .lastResult(
                  CliStrings
                      .format(
                          CliStrings.EXECUTE_FUNCTION__MSG__ERROR_IN_EXECUTING_ON_MEMBER_1_DETAILS_2,
                          functionId, member.getId(), e.getMessage()));
        } catch (CancellationException e) {
          context
              .getResultSender()
              .lastResult(
                  CliStrings
                      .format(
                          CliStrings.EXECUTE_FUNCTION__MSG__ERROR_IN_EXECUTING_ON_MEMBER_1_DETAILS_2,
                          functionId, member.getId(), e.getMessage()));
        } catch (InstantiationException e) {
          context
              .getResultSender()
              .lastResult(
                  CliStrings
                      .format(
                          CliStrings.EXECUTE_FUNCTION__MSG__RESULT_COLLECTOR_0_NOT_FOUND_ERROR_1,
                          resultCollectorName, e.getMessage()));
        } catch (IllegalAccessException e) {
          context
              .getResultSender()
              .lastResult(
                  CliStrings
                      .format(
                          CliStrings.EXECUTE_FUNCTION__MSG__RESULT_COLLECTOR_0_NOT_FOUND_ERROR_1,
                          resultCollectorName, e.getMessage()));
        } catch (Exception e) {
          context
              .getResultSender()
              .lastResult(
                  CliStrings
                      .format(
                          CliStrings.EXECUTE_FUNCTION__MSG__ERROR_IN_EXECUTING_ON_MEMBER_1_DETAILS_2,
                          functionId, member.getId(), e.getMessage()));
        }
      } else {
        context.getResultSender().lastResult(
            CliStrings.EXECUTE_FUNCTION__MSG__COULD_NOT_RETRIEVE_ARGUMENTS);
      }

    } catch(Exception ex){
      context.getResultSender().lastResult(ex.getMessage());
    } 
  }

  @Override
  public String getId() {
    return UserFunctionExecution.ID;
  }

  @Override
  public boolean hasResult() {
    return true;
  }

  @Override
  public boolean optimizeForWrite() {
    // no need of optimization since read-only.
    return false;
  }

  @Override
  public boolean isHA() {
    return false;
  }

}