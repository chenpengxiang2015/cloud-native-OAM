// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: performance.proto

package org.xgvela.oam.performance;

public final class performance {
  private performance() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_performance_StatsInfoReq_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_performance_StatsInfoReq_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_performance_StatsInfoReq_StatsResultInfoEntry_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_performance_StatsInfoReq_StatsResultInfoEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_performance_StatsInfoRsp_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_performance_StatsInfoRsp_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n\021performance.proto\022\013performance\"\217\001\n\014Sta" +
      "tsInfoReq\022G\n\017statsResultInfo\030\001 \003(\0132..per" +
      "formance.StatsInfoReq.StatsResultInfoEnt" +
      "ry\0326\n\024StatsResultInfoEntry\022\013\n\003key\030\001 \001(\t\022" +
      "\r\n\005value\030\002 \001(\001:\0028\001\"\036\n\014StatsInfoRsp\022\016\n\006re" +
      "sult\030\001 \001(\t2b\n\025PerfStatisticsService\022I\n\rS" +
      "tatisticsReq\022\031.performance.StatsInfoReq\032" +
      "\031.performance.StatsInfoRsp\"\0000\001B9\n\032com.in" +
      "spur.oam.performanceB\013performanceP\001\242\002\013pe" +
      "rformanceb\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_performance_StatsInfoReq_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_performance_StatsInfoReq_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_performance_StatsInfoReq_descriptor,
        new String[] { "StatsResultInfo", });
    internal_static_performance_StatsInfoReq_StatsResultInfoEntry_descriptor =
      internal_static_performance_StatsInfoReq_descriptor.getNestedTypes().get(0);
    internal_static_performance_StatsInfoReq_StatsResultInfoEntry_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_performance_StatsInfoReq_StatsResultInfoEntry_descriptor,
        new String[] { "Key", "Value", });
    internal_static_performance_StatsInfoRsp_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_performance_StatsInfoRsp_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_performance_StatsInfoRsp_descriptor,
        new String[] { "Result", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
