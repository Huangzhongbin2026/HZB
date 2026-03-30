package com.ruixiaomi.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruixiaomi.system.dto.PortalAiSubmitDTO;
import com.ruixiaomi.system.dto.PortalAssessmentPreviewDTO;
import com.ruixiaomi.system.dto.PortalAssessmentSubmitDTO;
import com.ruixiaomi.system.dto.PortalDueChangeDuplicateCheckDTO;
import com.ruixiaomi.system.dto.PortalDueChangePreviewDTO;
import com.ruixiaomi.system.dto.PortalDueChangeSubmitDTO;
import com.ruixiaomi.system.dto.PortalExpeditedDuplicateCheckDTO;
import com.ruixiaomi.system.dto.PortalExpeditedPreviewDTO;
import com.ruixiaomi.system.dto.PortalExpeditedSubmitDTO;
import com.ruixiaomi.system.dto.PortalManualSubmitDTO;
import com.ruixiaomi.system.dto.PortalUserContextDTO;
import com.ruixiaomi.system.entity.CustomerDueChangeTaskEntity;
import com.ruixiaomi.system.entity.DeliveryAssessmentTaskEntity;
import com.ruixiaomi.system.entity.ExpeditedTaskEntity;
import com.ruixiaomi.system.entity.PortalTaskRecordEntity;
import com.ruixiaomi.system.entity.SysRoleEntity;
import com.ruixiaomi.system.entity.SysUserEntity;
import com.ruixiaomi.system.exception.BusinessException;
import com.ruixiaomi.system.mapper.PortalMapper;
import com.ruixiaomi.system.mapper.RbacMapper;
import com.ruixiaomi.system.vo.AuthSessionVO;
import com.ruixiaomi.system.vo.PortalAiParseVO;
import com.ruixiaomi.system.vo.PortalAssessmentPreviewVO;
import com.ruixiaomi.system.vo.PortalAssessmentSubmitResultVO;
import com.ruixiaomi.system.vo.PortalBootstrapVO;
import com.ruixiaomi.system.vo.PortalDueChangeDuplicateCheckVO;
import com.ruixiaomi.system.vo.PortalDueChangePreviewVO;
import com.ruixiaomi.system.vo.PortalDueChangeSubmitResultVO;
import com.ruixiaomi.system.vo.PortalExpeditedDuplicateCheckVO;
import com.ruixiaomi.system.vo.PortalExpeditedPreviewVO;
import com.ruixiaomi.system.vo.PortalHistoryVO;
import com.ruixiaomi.system.vo.PortalUploadProofVO;
import com.ruixiaomi.system.vo.UserVO;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalService {

  private static final String QUESTIONER_ROLE_CODE = "questioner";
  private static final String QUESTIONER_ROLE_NAME = "提问人";

  private final PortalMapper portalMapper;
  private final RbacMapper rbacMapper;
  private final TaskService taskService;
  private final AuthService authService;
  private final PasswordEncoder passwordEncoder;
  private final PortalAiService portalAiService;
  private final PortalAssessmentService portalAssessmentService;
  private final PortalDueChangeService portalDueChangeService;
  private final PortalExpeditedService portalExpeditedService;
  private final TaskDataRecordService taskDataRecordService;
  private final ObjectMapper objectMapper;

  public PortalService(PortalMapper portalMapper, RbacMapper rbacMapper, TaskService taskService, AuthService authService,
                       PasswordEncoder passwordEncoder, PortalAiService portalAiService,
                       PortalAssessmentService portalAssessmentService,
                       PortalDueChangeService portalDueChangeService,
                       PortalExpeditedService portalExpeditedService,
                       TaskDataRecordService taskDataRecordService,
                       ObjectMapper objectMapper) {
    this.portalMapper = portalMapper;
    this.rbacMapper = rbacMapper;
    this.taskService = taskService;
    this.authService = authService;
    this.passwordEncoder = passwordEncoder;
    this.portalAiService = portalAiService;
    this.portalAssessmentService = portalAssessmentService;
    this.portalDueChangeService = portalDueChangeService;
    this.portalExpeditedService = portalExpeditedService;
    this.taskDataRecordService = taskDataRecordService;
    this.objectMapper = objectMapper;
  }

  @Transactional(rollbackFor = Exception.class)
  public PortalBootstrapVO bootstrap(PortalUserContextDTO request) {
    SysUserEntity user = ensurePortalUser(request.displayName(), request.feishuId());
    return new PortalBootstrapVO(toPortalUserVO(user), listHistories(request.feishuId()));
  }

  @Transactional(rollbackFor = Exception.class)
  public PortalHistoryVO submitManual(PortalManualSubmitDTO request) {
    SysUserEntity user = ensurePortalUser(request.displayName(), request.feishuId());
    String taskType = normalizeTaskType(request.taskType());
    Map<String, String> payload = request.payload() == null ? Map.of() : request.payload();
    Object createdTask = createTaskByType(taskType, payload, user);
    String sourceText = request.sourceText() == null || request.sourceText().isBlank()
      ? buildManualSourceText(taskType, payload)
      : request.sourceText().trim();
    return saveRecord(taskType, payload, sourceText, createdTask, user);
  }

  @Transactional(rollbackFor = Exception.class)
  public PortalHistoryVO submitAi(PortalAiSubmitDTO request) {
    SysUserEntity user = ensurePortalUser(request.displayName(), request.feishuId());
    PortalAiService.PortalAiParseResult parseResult = portalAiService.parseSubmission(request.content());
    Map<String, String> sanitizedPayload = sanitizeAiPayload(parseResult.taskType(), parseResult.payload(), request.content());
    Object createdTask = createTaskByType(parseResult.taskType(), sanitizedPayload, user);
    return saveRecord(parseResult.taskType(), sanitizedPayload, request.content(), createdTask, user);
  }

  public PortalAiParseVO parseAi(PortalAiSubmitDTO request) {
    ensurePortalUser(request.displayName(), request.feishuId());
    PortalAiService.PortalAiParseResult parseResult = portalAiService.parseSubmission(request.content());
    return new PortalAiParseVO(
      parseResult.taskType(),
      parseResult.taskTypeLabel(),
      sanitizeAiPayload(parseResult.taskType(), parseResult.payload(), request.content())
    );
  }

  public PortalAssessmentPreviewVO previewAssessment(PortalAssessmentPreviewDTO request) {
    SysUserEntity user = ensurePortalUser(request.displayName(), request.feishuId());
    Map<String, String> payload = portalAssessmentService.normalizePayloadForRecord(
      request.crmNo(), request.productInformation(), request.needTime(), request.remarks());
    String sourceText = request.sourceText() == null || request.sourceText().isBlank()
      ? buildManualSourceText("ASSESSMENT", payload)
      : request.sourceText().trim();
    Long recordId = taskDataRecordService.prepareRecord(request.recordId(), "ASSESSMENT", user, payload, sourceText);
    taskDataRecordService.startStage(recordId, "PREVIEW");
    try {
      PortalAssessmentPreviewVO preview = portalAssessmentService.preview(request.crmNo(), request.productInformation(), request.needTime(),
        request.remarks(), request.displayName());
      taskDataRecordService.finishStageSuccess(recordId, "PREVIEW", payload, preview, null, null, false);
      return new PortalAssessmentPreviewVO(
        recordId,
        preview.crmNo(),
        preview.stockPrepareDate(),
        preview.inventoryDetailUrl(),
        preview.needTime(),
        preview.remarks(),
        preview.duplicatedToday(),
        preview.todayDuplicateCount(),
        preview.duplicatedHistory(),
        preview.historyDuplicateCount(),
        preview.todayDuplicateTasks(),
        preview.productTable()
      );
    } catch (RuntimeException exception) {
      taskDataRecordService.finishStageFailure(recordId, "PREVIEW", payload, null, exception.getMessage());
      throw exception;
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public PortalAssessmentSubmitResultVO submitAssessment(PortalAssessmentSubmitDTO request) {
    SysUserEntity user = ensurePortalUser(request.displayName(), request.feishuId());
    Map<String, String> payload = portalAssessmentService.normalizePayloadForRecord(
      request.crmNo(), request.productInformation(), request.needTime(), request.remarks());
    if (request.duplicateAction() != null && !request.duplicateAction().isBlank()) {
      payload = new java.util.LinkedHashMap<>(payload);
      payload.put("duplicateAction", request.duplicateAction().trim());
    }
    String sourceText = request.sourceText() == null || request.sourceText().isBlank()
      ? buildManualSourceText("ASSESSMENT", payload)
      : request.sourceText().trim();
    Long recordId = taskDataRecordService.prepareRecord(request.recordId(), "ASSESSMENT", user, payload, sourceText);
    taskDataRecordService.startStage(recordId, "SUBMIT");
    try {
      PortalAssessmentService.AssessmentSubmitResult submitResult = portalAssessmentService.submit(request, user.getDisplayName());
      PortalHistoryVO history = saveRecord("ASSESSMENT", payload, sourceText, submitResult.tasks(), user);
      taskDataRecordService.finishStageSuccess(
        recordId,
        "SUBMIT",
        payload,
        submitResult,
        buildAssessmentSubmitTracePayload(request),
        resolveTaskIds(submitResult.tasks()),
        true
      );
      return new PortalAssessmentSubmitResultVO(
        history,
        submitResult.tasks().size(),
        submitResult.rejectedMessages().size(),
        submitResult.rejectedMessages(),
        submitResult.joinedExisting()
      );
    } catch (RuntimeException exception) {
      taskDataRecordService.finishStageFailure(recordId, "SUBMIT", payload, buildAssessmentSubmitTracePayload(request), exception.getMessage());
      throw exception;
    }
  }

  public PortalDueChangePreviewVO previewDueChange(PortalDueChangePreviewDTO request) {
    SysUserEntity user = ensurePortalUser(request.displayName(), request.feishuId());
    Map<String, String> payload = portalDueChangeService.normalizePayloadForRecord(
      request.questionType(), request.contractNo(), request.projectName(), request.targetDeliveryDate(), request.changeReason(),
      request.allowPartialShipmentIfIncomplete(), request.delayProofUrl(), false);
    String sourceText = request.sourceText() == null || request.sourceText().isBlank()
      ? buildManualSourceText("DUE_CHANGE", payload)
      : request.sourceText().trim();
    Long recordId = taskDataRecordService.prepareRecord(request.recordId(), "DUE_CHANGE", user, payload, sourceText);
    taskDataRecordService.startStage(recordId, "PREVIEW");
    try {
      PortalDueChangePreviewVO preview = portalDueChangeService.preview(request.questionType(), request.contractNo());
      taskDataRecordService.finishStageSuccess(recordId, "PREVIEW", payload, preview, null, null, false);
      return new PortalDueChangePreviewVO(
        recordId,
        preview.questionType(),
        preview.contractNo(),
        preview.crmNo(),
        preview.salesDept(),
        preview.projectName(),
        preview.topFieldsEditable(),
        preview.minCustomerExpectedDate(),
        preview.maxCustomerExpectedDate(),
        preview.productTable()
      );
    } catch (RuntimeException exception) {
      taskDataRecordService.finishStageFailure(recordId, "PREVIEW", payload, null, exception.getMessage());
      throw exception;
    }
  }

  public PortalDueChangeDuplicateCheckVO checkDueChangeDuplicate(PortalDueChangeDuplicateCheckDTO request) {
    SysUserEntity user = ensurePortalUser(request.displayName(), request.feishuId());
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("questionType", defaultIfBlank(request.questionType(), "客期提前"));
    payload.put("contractNo", request.contractNo() == null ? "" : request.contractNo().replaceAll("\\s+", ""));
    Long recordId = taskDataRecordService.prepareRecord(request.recordId(), "DUE_CHANGE", user, payload, "");
    taskDataRecordService.startStage(recordId, "DUPLICATE_CHECK");
    try {
      PortalDueChangeDuplicateCheckVO result = portalDueChangeService.checkDuplicate(request.questionType(), request.contractNo());
      taskDataRecordService.finishStageSuccess(recordId, "DUPLICATE_CHECK", payload, result, null, null, false);
      return new PortalDueChangeDuplicateCheckVO(recordId, result.duplicated(), result.duplicateCount());
    } catch (RuntimeException exception) {
      taskDataRecordService.finishStageFailure(recordId, "DUPLICATE_CHECK", payload, null, exception.getMessage());
      throw exception;
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public PortalDueChangeSubmitResultVO submitDueChange(PortalDueChangeSubmitDTO request) {
    SysUserEntity user = ensurePortalUser(request.displayName(), request.feishuId());
    Map<String, String> payload = portalDueChangeService.normalizePayloadForRecord(
      request.questionType(), request.contractNo(), request.projectName(), request.targetDeliveryDate(),
      request.changeReason(), request.allowPartialShipmentIfIncomplete(), request.delayProofUrl(),
      Boolean.TRUE.equals(request.repeatSubmitConfirmed()));
    String sourceText = request.sourceText() == null || request.sourceText().isBlank()
      ? buildManualSourceText("DUE_CHANGE", payload)
      : request.sourceText().trim();
    Long recordId = taskDataRecordService.prepareRecord(request.recordId(), "DUE_CHANGE", user, payload, sourceText);
    taskDataRecordService.startStage(recordId, "SUBMIT");
    try {
      PortalDueChangeService.DueChangeSubmitResult submitResult = portalDueChangeService.submit(request, user.getDisplayName());
      PortalHistoryVO history = saveRecord("DUE_CHANGE", payload, sourceText, submitResult.tasks(), user);
      taskDataRecordService.finishStageSuccess(
        recordId,
        "SUBMIT",
        payload,
        submitResult,
        buildDueChangeSubmitTracePayload(request),
        resolveTaskIds(submitResult.tasks()),
        true
      );
      return new PortalDueChangeSubmitResultVO(history, submitResult.tasks().size());
    } catch (RuntimeException exception) {
      taskDataRecordService.finishStageFailure(recordId, "SUBMIT", payload, buildDueChangeSubmitTracePayload(request), exception.getMessage());
      throw exception;
    }
  }

  public PortalUploadProofVO uploadDueChangeProof(org.springframework.web.multipart.MultipartFile file) {
    return portalDueChangeService.uploadProof(file);
  }

  public PortalExpeditedPreviewVO previewExpedited(PortalExpeditedPreviewDTO request) {
    SysUserEntity user = ensurePortalUser(request.displayName(), request.feishuId());
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("contractNo", request.contractNo() == null ? "" : request.contractNo().replaceAll("\\s+", ""));
    Long recordId = taskDataRecordService.prepareRecord(request.recordId(), "EXPEDITED", user, payload, "");
    taskDataRecordService.startStage(recordId, "PREVIEW");
    try {
      PortalExpeditedPreviewVO preview = portalExpeditedService.preview(request.contractNo());
      taskDataRecordService.finishStageSuccess(recordId, "PREVIEW", payload, preview, null, null, false);
      return new PortalExpeditedPreviewVO(
        recordId,
        preview.contractNo(),
        preview.crmNumber(),
        preview.salesDept(),
        preview.projectName(),
        preview.kitDate(),
        preview.detailFieldsEditable(),
        preview.productTable()
      );
    } catch (RuntimeException exception) {
      taskDataRecordService.finishStageFailure(recordId, "PREVIEW", payload, null, exception.getMessage());
      throw exception;
    }
  }

  public PortalExpeditedDuplicateCheckVO checkExpeditedDuplicate(PortalExpeditedDuplicateCheckDTO request) {
    SysUserEntity user = ensurePortalUser(request.displayName(), request.feishuId());
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("contractNo", request.contractNo() == null ? "" : request.contractNo().replaceAll("\\s+", ""));
    Long recordId = taskDataRecordService.prepareRecord(request.recordId(), "EXPEDITED", user, payload, "");
    taskDataRecordService.startStage(recordId, "DUPLICATE_CHECK");
    try {
      PortalExpeditedDuplicateCheckVO result = portalExpeditedService.checkDuplicate(request.contractNo());
      taskDataRecordService.finishStageSuccess(recordId, "DUPLICATE_CHECK", payload, result, null, null, false);
      return new PortalExpeditedDuplicateCheckVO(recordId, result.duplicated(), result.duplicateCount());
    } catch (RuntimeException exception) {
      taskDataRecordService.finishStageFailure(recordId, "DUPLICATE_CHECK", payload, null, exception.getMessage());
      throw exception;
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public PortalHistoryVO submitExpedited(PortalExpeditedSubmitDTO request) {
    SysUserEntity user = ensurePortalUser(request.displayName(), request.feishuId());
    Map<String, String> payload = new java.util.LinkedHashMap<>();
    payload.put("contractNo", valueOf(Map.of("contractNo", request.contractNo()), "contractNo").replaceAll("\\s+", ""));
    payload.put("projectName", "");
    payload.put("projectUrgency", valueOf(Map.of("projectUrgency", defaultIfBlank(request.projectUrgency(), "")), "projectUrgency"));
    payload.put("latestArrivalTime", valueOf(Map.of("latestArrivalTime", defaultIfBlank(request.latestArrivalTime(), "")), "latestArrivalTime"));
    payload.put("acceptPartialShipment", valueOf(Map.of("acceptPartialShipment", defaultIfBlank(request.acceptPartialShipment(), "")), "acceptPartialShipment"));
    payload.put("mostUrgentProductList", valueOf(Map.of("mostUrgentProductList", defaultIfBlank(request.mostUrgentProductList(), "")), "mostUrgentProductList"));
    payload.put("delayedDeliveryImpact", valueOf(Map.of("delayedDeliveryImpact", defaultIfBlank(request.delayedDeliveryImpact(), "")), "delayedDeliveryImpact"));
    payload.put("selectedCount", String.valueOf(request.productArray() == null ? 0 : request.productArray().size()));
    payload.put("repeatSubmitConfirmed", Boolean.TRUE.equals(request.repeatSubmitConfirmed()) ? "是" : "否");
    String sourceText = request.sourceText() == null || request.sourceText().isBlank()
      ? buildManualSourceText("EXPEDITED", payload)
      : request.sourceText().trim();
    Long recordId = taskDataRecordService.prepareRecord(request.recordId(), "EXPEDITED", user, payload, sourceText);
    taskDataRecordService.startStage(recordId, "SUBMIT");
    try {
      List<ExpeditedTaskEntity> createdTasks = portalExpeditedService.submit(request, user.getDisplayName());
      String projectName = createdTasks.isEmpty() ? "" : defaultIfBlank(createdTasks.getFirst().getProjectName(), "");
      payload.put("projectName", projectName);
      PortalHistoryVO history = saveRecord("EXPEDITED", payload, sourceText, createdTasks, user);
      taskDataRecordService.finishStageSuccess(
        recordId,
        "SUBMIT",
        payload,
        createdTasks,
        buildExpeditedSubmitTracePayload(request),
        resolveTaskIds(createdTasks),
        true
      );
      return history;
    } catch (RuntimeException exception) {
      taskDataRecordService.finishStageFailure(recordId, "SUBMIT", payload, buildExpeditedSubmitTracePayload(request), exception.getMessage());
      throw exception;
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public AuthSessionVO buildAdminEntrySession(PortalUserContextDTO request) {
    SysUserEntity user = ensurePortalUser(request.displayName(), request.feishuId());
    return authService.issueSessionForUser(user.getId());
  }

  public List<PortalHistoryVO> listHistories(String feishuId) {
    return portalMapper.selectTaskRecordsByFeishuId(feishuId).stream().map(this::toHistoryVO).toList();
  }

  private SysUserEntity ensurePortalUser(String displayName, String feishuId) {
    if (displayName == null || displayName.isBlank() || feishuId == null || feishuId.isBlank()) {
      throw new BusinessException(400, "缺少用户身份信息");
    }

    String normalizedDisplayName = displayName.trim();
    String normalizedFeishuId = feishuId.trim();

    SysUserEntity existingByName = rbacMapper.selectUserByDisplayName(normalizedDisplayName);
    if (existingByName != null) {
      syncPortalIdentity(existingByName, normalizedDisplayName, normalizedFeishuId);
      ensureQuestionerRole(existingByName.getId());
      return rbacMapper.selectUserById(existingByName.getId());
    }

    SysUserEntity existingByFeishuId = rbacMapper.selectUserByFeishuId(normalizedFeishuId);
    if (existingByFeishuId != null) {
      syncPortalIdentity(existingByFeishuId, normalizedDisplayName, normalizedFeishuId);
      ensureQuestionerRole(existingByFeishuId.getId());
      return rbacMapper.selectUserById(existingByFeishuId.getId());
    }

    SysUserEntity entity = new SysUserEntity();
    entity.setUsername(generateAvailableUsername(normalizedDisplayName));
    entity.setPasswordHash(passwordEncoder.encode(authService.getInitialPassword()));
    entity.setDisplayName(normalizedDisplayName);
    entity.setFeishuId(normalizedFeishuId);
    entity.setUserType("PLATFORM");
    entity.setMobile("");
    entity.setEmail("");
    entity.setStatus(1);
    entity.setRemark("portal-auto-created");
    rbacMapper.insertUser(entity);
    ensureQuestionerRole(entity.getId());
    return rbacMapper.selectUserById(entity.getId());
  }

  private void syncPortalIdentity(SysUserEntity user, String displayName, String feishuId) {
    boolean changed = false;
    if (user.getMobile() == null) {
      user.setMobile("");
      changed = true;
    }
    if (user.getEmail() == null) {
      user.setEmail("");
      changed = true;
    }
    if (user.getStatus() == null) {
      user.setStatus(1);
      changed = true;
    }
    if (!displayName.equals(defaultIfBlank(user.getDisplayName(), ""))) {
      user.setDisplayName(displayName);
      changed = true;
    }
    if (!feishuId.equals(defaultIfBlank(user.getFeishuId(), ""))) {
      user.setFeishuId(feishuId);
      changed = true;
    }
    if (changed) {
      rbacMapper.updateUser(user);
    }
  }

  private void ensureQuestionerRole(Long userId) {
    SysRoleEntity role = rbacMapper.selectRoleByCode(QUESTIONER_ROLE_CODE);
    if (role == null) {
      role = rbacMapper.selectRoleByName(QUESTIONER_ROLE_NAME);
    }
    if (role == null) {
      throw new BusinessException(500, "未找到提问人角色，请先初始化 questioner 角色");
    }
    List<String> roleCodes = rbacMapper.selectRoleCodesByUserId(userId);
    if (!roleCodes.contains(role.getRoleCode())) {
      rbacMapper.insertUserRole(userId, role.getId());
    }
  }

  private UserVO toPortalUserVO(SysUserEntity user) {
    return new UserVO(user.getId(), user.getUsername(), user.getDisplayName(), user.getFeishuId(), user.getEmail(), user.getMobile(),
      user.getStatus() != null && user.getStatus() == 1 ? "enabled" : "disabled", rbacMapper.selectRoleCodesByUserId(user.getId()));
  }

  private PortalHistoryVO saveRecord(String taskType, Map<String, String> payload, String sourceText, Object createdTask, SysUserEntity user) {
    PortalTaskRecordEntity entity = new PortalTaskRecordEntity();
    entity.setTaskType(taskType);
    entity.setTaskTypeLabel(taskTypeLabel(taskType));
    entity.setTaskId(resolveTaskId(createdTask));
    entity.setRecordTitle(buildRecordTitle(taskType, payload));
    entity.setQuestionerName(user.getDisplayName());
    entity.setQuestionerFeishuId(user.getFeishuId());
    entity.setSourceText(sourceText);
    entity.setParsedPayload(writeJson(payload));
    entity.setTaskSnapshot(writeJson(createdTask));
    portalMapper.insertTaskRecord(entity);
    return toHistoryVO(entity);
  }

  private Object createTaskByType(String taskType, Map<String, String> payload, SysUserEntity user) {
    return switch (taskType) {
      case "EXPEDITED" -> taskService.createExpeditedTask(buildExpeditedTask(payload, user));
      case "ASSESSMENT" -> taskService.createDeliveryAssessmentTask(buildAssessmentTask(payload, user));
      case "DUE_CHANGE" -> taskService.createCustomerDueChangeTask(buildDueChangeTask(payload, user));
      default -> throw new BusinessException(400, "未知任务类型");
    };
  }

  private ExpeditedTaskEntity buildExpeditedTask(Map<String, String> payload, SysUserEntity user) {
    String contractNo = valueOf(payload, "contractNo");
    if (contractNo.isBlank()) {
      throw new BusinessException(400, "合同编号不能为空");
    }
    ExpeditedTaskEntity entity = new ExpeditedTaskEntity();
    entity.setContractNo(contractNo);
    entity.setProjectName(valueOf(payload, "projectName"));
    entity.setProjectUrgency(defaultIfBlank(valueOf(payload, "projectUrgency"), "一般"));
    entity.setLatestArrivalDate(valueOf(payload, "latestArrivalTime"));
    entity.setAcceptPartialDelivery("是".equals(valueOf(payload, "acceptPartialShipment")) ? 1 : 0);
    entity.setMostUrgentItems(valueOf(payload, "mostUrgentProductList"));
    entity.setLateDeliveryImpact(valueOf(payload, "delayedDeliveryImpact"));
    entity.setQuestionerName(user.getDisplayName());
    entity.setTaskEvalStatus("未评估");
    entity.setIsRepeatSubmit(0);
    entity.setIsClosedLoop(0);
    entity.setIsVirtual(0);
    return entity;
  }

  private DeliveryAssessmentTaskEntity buildAssessmentTask(Map<String, String> payload, SysUserEntity user) {
    String crmNo = valueOf(payload, "crmNo");
    String productInformation = valueOf(payload, "productInformation");
    if (crmNo.isBlank() && productInformation.isBlank()) {
      throw new BusinessException(400, "CRM 编号或产品型号数量至少填写一项");
    }
    DeliveryAssessmentTaskEntity entity = new DeliveryAssessmentTaskEntity();
    entity.setCrmNo(crmNo);
    entity.setProductModel(productInformation);
    entity.setCustomerExpectedDate(valueOf(payload, "needTime"));
    entity.setRemark(valueOf(payload, "remarks"));
    entity.setProjectName(valueOf(payload, "projectName"));
    entity.setQuestionerName(user.getDisplayName());
    entity.setTaskType(crmNo.isBlank() ? "型号交期咨询" : "CRM交期咨询");
    entity.setIsRepeatSubmit(0);
    entity.setTaskEvalStatus("待评估");
    return entity;
  }

  private CustomerDueChangeTaskEntity buildDueChangeTask(Map<String, String> payload, SysUserEntity user) {
    String contractNo = valueOf(payload, "contractNo");
    if (contractNo.isBlank()) {
      throw new BusinessException(400, "合同编号不能为空");
    }
    String questionType = defaultIfBlank(valueOf(payload, "questionType"), "客期提前");
    CustomerDueChangeTaskEntity entity = new CustomerDueChangeTaskEntity();
    entity.setContractNo(contractNo);
    entity.setProjectName(valueOf(payload, "projectName"));
    entity.setTaskType(questionType);
    if ("客期延后".equals(questionType)) {
      entity.setDelayReason(valueOf(payload, "changeReason"));
      entity.setDelayToDate(valueOf(payload, "targetDeliveryDate"));
    } else {
      entity.setAdvanceReason(valueOf(payload, "changeReason"));
      entity.setAdvanceToDate(valueOf(payload, "targetDeliveryDate"));
    }
    String partial = valueOf(payload, "allowPartialShipmentIfIncomplete");
    entity.setAgreePartialIfNotComplete(partial.isBlank() ? null : ("是".equals(partial) ? 1 : 0));
    entity.setQuestionerName(user.getDisplayName());
    entity.setTaskEvalStatus("待评估");
    entity.setIsRepeatSubmit(0);
    entity.setIsClosedLoop(0);
    return entity;
  }

  private String buildRecordTitle(String taskType, Map<String, String> payload) {
    return switch (taskType) {
      case "EXPEDITED" -> joinTitle(taskTypeLabel(taskType), valueOf(payload, "contractNo"), valueOf(payload, "projectName"));
      case "ASSESSMENT" -> !valueOf(payload, "crmNo").isBlank()
        ? joinTitle(taskTypeLabel(taskType), valueOf(payload, "crmNo"), valueOf(payload, "projectName"))
        : joinTitle(taskTypeLabel(taskType), valueOf(payload, "productInformation"));
      case "DUE_CHANGE" -> joinTitle(taskTypeLabel(taskType), valueOf(payload, "contractNo"), valueOf(payload, "projectName"));
      default -> taskTypeLabel(taskType);
    };
  }

  private Map<String, Object> buildAssessmentSubmitTracePayload(PortalAssessmentSubmitDTO request) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("recordId", request.recordId());
    payload.put("crmNo", request.crmNo());
    payload.put("productInformation", request.productInformation());
    payload.put("needTime", request.needTime());
    payload.put("remarks", request.remarks());
    payload.put("duplicateAction", request.duplicateAction());
    payload.put("allowPartialInvalidSubmit", request.allowPartialInvalidSubmit());
    payload.put("productRows", request.productRows());
    return payload;
  }

  private Map<String, Object> buildDueChangeSubmitTracePayload(PortalDueChangeSubmitDTO request) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("recordId", request.recordId());
    payload.put("questionType", request.questionType());
    payload.put("contractNo", request.contractNo());
    payload.put("projectName", request.projectName());
    payload.put("targetDeliveryDate", request.targetDeliveryDate());
    payload.put("changeReason", request.changeReason());
    payload.put("allowPartialShipmentIfIncomplete", request.allowPartialShipmentIfIncomplete());
    payload.put("delayProofUrl", request.delayProofUrl());
    payload.put("repeatSubmitConfirmed", request.repeatSubmitConfirmed());
    payload.put("productArray", request.productArray());
    return payload;
  }

  private Map<String, Object> buildExpeditedSubmitTracePayload(PortalExpeditedSubmitDTO request) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("recordId", request.recordId());
    payload.put("contractNo", request.contractNo());
    payload.put("projectUrgency", request.projectUrgency());
    payload.put("latestArrivalTime", request.latestArrivalTime());
    payload.put("acceptPartialShipment", request.acceptPartialShipment());
    payload.put("mostUrgentProductList", request.mostUrgentProductList());
    payload.put("delayedDeliveryImpact", request.delayedDeliveryImpact());
    payload.put("repeatSubmitConfirmed", request.repeatSubmitConfirmed());
    payload.put("productArray", request.productArray());
    return payload;
  }

  private List<Long> resolveTaskIds(List<?> tasks) {
    List<Long> taskIds = new ArrayList<>();
    if (tasks == null) {
      return taskIds;
    }
    for (Object task : tasks) {
      Long taskId = resolveTaskId(task);
      if (taskId != null && taskId > 0) {
        taskIds.add(taskId);
      }
    }
    return taskIds;
  }

  private String joinTitle(String... parts) {
    StringBuilder builder = new StringBuilder();
    for (String part : parts) {
      if (part == null || part.isBlank()) {
        continue;
      }
      if (!builder.isEmpty()) {
        builder.append(" | ");
      }
      builder.append(part.trim());
    }
    return builder.isEmpty() ? "任务记录" : builder.toString();
  }

  private String taskTypeLabel(String taskType) {
    return switch (taskType) {
      case "EXPEDITED" -> "订单加急任务";
      case "ASSESSMENT" -> "未下单咨询任务";
      case "DUE_CHANGE" -> "客期变更任务";
      default -> "任务记录";
    };
  }

  private Long resolveTaskId(Object task) {
    if (task instanceof List<?> list && !list.isEmpty()) {
      return resolveTaskId(list.getFirst());
    }
    if (task instanceof ExpeditedTaskEntity entity) {
      return entity.getId();
    }
    if (task instanceof DeliveryAssessmentTaskEntity entity) {
      return entity.getId();
    }
    if (task instanceof CustomerDueChangeTaskEntity entity) {
      return entity.getId();
    }
    return 0L;
  }

  private PortalHistoryVO toHistoryVO(PortalTaskRecordEntity entity) {
    return new PortalHistoryVO(entity.getId(), entity.getTaskType(), entity.getTaskTypeLabel(), entity.getTaskId(),
      entity.getRecordTitle(), entity.getSourceText(), entity.getParsedPayload(), entity.getTaskSnapshot(), entity.getCreatedAt());
  }

  private String buildManualSourceText(String taskType, Map<String, String> payload) {
    return taskTypeLabel(taskType) + " 手动发起\n" + payload.entrySet().stream()
      .map(entry -> entry.getKey() + ": " + valueOf(payload, entry.getKey()))
      .reduce((left, right) -> left + "\n" + right)
      .orElse("");
  }

  private String normalizeTaskType(String taskType) {
    if (taskType == null) {
      throw new BusinessException(400, "任务类型不能为空");
    }
    return switch (taskType.trim().toLowerCase(Locale.ROOT)) {
      case "expedited", "order-expedited", "订单加急任务" -> "EXPEDITED";
      case "assessment", "preorder-assessment", "未下单咨询任务" -> "ASSESSMENT";
      case "due-change", "customer-due-change", "客期变更任务" -> "DUE_CHANGE";
      default -> throw new BusinessException(400, "不支持的任务类型");
    };
  }

  private String generateAvailableUsername(String displayName) {
    String base = toPinyinInitials(displayName);
    if (base.isBlank()) {
      base = "user";
    }
    if (rbacMapper.selectUserByUsername(base) == null) {
      return base;
    }
    for (int index = 1; index <= 99; index += 1) {
      String candidate = base + String.format("%02d", index);
      if (rbacMapper.selectUserByUsername(candidate) == null) {
        return candidate;
      }
    }
    return base + System.currentTimeMillis() % 1000;
  }

  private String toPinyinInitials(String displayName) {
    HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
    format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
    format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    StringBuilder builder = new StringBuilder();
    for (char current : displayName.trim().toCharArray()) {
      if (Character.isWhitespace(current)) {
        continue;
      }
      if (current < 128 && Character.isLetterOrDigit(current)) {
        builder.append(Character.toLowerCase(current));
        continue;
      }
      try {
        String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(current, format);
        if (pinyin != null && pinyin.length > 0 && !pinyin[0].isBlank()) {
          builder.append(Character.toLowerCase(pinyin[0].charAt(0)));
        }
      } catch (Exception ignored) {
        // ignored
      }
    }
    String result = builder.toString().replaceAll("[^a-z0-9]", "");
    return result.isBlank() ? "user" : result;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "JSON 序列化失败: " + exception.getMessage());
    }
  }

  private String valueOf(Map<String, String> payload, String key) {
    return payload == null ? "" : payload.getOrDefault(key, "").trim();
  }

  private String defaultIfBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private Map<String, String> sanitizeAiPayload(String taskType, Map<String, String> payload, String sourceText) {
    Map<String, String> sanitized = new LinkedHashMap<>(payload == null ? Map.of() : payload);
    if (!containsExplicitDateExpression(sourceText)) {
      if ("EXPEDITED".equals(taskType)) {
        sanitized.put("latestArrivalTime", "");
      }
      if ("DUE_CHANGE".equals(taskType)) {
        sanitized.put("targetDeliveryDate", "");
      }
    }
    return sanitized;
  }

  private boolean containsExplicitDateExpression(String content) {
    if (content == null || content.isBlank()) {
      return false;
    }
    return Pattern.compile("\\d{4}[年./-]\\d{1,2}[月./-]\\d{1,2}|\\d{1,2}月\\d{1,2}[日号]?|\\d{4}-\\d{1,2}-\\d{1,2}")
      .matcher(content)
      .find();
  }
}