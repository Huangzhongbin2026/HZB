package com.ruixiaomi.system.mapper;

import com.ruixiaomi.system.entity.ManualConsultMessageEntity;
import com.ruixiaomi.system.entity.ManualConsultSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ManualConsultMapper {

  int insertSession(ManualConsultSessionEntity entity);

  int updateSessionRouting(ManualConsultSessionEntity entity);

  int transferSession(@Param("sessionId") Long sessionId, @Param("serviceUserId") Long serviceUserId,
                      @Param("serviceDisplayName") String serviceDisplayName, @Param("serviceAlias") String serviceAlias,
                      @Param("serviceGroup") String serviceGroup, @Param("matchedBy") String matchedBy,
                      @Param("transferredFromUserId") Long transferredFromUserId,
                      @Param("transferredFromName") String transferredFromName);

  int closeSession(@Param("sessionId") Long sessionId, @Param("closedByType") String closedByType,
                   @Param("closedByName") String closedByName, @Param("satisfactionScore") Integer satisfactionScore,
                   @Param("satisfactionComment") String satisfactionComment);

  int touchSessionAfterMessage(@Param("sessionId") Long sessionId, @Param("preview") String preview,
                               @Param("targetUnreadField") String targetUnreadField);

  ManualConsultSessionEntity selectSessionById(@Param("id") Long id);

  List<ManualConsultSessionEntity> selectPortalSessions(@Param("questionerFeishuId") String questionerFeishuId);

  List<ManualConsultSessionEntity> selectAdminSessions(@Param("keyword") String keyword, @Param("status") String status,
                                                       @Param("currentUserId") Long currentUserId,
                                                       @Param("includeAll") boolean includeAll);

  List<ManualConsultSessionEntity> selectWorkbenchSessionsByUserName(@Param("currentUserName") String currentUserName);

  List<String> selectParticipantKeysByType(@Param("sessionId") Long sessionId, @Param("participantType") String participantType);

  int clearPortalUnread(@Param("sessionId") Long sessionId);

  int clearServiceUnread(@Param("sessionId") Long sessionId);

  int insertParticipant(@Param("sessionId") Long sessionId, @Param("participantType") String participantType,
                        @Param("participantKey") String participantKey, @Param("participantName") String participantName);

  int insertMessage(ManualConsultMessageEntity entity);

  List<ManualConsultMessageEntity> selectMessagesBySessionId(@Param("sessionId") Long sessionId);
}
