// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: performance.proto

package org.xgvela.oam.performance;

public interface StatsInfoReqOrBuilder extends
    // @@protoc_insertion_point(interface_extends:performance.StatsInfoReq)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>map&lt;string, double&gt; statsResultInfo = 1;</code>
   */
  int getStatsResultInfoCount();
  /**
   * <code>map&lt;string, double&gt; statsResultInfo = 1;</code>
   */
  boolean containsStatsResultInfo(
          String key);
  /**
   * Use {@link #getStatsResultInfoMap()} instead.
   */
  @Deprecated
  java.util.Map<String, Double>
  getStatsResultInfo();
  /**
   * <code>map&lt;string, double&gt; statsResultInfo = 1;</code>
   */
  java.util.Map<String, Double>
  getStatsResultInfoMap();
  /**
   * <code>map&lt;string, double&gt; statsResultInfo = 1;</code>
   */

  double getStatsResultInfoOrDefault(
          String key,
          double defaultValue);
  /**
   * <code>map&lt;string, double&gt; statsResultInfo = 1;</code>
   */

  double getStatsResultInfoOrThrow(
          String key);
}